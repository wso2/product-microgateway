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
	envoy "github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/api"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// GetRoutesClustersEndpoints generates the routes, clusters and endpoints (envoy)
// when the openAPI Json is provided. For websockets apiJsn created from api.yaml file is considerd.
func GetRoutesClustersEndpoints(mgwSwagger mgw.MgwSwagger, upstreamCerts []byte, interceptorCerts []byte,
	vHost string, organizationID string) ([]*routev3.Route, []*clusterv3.Cluster, []*corev3.Address) {
	var routes []*routev3.Route
	var clusters []*clusterv3.Cluster
	var endpoints []*corev3.Address

	routes, clusters, endpoints = envoy.CreateRoutesWithClusters(mgwSwagger, upstreamCerts, interceptorCerts,
		vHost, organizationID)
	//TODO: (VirajSalaka) Decide if this needs to be added to the MgwSwagger

	return routes, clusters, endpoints
}

// GetProductionListenerAndRouteConfig generates the listener and routesconfiguration configurations.
//
// The VirtualHost is named as "default".
// The provided set of envoy routes will be assigned under the virtual host
//
// The RouteConfiguration is named as "default"
func GetProductionListenerAndRouteConfig(vhostToRouteArrayMap map[string][]*routev3.Route) ([]*listenerv3.Listener, *routev3.RouteConfiguration) {
	listeners := envoy.CreateListenersWithRds()
	vHosts := envoy.CreateVirtualHosts(vhostToRouteArrayMap)
	routeConfig := envoy.CreateRoutesConfigForRds(vHosts)

	return listeners, routeConfig
}

// GetCacheResources converts the envoy endpoints, clusters, routes, and listener to
// the resource type which is the format required for the Xds cache.
//
// The returned resources are listeners, clusters, routeConfigurations, endpoints
func GetCacheResources(endpoints []*corev3.Address, clusters []*clusterv3.Cluster,
	listeners []*listenerv3.Listener, routeConfig *routev3.RouteConfiguration) (
	listenerRes []types.Resource, clusterRes []types.Resource, routeConfigRes []types.Resource,
	endpointRes []types.Resource) {

	listenerRes = []types.Resource{}
	clusterRes = []types.Resource{}
	routeConfigRes = []types.Resource{routeConfig}
	endpointRes = []types.Resource{}
	for _, cluster := range clusters {
		clusterRes = append(clusterRes, cluster)
	}
	for _, endpoint := range endpoints {
		endpointRes = append(endpointRes, endpoint)
	}
	for _, listener := range listeners {
		listenerRes = append(listenerRes, listener)
	}
	return listenerRes, clusterRes, routeConfigRes, endpointRes
}

// UpdateRoutesConfig updates the existing routes configuration with the provided map of vhost to array of routes.
// All the already existing routes (within the routeConfiguration) will be removed.
func UpdateRoutesConfig(routeConfig *routev3.RouteConfiguration, vhostToRouteArrayMap map[string][]*routev3.Route) {
	routeConfig.VirtualHosts = envoy.CreateVirtualHosts(vhostToRouteArrayMap)
}

