/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package envoyconf

import (
	"errors"
	"net"
	"regexp"
	"strconv"

	"github.com/wso2/product-microgateway/adapter/internal/interceptor"
	"google.golang.org/protobuf/types/known/durationpb"
	"google.golang.org/protobuf/types/known/structpb"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	lua "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/lua/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/any"
	"github.com/golang/protobuf/ptypes/wrappers"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"google.golang.org/protobuf/types/known/wrapperspb"

	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/svcdiscovery"

	"strings"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
)

// CreateRoutesWithClusters creates envoy routes along with clusters and endpoint instances.
// This creates routes for all the swagger resources and link to clusters.
// Create clusters for api level production and sandbox endpoints.
// If a resource has resource level endpoint, it create another cluster and
// link it. If resources doesn't has resource level endpoints, those clusters are linked
// to the api level clusters.
//
// First set of routes, clusters, addresses represents the production endpoints related
// configurations. Next set represents the sandbox endpoints related configurations.
func CreateRoutesWithClusters(mgwSwagger model.MgwSwagger, upstreamCerts map[string][]byte, interceptorCerts map[string][]byte, vHost string, organizationID string) (routesP []*routev3.Route,
	clustersP []*clusterv3.Cluster, addressesP []*corev3.Address) {
	var (
		routes    []*routev3.Route
		clusters  []*clusterv3.Cluster
		endpoints []*corev3.Address

		apiRequestInterceptor  model.InterceptEndpoint
		apiResponseInterceptor model.InterceptEndpoint
	)

	apiTitle := mgwSwagger.GetTitle()
	apiVersion := mgwSwagger.GetVersion()

	conf, _ := config.ReadConfigs()
	timeout := conf.Envoy.ClusterTimeoutInSeconds

	// The any upstream endpoint's basepath.
	// The developer has to stick into the same basePath when adding any endpoint to an api.
	apiLevelbasePath := ""
	// Restricting all endpoints belongs to same api to have same basepath when x-wso2-endpoints referencing is enabled
	strictBasePath := false

	apiLevelClusterNameProd := ""
	apiLevelClusterNameSand := ""

	// check API level production endpoints available
	if mgwSwagger.GetProdEndpoints() != nil && len(mgwSwagger.GetProdEndpoints().Endpoints) > 0 {
		apiLevelEndpointProd := mgwSwagger.GetProdEndpoints()
		apiLevelbasePath = strings.TrimSuffix(apiLevelEndpointProd.Endpoints[0].Basepath, "/")
		apiLevelClusterNameProd = getClusterName(apiLevelEndpointProd.EndpointPrefix, organizationID, vHost, apiTitle,
			apiVersion, "")
		if !strings.Contains(apiLevelEndpointProd.EndpointPrefix, xWso2EPClustersConfigNamePrefix) {
			cluster, address, err := processEndpoints(apiLevelClusterNameProd, apiLevelEndpointProd,
				upstreamCerts, timeout, apiLevelbasePath)
			if err != nil {
				apiLevelClusterNameProd = ""
				logger.LoggerOasparser.Errorf("Error while adding api level production endpoints for %s. %v", apiTitle, err.Error())
			} else {
				clusters = append(clusters, cluster)
				endpoints = append(endpoints, address...)
			}
		}
	} else {
		logger.LoggerOasparser.Warnf("API level Production endpoints are not defined for %v : %v", apiTitle, apiVersion)
	}
	// check API level sandbox endpoints available
	if mgwSwagger.GetSandEndpoints() != nil && len(mgwSwagger.GetSandEndpoints().Endpoints) > 0 {
		apiLevelEndpointSand := mgwSwagger.GetSandEndpoints()
		if apiLevelbasePath == "" {
			apiLevelbasePath = strings.TrimSuffix(apiLevelEndpointSand.Endpoints[0].Basepath, "/")
		}
		apiLevelClusterNameSand = getClusterName(apiLevelEndpointSand.EndpointPrefix, organizationID, vHost,
			apiTitle, apiVersion, "")
		if !strings.Contains(apiLevelEndpointSand.EndpointPrefix, xWso2EPClustersConfigNamePrefix) {
			cluster, address, err := processEndpoints(apiLevelClusterNameSand, apiLevelEndpointSand,
				upstreamCerts, timeout, apiLevelbasePath)
			if err != nil {
				apiLevelClusterNameSand = ""
				logger.LoggerOasparser.Errorf("Error while adding api level sandbox endpoints for %s. %v", apiTitle, err.Error())
			} else {
				clusters = append(clusters, cluster)
				endpoints = append(endpoints, address...)
			}
		}
	} else {
		logger.LoggerOasparser.Debugf("API level Sandbox endpoints are not defined for %s", apiTitle)
	}
	// check if x-wso2-endpoints are available
	xWso2Endpoints := mgwSwagger.GetXWso2Endpoints()
	if len(xWso2Endpoints) > 0 {
		logger.LoggerOasparser.Debugf("x-wso2-endpoints clusters found for %v : %v", apiTitle, apiVersion)
		for epName, endpointCluster := range xWso2Endpoints {
			if apiLevelbasePath == "" {
				apiLevelbasePath = strings.TrimSuffix(endpointCluster.Endpoints[0].Basepath, "/")
			}
			epClusterName := getClusterName(endpointCluster.EndpointPrefix, organizationID, vHost, apiTitle,
				apiVersion, "")
			cluster, addresses, err := processEndpoints(epClusterName, endpointCluster, upstreamCerts, timeout, apiLevelbasePath)
			if err != nil {
				logger.LoggerOasparser.Errorf("Error while adding x-wso2-endpoints cluster %v for %s. %v ", epName, apiTitle, err.Error())
			} else {
				strictBasePath = true
				clusters = append(clusters, cluster)
				endpoints = append(endpoints, addresses...)
			}
		}
	}

	var interceptorErr error
	apiRequestInterceptor, interceptorErr = mgwSwagger.GetInterceptor(mgwSwagger.GetVendorExtensions(), xWso2requestInterceptor)
	// if lua filter exists on api level, add cluster
	if interceptorErr == nil && apiRequestInterceptor.Enable {
		logger.LoggerOasparser.Debugf("API level request interceptors found for %v : %v", apiTitle, apiVersion)
		apiRequestInterceptor.ClusterName = getClusterName(requestInterceptClustersNamePrefix, organizationID, vHost,
			apiTitle, apiVersion, "")
		cluster, addresses, err := CreateLuaCluster(interceptorCerts, apiRequestInterceptor)
		if err != nil {
			apiRequestInterceptor = model.InterceptEndpoint{}
			logger.LoggerOasparser.Errorf("Error while adding api level request intercepter external cluster for %s. %v",
				apiTitle, err.Error())
		} else {
			clusters = append(clusters, cluster)
			endpoints = append(endpoints, addresses...)
		}
	}
	apiResponseInterceptor, interceptorErr = mgwSwagger.GetInterceptor(mgwSwagger.GetVendorExtensions(), xWso2responseInterceptor)
	// if lua filter exists on api level, add cluster
	if interceptorErr == nil && apiResponseInterceptor.Enable {
		logger.LoggerOasparser.Debugln("API level response interceptors found for " + mgwSwagger.GetID())
		apiResponseInterceptor.ClusterName = getClusterName(responseInterceptClustersNamePrefix, organizationID, vHost,
			apiTitle, apiVersion, "")
		cluster, addresses, err := CreateLuaCluster(interceptorCerts, apiResponseInterceptor)
		if err != nil {
			apiResponseInterceptor = model.InterceptEndpoint{}
			logger.LoggerOasparser.Errorf("Error while adding api level response intercepter external cluster for %s. %v", apiTitle, err.Error())
		} else {
			clusters = append(clusters, cluster)
			endpoints = append(endpoints, addresses...)
		}
	}

	if mgwSwagger.GetAPIType() != model.WS {
		for _, resource := range mgwSwagger.GetResources() {
			resourceRequestInterceptor := apiRequestInterceptor
			resourceResponseInterceptor := apiResponseInterceptor
			clusterNameProd := apiLevelClusterNameProd
			clusterNameSand := apiLevelClusterNameSand
			resourceBasePath := ""
			resourcePath := resource.GetPath()
			if strictBasePath || ((resource.GetProdEndpoints() == nil || len(resource.GetProdEndpoints().Endpoints) < 1) &&
				(resource.GetSandEndpoints() == nil || len(resource.GetSandEndpoints().Endpoints) < 1)) {
				resourceBasePath = apiLevelbasePath
			}

			// resource level check production endpoints
			if resource.GetProdEndpoints() != nil && len(resource.GetProdEndpoints().Endpoints) > 0 {
				prevResourceBasePath := resourceBasePath
				endpointProd := resource.GetProdEndpoints()
				if resourceBasePath == "" {
					resourceBasePath = strings.TrimSuffix(endpointProd.Endpoints[0].Basepath, "/")
				}
				clusterNameProd = getClusterName(endpointProd.EndpointPrefix, organizationID, vHost,
					mgwSwagger.GetTitle(), apiVersion, "")
				if !strings.Contains(endpointProd.EndpointPrefix, xWso2EPClustersConfigNamePrefix) {
					clusterNameProd = getClusterName(endpointProd.EndpointPrefix, organizationID, vHost,
						mgwSwagger.GetTitle(), apiVersion, resource.GetID())
					clusterProd, addressProd, err := processEndpoints(clusterNameProd, endpointProd, upstreamCerts, timeout, resourceBasePath)
					if err != nil {
						clusterNameProd = apiLevelClusterNameProd
						// reverting resource base path setting as production cluster creation has failed
						resourceBasePath = prevResourceBasePath
						logger.LoggerOasparser.Errorf("Error while adding resource level production endpoints for %s:%v-%v. %v",
							apiTitle, apiVersion, resourcePath, err.Error())
					} else {
						clusters = append(clusters, clusterProd)
						endpoints = append(endpoints, addressProd...)
					}
				}
			}
			if clusterNameProd == "" {
				logger.LoggerOasparser.Warnf("Production environment endpoints are not available for the resource %v:%v-%v",
					apiTitle, apiVersion, resourcePath)
			}

			// resource level check sandbox endpoints
			if resource.GetSandEndpoints() != nil && len(resource.GetSandEndpoints().Endpoints) > 0 {
				prevResourceBasePath := resourceBasePath
				endpointSand := resource.GetSandEndpoints()
				if resourceBasePath == "" {
					resourceBasePath = strings.TrimSuffix(endpointSand.Endpoints[0].Basepath, "/")
				}
				clusterNameSand = getClusterName(endpointSand.EndpointPrefix, organizationID, vHost, apiTitle,
					apiVersion, "")
				if !strings.Contains(endpointSand.EndpointPrefix, xWso2EPClustersConfigNamePrefix) {
					clusterNameSand = getClusterName(endpointSand.EndpointPrefix, organizationID, vHost, apiTitle,
						apiVersion, resource.GetID())
					clusterSand, addressSand, err := processEndpoints(clusterNameSand, endpointSand, upstreamCerts, timeout, resourceBasePath)
					if err != nil {
						clusterNameSand = apiLevelClusterNameSand
						// reverting resource base path setting as sandbox cluster creation has failed
						resourceBasePath = prevResourceBasePath
						logger.LoggerOasparser.Errorf("Error while adding resource level sandbox endpoints for %s:%v-%v. %v",
							apiTitle, apiVersion, resourcePath, err.Error())
					} else {
						clusters = append(clusters, clusterSand)
						endpoints = append(endpoints, addressSand...)
					}
				}
			}
			if clusterNameSand == "" {
				logger.LoggerOasparser.Debugf("Sandbox environment endpoints are not available for the resource %v:%v-%v",
					apiTitle, apiVersion, resourcePath)
			}

			// if both resource level sandbox and production are same as api level, api level clusters will be applied with the api level basepath
			if clusterNameProd == apiLevelClusterNameProd && clusterNameSand == apiLevelClusterNameSand {
				resourceBasePath = apiLevelbasePath
			}
			if clusterNameProd != "" && clusterNameProd == apiLevelClusterNameProd && resourceBasePath != apiLevelbasePath {
				logger.LoggerOasparser.Errorf("Error while adding resource level production endpoints for %s:%v-%v. sandbox endpoint basepath : %v and production basepath : %v mismatched",
					apiTitle, apiVersion, resourcePath, resourceBasePath, apiLevelbasePath)
				clusterNameProd = ""
			} else if clusterNameSand != "" && clusterNameSand == apiLevelClusterNameSand && resourceBasePath != apiLevelbasePath {
				logger.LoggerOasparser.Errorf("Error while adding resource level sandbox endpoints for %s:%v-%v. production endpoint basepath : %v and sandbox basepath : %v mismatched",
					apiTitle, apiVersion, resourcePath, resourceBasePath, apiLevelbasePath)
				clusterNameSand = ""
			}

			reqInterceptorVal, err := mgwSwagger.GetInterceptor(resource.GetVendorExtensions(), xWso2requestInterceptor)
			if err == nil && reqInterceptorVal.Enable {
				logger.LoggerOasparser.Debugf("Resource level request interceptors found for %v:%v-%v", apiTitle, apiVersion, resource.GetPath())
				reqInterceptorVal.ClusterName = getClusterName(requestInterceptClustersNamePrefix, organizationID, vHost,
					apiTitle, apiVersion, resource.GetID())
				cluster, addresses, err := CreateLuaCluster(interceptorCerts, reqInterceptorVal)
				if err != nil {
					logger.LoggerOasparser.Errorf("Error while adding resource level request intercept external cluster for %s. %v",
						apiTitle, err.Error())
				} else {
					resourceRequestInterceptor = reqInterceptorVal
					clusters = append(clusters, cluster)
					endpoints = append(endpoints, addresses...)
				}
			}
			respInterceptorVal, err := mgwSwagger.GetInterceptor(resource.GetVendorExtensions(), xWso2responseInterceptor)
			if err == nil && respInterceptorVal.Enable {
				logger.LoggerOasparser.Debugf("Resource level response interceptors found for %v:%v-%v"+apiTitle, apiVersion, resource.GetPath())
				respInterceptorVal.ClusterName = getClusterName(responseInterceptClustersNamePrefix, organizationID,
					vHost, apiTitle, apiVersion, resource.GetID())
				cluster, addresses, err := CreateLuaCluster(interceptorCerts, respInterceptorVal)
				if err != nil {
					logger.LoggerOasparser.Errorf("Error while adding resource level response intercept external cluster for %s. %v",
						apiTitle, err.Error())
				} else {
					resourceResponseInterceptor = respInterceptorVal
					clusters = append(clusters, cluster)
					endpoints = append(endpoints, addresses...)
				}
			}

			routeP := createRoute(genRouteCreateParams(&mgwSwagger, resource, vHost, resourceBasePath, clusterNameProd,
				clusterNameSand, resourceRequestInterceptor, resourceResponseInterceptor, organizationID))
			routes = append(routes, routeP)
		}
	}
	if mgwSwagger.GetAPIType() == model.WS {
		for _, resource := range mgwSwagger.GetResources() {
			routesP := createRoute(genRouteCreateParams(&mgwSwagger, resource, vHost, apiLevelbasePath, apiLevelClusterNameProd,
				apiLevelClusterNameSand, apiRequestInterceptor, apiResponseInterceptor, organizationID))
			routes = append(routes, routesP)
		}
	}
	return routes, clusters, endpoints
}

