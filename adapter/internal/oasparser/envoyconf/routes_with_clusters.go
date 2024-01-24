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
	"fmt"
	"net"
	"regexp"
	"strconv"

	"github.com/wso2/product-microgateway/adapter/internal/interceptor"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/durationpb"
	"google.golang.org/protobuf/types/known/structpb"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	cors_filter_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/cors/v3"
	local_rate_limitv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/local_ratelimit/v3"
	lua "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/lua/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	upstreams "github.com/envoyproxy/go-control-plane/envoy/extensions/upstreams/http/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	metadatav3 "github.com/envoyproxy/go-control-plane/envoy/type/metadata/v3"
	typev3 "github.com/envoyproxy/go-control-plane/envoy/type/v3"
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

// Constants relevant to the route related rate-limit configurations
const (
	DescriptorKeyForOrg               = "org"
	DescriptorKeyForVhost             = "vhost"
	DescriptorKeyForPath              = "path"
	DescriptorKeyForMethod            = "method"
	DescriptorValueForAPIMethod       = "ALL"
	DescriptorValueForOperationMethod = ":method"
	DescriptorKeyForSubscription      = "subscription"
	DescriptorKeyForPolicy            = "policy"

	descriptorMetadataKeyForSubscription = "ratelimit:subscription"
	descriptorMetadataKeyForUsagePolicy  = "ratelimit:usage-policy"
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
	apiRequestInterceptor, interceptorErr = mgwSwagger.GetInterceptor(mgwSwagger.GetVendorExtensions(), xWso2requestInterceptor, APILevelInterceptor)
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
	apiResponseInterceptor, interceptorErr = mgwSwagger.GetInterceptor(mgwSwagger.GetVendorExtensions(), xWso2responseInterceptor, APILevelInterceptor)
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

	for _, resource := range mgwSwagger.GetResources() {
		resourceRequestInterceptor := apiRequestInterceptor
		resourceResponseInterceptor := apiResponseInterceptor
		clusterNameProd := apiLevelClusterNameProd
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

		// if both resource level sandbox and production are same as api level, api level clusters will be applied with the api level basepath
		if clusterNameProd == apiLevelClusterNameProd {
			resourceBasePath = apiLevelbasePath
		}
		if clusterNameProd != "" && clusterNameProd == apiLevelClusterNameProd && resourceBasePath != apiLevelbasePath {
			logger.LoggerOasparser.Errorf("Error while adding resource level production endpoints for %s:%v-%v. resource basepath : %v and API basepath : %v mismatched",
				apiTitle, apiVersion, resourcePath, resourceBasePath, apiLevelbasePath)
			clusterNameProd = ""
		}

		// create resource level request interceptor cluster
		reqInterceptorVal, err := mgwSwagger.GetInterceptor(resource.GetVendorExtensions(), xWso2requestInterceptor, ResourceLevelInterceptor)
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

		// create operational level response interceptor clusters
		operationalReqInterceptors := mgwSwagger.GetOperationInterceptors(apiRequestInterceptor, resourceRequestInterceptor, resource.GetMethod(),
			xWso2requestInterceptor)
		for method, opI := range operationalReqInterceptors {
			if opI.Enable && opI.Level == OperationLevelInterceptor {
				logger.LoggerOasparser.Debugf("Operation level request interceptors found for %v:%v-%v-%v", apiTitle, apiVersion, resource.GetPath(),
					opI.ClusterName)
				opID := opI.ClusterName
				opI.ClusterName = getClusterName(requestInterceptClustersNamePrefix, organizationID, vHost, apiTitle, apiVersion, opID)
				cluster, addresses, err := CreateLuaCluster(interceptorCerts, opI)
				if err != nil {
					logger.LoggerOasparser.Errorf("Error while adding operational level request intercept external cluster for %v:%v-%v-%v. %v",
						apiTitle, apiVersion, resource.GetPath(), opID, err.Error())
					// setting resource level interceptor to failed operation level interceptor.
					operationalReqInterceptors[method] = resourceRequestInterceptor
				} else {
					clusters = append(clusters, cluster)
					endpoints = append(endpoints, addresses...)
				}
			}
		}

		// create resource level response interceptor cluster
		respInterceptorVal, err := mgwSwagger.GetInterceptor(resource.GetVendorExtensions(), xWso2responseInterceptor, ResourceLevelInterceptor)
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

		// create operation level response interceptor clusters
		operationalRespInterceptorVal := mgwSwagger.GetOperationInterceptors(apiResponseInterceptor, resourceResponseInterceptor, resource.GetMethod(),
			xWso2responseInterceptor)
		for method, opI := range operationalRespInterceptorVal {
			if opI.Enable && opI.Level == OperationLevelInterceptor {
				logger.LoggerOasparser.Debugf("Operational level response interceptors found for %v:%v-%v-%v", apiTitle, apiVersion, resource.GetPath(),
					opI.ClusterName)
				opID := opI.ClusterName
				opI.ClusterName = getClusterName(responseInterceptClustersNamePrefix, organizationID, vHost, apiTitle, apiVersion, opID)
				cluster, addresses, err := CreateLuaCluster(interceptorCerts, opI)
				if err != nil {
					logger.LoggerOasparser.Errorf("Error while adding operational level response intercept external cluster for %v:%v-%v-%v. %v",
						apiTitle, apiVersion, resource.GetPath(), opID, err.Error())
					// setting resource level interceptor to failed operation level interceptor.
					operationalRespInterceptorVal[method] = resourceResponseInterceptor
				} else {
					clusters = append(clusters, cluster)
					endpoints = append(endpoints, addresses...)
				}
			}
		}

		routeP := createRoute(genRouteCreateParams(&mgwSwagger, resource, vHost, resourceBasePath, clusterNameProd, operationalReqInterceptors, operationalRespInterceptorVal, organizationID))
		routes = append(routes, routeP)
	}
	if mgwSwagger.GetAPIType() == model.WS {
		routesP := createRoute(genRouteCreateParams(&mgwSwagger, nil, vHost, apiLevelbasePath, apiLevelClusterNameProd, nil, nil, organizationID))
		routes = append(routes, routesP)
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

// CreateRateLimitCluster creates cluster relevant to the rate limit service
func CreateRateLimitCluster() (*clusterv3.Cluster, []*corev3.Address, error) {
	conf, _ := config.ReadConfigs()
	var sslCertSanHostName string = ""
	if conf.Envoy.RateLimit.SSLCertSANHostname == "" {
		sslCertSanHostName = conf.Envoy.RateLimit.Host
	} else {
		sslCertSanHostName = conf.Envoy.RateLimit.SSLCertSANHostname
	}
	rlCluster := &model.EndpointCluster{
		Endpoints: []model.Endpoint{
			{
				Host:    conf.Envoy.RateLimit.Host,
				URLType: httpsURLType,
				Port:    conf.Envoy.RateLimit.Port,
			},
		},
	}
	cluster, address, rlErr := processEndpoints(rateLimitClusterName, rlCluster, nil, 20, "")
	if rlErr != nil {
		return nil, nil, rlErr
	}
	config := &upstreams.HttpProtocolOptions{
		UpstreamHttpProtocolOptions: &corev3.UpstreamHttpProtocolOptions{
			AutoSni: true,
		},
		UpstreamProtocolOptions: &upstreams.HttpProtocolOptions_ExplicitHttpConfig_{
			ExplicitHttpConfig: &upstreams.HttpProtocolOptions_ExplicitHttpConfig{
				ProtocolConfig: &upstreams.HttpProtocolOptions_ExplicitHttpConfig_Http2ProtocolOptions{
					Http2ProtocolOptions: &corev3.Http2ProtocolOptions{},
				},
			},
		},
	}
	MarshalledHTTPProtocolOptions, err := proto.Marshal(config)
	if err != nil {
		return nil, nil, err
	}
	cluster.TypedExtensionProtocolOptions = map[string]*anypb.Any{
		"envoy.extensions.upstreams.http.v3.HttpProtocolOptions": {
			TypeUrl: httpProtocolOptionsName,
			Value:   MarshalledHTTPProtocolOptions,
		},
	}
	tlsCert := generateTLSCert(conf.Envoy.RateLimit.KeyFilePath, conf.Envoy.RateLimit.CertFilePath)

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
	trustedCASrc := &corev3.DataSource{
		Specifier: &corev3.DataSource_Filename{
			Filename: conf.Envoy.RateLimit.CaCertFilePath,
		},
	}
	upstreamTLSContext.Sni = sslCertSanHostName
	upstreamTLSContext.CommonTlsContext.ValidationContextType = &tlsv3.CommonTlsContext_ValidationContext{
		ValidationContext: &tlsv3.CertificateValidationContext{
			TrustedCa: trustedCASrc,
			MatchTypedSubjectAltNames: []*tlsv3.SubjectAltNameMatcher{
				{
					SanType: tlsv3.SubjectAltNameMatcher_DNS,
					Matcher: &envoy_type_matcherv3.StringMatcher{
						MatchPattern: &envoy_type_matcherv3.StringMatcher_Exact{
							Exact: sslCertSanHostName,
						},
					},
				},
			},
		},
	}
	marshalledTLSContext, err := anypb.New(upstreamTLSContext)
	if err != nil {
		return nil, nil, errors.New("internal Error while marshalling the upstream TLS Context")
	}

	cluster.TransportSocketMatches[0] = &clusterv3.Cluster_TransportSocketMatch{
		Name: "ts" + strconv.Itoa(0),
		Match: &structpb.Struct{
			Fields: map[string]*structpb.Value{
				"lb_id": structpb.NewStringValue(strconv.Itoa(0)),
			},
		},
		TransportSocket: &corev3.TransportSocket{
			Name: transportSocketName,
			ConfigType: &corev3.TransportSocket_TypedConfig{
				TypedConfig: marshalledTLSContext,
			},
		},
	}
	return cluster, address, nil
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
			marshalledTLSContext, err := anypb.New(upstreamtlsContext)
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
	conf, _ := config.ReadConfigs()

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
		TransportSocketMatches: transportSocketMatches,
		DnsRefreshRate:         durationpb.New(time.Duration(conf.Envoy.Upstream.DNS.DNSRefreshRate) * time.Millisecond),
		RespectDnsTtl:          conf.Envoy.Upstream.DNS.RespectDNSTtl,
	}

	if len(clusterDetails.Endpoints) > 1 {
		cluster.HealthChecks = createHealthCheck()
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
	var tlsCert *tlsv3.TlsCertificate
	//TODO: (VirajSalaka) Error Handling
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
		return nil
	}
	tlsCert = generateTLSCert(conf.Envoy.KeyStore.KeyPath, conf.Envoy.KeyStore.CertPath)

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

	sanType := tlsv3.SubjectAltNameMatcher_IP_ADDRESS
	// Sni should be assigned when there is a hostname
	if net.ParseIP(address.GetSocketAddress().GetAddress()) == nil {
		upstreamTLSContext.Sni = address.GetSocketAddress().GetAddress()
		sanType = tlsv3.SubjectAltNameMatcher_DNS
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
		subjectAltNames := []*tlsv3.SubjectAltNameMatcher{
			{
				SanType: sanType,
				Matcher: &envoy_type_matcherv3.StringMatcher{
					MatchPattern: &envoy_type_matcherv3.StringMatcher_Exact{
						Exact: addressString,
					},
				},
			},
		}
		upstreamTLSContext.CommonTlsContext.GetValidationContext().MatchTypedSubjectAltNames = subjectAltNames
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
	prodRouteConfig := params.prodRouteConfig
	sandRouteConfig := params.sandRouteConfig
	endpointBasepath := params.endpointBasePath
	requestInterceptor := params.requestInterceptor
	responseInterceptor := params.responseInterceptor
	ratelimitLevel := params.rateLimitLevel
	isRLPolicyAvailable := params.isRLPolicyAvailable
	config, _ := config.ReadConfigs()

	logger.LoggerOasparser.Debug("creating a route....")
	var (
		router       routev3.Route
		action       *routev3.Route_Route
		match        *routev3.RouteMatch
		decorator    *routev3.Decorator
		resourcePath string
	)
	basePath := getFilteredBasePath(xWso2Basepath, endpointBasepath)

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
	if len(requestInterceptor) < 1 && len(responseInterceptor) < 1 {
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

	if strings.Contains(resourcePath, "?") {
		resourcePath = strings.Split(resourcePath, "?")[0]
	}
	resourceRegex := generatePathRegexSegment(resourcePath, false)
	substitutionString := generateSubstitutionString(resourcePath, endpointBasepath)
	if strings.HasSuffix(resourcePath, "/*") {
		resourceRegex = strings.TrimSuffix(resourceRegex, "((/(.*))*)")
	}
	pathRegex := "^" + regexp.QuoteMeta(basePath) + resourceRegex

	if xWso2Basepath != "" {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
				// TODO: (VirajSalaka) Provide prefix rewrite since it is simple
				RegexRewrite: &envoy_type_matcherv3.RegexMatchAndSubstitute{
					Pattern: &envoy_type_matcherv3.RegexMatcher{
						Regex: pathRegex,
					},
					Substitution: substitutionString,
				},
				UpgradeConfigs:    getUpgradeConfig(apiType),
				MaxStreamDuration: getMaxStreamDuration(apiType),
				Timeout:           ptypes.DurationProto(time.Duration(config.Envoy.Upstream.Timeouts.RouteTimeoutInSeconds) * time.Second),
				IdleTimeout:       ptypes.DurationProto(time.Duration(config.Envoy.Upstream.Timeouts.RouteIdleTimeoutInSeconds) * time.Second),
			},
		}
		if config.Envoy.RateLimit.Enabled && isRLPolicyAvailable {
			basePathForRLService := basePath
			if ratelimitLevel == RateLimitPolicyOperationLevel {
				basePathForRLService += resourcePathParam
			}
			rateLimit := routev3.RateLimit{
				Actions: []*routev3.RateLimit_Action{
					{
						ActionSpecifier: &routev3.RateLimit_Action_GenericKey_{
							GenericKey: &routev3.RateLimit_Action_GenericKey{
								DescriptorKey:   DescriptorKeyForOrg,
								DescriptorValue: params.organizationID,
							},
						},
					},
					{
						ActionSpecifier: &routev3.RateLimit_Action_GenericKey_{
							GenericKey: &routev3.RateLimit_Action_GenericKey{
								DescriptorKey:   DescriptorKeyForVhost,
								DescriptorValue: params.vHost,
							},
						},
					},
					{
						ActionSpecifier: &routev3.RateLimit_Action_GenericKey_{
							GenericKey: &routev3.RateLimit_Action_GenericKey{
								DescriptorKey:   DescriptorKeyForPath,
								DescriptorValue: basePathForRLService,
							},
						},
					},
				},
			}

			if ratelimitLevel == RateLimitPolicyAPILevel {
				rateLimit.Actions = append(rateLimit.Actions, &routev3.RateLimit_Action{
					ActionSpecifier: &routev3.RateLimit_Action_GenericKey_{
						GenericKey: &routev3.RateLimit_Action_GenericKey{
							DescriptorKey:   DescriptorKeyForMethod,
							DescriptorValue: DescriptorValueForAPIMethod,
						},
					},
				})

			} else {
				operationLevelRateLimitActions := []*routev3.RateLimit_Action{
					{
						ActionSpecifier: &routev3.RateLimit_Action_RequestHeaders_{
							RequestHeaders: &routev3.RateLimit_Action_RequestHeaders{
								DescriptorKey: DescriptorKeyForMethod,
								HeaderName:    DescriptorValueForOperationMethod,
							},
						},
					},
				}
				rateLimit.Actions = append(rateLimit.Actions, operationLevelRateLimitActions...)
			}
			action.Route.RateLimits = []*routev3.RateLimit{&rateLimit}
		}

		if config.Envoy.RateLimit.Enabled {
			action.Route.RateLimits = append(action.Route.RateLimits, &routev3.RateLimit{
				Actions: []*routev3.RateLimit_Action{
					{
						ActionSpecifier: &routev3.RateLimit_Action_Metadata{
							Metadata: &routev3.RateLimit_Action_MetaData{
								DescriptorKey: DescriptorKeyForSubscription,
								MetadataKey: &metadatav3.MetadataKey{
									Key: extAuthzFilterName,
									Path: []*metadatav3.MetadataKey_PathSegment{
										{
											Segment: &metadatav3.MetadataKey_PathSegment_Key{
												Key: descriptorMetadataKeyForSubscription,
											},
										},
									},
								},
								Source:       routev3.RateLimit_Action_MetaData_DYNAMIC,
								SkipIfAbsent: true,
							},
						},
					},
					{
						ActionSpecifier: &routev3.RateLimit_Action_Metadata{
							Metadata: &routev3.RateLimit_Action_MetaData{
								DescriptorKey: DescriptorKeyForPolicy,
								MetadataKey: &metadatav3.MetadataKey{
									Key: extAuthzFilterName,
									Path: []*metadatav3.MetadataKey_PathSegment{
										{
											Segment: &metadatav3.MetadataKey_PathSegment_Key{
												Key: descriptorMetadataKeyForUsagePolicy,
											},
										},
									},
								},
								Source:       routev3.RateLimit_Action_MetaData_DYNAMIC,
								SkipIfAbsent: true,
							},
						},
					},
				},
			})
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

	corsFilter, _ := anypb.New(corsPolicy)

	logger.LoggerOasparser.Debug("adding route ", resourcePath)
	router = routev3.Route{
		Name:      getRouteName(params.apiUUID), //Categorize routes with same base path
		Match:     match,
		Action:    action,
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			wellknown.HTTPExternalAuthorization: extAuthzFilter,
			wellknown.Lua:                       luaFilter,
			wellknown.CORS:                      corsFilter,
		},
	}
	return &router
}

func getInlineLuaScript(requestInterceptor map[string]model.InterceptEndpoint, responseInterceptor map[string]model.InterceptEndpoint,
	requestContext *interceptor.InvocationContext) string {

	i := &interceptor.Interceptor{
		Context:      requestContext,
		RequestFlow:  make(map[string]interceptor.Config),
		ResponseFlow: make(map[string]interceptor.Config),
	}
	if len(requestInterceptor) > 0 {
		i.IsRequestFlowEnabled = true
		for method, op := range requestInterceptor {
			i.RequestFlow[method] = interceptor.Config{
				ExternalCall: &interceptor.HTTPCallConfig{
					ClusterName: op.ClusterName,
					// multiplying in seconds here because in configs we are directly getting config to time.Duration
					// which is in nano seconds, so multiplying it in seconds here
					Timeout: strconv.FormatInt((op.RequestTimeout * time.Second).Milliseconds(), 10),
				},
				Include: op.Includes,
			}
		}
	}
	if len(responseInterceptor) > 0 {
		i.IsResponseFlowEnabled = true
		for method, op := range responseInterceptor {
			i.ResponseFlow[method] = interceptor.Config{
				ExternalCall: &interceptor.HTTPCallConfig{
					ClusterName: op.ClusterName,
					// multiplying in seconds here because in configs we are directly getting config to time.Duration
					// which is in nano seconds, so multiplying it in seconds here
					Timeout: strconv.FormatInt((op.RequestTimeout * time.Second).Milliseconds(), 10),
				},
				Include: op.Includes,
			}
		}
	}
	return interceptor.GetInterceptor(i)
}

func createSystemRoute(path string, pathSubstitute string, clusterName string) *routev3.Route {
	routeName := fmt.Sprintf("%s#%s", "system", path)
	return createStaticRoute(routeName, path, pathSubstitute, clusterName)
}

func createStaticRoute(routeName, path string, pathSubstitute string, clusterName string) *routev3.Route {
	var (
		router    routev3.Route
		action    *routev3.Route_Route
		match     *routev3.RouteMatch
		decorator *routev3.Decorator
	)

	match = &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_Path{
			Path: path,
		},
	}
	hostRewriteSpecifier := &routev3.RouteAction_AutoHostRewrite{
		AutoHostRewrite: &wrapperspb.BoolValue{
			Value: true,
		},
	}
	decorator = &routev3.Decorator{
		Operation: path,
	}
	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_Disabled{
			Disabled: true,
		},
	}
	filter := marshalFilterConfig(&perFilterConfig)

	action = &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: hostRewriteSpecifier,
			PrefixRewrite:        pathSubstitute,
		},
	}
	action.Route.ClusterSpecifier = &routev3.RouteAction_Cluster{
		Cluster: clusterName,
	}

	router = routev3.Route{
		Name:      routeName, //Categorize routes with same base path
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

// CreateTokenRoute generates a route for the jwt /testkey endpoint
func CreateTokenRoute() *routev3.Route {
	return createSystemRoute(testKeyPath, "/testkey", extAuthzHTTPCluster)
}

// CreateJwksEndpoint generates a route for JWKS /.wellknown/jwks endpoint
func CreateJwksEndpoint() *routev3.Route {
	conf, _ := config.ReadConfigs()
	route := createSystemRoute(jwksPath, "/jwks", extAuthzHTTPCluster)
	ratelimitPerRoute := &local_rate_limitv3.LocalRateLimit{
		StatPrefix: jwksRateLimitStatPrefix,
		TokenBucket: &typev3.TokenBucket{
			MaxTokens:     conf.Enforcer.JwtGenerator.JwksRatelimitQuota,
			TokensPerFill: &wrapperspb.UInt32Value{Value: conf.Enforcer.JwtGenerator.JwksRatelimitQuota},
			FillInterval:  durationpb.New(time.Duration(conf.Enforcer.JwtGenerator.JwksRatelimitTimeWindowInSeconds) * time.Second)},
		FilterEnabled: &corev3.RuntimeFractionalPercent{
			DefaultValue: &typev3.FractionalPercent{
				Numerator:   100,
				Denominator: typev3.FractionalPercent_HUNDRED,
			},
			RuntimeKey: jwksRateLimitEnabledRuntimeKey,
		},
		FilterEnforced: &corev3.RuntimeFractionalPercent{
			DefaultValue: &typev3.FractionalPercent{
				Numerator:   100,
				Denominator: typev3.FractionalPercent_HUNDRED,
			},
			RuntimeKey: jwksRateLimitEnforcedRuntimeKey,
		},
	}
	buffer := proto.NewBuffer(nil)
	buffer.SetDeterministic(true)
	_ = buffer.Marshal(ratelimitPerRoute)

	currentFilterMap := route.GetTypedPerFilterConfig()
	currentFilterMap[localRatelimitFilterName] = &any.Any{
		TypeUrl: localRateLimitPerRouteName,
		Value:   buffer.Bytes(),
	}
	route.TypedPerFilterConfig = currentFilterMap
	return route
}

func marshalFilterConfig(perFilterConfig *extAuthService.ExtAuthzPerRoute) *anypb.Any {
	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(perFilterConfig)
	filter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}
	return filter
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
		Name:  getSystemRouteName(healthPath), //Categorize routes with same base path
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
		Name:  getSystemRouteName(readyPath), //Categorize routes with same base path
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

// generatePathRegexSegment - generates a regex segment for a given resource path
// resourcePath - resource path of the api
// skipEscapeMetaChars - skip escaping meta chars (special chars like `.(){}`.) True this for route substitution path.
func generatePathRegexSegment(resourcePath string, skipEscapeMetaChars bool) string {
	pathParaRegex := "([^/]+)"
	wildCardRegex := "((/(.*))*)"
	trailingSlashRegex := "(/{0,1})"

	pathParaReplaceRegex := `{([^}]+)}`
	wildCardReplaceRegex := "/*"
	if !skipEscapeMetaChars {
		resourcePath = regexp.QuoteMeta(resourcePath)
		pathParaReplaceRegex = `\\{([^}]+)\\}`
		wildCardReplaceRegex = "/\\*"
	}

	resourceRegex := ""
	matcher := regexp.MustCompile(pathParaReplaceRegex)
	resourceRegex = matcher.ReplaceAllString(resourcePath, pathParaRegex)
	if strings.HasSuffix(resourceRegex, wildCardReplaceRegex) {
		resourceRegex = strings.TrimSuffix(resourceRegex, wildCardReplaceRegex) + wildCardRegex
	} else {
		resourceRegex = strings.TrimSuffix(resourceRegex, "/") + trailingSlashRegex
	}
	return resourceRegex
}

// generatePathRegexSegmentForSubstitutionString generates the regex for substitution string.
// This function doesn't transform `.` characters to its regex matcher, compared to generating router matching regex.
// Path parameters will be transformed to regex matchers.
func generatePathRegexSegmentForSubstitutionString(resourcePath string) string {
	return generatePathRegexSegment(resourcePath, true)
}

func generateSubstitutionString(resourcePath string, endpointBasepath string) string {
	pathParaRegex := "([^/]+)"
	trailingSlashRegex := "(/{0,1})"
	wildCardRegex := "((/(.*))*)"
	pathParamIndex := 0
	resourceRegex := generatePathRegexSegmentForSubstitutionString(resourcePath)
	for {
		pathParaRemains := strings.Contains(resourceRegex, pathParaRegex)
		if !pathParaRemains {
			break
		}
		pathParamIndex++
		resourceRegex = strings.Replace(resourceRegex, pathParaRegex, fmt.Sprintf("\\%d", pathParamIndex), 1)
	}
	if strings.HasSuffix(resourceRegex, wildCardRegex) {
		resourceRegex = strings.TrimSuffix(resourceRegex, wildCardRegex)
	} else if strings.HasSuffix(resourcePath, "/") {
		resourceRegex = strings.TrimSuffix(resourceRegex, trailingSlashRegex) + "/"
	} else {
		resourceRegex = strings.TrimSuffix(resourceRegex, trailingSlashRegex)
	}
	return endpointBasepath + resourceRegex
}

func getFilteredBasePath(xWso2Basepath string, basePath string) string {
	var modifiedBasePath string

	if strings.TrimSpace(xWso2Basepath) != "" {
		modifiedBasePath = xWso2Basepath
	} else {
		modifiedBasePath = basePath
		// TODO: (VirajSalaka) Decide if it is possible to proceed without both basepath options
	}

	if !strings.HasPrefix(modifiedBasePath, "/") {
		modifiedBasePath = "/" + modifiedBasePath
	}
	modifiedBasePath = strings.TrimSuffix(modifiedBasePath, "/")
	return modifiedBasePath
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
	endRegex := "(\\?([^/]+))?"
	newPath := generatePathRegexSegment(fullpath, false)
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

func getCorsPolicy(corsConfig *model.CorsConfig) *cors_filter_v3.CorsPolicy {

	if corsConfig == nil || !corsConfig.Enabled {
		return nil
	}

	conf, _ := config.ReadConfigs()
	if len(conf.Envoy.Cors.MandatoryHeaders) > 0 {
		corsConfig.AccessControlAllowHeaders = append(corsConfig.AccessControlAllowHeaders, conf.Envoy.Cors.MandatoryHeaders...)
	}

	stringMatcherArray := []*envoy_type_matcherv3.StringMatcher{}
	for _, origin := range corsConfig.AccessControlAllowOrigins {
		regexMatcher := &envoy_type_matcherv3.StringMatcher{
			MatchPattern: &envoy_type_matcherv3.StringMatcher_SafeRegex{
				SafeRegex: &envoy_type_matcherv3.RegexMatcher{
					// adds escape character when necessary
					Regex: regexp.QuoteMeta(origin),
				},
			},
		}
		stringMatcherArray = append(stringMatcherArray, regexMatcher)
	}

	corsPolicy := &cors_filter_v3.CorsPolicy{
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
	prodClusterName string, requestInterceptor map[string]model.InterceptEndpoint,
	responseInterceptor map[string]model.InterceptEndpoint, organizationID string) *routeCreateParams {

	var rlMethodDescriptorValue string
	var isRLPolicyAvailable bool = false
	if swagger.RateLimitLevel == RateLimitPolicyAPILevel {
		rlMethodDescriptorValue = RateLimitPolicyAPILevel
		isRLPolicyAvailable = true
	} else if swagger.RateLimitLevel == RateLimitPolicyOperationLevel {
		for _, operation := range resource.GetMethod() {
			if operation.RateLimitPolicy != "" {
				isRLPolicyAvailable = true
				rlMethodDescriptorValue = RateLimitPolicyOperationLevel
				break
			}
		}
	}
	params := &routeCreateParams{
		organizationID:      organizationID,
		apiUUID:             swagger.GetID(),
		title:               swagger.GetTitle(),
		apiType:             swagger.GetAPIType(),
		version:             swagger.GetVersion(),
		vHost:               vHost,
		xWSO2BasePath:       swagger.GetXWso2Basepath(),
		AuthHeader:          swagger.GetXWSO2AuthHeader(),
		prodClusterName:     prodClusterName,
		endpointBasePath:    endpointBasePath,
		corsPolicy:          swagger.GetCorsConfig(),
		resourcePathParam:   "",
		resourceMethods:     getDefaultResourceMethods(swagger.GetAPIType()),
		requestInterceptor:  requestInterceptor,
		responseInterceptor: responseInterceptor,
		rateLimitLevel:      rlMethodDescriptorValue,
		isRLPolicyAvailable: isRLPolicyAvailable,
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

func getSystemRouteName(apiContext string) string {
	return fmt.Sprintf("%s#%s", "system", apiContext)
}

func getRouteName(apiUUID string) string {
	// Get route name from this function incase
	// if we want to append something to the route name (eg: API context) in future.
	// So the route name would be <apiUUID>#<apiContext>
	return apiUUID
}
