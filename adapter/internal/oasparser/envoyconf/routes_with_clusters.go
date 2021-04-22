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
	"net"
	"regexp"

	"google.golang.org/protobuf/types/known/durationpb"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/any"
	"github.com/golang/protobuf/ptypes/wrappers"
	mgw "github.com/wso2/adapter/internal/oasparser/model"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/wso2/adapter/config"
	"github.com/wso2/adapter/internal/oasparser/model"
	"github.com/wso2/adapter/internal/svcdiscovery"
	logger "github.com/wso2/adapter/loggers"

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
func CreateRoutesWithClusters(mgwSwagger model.MgwSwagger, upstreamCerts []byte, vHost string) (routesP []*routev3.Route,
	clustersP []*clusterv3.Cluster, addressesP []*corev3.Address) {
	var (
		routesProd []*routev3.Route
		clusters   []*clusterv3.Cluster
		endpoints  []*corev3.Address

		apiLevelEndpointProd    []model.Endpoint
		apilevelClusterProd     *clusterv3.Cluster
		apiLevelClusterNameProd string

		apiLevelEndpointSand    []model.Endpoint
		apilevelClusterSand     *clusterv3.Cluster
		apiLevelClusterNameSand string
	)
	// To keep track of API Level production endpoint basePath
	apiEndpointBasePath := ""

	apiTitle := mgwSwagger.GetTitle()
	apiVersion := mgwSwagger.GetVersion()

	// check API level production endpoints available
	if len(mgwSwagger.GetProdEndpoints()) > 0 {
		apiLevelEndpointProd = mgwSwagger.GetProdEndpoints()
		apilevelAddressP := createAddress(apiLevelEndpointProd[0].Host, apiLevelEndpointProd[0].Port)
		apiLevelClusterNameProd = strings.TrimSpace(prodClustersConfigNamePrefix + vHost + "_" +
			strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
		apilevelClusterProd = createCluster(apilevelAddressP, apiLevelClusterNameProd, apiLevelEndpointProd[0].URLType,
			upstreamCerts)
		clusters = append(clusters, apilevelClusterProd)
		endpoints = append(endpoints, apilevelAddressP)
		apiEndpointBasePath = apiLevelEndpointProd[0].Basepath

		if apiLevelEndpointProd[0].ServiceDiscoveryString != "" {
			//add the api level cluster name to the ClusterConsulKeyMap
			svcdiscovery.ClusterConsulKeyMap[apiLevelClusterNameProd] = apiLevelEndpointProd[0].ServiceDiscoveryString
			logger.LoggerOasparser.Debugln("Consul cluster added for API level Production: ", apiLevelClusterNameProd, " ",
				apiLevelEndpointProd[0].ServiceDiscoveryString)
		}
	} else {
		logger.LoggerOasparser.Warn("API level Producton endpoints are not defined")
	}

	// check API level sandbox endpoints availble
	if len(mgwSwagger.GetSandEndpoints()) > 0 {
		apiLevelEndpointSand = mgwSwagger.GetSandEndpoints()
		if apiEndpointBasePath != apiLevelEndpointSand[0].Basepath && len(apiLevelEndpointProd) > 0 {
			logger.LoggerOasparser.Warnf("Sandbox API level endpoint basepath is different compared to API level production endpoint "+
				"for the API %v:%v. Hence Sandbox endpoints are not applied", apiTitle, apiVersion)
		} else {
			apilevelAddressSand := createAddress(apiLevelEndpointSand[0].Host, apiLevelEndpointSand[0].Port)
			apiLevelClusterNameSand = strings.TrimSpace(sandClustersConfigNamePrefix + vHost + "_" +
				strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
			apilevelClusterSand = createCluster(apilevelAddressSand, apiLevelClusterNameSand, apiLevelEndpointSand[0].URLType,
				upstreamCerts)
			clusters = append(clusters, apilevelClusterSand)
			endpoints = append(endpoints, apilevelAddressSand)
			if apiLevelEndpointSand[0].ServiceDiscoveryString != "" {
				//add the api level cluster name to the ClusterConsulKeyMap
				svcdiscovery.ClusterConsulKeyMap[apiLevelClusterNameSand] = apiLevelEndpointSand[0].ServiceDiscoveryString
				logger.LoggerOasparser.Debugln("Consul cluster added for API level Sandbox: ", apiLevelClusterNameSand, " ",
					apiLevelEndpointSand[0].ServiceDiscoveryString)
			}
		}
	}

	for _, resource := range mgwSwagger.GetResources() {

		clusterRefSand := ""
		clusterRefProd := ""
		// The upstream endpoint's basepath.
		// The production endpoint's basepath is set. The developer has to stick
		// into the same basePath when using the sandbox endpoint.
		// TODO: (VirajSalaka) Finalize whether to proceed with this limitation.
		endpointBasepath := ""

		// resource level check production endpoints
		if len(resource.GetProdEndpoints()) > 0 {
			endpointProd := resource.GetProdEndpoints()
			addressProd := createAddress(endpointProd[0].Host, endpointProd[0].Port)
			// TODO: (VirajSalaka) 0 is hardcoded as only one endpoint is supported at the moment
			clusterNameProd := strings.TrimSpace(apiLevelClusterNameProd + "_" + strings.Replace(resource.GetID(), " ", "", -1) +
				"0")
			clusterProd := createCluster(addressProd, clusterNameProd, endpointProd[0].URLType, upstreamCerts)
			clusters = append(clusters, clusterProd)
			clusterRefProd = clusterProd.GetName()
			endpoints = append(endpoints, addressProd)
			endpointBasepath = endpointProd[0].Basepath

			//add to ClusterConsulKeyMap: resource level prod endpoints
			if endpointProd[0].ServiceDiscoveryString != "" {
				svcdiscovery.ClusterConsulKeyMap[clusterNameProd] = endpointProd[0].ServiceDiscoveryString
				logger.LoggerOasparser.Debugln("Consul cluster added for Resource level Production:", clusterNameProd, " ",
					endpointProd[0].ServiceDiscoveryString)
			}

			// API level check
		} else if len(mgwSwagger.GetProdEndpoints()) > 0 {
			clusterRefProd = apilevelClusterProd.GetName()
			endpointBasepath = apiLevelEndpointProd[0].Basepath

		} else {
			logger.LoggerOasparser.Warnf("Production environment endpoints are not available for the resource %v:%v-%v",
				apiTitle, apiVersion, resource.GetPath())
		}

		// resource level check sandbox endpoints
		if len(resource.GetSandEndpoints()) > 0 {
			endpointSand := resource.GetSandEndpoints()
			addressSand := createAddress(endpointSand[0].Host, endpointSand[0].Port)
			// TODO: (VirajSalaka) 0 is hardcoded as only one endpoint is supported at the moment
			clusterNameSand := strings.TrimSpace(apiLevelClusterNameSand + "_" + strings.Replace(resource.GetID(), " ", "", -1) +
				"0")
			if endpointBasepath != endpointSand[0].Basepath {
				logger.LoggerOasparser.Warnf("Sandbox endpoint basepath is different compared to production endpoint "+
					"for the resource %v:%v-%v. Hence Sandbox endpoints are not applied", apiTitle, apiVersion, resource.GetPath())
			} else {
				// sandbox cluster is not created if the basepath component of the endpoint is different compared to production
				// endpoints
				clusterSand := createCluster(addressSand, clusterNameSand, endpointSand[0].URLType, upstreamCerts)
				clusters = append(clusters, clusterSand)
				endpoints = append(endpoints, addressSand)
				clusterRefSand = clusterSand.GetName()

				//add to ClusterConsulKeyMap: resource level sand endpoints
				if endpointSand[0].ServiceDiscoveryString != "" {
					svcdiscovery.ClusterConsulKeyMap[clusterNameSand] = endpointSand[0].ServiceDiscoveryString
					logger.LoggerOasparser.Debugln("Consul cluster added for API level Sandbox:", clusterNameSand, " ",
						endpointSand[0].ServiceDiscoveryString)
				}
			}

			// API level check
			// Due to endpoint basePath restriction, the apiLevelEndpointSand may not be initialized.
		} else if len(mgwSwagger.GetSandEndpoints()) > 0 || apiLevelEndpointSand != nil {
			endpointSand := apiLevelEndpointSand
			if endpointBasepath != endpointSand[0].Basepath && clusterRefProd != "" {
				logger.LoggerOasparser.Warnf("Sandbox endpoint basepath of API is different compared to production endpoint "+
					"for the resource %v:%v-%v. Hence Sandbox endpoints are not applied", apiTitle, apiVersion, resource.GetPath())
			} else {
				clusterRefSand = apilevelClusterSand.GetName()
				endpointBasepath = endpointSand[0].Basepath
			}
		} else {
			logger.LoggerOasparser.Debugf("Sandbox environment endpoints are not available for the resource %v:%v-%v",
				apiTitle, apiVersion, resource.GetPath())
		}

		routeP := createRoute(genRouteCreateParams(&mgwSwagger, &resource, vHost, endpointBasepath, clusterRefProd, clusterRefSand))
		routesProd = append(routesProd, routeP)
	}
	if mgwSwagger.GetAPIType() == mgw.WS {
		routesP := createRoute(genRouteCreateParams(&mgwSwagger, nil, vHost, apiEndpointBasePath, apilevelClusterProd.GetName(),
			apilevelClusterSand.GetName()))
		routesProd = append(routesProd, routesP)
	}
	return routesProd, clusters, endpoints
}

// createCluster creates cluster configuration. AddressConfiguration, cluster name and
// urlType (http or https) is required to be provided.
func createCluster(address *corev3.Address, clusterName string, urlType string, upstreamCerts []byte) *clusterv3.Cluster {
	logger.LoggerOasparser.Debug("creating a cluster....")
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}

	cluster := clusterv3.Cluster{
		Name:                 clusterName,
		ConnectTimeout:       ptypes.DurationProto(conf.Envoy.ClusterTimeoutInSeconds * time.Second),
		ClusterDiscoveryType: &clusterv3.Cluster_Type{Type: clusterv3.Cluster_STRICT_DNS},
		DnsLookupFamily:      clusterv3.Cluster_V4_ONLY,
		LbPolicy:             clusterv3.Cluster_ROUND_ROBIN,
		LoadAssignment: &endpointv3.ClusterLoadAssignment{
			ClusterName: clusterName,
			Endpoints: []*endpointv3.LocalityLbEndpoints{
				{
					LbEndpoints: []*endpointv3.LbEndpoint{
						{
							HostIdentifier: &endpointv3.LbEndpoint_Endpoint{
								Endpoint: &endpointv3.Endpoint{
									Address: address,
								},
							},
						},
					},
				},
			},
		},
	}
	if strings.HasPrefix(urlType, httpsURLType) || strings.HasPrefix(urlType, wssURLType) {
		upstreamtlsContext := createUpstreamTLSContext(upstreamCerts, address)
		marshalledTLSContext, err := ptypes.MarshalAny(upstreamtlsContext)
		if err != nil {
			logger.LoggerOasparser.Error("Internal Error while marshalling the upstream TLS Context.")
		} else {
			upstreamTransportSocket := &corev3.TransportSocket{
				Name: transportSocketName,
				ConfigType: &corev3.TransportSocket_TypedConfig{
					TypedConfig: marshalledTLSContext,
				},
			}
			cluster.TransportSocket = upstreamTransportSocket
		}
	}
	return &cluster
}

func createUpstreamTLSContext(upstreamCerts []byte, address *corev3.Address) *tlsv3.UpstreamTlsContext {
	conf, errReadConfig := config.ReadConfigs()
	//TODO: (VirajSalaka) Error Handling
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
		return nil
	}
	tlsCert := generateTLSCert(conf.Envoy.KeyStore.PrivateKeyLocation, conf.Envoy.KeyStore.PublicKeyLocation)
	// Convert the cipher string to a string array
	ciphersArray := strings.Split(conf.Envoy.Upstream.TLS.Ciphers, ",")
	for i := range ciphersArray {
		ciphersArray[i] = strings.TrimSpace(ciphersArray[i])
	}

	upstreamTLSContext := &tlsv3.UpstreamTlsContext{
		CommonTlsContext: &tlsv3.CommonTlsContext{
			TlsParams: &tlsv3.TlsParameters{
				TlsMinimumProtocolVersion: createTLSProtocolVersion(conf.Envoy.Upstream.TLS.MinVersion),
				TlsMaximumProtocolVersion: createTLSProtocolVersion(conf.Envoy.Upstream.TLS.MaxVersion),
				CipherSuites:              ciphersArray,
			},
			TlsCertificates: []*tlsv3.TlsCertificate{tlsCert},
		},
	}

	// Sni should be assigned when there is a hostname
	if net.ParseIP(address.GetSocketAddress().GetAddress()) == nil {
		upstreamTLSContext.Sni = address.GetSocketAddress().GetAddress()
	}

	if !conf.Envoy.Upstream.TLS.DisableSSLVerification {
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
					Filename: conf.Envoy.Upstream.TLS.CACrtPath,
				},
			}
		}

		upstreamTLSContext.CommonTlsContext.ValidationContextType = &tlsv3.CommonTlsContext_ValidationContext{
			ValidationContext: &tlsv3.CertificateValidationContext{
				TrustedCa: trustedCASrc,
			},
		}
	}

	if conf.Envoy.Upstream.TLS.VerifyHostName && !conf.Envoy.Upstream.TLS.DisableSSLVerification {
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
	endpointBasepath := params.endpointBasePath

	logger.LoggerOasparser.Debug("creating a route....")
	var (
		router       routev3.Route
		action       *routev3.Route_Route
		match        *routev3.RouteMatch
		decorator    *routev3.Decorator
		resourcePath string
	)

	// OPTIONS is always added even if it is not listed under resources
	// This is required to handle CORS preflight request fail scenario
	methodRegex := strings.Join(resourceMethods, "|")
	if !strings.Contains(methodRegex, "OPTIONS") {
		methodRegex = methodRegex + "|OPTIONS"
	}
	headerMatcherArray := routev3.HeaderMatcher{
		Name: httpMethodHeader,
		HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
			SafeRegexMatch: &envoy_type_matcherv3.RegexMatcher{
				EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
					GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
						MaxProgramSize: nil,
					},
				},
				Regex: "^(" + methodRegex + ")$",
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
	if apiType == mgw.WS {
		decorator = &routev3.Decorator{
			Operation: endpointBasepath,
		}
	} else if apiType == mgw.HTTP {
		decorator = &routev3.Decorator{
			Operation: resourcePath,
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
	contextExtensions["prodClusterName"] = prodClusterName
	contextExtensions["sandClusterName"] = sandClusterName

	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_CheckSettings{
			CheckSettings: &extAuthService.CheckSettings{
				ContextExtensions: contextExtensions,
			},
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&perFilterConfig)
	filter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}

	// if xWso2Basepath is not different compared to endpointBasepath, no need to substitute.
	if xWso2Basepath != "" && xWso2Basepath != endpointBasepath {
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
			},
		}
	} else {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
			},
		}
	}

	if prodClusterName != "" && sandClusterName != "" {
		headerBasedClusterSpecifier := &routev3.RouteAction_ClusterHeader{
			ClusterHeader: clusterHeaderName,
		}
		action.Route.ClusterSpecifier = headerBasedClusterSpecifier
		logger.LoggerOasparser.Debug("adding cluster header")
	} else if prodClusterName != "" {
		directClusterSpecifier := &routev3.RouteAction_Cluster{
			Cluster: prodClusterName,
		}
		action.Route.ClusterSpecifier = directClusterSpecifier
		logger.LoggerOasparser.Debugf("adding cluster: %v", prodClusterName)
	} else {
		directClusterSpecifier := &routev3.RouteAction_Cluster{
			Cluster: sandClusterName,
		}
		action.Route.ClusterSpecifier = directClusterSpecifier
		logger.LoggerOasparser.Debugf("adding cluster: %v", sandClusterName)
	}

	if corsPolicy != nil {
		action.Route.Cors = corsPolicy
	}

	logger.LoggerOasparser.Debug("adding route ", resourcePath)
	router = routev3.Route{
		Name:      xWso2Basepath, //Categorize routes with same base path
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

	if strings.Contains(fullpath, "{") && strings.Contains(fullpath, "}") {
		res1 := strings.Split(fullpath, "/")

		for i, p := range res1 {
			if strings.Contains(p, "{") && strings.Contains(p, "}") {
				startP := strings.Index(p, "{")
				endP := strings.Index(p, "}")
				res1[i] = p[:startP] + pathParaRegex + p[endP+1:]
			}
		}
		newPath = strings.Join(res1[:], "/")

	} else {
		newPath = fullpath
	}

	if strings.HasSuffix(newPath, "/*") {
		newPath = strings.TrimSuffix(newPath, "/*") + wildCardRegex
	}
	return "^" + newPath + endRegex + "$"
}

func getUpgradeConfig(apiType string) []*routev3.RouteAction_UpgradeConfig {
	var upgradeConfig []*routev3.RouteAction_UpgradeConfig
	if apiType == mgw.WS {
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
	prodClusterName string, sandClusterName string) *routeCreateParams {
	params := &routeCreateParams{
		title:             swagger.GetTitle(),
		apiType:           swagger.GetAPIType(),
		version:           swagger.GetVersion(),
		vHost:             vHost,
		xWSO2BasePath:     swagger.GetXWso2Basepath(),
		AuthHeader:        swagger.GetXWSO2AuthHeader(),
		prodClusterName:   prodClusterName,
		sandClusterName:   sandClusterName,
		endpointBasePath:  endpointBasePath,
		corsPolicy:        swagger.GetCorsConfig(),
		resourcePathParam: "",
		resourceMethods:   getDefaultResourceMethods(swagger.GetAPIType()),
	}

	if resource != nil {
		params.resourceMethods = resource.GetMethodList()
		params.resourcePathParam = resource.GetPath()
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
	if apiType == mgw.WS {
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
	if apiType == mgw.WS {
		defaultResourceMethods = []string{"GET"}
	}
	return defaultResourceMethods
}