func getClusterName(epPrefix string, organizationID string, vHost string, swaggerTitle string, swaggerVersion string,
	resourceID string) string {
	if resourceID != "" {
		return strings.TrimSpace(organizationID+"_"+epPrefix+"_"+vHost+"_"+strings.Replace(swaggerTitle, " ", "", -1)+swaggerVersion) +
			"_" + strings.Replace(resourceID, " ", "", -1) + "0"
	}
	return strings.TrimSpace(organizationID + "_" + epPrefix + "_" + vHost + "_" + strings.Replace(swaggerTitle, " ", "", -1) +
		swaggerVersion)
}

// CreateLuaCluster creates lua cluster configuration.
func CreateLuaCluster(interceptorCerts map[string][]byte, endpoint model.InterceptEndpoint) (*clusterv3.Cluster, []*corev3.Address, error) {
	logger.LoggerOasparser.Debug("creating a lua cluster ", endpoint.ClusterName)
	return processEndpoints(endpoint.ClusterName, &endpoint.EndpointCluster, interceptorCerts, endpoint.ClusterTimeout, endpoint.EndpointCluster.Endpoints[0].Basepath)
}

// CreateTracingCluster creates a cluster definition for router's tracing server.
func CreateTracingCluster(conf *config.Config) (*clusterv3.Cluster, []*corev3.Address, error) {
	var epHost string
	var epPort uint32
	var epPath string
	epTimeout := conf.Envoy.ClusterTimeoutInSeconds
	epCluster := &model.EndpointCluster{
		Endpoints: []model.Endpoint{
			{
				Host:    "",
				URLType: "http",
				Port:    uint32(9411),
			},
		},
	}

	if epHost = conf.Tracing.ConfigProperties[tracerHost]; len(epHost) <= 0 {
		return nil, nil, errors.New("Invalid host provided for tracing endpoint")
	}
	if epPath = conf.Tracing.ConfigProperties[tracerEndpoint]; len(epPath) <= 0 {
		return nil, nil, errors.New("Invalid endpoint path provided for tracing endpoint")
	}
	if port, err := strconv.ParseUint(conf.Tracing.ConfigProperties[tracerPort], 10, 32); err == nil {
		epPort = uint32(port)
	} else {
		return nil, nil, errors.New("Invalid port provided for tracing endpoint")
	}

	epCluster.Endpoints[0].Host = epHost
	epCluster.Endpoints[0].Port = epPort
	epCluster.Endpoints[0].Basepath = epPath

	return processEndpoints(tracingClusterName, epCluster, nil, epTimeout, epPath)
}

