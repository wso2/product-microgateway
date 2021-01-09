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
	//endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	//tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/any"
	"google.golang.org/protobuf/types/known/wrapperspb"

	//"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/model"

	"strings"
	//"time"

	"github.com/golang/protobuf/proto"
	//"github.com/golang/protobuf/ptypes"
)

//CreateRouteWithClustersWebSocket creates clusters and routes for web socket APIs
func CreateRouteWithClustersWebSocket(mgwSwagger model.MgwSwagger, upstreamCerts []byte) (routesP []*routev3.Route,
	clustersP []*clusterv3.Cluster, addressesP []*corev3.Address) {

	var (
		routesProd []*routev3.Route
		clusters   []*clusterv3.Cluster
		endpoints  []*corev3.Address

		endpointProd    []model.Endpoint
		clusterProd     *clusterv3.Cluster
		clusterNameProd string

		endpointSand    []model.Endpoint
		clusterSand     *clusterv3.Cluster
		clusterNameSand string
	)

	apiEndpointBasePath := ""

	apiTitle := mgwSwagger.GetTitle()
	apiVersion := mgwSwagger.GetVersion()
	apiBasePath := mgwSwagger.GetXWso2Basepath()

	if len(mgwSwagger.GetProdEndpoints()) > 0 {
		endpointProd = mgwSwagger.GetProdEndpoints()
		addressProd := createAddress(endpointProd[0].Host, endpointProd[0].Port)
		clusterNameProd = strings.TrimSpace(prodClustersConfigNamePrefix +
			strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
		clusterProd = createCluster(addressProd, clusterNameProd, endpointProd[0].URLType, upstreamCerts)
		clusters = append(clusters, clusterProd)
		endpoints = append(endpoints, addressProd)
		apiEndpointBasePath = endpointProd[0].Basepath
	} else {
		logger.LoggerOasparser.Warn("Production endpoints are not defined")
	}

	if len(mgwSwagger.GetSandEndpoints()) > 0 {
		endpointSand = mgwSwagger.GetSandEndpoints()
		addressSand := createAddress(endpointSand[0].Host, endpointSand[0].Port)
		clusterNameSand = strings.TrimSpace(sandClustersConfigNamePrefix +
			strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
		clusterSand = createCluster(addressSand, clusterNameSand, endpointSand[0].URLType, upstreamCerts)
		clusters = append(clusters, clusterSand)
		endpoints = append(endpoints, addressSand)
	}

	route := createRouteWebSocket(apiTitle, apiBasePath, apiVersion, apiEndpointBasePath, clusterNameProd, clusterNameSand)
	routesProd = append(routesProd, route)
	return routesProd, clusters, endpoints
}

func createRouteWebSocket(title string, xWso2BasePath string, version string,
	endPointBasePath string, prodClusterName string, sandClusterName string) *routev3.Route {
	logger.LoggerOasparser.Debug("creating a route for web socket...")

	var (
		router    routev3.Route
		action    *routev3.Route_Route
		match     *routev3.RouteMatch
		decorator *routev3.Decorator
	)
	methodArr := []string{"GET"}
	headerMatcherArray := routev3.HeaderMatcher{
		Name: httpMethodHeader,
		HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
			SafeRegexMatch: &envoy_type_matcherv3.RegexMatcher{
				EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
					GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
						MaxProgramSize: nil,
					},
				},
				Regex: "^(" + strings.Join(methodArr, "|") + ")$",
			},
		},
	}
	routePath := generateRoutePaths(xWso2BasePath, endPointBasePath, "")

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

	decorator = &routev3.Decorator{
		Operation: xWso2BasePath,
	}

	var contextExtensions = make(map[string]string)
	contextExtensions[pathContextExtension] = endPointBasePath
	if xWso2BasePath != "" {
		contextExtensions[basePathContextExtension] = xWso2BasePath
	} else {
		contextExtensions[basePathContextExtension] = endPointBasePath
	}
	contextExtensions[methodContextExtension] = strings.Join(methodArr, " ")
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

	if xWso2BasePath != "" {
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
						Regex: xWso2BasePath,
					},
					Substitution: endPointBasePath,
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
	logger.LoggerOasparser.Debug("adding route ", endPointBasePath)

	router = routev3.Route{
		Name:      xWso2BasePath, //Categorize routes with same base path
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
