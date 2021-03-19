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
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/micro-gw/internal/svcdiscovery"
	logger "github.com/wso2/micro-gw/loggers"
	"log"
	"reflect"
	"sync"
)

var (
	onceUpdateMeshCerts sync.Once
)

const (
	transportSocketName = "envoy.transport_sockets.tls"
)

func startConsulServiceDiscovery() {
	for apiKey, clusterList := range openAPIClustersMap {
		for _, cluster := range clusterList {
			if consulSyntax, ok := svcdiscovery.ClusterConsulKeyMap[cluster.Name]; ok {
				svcdiscovery.InitConsul() //initialize consul client and load certs
				onceUpdateMeshCerts.Do(func() {
					if svcdiscovery.MeshEnabled {
						go listenForMeshCertUpdates() //runs in background
					}
				})
				query, errConSyn := svcdiscovery.ParseQueryString(consulSyntax)
				log.Println("consul syntax: ", consulSyntax)
				if errConSyn != nil {
					logger.LoggerXds.Error("consul syntax parse error ", errConSyn)
					return
				}
				logger.LoggerXds.Debugln(query)
				go getServiceDiscoveryData(query, cluster.Name, apiKey)
			}
		}
	}
}

func listenForMeshCertUpdates() {
	for {
		select {
		case <-svcdiscovery.MeshUpdateSignal:
			updateCertsForServiceMesh()
		}
	}
}

func updateCertsForServiceMesh() {
	//update each cluster with new certs
	for _, clusters := range openAPIClustersMap {
		for _, cluster := range clusters { //iterate through all clusters
			if cluster.TransportSocket != nil { //has transport socket==> https/wss
				logger.LoggerXds.Println(svcdiscovery.MeshCACert, "svcdiscovery.MeshCACert")
				if svcdiscovery.MeshCACert == "" || svcdiscovery.MeshServiceKey == "" || svcdiscovery.MeshServiceCert == "" {
					logger.LoggerXds.Warn("Mesh certs are empty")
					return
				}
				upstreamTLSContext := svcdiscovery.CreateUpstreamTLSContext(svcdiscovery.MeshCACert,
					svcdiscovery.MeshServiceKey, svcdiscovery.MeshServiceCert)
				marshalledTLSContext, err := ptypes.MarshalAny(upstreamTLSContext)
				if err != nil {
					logger.LoggerXds.Error("Internal Error while marshalling the upstream TLS Context.")
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
	}

	//send the update to Router
	for apiKey := range openAPIClustersMap {
		updateXDSRouteCacheForServiceDiscovery(apiKey)
	}
}

func getServiceDiscoveryData(query svcdiscovery.Query, clusterName string, apiKey string) {
	logger.LoggerXds.Println("get service discovery data")
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
			if _, clusterExists := openAPIClustersMap[apiKey]; !clusterExists {
				logger.LoggerXds.Debugln("Consul service discovery stopped for cluster ", clusterName, " in API ",
					apiKey, " upon API removal")
				stopConsulDiscoveryFor(clusterName)
				return
			}
			logger.LoggerXds.Println("Results: ", queryResultsList)
			val := svcdiscovery.GetClusterConsulResultMap(clusterName)
			if val != nil {
				if !reflect.DeepEqual(val, queryResultsList) {
					svcdiscovery.SetClusterConsulResultMap(clusterName, queryResultsList)
					//update the envoy cluster
					updateRoute(apiKey, clusterName, queryResultsList)
				}
			} else {
				logger.LoggerXds.Debugln("updating cluster from the consul service registry, removed the default host")
				svcdiscovery.SetClusterConsulResultMap(clusterName, queryResultsList)
				updateRoute(apiKey, clusterName, queryResultsList)
			}
		}
	}
}

func updateRoute(apiKey string, clusterName string, queryResultsList []svcdiscovery.Upstream) {
	if clusterList, available := openAPIClustersMap[apiKey]; available {
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
				updateXDSRouteCacheForServiceDiscovery(apiKey)
			}
		}
	}
}

func updateXDSRouteCacheForServiceDiscovery(apiKey string) {
	for key, envoyLabelList := range openAPIEnvoyMap {
		if key == apiKey {
			for _, label := range envoyLabelList {
				listeners, clusters, routes, endpoints, _ := generateEnvoyResoucesForLabel(label)
				updateXdsCacheWithLock(label, endpoints, clusters, routes, listeners)
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
