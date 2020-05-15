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
	"fmt"
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
		clusterProd          v2.Cluster
		apilevelClusterProd  v2.Cluster
		clusterNameProd      string
		addressProd          core.Address
		cluster_refProd      string
		endpointsProd        []*core.Address

		routesSand           []*v2route.Route
		clustersSand         []*v2.Cluster
		endpointSand         []apiDefinition.Endpoint
		apiLevelEndpointSand []apiDefinition.Endpoint
		clusterSand          v2.Cluster
		apilevelClusterSand  v2.Cluster
		clusterNameSand      string
		addressSand          core.Address
		cluster_refSand      string
		endpointsSand        []*core.Address
	)
	//check API level sandbox endpoints availble
	if s.IsEndpointsAvailable(mgwSwagger.GetSandEndpoints()) {
		apiLevelEndpointSand = mgwSwagger.GetSandEndpoints()
		apilevelAddressSand := createAddress(apiLevelEndpointSand[0].Host, apiLevelEndpointSand[0].Port)
		apiLevelClusterNameS := "clusterSand_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion()
		apilevelClusterSand = createCluster(apilevelAddressSand, apiLevelClusterNameS)
		clustersSand = append(clustersSand, &apilevelClusterSand)

		endpointsSand = append(endpointsSand, &apilevelAddressSand)
	}

	//check API level production endpoints available
	if s.IsEndpointsAvailable(mgwSwagger.GetProdEndpoints()) {
		apiLevelEndpointProd = mgwSwagger.GetProdEndpoints()
		apilevelAddressP := createAddress(apiLevelEndpointProd[0].Host, apiLevelEndpointProd[0].Port)
		apiLevelClusterNameP := "clusterProd_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) + mgwSwagger.GetVersion()
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
			addressSand = createAddress(endpointSand[0].Host, endpointSand[0].Port)
			clusterNameSand = "clusterSand_" + strings.Replace(resource.GetId(), " ", "", -1) + string(ind)
			clusterSand = createCluster(addressSand, clusterNameSand)
			clustersSand = append(clustersSand, &clusterSand)

			cluster_refSand = clusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(endpointSand[0].Host,endpointSand[0].Basepath, resource.GetPath(), cluster_refSand)
			routesSand = append(routesSand, &routeS)

			endpointsSand = append(endpointsSand, &addressSand)

			//API level check
		} else if s.IsEndpointsAvailable(mgwSwagger.GetSandEndpoints()) {
			endpointSand = apiLevelEndpointSand
			cluster_refSand = apilevelClusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(endpointSand[0].Host,endpointSand[0].Basepath, resource.GetPath(), cluster_refSand)
			routesSand = append(routesSand, &routeS)

		}

		//resource level check production endpoints
		if s.IsEndpointsAvailable(resource.GetProdEndpoints()) {
			endpointProd = resource.GetProdEndpoints()
			addressProd = createAddress(endpointProd[0].Host, endpointProd[0].Port)
			clusterNameProd = "clusterProd_" + strings.Replace(resource.GetId(), " ", "", -1) + string(ind)
			clusterProd = createCluster(addressProd, clusterNameProd)
			clustersProd = append(clustersProd, &clusterProd)

			cluster_refProd = clusterProd.GetName()

			//production endpoints
			routeP := createRoute(endpointProd[0].Host,endpointProd[0].Basepath, resource.GetPath(), cluster_refProd)
			routesProd = append(routesProd, &routeP)

			endpointsProd = append(endpointsProd, &addressProd)

			//API level check
		} else if s.IsEndpointsAvailable(mgwSwagger.GetProdEndpoints()) {
			endpointProd = apiLevelEndpointProd
			cluster_refProd = apilevelClusterProd.GetName()

			//production endpoints
			routeP := createRoute(endpointProd[0].Host,endpointProd[0].Basepath, resource.GetPath(), cluster_refProd)
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
	fmt.Println(h.GetAddress())
	return cluster
}

func createRoute(HostUrl string, basepath string, resourcePath string, clusterName string) v2route.Route {
	var fullPath = basepath + resourcePath
	var route v2route.Route

	routepath, isHavingPathparameter := GenerateRegex(fullPath)

	RouteAction := &v2route.Route_Route{
		Route: &v2route.RouteAction{
			HostRewriteSpecifier: &v2route.RouteAction_HostRewrite{
				HostRewrite: HostUrl,
			},
			ClusterSpecifier: &v2route.RouteAction_Cluster{
				Cluster: clusterName,
			},
		},
	}

	if isHavingPathparameter {
		route = v2route.Route{
			Match: &v2route.RouteMatch{
				PathSpecifier: &v2route.RouteMatch_SafeRegex{
					SafeRegex: &envoy_type_matcher.RegexMatcher{
						EngineType: &envoy_type_matcher.RegexMatcher_GoogleRe2{
							GoogleRe2: &envoy_type_matcher.RegexMatcher_GoogleRE2{
								MaxProgramSize: nil,
							},
						},
						Regex: routepath,
					},
				},
				/*Headers: []*v2route.HeaderMatcher {
					{
						Name: "x-some-host",
						HeaderMatchSpecifier: &v2route.HeaderMatcher_ExactMatch{
							ExactMatch: HostUrl,
						},
					},
					{
						Name: "x-some-proto",
						HeaderMatchSpecifier: &v2route.HeaderMatcher_ExactMatch{
							ExactMatch: "https",
						},
					},
					{
						Name: "x-some-port",
						HeaderMatchSpecifier: &v2route.HeaderMatcher_ExactMatch{
							ExactMatch: "443",
						},
					},
				},  */
			},
			Action: RouteAction,
			Metadata: nil,
		}
	} else {
		route = v2route.Route{
			Match: &v2route.RouteMatch{
				PathSpecifier: &v2route.RouteMatch_Path{Path: routepath},
			},

			Action: RouteAction,

			Metadata: nil,
		}
	}
	fmt.Println(HostUrl, routepath)
	return route
}


func GenerateRegex(fullpath string) (string, bool) {
	isHavingPathparameters := true
	regex := ".*"
	newPath := ""

	if strings.Contains(fullpath, "{") || strings.Contains(fullpath, "}") {
		res1 := strings.Split(fullpath, "/")
		fmt.Println(res1)

		for i, p := range res1 {
			if strings.Contains(p, "{") || strings.Contains(p, "}"){
				res1[i] = regex
			}
		}
		newPath = strings.Join(res1[:], "/")

	} else {
		newPath = fullpath
		isHavingPathparameters = false
	}
	return newPath, isHavingPathparameters
}