// processEndpoints creates cluster configuration. AddressConfiguration, cluster name and
// urlType (http or https) is required to be provided.
// timeout cluster timeout
func processEndpoints(clusterName string, clusterDetails *model.EndpointCluster, upstreamCerts map[string][]byte,
	timeout time.Duration, basePath string) (*clusterv3.Cluster, []*corev3.Address, error) {
	// tls configs
	var transportSocketMatches []*clusterv3.Cluster_TransportSocketMatch
	// create loadbalanced/failover endpoints
	var lbEPs []*endpointv3.LocalityLbEndpoints
	// failover priority
	priority := 0
	// epType {loadbalance, failover}
	epType := clusterDetails.EndpointType

	addresses := []*corev3.Address{}

	for i, ep := range clusterDetails.Endpoints {
		// validating the basepath to be same for all upstreams of an api
		if strings.TrimSuffix(ep.Basepath, "/") != basePath {
			return nil, nil, errors.New("endpoint basepath mismatched for " + ep.RawURL + ". expected : " + basePath + " but found : " + ep.Basepath)
		}
		// create addresses for endpoints
		address := createAddress(ep.Host, ep.Port)
		addresses = append(addresses, address)

		// create loadbalance / failover endpoints
		localityLbEndpoints := &endpointv3.LocalityLbEndpoints{
			Priority: uint32(priority),
			LbEndpoints: []*endpointv3.LbEndpoint{
				{
					HostIdentifier: &endpointv3.LbEndpoint_Endpoint{
						Endpoint: &endpointv3.Endpoint{
							Address: address,
						},
					},
				},
			},
		}

		// create tls configs
		if strings.HasPrefix(ep.URLType, httpsURLType) || strings.HasPrefix(ep.URLType, wssURLType) {
			var epCert []byte
			if cert, found := upstreamCerts[ep.RawURL]; found {
				epCert = cert
			} else if defaultCerts, found := upstreamCerts["default"]; found {
				epCert = defaultCerts
			}

			upstreamtlsContext := createUpstreamTLSContext(epCert, address)
			marshalledTLSContext, err := ptypes.MarshalAny(upstreamtlsContext)
			if err != nil {
				return nil, nil, errors.New("internal Error while marshalling the upstream TLS Context")
			}
			transportSocketMatch := &clusterv3.Cluster_TransportSocketMatch{
				Name: "ts" + strconv.Itoa(i),
				Match: &structpb.Struct{
					Fields: map[string]*structpb.Value{
						"lb_id": structpb.NewStringValue(strconv.Itoa(i)),
					},
				},
				TransportSocket: &corev3.TransportSocket{
					Name: transportSocketName,
					ConfigType: &corev3.TransportSocket_TypedConfig{
						TypedConfig: marshalledTLSContext,
					},
				},
			}
			transportSocketMatches = append(transportSocketMatches, transportSocketMatch)
			localityLbEndpoints.LbEndpoints[0].Metadata = &corev3.Metadata{
				FilterMetadata: map[string]*structpb.Struct{
					"envoy.transport_socket_match": {
						Fields: map[string]*structpb.Value{
							"lb_id": structpb.NewStringValue(strconv.Itoa(i)),
						},
					},
				},
			}
		}
		lbEPs = append(lbEPs, localityLbEndpoints)

		// set priority for next endpoint
		if strings.HasPrefix(epType, "failover") {
			priority = priority + 1
		}
	}

	cluster := clusterv3.Cluster{
		Name:                 clusterName,
		ConnectTimeout:       ptypes.DurationProto(timeout * time.Second),
		ClusterDiscoveryType: &clusterv3.Cluster_Type{Type: clusterv3.Cluster_STRICT_DNS},
		DnsLookupFamily:      clusterv3.Cluster_V4_ONLY,
		LbPolicy:             clusterv3.Cluster_ROUND_ROBIN,
		LoadAssignment: &endpointv3.ClusterLoadAssignment{
			ClusterName: clusterName,
			Endpoints:   lbEPs,
		},
		// adding health check for cluster endpoints
		HealthChecks:           createHealthCheck(),
		TransportSocketMatches: transportSocketMatches,
	}

	if clusterDetails.Config != nil && clusterDetails.Config.CircuitBreakers != nil {
		config := clusterDetails.Config.CircuitBreakers
		thresholds := &clusterv3.CircuitBreakers_Thresholds{}
		if config.MaxConnections > 0 {
			thresholds.MaxConnections = wrapperspb.UInt32(uint32(config.MaxConnections))
		}
		if config.MaxConnectionPools > 0 {
			thresholds.MaxConnectionPools = wrapperspb.UInt32(uint32(config.MaxConnectionPools))
		}
		if config.MaxPendingRequests > 0 {
			thresholds.MaxPendingRequests = wrapperspb.UInt32(uint32(config.MaxPendingRequests))
		}
		if config.MaxRequests > 0 {
			thresholds.MaxRequests = wrapperspb.UInt32(uint32(config.MaxRequests))
		}
		if config.MaxRetries > 0 {
			thresholds.MaxRetries = wrapperspb.UInt32(uint32(config.MaxRetries))
		}
		cluster.CircuitBreakers = &clusterv3.CircuitBreakers{
			Thresholds: []*clusterv3.CircuitBreakers_Thresholds{
				thresholds,
			},
		}
	}

	// service discovery itself will be handling loadbancing etc.
	// Therefore mutiple endpoint support is not needed, hence consider only.
	serviceDiscoveryString := clusterDetails.Endpoints[0].ServiceDiscoveryString
	if serviceDiscoveryString != "" {
		//add the api level cluster name to the ClusterConsulKeyMap
		svcdiscovery.ClusterConsulKeyMap[clusterName] = serviceDiscoveryString
		logger.LoggerOasparser.Debugln("Consul cluster added for x-wso2-endpoints: ", clusterName, " ",
			serviceDiscoveryString)
	}

	return &cluster, addresses, nil
}

