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
package envoyCodegen

import (
	v2 "github.com/envoyproxy/go-control-plane/envoy/api/v2"
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	envoy_api_v2_endpoint "github.com/envoyproxy/go-control-plane/envoy/api/v2/endpoint"
	v2route "github.com/envoyproxy/go-control-plane/envoy/api/v2/route"
	envoy_type_matcher "github.com/envoyproxy/go-control-plane/envoy/type/matcher"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/config"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/models/apiDefinition"
	s "github.com/wso2/micro-gw/internal/pkg/oasparser/swaggerOperator"
	"log"
	"strings"
)

func CreateRoutesWithClusters(mgwSwagger apiDefinition.MgwSwagger) ([]*v2route.Route, []*v2.Cluster, []*core.Address, []*v2route.Route, []*v2.Cluster, []*core.Address) {
	var (
		routesProd           []*v2route.Route
		clustersProd         []*v2.Cluster
		endpointProd         []apiDefinition.Endpoint
		apiLevelEndpointProd []apiDefinition.Endpoint
		apilevelClusterProd  v2.Cluster
		cluster_refProd      string
		endpointsProd        []*core.Address

		routesSand           []*v2route.Route
		clustersSand         []*v2.Cluster
		endpointSand         []apiDefinition.Endpoint
		apiLevelEndpointSand []apiDefinition.Endpoint
		apilevelClusterSand  v2.Cluster
		cluster_refSand      string
		endpointsSand        []*core.Address
	)
	//check API level sandbox endpoints availble
	if s.IsEndpointsAvailable(mgwSwagger.GetSandEndpoints()) {
		apiLevelEndpointSand = mgwSwagger.GetSandEndpoints()
		apilevelAddressSand := createAddress(apiLevelEndpointSand[0].GetHost(), apiLevelEndpointSand[0].GetPort())
		apiLevelClusterNameS := strings.TrimSpace("clusterSand_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
		apilevelClusterSand = createCluster(apilevelAddressSand, apiLevelClusterNameS)
		clustersSand = append(clustersSand, &apilevelClusterSand)

		endpointsSand = append(endpointsSand, &apilevelAddressSand)
	}

	//check API level production endpoints available
	if s.IsEndpointsAvailable(mgwSwagger.GetProdEndpoints()) {
		apiLevelEndpointProd = mgwSwagger.GetProdEndpoints()
		apilevelAddressP := createAddress(apiLevelEndpointProd[0].GetHost(), apiLevelEndpointProd[0].GetPort())
		apiLevelClusterNameP := strings.TrimSpace("clusterProd_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion())
		apilevelClusterProd = createCluster(apilevelAddressP, apiLevelClusterNameP)
		clustersProd = append(clustersProd, &apilevelClusterProd)

		endpointsProd = append(endpointsProd, &apilevelAddressP)

	} else {
		log.Println("API level Producton endpoints are not defined")
	}

	for ind, resource := range mgwSwagger.GetResources() {

		//resource level check sandbox endpoints
		if s.IsEndpointsAvailable(resource.GetSandEndpoints()) {
			endpointSand = resource.GetSandEndpoints()
			addressSand := createAddress(endpointSand[0].GetHost(), endpointSand[0].GetPort())
			clusterNameSand := strings.TrimSpace("clusterSand_" + strings.Replace(resource.GetId(), " ", "", -1) + string(ind))
			clusterSand := createCluster(addressSand, clusterNameSand)
			clustersSand = append(clustersSand, &clusterSand)

			cluster_refSand = clusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(mgwSwagger.GetXWso2Basepath(), endpointSand[0], resource.GetPath(), cluster_refSand)
			routesSand = append(routesSand, &routeS)

			endpointsSand = append(endpointsSand, &addressSand)

			//API level check
		} else if s.IsEndpointsAvailable(mgwSwagger.GetSandEndpoints()) {
			endpointSand = apiLevelEndpointSand
			cluster_refSand = apilevelClusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(mgwSwagger.GetXWso2Basepath(), endpointSand[0], resource.GetPath(), cluster_refSand)
			routesSand = append(routesSand, &routeS)

		}

		//resource level check production endpoints
		if s.IsEndpointsAvailable(resource.GetProdEndpoints()) {
			endpointProd = resource.GetProdEndpoints()
			addressProd := createAddress(endpointProd[0].GetHost(), endpointProd[0].GetPort())
			clusterNameProd := strings.TrimSpace("clusterProd_" + strings.Replace(resource.GetId(), " ", "", -1) + string(ind))
			clusterProd := createCluster(addressProd, clusterNameProd)

			clustersProd = append(clustersProd, &clusterProd)

			cluster_refProd = clusterProd.GetName()

			//production endpoints
			routeP := createRoute(mgwSwagger.GetXWso2Basepath(), endpointProd[0], resource.GetPath(), cluster_refProd)
			routesProd = append(routesProd, &routeP)

			endpointsProd = append(endpointsProd, &addressProd)

			//API level check
		} else if s.IsEndpointsAvailable(mgwSwagger.GetProdEndpoints()) {
			endpointProd = apiLevelEndpointProd
			cluster_refProd = apilevelClusterProd.GetName()

			//production endpoints
			routeP := createRoute(mgwSwagger.GetXWso2Basepath(), endpointProd[0], resource.GetPath(), cluster_refProd)
			routesProd = append(routesProd, &routeP)

		} else {
			log.Panic("Producton endpoints are not defined")
		}
	}

	return routesProd, clustersProd, endpointsProd, routesSand, clustersSand, endpointsSand
}

func createCluster(address core.Address, clusterName string) v2.Cluster {

	h := &address
	cluster := v2.Cluster{
		Name:                 clusterName,
		ConnectTimeout:       ptypes.DurationProto(config.CLUSTER_CONNECT_TIMEOUT),
		ClusterDiscoveryType: &v2.Cluster_Type{Type: v2.Cluster_STRICT_DNS},
		DnsLookupFamily:      v2.Cluster_V4_ONLY,
		LbPolicy:             v2.Cluster_ROUND_ROBIN,
		LoadAssignment: &v2.ClusterLoadAssignment{
			ClusterName: clusterName,
			Endpoints: []*envoy_api_v2_endpoint.LocalityLbEndpoints{
				{
					LbEndpoints: []*envoy_api_v2_endpoint.LbEndpoint{
						{
							HostIdentifier: &envoy_api_v2_endpoint.LbEndpoint_Endpoint{
								Endpoint: &envoy_api_v2_endpoint.Endpoint{
									Address: h,
								},
							},
						},
					},
				},
			},
		},
	}
	//fmt.Println(h.GetAddress())
	return cluster
}

func createRoute(xWso2Basepath string,endpoint apiDefinition.Endpoint, resourcePath string, clusterName string) v2route.Route {
	var (
		route v2route.Route
		action *v2route.Route_Route
		match *v2route.RouteMatch
	)

	routePath,rewritePath, isHavingPathparameter := GenerateRoutePaths(xWso2Basepath,endpoint.GetBasepath(), resourcePath)

	if isHavingPathparameter {
		match = &v2route.RouteMatch{
			PathSpecifier: &v2route.RouteMatch_SafeRegex{
				SafeRegex: &envoy_type_matcher.RegexMatcher{
					EngineType: &envoy_type_matcher.RegexMatcher_GoogleRe2{
						GoogleRe2: &envoy_type_matcher.RegexMatcher_GoogleRE2{
							MaxProgramSize: nil,
						},
					},
					Regex: routePath,
				},
			},
		}

		if xWso2Basepath != "" {
			action = &v2route.Route_Route{
				Route: &v2route.RouteAction{
					HostRewriteSpecifier: &v2route.RouteAction_HostRewrite{
						HostRewrite: endpoint.GetHost(),
					},
					RegexRewrite: &envoy_type_matcher.RegexMatchAndSubstitute{
						Pattern:              &envoy_type_matcher.RegexMatcher{
							EngineType: &envoy_type_matcher.RegexMatcher_GoogleRe2{
								GoogleRe2: &envoy_type_matcher.RegexMatcher_GoogleRE2{
									MaxProgramSize: nil,
								},
							},
							Regex: xWso2Basepath,
						},
						Substitution: endpoint.GetBasepath(),
					},
					ClusterSpecifier: &v2route.RouteAction_Cluster{
						Cluster: clusterName,
					},
				},
			}
		} else {
			action =  &v2route.Route_Route{
				Route: &v2route.RouteAction{
					HostRewriteSpecifier: &v2route.RouteAction_HostRewrite{
						HostRewrite: endpoint.GetHost(),
					},
					ClusterSpecifier: &v2route.RouteAction_Cluster{
						Cluster: clusterName,
					},
				},
			}
		}

	} else {
		match = &v2route.RouteMatch{
			PathSpecifier: &v2route.RouteMatch_Prefix{routePath},
		}
		if xWso2Basepath != "" {
			action = &v2route.Route_Route{
				Route: &v2route.RouteAction{
					HostRewriteSpecifier: &v2route.RouteAction_HostRewrite{
						HostRewrite: endpoint.GetHost(),
					},
					PrefixRewrite: rewritePath,
					ClusterSpecifier: &v2route.RouteAction_Cluster{
						Cluster: clusterName,
					},
				},
			}
		} else {
			action = &v2route.Route_Route{
				Route: &v2route.RouteAction{
					HostRewriteSpecifier: &v2route.RouteAction_HostRewrite{
						HostRewrite: endpoint.GetHost(),
					},
					ClusterSpecifier: &v2route.RouteAction_Cluster{
						Cluster: clusterName,
					},
				},
			}
		}

	}

	route = v2route.Route{
		Match: match,
		Action: action,
		Metadata: nil,
	}

	//fmt.Println(endpoint.GetHost(), rewritePath, routePath)
	return route
}

//generates route paths for the api resources
func GenerateRoutePaths(xWso2Basepath string, basePath string, resourcePath string) (string, string, bool) {
	newPath := ""
	rewritePath, isHavingPathparameters := GenerateRegex(basePath + resourcePath)
	if xWso2Basepath != "" {
		fullpath := xWso2Basepath + resourcePath
		newPath, _ = GenerateRegex(fullpath)

	} else {
		fullpath := basePath + resourcePath
		newPath, _ = GenerateRegex(fullpath)
	}

	return newPath, rewritePath, isHavingPathparameters
}

//generates regex for the resources which have path paramaters.
func GenerateRegex(fullpath string) (string, bool) {
	isHavingPathparameters := true
	regex := "([^/]+)"
	newPath := ""

	if strings.Contains(fullpath, "{") || strings.Contains(fullpath, "}") {
		res1 := strings.Split(fullpath, "/")

		for i, p := range res1 {
			if strings.Contains(p, "{") || strings.Contains(p, "}"){
				res1[i] = regex
			}
		}
		newPath = "^" + strings.Join(res1[:], "/") + "$"

	} else {
		newPath = fullpath
		isHavingPathparameters = false
	}
	return newPath, isHavingPathparameters
}