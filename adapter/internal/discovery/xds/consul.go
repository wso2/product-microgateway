/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package xds

import (
	"fmt"
	"reflect"
	"sync"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/svcdiscovery"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"google.golang.org/protobuf/types/known/anypb"
)

var (
	onceUpdateMeshCerts sync.Once
)

const (
	transportSocketName = "envoy.transport_sockets.tls"
)

func startConsulServiceDiscovery(organizationID string) {
	for apiKey, clusterList := range orgIDOpenAPIClustersMap[organizationID] {
		for _, cluster := range clusterList {
			if consulSyntax, ok := svcdiscovery.ClusterConsulKeyMap[cluster.Name]; ok {
				svcdiscovery.InitConsul() //initialize consul client and load certs
				onceUpdateMeshCerts.Do(func() {
					if svcdiscovery.MeshEnabled {
						go listenForMeshCertUpdates(organizationID) //runs in background
					}
				})
				query, errConSyn := svcdiscovery.ParseQueryString(consulSyntax)
				if errConSyn != nil {
					logger.LoggerXds.ErrorC(logging.ErrorDetails{
						Message:   fmt.Sprintf("Consul syntax parse error %v", errConSyn.Error()),
						Severity:  logging.CRITICAL,
						ErrorCode: 1402,
					})
					return
				}
				logger.LoggerXds.Debugln("consul query values: ", query)
				go getServiceDiscoveryData(query, cluster.Name, apiKey, organizationID)
			}
		}
	}

}

func listenForMeshCertUpdates(organizationID string) {
	for {
		select {
		case <-svcdiscovery.MeshUpdateSignal:
			updateCertsForServiceMesh(organizationID)
		}
	}
}

func updateCertsForServiceMesh(organizationID string) {
	//update each cluster with new certs
	for _, clusters := range orgIDOpenAPIClustersMap[organizationID] {
		for _, cluster := range clusters { //iterate through all clusters

			if svcdiscovery.MeshCACert == "" || svcdiscovery.MeshServiceKey == "" || svcdiscovery.MeshServiceCert == "" {
				logger.LoggerXds.Warn("Mesh certs are empty")
				return
			}
			upstreamTLSContext := svcdiscovery.CreateUpstreamTLSContext(svcdiscovery.MeshCACert,
				svcdiscovery.MeshServiceKey, svcdiscovery.MeshServiceCert)

			marshalledTLSContext, err := anypb.New(upstreamTLSContext)
			if err != nil {
				logger.LoggerXds.ErrorC(logging.ErrorDetails{
					Message:   fmt.Sprintf("Internal Error while marshalling the upstream TLS Context. %v", err.Error()),
					Severity:  logging.CRITICAL,
					ErrorCode: 1403,
				})
			} else {
				//envoy config
				upstreamTransportSocket := &corev3.TransportSocket{
					Name: transportSocketName,
					ConfigType: &corev3.TransportSocket_TypedConfig{
						TypedConfig: marshalledTLSContext,
					},
				}
				cluster.TransportSocket = upstreamTransportSocket
			}

		}
	}

	//send the update to Router
	for apiKey := range orgIDOpenAPIClustersMap[organizationID] {
		updateXDSClusterCache(apiKey, organizationID)
	}
}

func getServiceDiscoveryData(query svcdiscovery.Query, clusterName string, apiKey string, organizationID string) {
	doneChan := make(chan bool)
	svcdiscovery.ClusterConsulDoneChanMap[clusterName] = doneChan
	resultChan := svcdiscovery.ConsulClientInstance.Poll(query, doneChan)
	for {
		select {
		case queryResultsList, ok := <-resultChan:
			if !ok { //ok==false --> result chan is closed
				logger.LoggerXds.Debugln("closed the result channel for cluster name: ", clusterName)
				return
			}
			//stop the process when API is deleted
			if _, clusterExists := orgIDOpenAPIClustersMap[organizationID][apiKey]; !clusterExists {
				logger.LoggerXds.Debugln("Consul service discovery stopped for cluster ", clusterName, " in API ",
					apiKey, " upon API removal")
				stopConsulDiscoveryFor(clusterName)
				return
			}
			val := svcdiscovery.GetClusterConsulResultMap(clusterName)
			if val != nil {
				if !reflect.DeepEqual(val, queryResultsList) {
					svcdiscovery.SetClusterConsulResultMap(clusterName, queryResultsList)
					//update the envoy cluster
					updateCluster(apiKey, clusterName, organizationID, queryResultsList)
				}
			} else {
				logger.LoggerXds.Debugln("updating cluster from the consul service registry, removed the default host")
				svcdiscovery.SetClusterConsulResultMap(clusterName, queryResultsList)
				updateCluster(apiKey, clusterName, organizationID, queryResultsList)
			}

			if svcdiscovery.MeshEnabled {
				updateCertsForServiceMesh(organizationID)
			}
		}
	}
}

func updateCluster(apiKey string, clusterName string, organizationID string, queryResultsList []svcdiscovery.Upstream) {
	if clusterList, available := orgIDOpenAPIClustersMap[organizationID][apiKey]; available {
		for i := range clusterList {
			if clusterList[i].Name == clusterName {
				var lbEndpointList []*endpointv3.LbEndpoint
				for _, result := range queryResultsList {
					address := &corev3.Address{Address: &corev3.Address_SocketAddress{
						SocketAddress: &corev3.SocketAddress{
							Address:  result.Address,
							Protocol: corev3.SocketAddress_TCP,
							PortSpecifier: &corev3.SocketAddress_PortValue{
								PortValue: uint32(result.ServicePort),
							},
						},
					}}

					lbEndPoint := &endpointv3.LbEndpoint{
						HostIdentifier: &endpointv3.LbEndpoint_Endpoint{
							Endpoint: &endpointv3.Endpoint{
								Address: address,
							},
						},
					}
					lbEndpointList = append(lbEndpointList, lbEndPoint)
				}
				clusterList[i].LoadAssignment = &endpointv3.ClusterLoadAssignment{
					ClusterName: clusterName,
					Endpoints: []*endpointv3.LocalityLbEndpoints{
						{
							LbEndpoints: lbEndpointList,
						},
					},
				}
				updateXDSClusterCache(apiKey, organizationID)
			}
		}
	}
}

func updateXDSClusterCache(apiKey string, organizationID string) {
	for key, envoyLabelList := range orgIDOpenAPIEnvoyMap[organizationID] {
		if key == apiKey {
			for _, label := range envoyLabelList {
				listeners, clusters, routes, endpoints, _ := GenerateEnvoyResoucesForLabel(label)
				UpdateXdsCacheWithLock(label, endpoints, clusters, routes, listeners)
				logger.LoggerXds.Info("Updated XDS cache by consul service discovery for API: ", apiKey)
			}
		}
	}
}

func stopConsulDiscoveryFor(clusterName string) {
	if doneChan, available := svcdiscovery.ClusterConsulDoneChanMap[clusterName]; available {
		close(doneChan)
	}
	delete(svcdiscovery.ClusterConsulResultMap, clusterName)
	delete(svcdiscovery.ClusterConsulKeyMap, clusterName)
}