func createHealthCheck() []*corev3.HealthCheck {
	conf, _ := config.ReadConfigs()
	return []*corev3.HealthCheck{
		{
			Timeout:            ptypes.DurationProto(time.Duration(conf.Envoy.Upstream.Health.Timeout) * time.Second),
			Interval:           ptypes.DurationProto(time.Duration(conf.Envoy.Upstream.Health.Interval) * time.Second),
			UnhealthyThreshold: wrapperspb.UInt32(uint32(conf.Envoy.Upstream.Health.UnhealthyThreshold)),
			HealthyThreshold:   wrapperspb.UInt32(uint32(conf.Envoy.Upstream.Health.HealthyThreshold)),
			// we only support tcp default healthcheck
			HealthChecker: &corev3.HealthCheck_TcpHealthCheck_{},
		},
	}
}

func createUpstreamTLSContext(upstreamCerts []byte, address *corev3.Address) *tlsv3.UpstreamTlsContext {
	conf, errReadConfig := config.ReadConfigs()
	//TODO: (VirajSalaka) Error Handling
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
		return nil
	}
	tlsCert := generateTLSCert(conf.Envoy.KeyStore.KeyPath, conf.Envoy.KeyStore.CertPath)
	// Convert the cipher string to a string array
	ciphersArray := strings.Split(conf.Envoy.Upstream.TLS.Ciphers, ",")
	for i := range ciphersArray {
		ciphersArray[i] = strings.TrimSpace(ciphersArray[i])
	}

	upstreamTLSContext := &tlsv3.UpstreamTlsContext{
		CommonTlsContext: &tlsv3.CommonTlsContext{
			TlsParams: &tlsv3.TlsParameters{
				TlsMinimumProtocolVersion: createTLSProtocolVersion(conf.Envoy.Upstream.TLS.MinimumProtocolVersion),
				TlsMaximumProtocolVersion: createTLSProtocolVersion(conf.Envoy.Upstream.TLS.MaximumProtocolVersion),
				CipherSuites:              ciphersArray,
			},
			TlsCertificates: []*tlsv3.TlsCertificate{tlsCert},
		},
	}

	// Sni should be assigned when there is a hostname
	if net.ParseIP(address.GetSocketAddress().GetAddress()) == nil {
		upstreamTLSContext.Sni = address.GetSocketAddress().GetAddress()
	}

	if !conf.Envoy.Upstream.TLS.DisableSslVerification {
		var trustedCASrc *corev3.DataSource

		if len(upstreamCerts) > 0 {
			trustedCASrc = &corev3.DataSource{
				Specifier: &corev3.DataSource_InlineBytes{
					InlineBytes: upstreamCerts,
				},
			}
		} else {
			trustedCASrc = &corev3.DataSource{
				Specifier: &corev3.DataSource_Filename{
					Filename: conf.Envoy.Upstream.TLS.TrustedCertPath,
				},
			}
		}

		upstreamTLSContext.CommonTlsContext.ValidationContextType = &tlsv3.CommonTlsContext_ValidationContext{
			ValidationContext: &tlsv3.CertificateValidationContext{
				TrustedCa: trustedCASrc,
			},
		}
	}

	if conf.Envoy.Upstream.TLS.VerifyHostName && !conf.Envoy.Upstream.TLS.DisableSslVerification {
		addressString := address.GetSocketAddress().GetAddress()
		subjectAltNames := []*envoy_type_matcherv3.StringMatcher{
			{
				MatchPattern: &envoy_type_matcherv3.StringMatcher_Exact{
					Exact: addressString,
				},
			},
		}
		upstreamTLSContext.CommonTlsContext.GetValidationContext().MatchSubjectAltNames = subjectAltNames
	}
	return upstreamTLSContext
}

