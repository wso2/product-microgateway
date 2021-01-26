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

import (
	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/wso2/micro-gw/loggers"

	wso2 "github.com/envoyproxy/go-control-plane/wso2/discovery/api"
	envoy "github.com/wso2/micro-gw/pkg/oasparser/envoyconf"
	"github.com/wso2/micro-gw/pkg/oasparser/model"
	mgw "github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/operator"
)

// GetProductionRoutesClustersEndpoints generates the routes, clusters and endpoints (envoy)
// when the openAPI Json is provided. For websockets apiJsn created from api.yaml file is considerd.
func GetProductionRoutesClustersEndpoints(byteArr []byte, upstreamCerts []byte, apiType string) ([]*routev3.Route, []*clusterv3.Cluster, []*corev3.Address, mgw.MgwSwagger) {
	var mgwSwagger mgw.MgwSwagger
	var routes []*routev3.Route
	var clusters []*clusterv3.Cluster
	var endpoints []*corev3.Address

	if apiType == mgw.HTTP {
		mgwSwagger = operator.GetMgwSwagger(byteArr)
	} else if apiType == mgw.WS {
		mgwSwagger = operator.GetMgwSwaggerWebSocket(byteArr)
	} else {
		// Unreachable else condition. Added in case previous apiType check fails due to any modifications.
		loggers.LoggerOasparser.Errorf("API type not currently supported with WSO2 Micro-gateway")
	}
	routes, clusters, endpoints = envoy.CreateRoutesWithClusters(mgwSwagger, upstreamCerts)
	//TODO: (VirajSalaka) Decide if this needs to be added to the MgwSwagger

	return routes, clusters, endpoints, mgwSwagger
}

// GetProductionListenerAndRouteConfig generates the listener and routesconfiguration configurations.
//
// The VirtualHost is named as "default".
// The provided set of envoy routes will be assigned under the virtual host
//
// The RouteConfiguration is named as "default"
func GetProductionListenerAndRouteConfig(routes []*routev3.Route) (*listenerv3.Listener, *routev3.RouteConfiguration) {
	listnerProd := envoy.CreateListenerWithRds("default")
	vHostName := "default"
	vHostP := envoy.CreateVirtualHost(vHostName, routes)
	routeConfigProd := envoy.CreateRoutesConfigForRds(vHostP)

	return listnerProd, routeConfigProd
}

// GetCacheResources converts the envoy endpoints, clusters, routes, and listener to
// the resource type which is the format required for the Xds cache.
//
// The returned resources are listeners, clusters, routeConfigurations, endpoints
func GetCacheResources(endpoints []*corev3.Address, clusters []*clusterv3.Cluster,
	listener *listenerv3.Listener, routeConfig *routev3.RouteConfiguration) (
	listenerRes []types.Resource, clusterRes []types.Resource, routeConfigRes []types.Resource,
	endpointRes []types.Resource) {

	listenerRes = []types.Resource{listener}
	clusterRes = []types.Resource{}
	routeConfigRes = []types.Resource{routeConfig}
	endpointRes = []types.Resource{}
	for _, cluster := range clusters {
		clusterRes = append(clusterRes, cluster)
	}
	for _, endpoint := range endpoints {
		endpointRes = append(endpointRes, endpoint)
	}
	return listenerRes, clusterRes, routeConfigRes, endpointRes
}

// UpdateRoutesConfig updates the existing routes configuration with the provided array of routes.
// All the already existing routes (within the routeConfiguration) will be removed.
func UpdateRoutesConfig(routeConfig *routev3.RouteConfiguration, routes []*routev3.Route) {
	vHostName := "default"
	vHost := envoy.CreateVirtualHost(vHostName, routes)
	routeConfig.VirtualHosts = []*routev3.VirtualHost{vHost}
}

// GetEnforcerAPI retrieves the ApiDS object model for a given swagger definition.
func GetEnforcerAPI(mgwSwagger model.MgwSwagger) *wso2.Api {
	prodUrls := []*wso2.Endpoint{}
	sandUrls := []*wso2.Endpoint{}
	for _, ep := range mgwSwagger.GetProdEndpoints() {
		prodEp := &wso2.Endpoint{
			Basepath: ep.Basepath,
			Host:     ep.Host,
			Port:     ep.Port,
			URLType:  ep.URLType,
		}
		prodUrls = append(prodUrls, prodEp)
	}

	for _, ep := range mgwSwagger.GetSandEndpoints() {
		sandEp := &wso2.Endpoint{
			Basepath: ep.Basepath,
			Host:     ep.Host,
			Port:     ep.Port,
			URLType:  ep.URLType,
		}
		sandUrls = append(sandUrls, sandEp)
	}

	return &wso2.Api{
		Id:             mgwSwagger.GetID(),
		Title:          mgwSwagger.GetTitle(),
		Description:    mgwSwagger.GetDescription(),
		BasePath:       mgwSwagger.GetXWso2Basepath(),
		Version:        mgwSwagger.GetVersion(),
		ProductionUrls: prodUrls,
		SandboxUrls:    sandUrls,
	}
}