// GetEnforcerAPI retrieves the ApiDS object model for a given swagger definition
// along with the vhost to deploy the API.
func GetEnforcerAPI(mgwSwagger model.MgwSwagger, lifeCycleState string, endpointSecurity mgw.APIEndpointSecurity, vhost string) *api.Api {
	prodUrls := []*api.Endpoint{}
	sandUrls := []*api.Endpoint{}
	resources := []*api.Resource{}
	securitySchemes := []*api.SecurityScheme{}

	logger.LoggerOasparser.Debugf("Security schemes in GetEnforcerAPI method %v:", mgwSwagger.GetSecurityScheme())
	for _, securityScheme := range mgwSwagger.GetSecurityScheme() {
		scheme := &api.SecurityScheme{
			DefinitionName: securityScheme.DefinitionName,
			Type:           securityScheme.Type,
			Name:           securityScheme.Name,
			In:             securityScheme.In,
		}
		securitySchemes = append(securitySchemes, scheme)
	}

	if mgwSwagger.GetProdEndpoints() != nil {
		for _, ep := range mgwSwagger.GetProdEndpoints().Endpoints {
			prodEp := &api.Endpoint{
				Basepath: ep.Basepath,
				Host:     ep.Host,
				Port:     ep.Port,
				URLType:  ep.URLType,
			}
			prodUrls = append(prodUrls, prodEp)
		}
	}

	if mgwSwagger.GetSandEndpoints() != nil {
		for _, ep := range mgwSwagger.GetSandEndpoints().Endpoints {
			sandEp := &api.Endpoint{
				Basepath: ep.Basepath,
				Host:     ep.Host,
				Port:     ep.Port,
				URLType:  ep.URLType,
			}
			sandUrls = append(sandUrls, sandEp)
		}
	}

	for _, res := range mgwSwagger.GetResources() {
		var operations = make([]*api.Operation, len(res.GetMethod()))
		for i, op := range res.GetMethod() {
			operations[i] = GetEnforcerAPIOperation(op)
		}
		resource := &api.Resource{
			Id:      res.GetID(),
			Methods: operations,
			Path:    res.GetPath(),
		}
		resources = append(resources, resource)
	}

	endpointSecurityDetails := &api.EndpointSecurity{
		SandBoxSecurityInfo: &api.SecurityInfo{
			Username:         endpointSecurity.Sandbox.Username,
			Password:         endpointSecurity.Sandbox.Password,
			SecurityType:     endpointSecurity.Sandbox.Type,
			Enabled:          endpointSecurity.Sandbox.Enabled,
			CustomParameters: endpointSecurity.Sandbox.CustomParameters,
		},
		ProductionSecurityInfo: &api.SecurityInfo{
			Username:         endpointSecurity.Production.Username,
			Password:         endpointSecurity.Production.Password,
			SecurityType:     endpointSecurity.Production.Type,
			Enabled:          endpointSecurity.Production.Enabled,
			CustomParameters: endpointSecurity.Production.CustomParameters,
		},
	}

	return &api.Api{
		Id:                  mgwSwagger.GetID(),
		Title:               mgwSwagger.GetTitle(),
		Description:         mgwSwagger.GetDescription(),
		BasePath:            mgwSwagger.GetXWso2Basepath(),
		Version:             mgwSwagger.GetVersion(),
		ApiType:             mgwSwagger.GetAPIType(),
		ProductionUrls:      prodUrls,
		SandboxUrls:         sandUrls,
		Resources:           resources,
		ApiLifeCycleState:   lifeCycleState,
		Tier:                mgwSwagger.GetXWso2ThrottlingTier(),
		SecurityScheme:      securitySchemes,
		EndpointSecurity:    endpointSecurityDetails,
		AuthorizationHeader: mgwSwagger.GetXWSO2AuthHeader(),
		DisableSecurity:     mgwSwagger.GetDisableSecurity(),
		OrganizationId:      mgwSwagger.OrganizationID,
		Vhost:               vhost,
	}
}

// GetEnforcerAPIOperation builds the operation object expected by the proto definition
func GetEnforcerAPIOperation(operation mgw.Operation) *api.Operation {
	secSchemas := make([]*api.SecurityList, len(operation.GetSecurity()))
	for i, security := range operation.GetSecurity() {
		mapOfSecurity := make(map[string]*api.Scopes)
		for key, scopes := range security {
			scopeList := &api.Scopes{
				Scopes: scopes,
			}
			mapOfSecurity[key] = scopeList
		}
		secSchema := &api.SecurityList{
			ScopeList: mapOfSecurity,
		}
		secSchemas[i] = secSchema
	}
	apiOperation := api.Operation{
		Method:          operation.GetMethod(),
		Security:        secSchemas,
		Tier:            operation.GetTier(),
		DisableSecurity: operation.GetDisableSecurity(),
	}
	return &apiOperation
}