func createTLSProtocolVersion(tlsVersion string) tlsv3.TlsParameters_TlsProtocol {
	switch tlsVersion {
	case "TLS1_0":
		return tlsv3.TlsParameters_TLSv1_0
	case "TLS1_1":
		return tlsv3.TlsParameters_TLSv1_1
	case "TLS1_2":
		return tlsv3.TlsParameters_TLSv1_2
	case "TLS1_3":
		return tlsv3.TlsParameters_TLSv1_3
	default:
		return tlsv3.TlsParameters_TLS_AUTO
	}
}

// createRoute creates route elements for the route configurations. API title, VHost, xWso2Basepath, API version,
// endpoint's basePath, resource Object (Microgateway's internal representation), production clusterName and
// sandbox clusterName needs to be provided.
func createRoute(params *routeCreateParams) *routev3.Route {
	// func createRoute(title string, apiType string, xWso2Basepath string, version string, endpointBasepath string,
	// 	resourcePathParam string, resourceMethods []string, prodClusterName string, sandClusterName string,
	// 	corsPolicy *routev3.CorsPolicy) *routev3.Route {
	title := params.title
	version := params.version
	vHost := params.vHost
	xWso2Basepath := params.xWSO2BasePath
	apiType := params.apiType
	corsPolicy := getCorsPolicy(params.corsPolicy)
	resourcePathParam := params.resourcePathParam
	resourceMethods := params.resourceMethods
	prodClusterName := params.prodClusterName
	sandClusterName := params.sandClusterName
	prodRouteConfig := params.prodRouteConfig
	sandRouteConfig := params.sandRouteConfig
	endpointBasepath := params.endpointBasePath
	requestInterceptor := params.requestInterceptor
	responseInterceptor := params.responseInterceptor
	config, _ := config.ReadConfigs()

	logger.LoggerOasparser.Debug("creating a route....")
	var (
		router                  routev3.Route
		action                  *routev3.Route_Route
		match                   *routev3.RouteMatch
		decorator               *routev3.Decorator
		resourcePath            string
		responseHeadersToRemove []string
	)

	// OPTIONS is always added even if it is not listed under resources
	// This is required to handle CORS preflight request fail scenario
	methodRegex := strings.Join(resourceMethods, "|")
	if !strings.Contains(methodRegex, "OPTIONS") {
		methodRegex = methodRegex + "|OPTIONS"
	}
	headerMatcherArray := routev3.HeaderMatcher{
		Name: httpMethodHeader,
		HeaderMatchSpecifier: &routev3.HeaderMatcher_StringMatch{
			StringMatch: &envoy_type_matcherv3.StringMatcher{
				MatchPattern: &envoy_type_matcherv3.StringMatcher_SafeRegex{
					SafeRegex: &envoy_type_matcherv3.RegexMatcher{
						EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
							GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
								MaxProgramSize: nil,
							},
						},
						Regex: "^(" + methodRegex + ")$",
					},
				},
			},
		},
	}
	resourcePath = resourcePathParam
	routePath := generateRoutePaths(xWso2Basepath, endpointBasepath, resourcePath)

	match = &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_SafeRegex{
			SafeRegex: &envoy_type_matcherv3.RegexMatcher{
				EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
					GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
						MaxProgramSize: nil,
					},
				},
				Regex: routePath,
			},
		},
		Headers: []*routev3.HeaderMatcher{&headerMatcherArray},
	}

	hostRewriteSpecifier := &routev3.RouteAction_AutoHostRewrite{
		AutoHostRewrite: &wrapperspb.BoolValue{
			Value: true,
		},
	}
	// route path could be empty only if there is no basePath for API or the endpoint available,
	// and resourcePath is also an empty string.
	// Empty check is added to run the gateway in failsafe mode, as if the decorator string is
	// empty, the route configuration does not apply.
	if strings.TrimSpace(routePath) != "" {
		decorator = &routev3.Decorator{
			Operation: vHost + ":" + routePath,
		}
	}

	var contextExtensions = make(map[string]string)
	contextExtensions[pathContextExtension] = resourcePath
	contextExtensions[vHostContextExtension] = vHost
	if xWso2Basepath != "" {
		contextExtensions[basePathContextExtension] = xWso2Basepath
	} else {
		contextExtensions[basePathContextExtension] = endpointBasepath
	}
	contextExtensions[methodContextExtension] = strings.Join(resourceMethods, " ")
	contextExtensions[apiVersionContextExtension] = version
	contextExtensions[apiNameContextExtension] = title
	// One of these values will be selected and added as the cluster-header http header
	// from enhancer
	// Even if the routing is based on direct cluster, these properties needs to be populated
	// to validate the key type component in the token.
	contextExtensions[prodClusterNameContextExtension] = prodClusterName
	contextExtensions[sandClusterNameContextExtension] = sandClusterName

	extAuthPerFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_CheckSettings{
			CheckSettings: &extAuthService.CheckSettings{
				ContextExtensions: contextExtensions,
			},
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&extAuthPerFilterConfig)
	extAuthzFilter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}

	var luaPerFilterConfig lua.LuaPerRoute
	if !requestInterceptor.Enable && !responseInterceptor.Enable {
		luaPerFilterConfig = lua.LuaPerRoute{
			Override: &lua.LuaPerRoute_Disabled{Disabled: true},
		}
	} else {
		// read from contextExtensions map since, it is updated with correct values with conditions
		// so, no need to change two places
		iInvCtx := &interceptor.InvocationContext{
			OrganizationID:   params.organizationID,
			BasePath:         contextExtensions[basePathContextExtension],
			SupportedMethods: contextExtensions[methodContextExtension],
			APIName:          contextExtensions[apiNameContextExtension],
			APIVersion:       contextExtensions[apiVersionContextExtension],
			PathTemplate:     contextExtensions[pathContextExtension],
			Vhost:            contextExtensions[vHostContextExtension],
			ProdClusterName:  contextExtensions[prodClusterNameContextExtension],
			SandClusterName:  contextExtensions[sandClusterNameContextExtension],
		}
		luaPerFilterConfig = lua.LuaPerRoute{
			Override: &lua.LuaPerRoute_SourceCode{
				SourceCode: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: getInlineLuaScript(requestInterceptor, responseInterceptor, iInvCtx),
					},
				},
			},
		}
	}

	luaMarshelled := proto.NewBuffer(nil)
	luaMarshelled.SetDeterministic(true)
	_ = luaMarshelled.Marshal(&luaPerFilterConfig)

	luaFilter := &any.Any{
		TypeUrl: luaPerRouteName,
		Value:   luaMarshelled.Bytes(),
	}

	if xWso2Basepath != "" {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
				// TODO: (VirajSalaka) Provide prefix rewrite since it is simple
				RegexRewrite: &envoy_type_matcherv3.RegexMatchAndSubstitute{
					Pattern: &envoy_type_matcherv3.RegexMatcher{
						EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
							GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
								MaxProgramSize: nil,
							},
						},
						Regex: xWso2Basepath,
					},
					Substitution: endpointBasepath,
				},
				UpgradeConfigs:    getUpgradeConfig(apiType),
				MaxStreamDuration: getMaxStreamDuration(apiType),
				Timeout:           ptypes.DurationProto(time.Duration(config.Envoy.Upstream.Timeouts.RouteTimeoutInSeconds) * time.Second),
				IdleTimeout:       ptypes.DurationProto(time.Duration(config.Envoy.Upstream.Timeouts.RouteIdleTimeoutInSeconds) * time.Second),
			},
		}
	} else {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
			},
		}
	}

	headerBasedClusterSpecifier := &routev3.RouteAction_ClusterHeader{
		ClusterHeader: clusterHeaderName,
	}
	action.Route.ClusterSpecifier = headerBasedClusterSpecifier
	logger.LoggerOasparser.Debug("added header based cluster")

	if (prodRouteConfig != nil && prodRouteConfig.RetryConfig != nil) ||
		(sandRouteConfig != nil && sandRouteConfig.RetryConfig != nil) {
		// Retry configs are always added via headers. This is to update the
		// default retry back-off base interval, which cannot be updated via headers.
		retryConfig := config.Envoy.Upstream.Retry
		commonRetryPolicy := &routev3.RetryPolicy{
			RetryOn: retryPolicyRetriableStatusCodes,
			NumRetries: &wrapperspb.UInt32Value{
				Value: 0,
				// If not set to 0, default value 1 will be
				// applied to both prod and sandbox even if they are not set.
			},
			RetriableStatusCodes: retryConfig.StatusCodes,
			RetryBackOff: &routev3.RetryPolicy_RetryBackOff{
				BaseInterval: &durationpb.Duration{
					Nanos: int32(retryConfig.BaseIntervalInMillis) * 1000,
				},
			},
		}
		action.Route.RetryPolicy = commonRetryPolicy
	}

	if corsPolicy != nil {
		action.Route.Cors = corsPolicy
	}
	// remove the 'x-envoy-upstream-service-time' from the response.
	responseHeadersToRemove = append(responseHeadersToRemove, upstreamServiceTimeHeader)

	logger.LoggerOasparser.Debug("adding route ", resourcePath)
	router = routev3.Route{
		Name:      xWso2Basepath, //Categorize routes with same base path
		Match:     match,
		Action:    action,
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			wellknown.HTTPExternalAuthorization: extAuthzFilter,
			wellknown.Lua:                       luaFilter,
		},
		ResponseHeadersToRemove: responseHeadersToRemove,
	}
	return &router
}

