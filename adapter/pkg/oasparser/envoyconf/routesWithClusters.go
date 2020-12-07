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
	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/golang/protobuf/ptypes/any"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/model"

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
func CreateRoutesWithClusters(mgwSwagger model.MgwSwagger) (routesP []*routev3.Route,
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
	apiBasePath := mgwSwagger.GetXWso2Basepath()

	// check API level production endpoints available
	if len(mgwSwagger.GetProdEndpoints()) > 0 {
		apiLevelEndpointProd = mgwSwagger.GetProdEndpoints()
		apilevelAddressP := createAddress(apiLevelEndpointProd[0].Host, apiLevelEndpointProd[0].Port)
		apiLevelClusterNameProd = strings.TrimSpace(prodClustersConfigNamePrefix +
			strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
		apilevelClusterProd = createCluster(apilevelAddressP, apiLevelClusterNameProd, apiLevelEndpointProd[0].URLType)
		clusters = append(clusters, apilevelClusterProd)
		endpoints = append(endpoints, apilevelAddressP)
		apiEndpointBasePath = apiLevelEndpointProd[0].Basepath

	} else {
		logger.LoggerOasparser.Warn("API level Producton endpoints are not defined")
	}

	// check API level sandbox endpoints availble
	if len(mgwSwagger.GetSandEndpoints()) > 0 {
		apiLevelEndpointSand = mgwSwagger.GetSandEndpoints()
		if apiEndpointBasePath != apiLevelEndpointSand[0].Basepath {
			logger.LoggerOasparser.Warnf("Sandbox API level endpoint basepath is different compared to API level production endpoint "+
				"for the API %v:%v. Hence Sandbox endpoints are not applied", apiTitle, apiVersion)
		} else {
			apilevelAddressSand := createAddress(apiLevelEndpointSand[0].Host, apiLevelEndpointSand[0].Port)
			apiLevelClusterNameSand = strings.TrimSpace(sandClustersConfigNamePrefix +
				strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
			apilevelClusterSand = createCluster(apilevelAddressSand, apiLevelClusterNameSand, apiLevelEndpointSand[0].URLType)
			clusters = append(clusters, apilevelClusterSand)
			endpoints = append(endpoints, apilevelAddressSand)
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
			clusterProd := createCluster(addressProd, clusterNameProd, endpointProd[0].URLType)
			clusters = append(clusters, clusterProd)
			clusterRefProd = clusterProd.GetName()
			endpoints = append(endpoints, addressProd)
			endpointBasepath = endpointProd[0].Basepath

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
				clusterSand := createCluster(addressSand, clusterNameSand, endpointSand[0].URLType)
				clusters = append(clusters, clusterSand)
				endpoints = append(endpoints, addressSand)
				clusterRefSand = clusterSand.GetName()
			}

			// API level check
			// Due to endpoint basePath restriction, the apiLevelEndpointSand may not be initialized.
		} else if len(mgwSwagger.GetSandEndpoints()) > 0 || apiLevelEndpointSand != nil {
			endpointSand := apiLevelEndpointSand
			if endpointBasepath != endpointSand[0].Basepath {
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

		routeP := createRoute(apiTitle, apiBasePath, apiVersion, endpointBasepath, resource, clusterRefProd, clusterRefSand)
		routesProd = append(routesProd, routeP)
	}
	return routesProd, clusters, endpoints
}

// createCluster creates cluster configuration. AddressConfiguration, cluster name and
// urlType (http or https) is required to be provided.
func createCluster(address *corev3.Address, clusterName string, urlType string) *clusterv3.Cluster {
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
	if strings.HasPrefix(urlType, httpsURLType) {
		upstreamtlsContext := &tlsv3.UpstreamTlsContext{
			CommonTlsContext: &tlsv3.CommonTlsContext{
				ValidationContextType: &tlsv3.CommonTlsContext_ValidationContext{
					ValidationContext: &tlsv3.CertificateValidationContext{
						TrustedCa: &corev3.DataSource{
							Specifier: &corev3.DataSource_Filename{
								Filename: defaultCACertPath,
							},
						},
					},
				},
			},
		}
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

// createRoute creates route elements for the route configurations. API title, xWso2Basepath, API version,
// endpoint's basePath, resource Object (Microgateway's internal representation), production clusterName and
// sandbox clusterName needs to be provided.
func createRoute(title string, xWso2Basepath string, version string, endpointBasepath string,
	resource model.Resource, prodClusterName string, sandClusterName string) *routev3.Route {

	logger.LoggerOasparser.Debug("creating a route....")
	var (
		router       routev3.Route
		action       *routev3.Route_Route
		match        *routev3.RouteMatch
		decorator    *routev3.Decorator
		resourcePath string
	)
	headerMatcherArray := routev3.HeaderMatcher{
		Name: httpMethodHeader,
		HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
			SafeRegexMatch: &envoy_type_matcherv3.RegexMatcher{
				EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
					GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
						MaxProgramSize: nil,
					},
				},
				Regex: "^(" + strings.Join(resource.GetMethod(), "|") + ")$",
			},
		},
	}
	resourcePath = resource.GetPath()
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
	// var headerBasedClusterSpecifier *routev3.RouteAction_ClusterHeader
	// var directClusterSpecifier *routev3.RouteAction_Cluster

	decorator = &routev3.Decorator{
		Operation: resourcePath,
	}
	var contextExtensions = make(map[string]string)
	contextExtensions[pathContextExtension] = resourcePath
	if xWso2Basepath != "" {
		contextExtensions[basePathContextExtension] = xWso2Basepath
	} else {
		contextExtensions[basePathContextExtension] = endpointBasepath
	}
	contextExtensions[methodContextExtension] = strings.Join(resource.GetMethod(), " ")
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
			},
		}
	} else {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
			},
		}
	}

	//TODO: (VirajSalaka) Introduce a separate function
	if !(prodClusterName == "" || sandClusterName == "") {
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
	logger.LoggerOasparser.Debug("adding route ", resourcePath)

	router = routev3.Route{
		Name:      xWso2Basepath, //Categorize routes with same base path
		Match:     match,
		Action:    action,
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			extAuthzPerRouteName: filter,
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
	endRegex := "(\\?([^/]+))?"
	newPath := ""

	if strings.Contains(fullpath, "{") || strings.Contains(fullpath, "}") {
		res1 := strings.Split(fullpath, "/")

		for i, p := range res1 {
			if strings.Contains(p, "{") || strings.Contains(p, "}") {
				res1[i] = pathParaRegex
			}
		}
		newPath = "^" + strings.Join(res1[:], "/") + endRegex + "$"

	} else {
		newPath = "^" + fullpath + endRegex + "$"
	}
	return newPath
}
