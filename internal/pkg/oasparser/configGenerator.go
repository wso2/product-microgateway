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

package oasparser

//package envoy_config_generator

import (
	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"

	enovoy "github.com/wso2/micro-gw/internal/pkg/oasparser/envoyCodegen"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/models/envoy"
	swgger "github.com/wso2/micro-gw/internal/pkg/oasparser/swaggerOperator"
)

// /**
//  * Get all production resources for envoy from file.
//  *
//  * @param byte[]  swagger as byte array
//  * @return []types.Resource Production listeners
//  * @return []types.Resource Production clusters
//  * @return []types.Resource Production routes
//  * @return []types.Resource Production endpoints
//  */
// func GetProductionSourcesFromByteArray(byteArr []byte) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
// 	logger.LoggerOasparser.Debug("debug check....................")
// 	mgwSwaggers := swgger.GenerateMgwSwaggerFromByteArray(byteArr)
// 	return getProductionSources(mgwSwaggers)
// }

// func getProductionSources(mgwSwaggers []apiDefinition.MgwSwagger) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
// 	var (
// 		routesP    []*routev3.Route
// 		clustersP  []*clusterv3.Cluster
// 		endpointsP []*corev3.Address
// 	)

// 	for _, swagger := range mgwSwaggers {
// 		routes, clusters, endpoints, _, _, _ := enovoy.CreateRoutesWithClusters(swagger)
// 		routesP = append(routesP, routes...)
// 		clustersP = append(clustersP, clusters...)
// 		endpointsP = append(endpointsP, endpoints...)
// 	}

// 	envoyNodeProd := new(envoy.EnvoyNode)

// 	if len(mgwSwaggers) > 0 {
// 		vHost_NameP := "default"
// 		vHostP, _ := enovoy.CreateVirtualHost(vHost_NameP, routesP)
// 		// listenerNameP := "listenerProd_1"
// 		// routeConfigNameP := "routeProd_" + strings.Replace(mgwSwaggers[0].GetTitle(), " ", "", -1) + mgwSwaggers[0].GetVersion()
// 		// listnerProd := enovoy.CreateListener(listenerNameP, routeConfigNameP, vHostP)
// 		listnerProd := enovoy.CreateListenerWithRds("default")
// 		routeConfigProd := enovoy.CreateRoutesConfigForRds(vHostP)

// 		envoyNodeProd.SetListener(&listnerProd)
// 		envoyNodeProd.SetClusters(clustersP)
// 		envoyNodeProd.SetRoutes(routesP)
// 		envoyNodeProd.SetEndpoints(endpointsP)
// 		envoyNodeProd.SetRouteConfigs(&routeConfigProd)

// 	} else {
// 		logger.LoggerOasparser.Error("No Api definitions found")
// 	}

// 	logger.LoggerOasparser.Info(len(routesP), " routes are generated successfully")
// 	logger.LoggerOasparser.Info(len(clustersP), " clusters are generated successfully")
// 	logger.LoggerOasparser.Info(len(endpointsP), " endpoints are generated successfully")
// 	return envoyNodeProd.GetSources()
// }

/*
GetProductionRoutesClustersEndpoints is a method to generate and provide the routes, clusters and endpoints when the openAPI is provided.

@param byte[]  openAPI json as a byte array
@return []*routev3.Route Routes
@return []*clusterv3.Cluster Clusters
@return []*corev3.Address Endpoints
*/
func GetProductionRoutesClustersEndpoints(byteArr []byte) ([]*routev3.Route, []*clusterv3.Cluster, []*corev3.Address) {
	mgwSwagger := swgger.GetMgwSwagger(byteArr)
	routes, clusters, endpoints, _, _, _ := enovoy.CreateRoutesWithClusters(mgwSwagger)
	return routes, clusters, endpoints
}

/*
GetProductionListenerAndRouteConfig is a method to generate and provide the listener and routesconfiguration configurations.

@param []*routev3.Route Envoy routes array
@return *listenerv3.Listener Envoy Listener
@return *routev3.RouteConfiguration Envoy RouteConfiguration
*/
func GetProductionListenerAndRouteConfig(routes []*routev3.Route) (*listenerv3.Listener, *routev3.RouteConfiguration) {
	listnerProd := enovoy.CreateListenerWithRds("default")
	vHostName := "default"
	vHostP, _ := enovoy.CreateVirtualHost(vHostName, routes)
	routeConfigProd := enovoy.CreateRoutesConfigForRds(vHostP)

	return &listnerProd, &routeConfigProd
}

func GetCacheResources(endpoints []*corev3.Address, clusters []*clusterv3.Cluster, listener *listenerv3.Listener,
	routeConfig *routev3.RouteConfiguration) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	envoyNodeProd := new(envoy.EnvoyNode)
	envoyNodeProd.SetListener(listener)
	envoyNodeProd.SetClusters(clusters)
	envoyNodeProd.SetEndpoints(endpoints)
	envoyNodeProd.SetRouteConfigs(routeConfig)

	return envoyNodeProd.GetSources()
}

/*
UpdateRoutesConfig is a method to update the existing routes configuration with the provided array of routes.
All the already existing routes (within the routeConfiguration) will be removed.

@param *routev3.RouteConfiguration Envoy RouteConfiguration
@param []*routev3.Route Envoy routes array
*/
func UpdateRoutesConfig(routeConfig *routev3.RouteConfiguration, routes []*routev3.Route) {
	vHostName := "default"
	vHost, _ := enovoy.CreateVirtualHost(vHostName, routes)
	routeConfig.VirtualHosts = []*routev3.VirtualHost{&vHost}
	//return []types.Resource{routeConfig}
}