func getInlineLuaScript(requestInterceptor model.InterceptEndpoint, responseInterceptor model.InterceptEndpoint,
	requestContext *interceptor.InvocationContext) string {

	i := &interceptor.Interceptor{
		Context:              requestContext,
		RequestExternalCall:  &interceptor.HTTPCallConfig{}, // assign default values ("false" if req flow disabled)
		ResponseExternalCall: &interceptor.HTTPCallConfig{},
		ReqFlowInclude:       &interceptor.RequestInclusions{}, // assign default values ("false" if headers not included req details)
		RespFlowInclude:      &interceptor.RequestInclusions{},
	}
	if requestInterceptor.Enable {
		i.RequestExternalCall = &interceptor.HTTPCallConfig{
			Enable:      true,
			ClusterName: requestInterceptor.ClusterName,
			// multiplying in seconds here because in configs we are directly getting config to time.Duration
			// which is in nano seconds, so multiplying it in seconds here
			Timeout: strconv.FormatInt((requestInterceptor.RequestTimeout * time.Second).Milliseconds(), 10),
		}
		i.ReqFlowInclude = requestInterceptor.Includes
	}
	if responseInterceptor.Enable {
		i.ResponseExternalCall = &interceptor.HTTPCallConfig{
			Enable:      true,
			ClusterName: responseInterceptor.ClusterName,
			// multiplying in seconds here because in configs we are directly getting config to time.Duration
			// which is in nano seconds, so multiplying it in seconds here
			Timeout: strconv.FormatInt((requestInterceptor.RequestTimeout * time.Second).Milliseconds(), 10),
		}
		i.RespFlowInclude = responseInterceptor.Includes
	}
	return interceptor.GetInterceptor(i)
}

// CreateTokenRoute generates a route for the jwt /testkey endpoint
func CreateTokenRoute() *routev3.Route {
	var (
		router    routev3.Route
		action    *routev3.Route_Route
		match     *routev3.RouteMatch
		decorator *routev3.Decorator
	)

	match = &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_Path{
			Path: testKeyPath,
		},
	}

	hostRewriteSpecifier := &routev3.RouteAction_AutoHostRewrite{
		AutoHostRewrite: &wrapperspb.BoolValue{
			Value: true,
		},
	}

	decorator = &routev3.Decorator{
		Operation: testKeyPath,
	}

	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_Disabled{
			Disabled: true,
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&perFilterConfig)
	filter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}

	action = &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: hostRewriteSpecifier,
			RegexRewrite: &envoy_type_matcherv3.RegexMatchAndSubstitute{
				Pattern: &envoy_type_matcherv3.RegexMatcher{
					EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
						GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
							MaxProgramSize: nil,
						},
					},
					Regex: testKeyPath,
				},
				Substitution: "/",
			},
		},
	}

	directClusterSpecifier := &routev3.RouteAction_Cluster{
		Cluster: "token_cluster",
	}
	action.Route.ClusterSpecifier = directClusterSpecifier

	router = routev3.Route{
		Name:      testKeyPath, //Categorize routes with same base path
		Match:     match,
		Action:    action,
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			wellknown.HTTPExternalAuthorization: filter,
		},
	}
	return &router
}

// CreateHealthEndpoint generates a route for the jwt /health endpoint
// Replies with direct response.
func CreateHealthEndpoint() *routev3.Route {
	var (
		router    routev3.Route
		match     *routev3.RouteMatch
		decorator *routev3.Decorator
	)

	match = &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_Path{
			Path: healthPath,
		},
	}

	decorator = &routev3.Decorator{
		Operation: healthPath,
	}

	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_Disabled{
			Disabled: true,
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&perFilterConfig)
	filter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}

	router = routev3.Route{
		Name:  healthPath, //Categorize routes with same base path
		Match: match,
		Action: &routev3.Route_DirectResponse{
			DirectResponse: &routev3.DirectResponseAction{
				Status: 200,
				Body: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: healthEndpointResponse,
					},
				},
			},
		},
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			wellknown.HTTPExternalAuthorization: filter,
		},
	}
	return &router
}

// CreateReadyEndpoint generates a route for the router /ready endpoint
// Replies with direct response.
func CreateReadyEndpoint() *routev3.Route {
	var (
		router    routev3.Route
		match     *routev3.RouteMatch
		decorator *routev3.Decorator
	)

	match = &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_Path{
			Path: readyPath,
		},
	}

	decorator = &routev3.Decorator{
		Operation: readyPath,
	}

	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_Disabled{
			Disabled: true,
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&perFilterConfig)
	filter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}

	router = routev3.Route{
		Name:  readyPath, //Categorize routes with same base path
		Match: match,
		Action: &routev3.Route_DirectResponse{
			DirectResponse: &routev3.DirectResponseAction{
				Status: 200,
				Body: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: readyEndpointResponse,
					},
				},
			},
		},
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			wellknown.HTTPExternalAuthorization: filter,
		},
	}
	return &router
}

// generateRoutePaths generates route paths for the api resources.
func generateRoutePaths(xWso2Basepath, basePath, resourcePath string) string {
	prefix := ""
	newPath := ""
	if strings.TrimSpace(xWso2Basepath) != "" {
		prefix = basepathConsistent(xWso2Basepath)

	} else {
		prefix = basepathConsistent(basePath)
		// TODO: (VirajSalaka) Decide if it is possible to proceed without both basepath options
	}
	if strings.Contains(resourcePath, "?") {
		resourcePath = strings.Split(resourcePath, "?")[0]
	}
	fullpath := prefix + resourcePath
	newPath = generateRegex(fullpath)
	return newPath
}

func basepathConsistent(basePath string) string {
	modifiedBasePath := basePath
	if !strings.HasPrefix(basePath, "/") {
		modifiedBasePath = "/" + modifiedBasePath
	}
	modifiedBasePath = strings.TrimSuffix(modifiedBasePath, "/")
	return modifiedBasePath
}

// generateRegex generates regex for the resources which have path paramaters
// such that the envoy configuration can use it as a route.
// If path has path parameters ({id}), append a regex pattern (pathParaRegex).
// To avoid query parameter issues, add a regex pattern ( endRegex) for end of all routes.
// It takes the path value as an input and then returns the regex value.
// TODO: (VirajSalaka) Improve regex specifically for strings, integers etc.
func generateRegex(fullpath string) string {
	pathParaRegex := "([^/]+)"
	wildCardRegex := "((/(.*))*)"
	endRegex := "(\\?([^/]+))?"
	newPath := ""

	// Check and replace all the path parameters
	matcher := regexp.MustCompile(`{([^}]+)}`)
	newPath = matcher.ReplaceAllString(fullpath, pathParaRegex)

	if strings.HasSuffix(newPath, "/*") {
		newPath = strings.TrimSuffix(newPath, "/*") + wildCardRegex
	}
	return "^" + newPath + endRegex + "$"
}

func getUpgradeConfig(apiType string) []*routev3.RouteAction_UpgradeConfig {
	var upgradeConfig []*routev3.RouteAction_UpgradeConfig
	if apiType == model.WS {
		upgradeConfig = []*routev3.RouteAction_UpgradeConfig{{
			UpgradeType: "websocket",
			Enabled:     &wrappers.BoolValue{Value: true},
		}}
	} else {
		upgradeConfig = []*routev3.RouteAction_UpgradeConfig{{
			UpgradeType: "websocket",
			Enabled:     &wrappers.BoolValue{Value: false},
		}}
	}
	return upgradeConfig
}

func getCorsPolicy(corsConfig *model.CorsConfig) *routev3.CorsPolicy {

	if corsConfig == nil || !corsConfig.Enabled {
		return nil
	}

	stringMatcherArray := []*envoy_type_matcherv3.StringMatcher{}
	for _, origin := range corsConfig.AccessControlAllowOrigins {
		regexMatcher := &envoy_type_matcherv3.StringMatcher{
			MatchPattern: &envoy_type_matcherv3.StringMatcher_SafeRegex{
				SafeRegex: &envoy_type_matcherv3.RegexMatcher{
					EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
						GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
							MaxProgramSize: nil,
						},
					},
					// adds escape character when necessary
					Regex: regexp.QuoteMeta(origin),
				},
			},
		}
		stringMatcherArray = append(stringMatcherArray, regexMatcher)
	}

	corsPolicy := &routev3.CorsPolicy{
		AllowCredentials: &wrapperspb.BoolValue{
			Value: corsConfig.AccessControlAllowCredentials,
		},
	}

	if len(stringMatcherArray) > 0 {
		corsPolicy.AllowOriginStringMatch = stringMatcherArray
	}
	if len(corsConfig.AccessControlAllowMethods) > 0 {
		corsPolicy.AllowMethods = strings.Join(corsConfig.AccessControlAllowMethods, ", ")
	}
	if len(corsConfig.AccessControlAllowHeaders) > 0 {
		corsPolicy.AllowHeaders = strings.Join(corsConfig.AccessControlAllowHeaders, ", ")
	}
	if len(corsConfig.AccessControlExposeHeaders) > 0 {
		corsPolicy.ExposeHeaders = strings.Join(corsConfig.AccessControlExposeHeaders, ", ")
	}
	return corsPolicy
}

func genRouteCreateParams(swagger *model.MgwSwagger, resource *model.Resource, vHost, endpointBasePath string,
	prodClusterName string, sandClusterName string, requestInterceptor model.InterceptEndpoint,
	responseInterceptor model.InterceptEndpoint, organizationID string) *routeCreateParams {
	params := &routeCreateParams{
		organizationID:      organizationID,
		title:               swagger.GetTitle(),
		apiType:             swagger.GetAPIType(),
		version:             swagger.GetVersion(),
		vHost:               vHost,
		xWSO2BasePath:       swagger.GetXWso2Basepath(),
		AuthHeader:          swagger.GetXWSO2AuthHeader(),
		prodClusterName:     prodClusterName,
		sandClusterName:     sandClusterName,
		endpointBasePath:    endpointBasePath,
		corsPolicy:          swagger.GetCorsConfig(),
		resourcePathParam:   "",
		resourceMethods:     getDefaultResourceMethods(swagger.GetAPIType()),
		requestInterceptor:  requestInterceptor,
		responseInterceptor: responseInterceptor,
	}

	if resource != nil {
		params.resourceMethods = resource.GetMethodList()
		params.resourcePathParam = resource.GetPath()
	}

	if swagger.GetProdEndpoints() != nil {
		params.prodRouteConfig = swagger.GetProdEndpoints().Config
	}
	if swagger.GetSandEndpoints() != nil {
		params.sandRouteConfig = swagger.GetSandEndpoints().Config
	}
	return params
}

// createAddress generates an address from the given host and port
func createAddress(remoteHost string, port uint32) *corev3.Address {
	address := corev3.Address{Address: &corev3.Address_SocketAddress{
		SocketAddress: &corev3.SocketAddress{
			Address:  remoteHost,
			Protocol: corev3.SocketAddress_TCP,
			PortSpecifier: &corev3.SocketAddress_PortValue{
				PortValue: uint32(port),
			},
		},
	}}
	return &address
}

// getMaxStreamDuration configures a maximum duration for a websocket route.
func getMaxStreamDuration(apiType string) *routev3.RouteAction_MaxStreamDuration {
	var maxStreamDuration *routev3.RouteAction_MaxStreamDuration = nil
	if apiType == model.WS {
		maxStreamDuration = &routev3.RouteAction_MaxStreamDuration{
			MaxStreamDuration: &durationpb.Duration{
				Seconds: 60 * 60 * 24,
			},
		}
	}
	return maxStreamDuration
}

func getDefaultResourceMethods(apiType string) []string {
	var defaultResourceMethods []string = nil
	if apiType == model.WS {
		defaultResourceMethods = []string{"GET"}
	}
	return defaultResourceMethods
}
