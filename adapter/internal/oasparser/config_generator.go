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
	"fmt"
	"strconv"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	envoy "github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/api"
)

// GetRoutesClustersEndpoints generates the routes, clusters and endpoints (envoy)
// when the openAPI Json is provided. For websockets apiJsn created from api.yaml file is considered.
func GetRoutesClustersEndpoints(mgwSwagger mgw.MgwSwagger, upstreamCerts map[string][]byte, interceptorCerts map[string][]byte,
	vHost string, organizationID string) ([]*routev3.Route, []*clusterv3.Cluster, []*corev3.Address, error) {

	routes, clusters, endpoints, err := envoy.CreateRoutesWithClusters(mgwSwagger, upstreamCerts, interceptorCerts,
		vHost, organizationID)
	if err != nil {
		return nil, nil, nil, fmt.Errorf("Error while creating routes, clusters and endpoints. %v", err)
	}
	return routes, clusters, endpoints, nil
}

// GetGlobalClusters generates initial internal clusters for given environment.
func GetGlobalClusters() ([]*clusterv3.Cluster, []*corev3.Address) {
	var (
		clusters  []*clusterv3.Cluster
		endpoints []*corev3.Address
	)
	conf, _ := config.ReadConfigs()

	if conf.Tracing.Enabled && conf.Tracing.Type != envoyconf.TracerTypeAzure {
		logger.LoggerOasparser.Debugln("Creating global cluster - Tracing")
		if c, e, err := envoyconf.CreateTracingCluster(conf); err == nil {
			clusters = append(clusters, c)
			endpoints = append(endpoints, e...)
		} else {
			logger.LoggerOasparser.Error("Failed to initialize tracer's cluster. Router tracing will be disabled. ", err)
			conf.Tracing.Enabled = false
		}
	}

	return clusters, endpoints
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
func GetEnforcerAPI(mgwSwagger model.MgwSwagger, vhost string) *api.Api {
	resources := []*api.Resource{}
	securitySchemes := []*api.SecurityScheme{}
	securityList := []*api.SecurityList{}
	isMockedAPI := mgwSwagger.EndpointImplementationType == constants.MockedOASEndpointType
	clientCertificates := []*api.Certificate{}

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

	for _, security := range mgwSwagger.GetSecurity() {
		mapOfSecurity := make(map[string]*api.Scopes)
		for key, scopes := range security {
			scopeList := &api.Scopes{
				Scopes: scopes,
			}
			mapOfSecurity[key] = scopeList
		}
		securityMap := &api.SecurityList{
			ScopeList: mapOfSecurity,
		}
		securityList = append(securityList, securityMap)
	}

	for _, res := range mgwSwagger.GetResources() {
		var operations = make([]*api.Operation, len(res.GetMethod()))
		for i, op := range res.GetMethod() {
			operations[i] = GetEnforcerAPIOperation(*op, isMockedAPI)
		}
		resource := &api.Resource{
			Id:      res.GetID(),
			Methods: operations,
			Path:    res.GetPath(),
		}
		if res.GetProdEndpoints() != nil {
			resource.ProductionEndpoints = generateRPCEndpointCluster(res.GetProdEndpoints())
		}
		if res.GetSandEndpoints() != nil {
			resource.SandboxEndpoints = generateRPCEndpointCluster(res.GetSandEndpoints())
		}
		resources = append(resources, resource)
	}

	endpointSecurityDetails := &api.EndpointSecurity{}

	if mgwSwagger.GetProdEndpoints() != nil {
		endpointSecurityDetails.ProductionSecurityInfo = &api.SecurityInfo{
			Username:         mgwSwagger.GetProdEndpoints().SecurityConfig.Username,
			Password:         mgwSwagger.GetProdEndpoints().SecurityConfig.Password,
			SecurityType:     mgwSwagger.GetProdEndpoints().SecurityConfig.Type,
			Enabled:          mgwSwagger.GetProdEndpoints().SecurityConfig.Enabled,
			CustomParameters: mgwSwagger.GetProdEndpoints().SecurityConfig.CustomParameters,
		}
	}
	if mgwSwagger.GetSandEndpoints() != nil {
		endpointSecurityDetails.SandBoxSecurityInfo = &api.SecurityInfo{
			Username:         mgwSwagger.GetSandEndpoints().SecurityConfig.Username,
			Password:         mgwSwagger.GetSandEndpoints().SecurityConfig.Password,
			SecurityType:     mgwSwagger.GetSandEndpoints().SecurityConfig.Type,
			Enabled:          mgwSwagger.GetSandEndpoints().SecurityConfig.Enabled,
			CustomParameters: mgwSwagger.GetSandEndpoints().SecurityConfig.CustomParameters,
		}
	}

	for _, cert := range mgwSwagger.GetClientCerts() {
		certificate := &api.Certificate{
			Alias:   cert.Alias,
			Tier:    cert.Tier,
			Content: cert.Content,
		}
		clientCertificates = append(clientCertificates, certificate)
	}

	return &api.Api{
		Id:                    mgwSwagger.GetID(),
		Title:                 mgwSwagger.GetTitle(),
		Description:           mgwSwagger.GetDescription(),
		BasePath:              mgwSwagger.GetXWso2Basepath(),
		Version:               mgwSwagger.GetVersion(),
		ApiType:               mgwSwagger.GetAPIType(),
		ProductionEndpoints:   generateRPCEndpointCluster(mgwSwagger.GetProdEndpoints()),
		SandboxEndpoints:      generateRPCEndpointCluster(mgwSwagger.GetSandEndpoints()),
		Resources:             resources,
		ApiLifeCycleState:     mgwSwagger.LifecycleStatus,
		Tier:                  mgwSwagger.GetXWso2ThrottlingTier(),
		SecurityScheme:        securitySchemes,
		Security:              securityList,
		EndpointSecurity:      endpointSecurityDetails,
		AuthorizationHeader:   mgwSwagger.GetXWSO2AuthHeader(),
		DisableSecurity:       mgwSwagger.GetDisableSecurity(),
		OrganizationId:        mgwSwagger.OrganizationID,
		Vhost:                 vhost,
		IsMockedApi:           isMockedAPI,
		ClientCertificates:    clientCertificates,
		MutualSSL:             mgwSwagger.GetXWSO2MutualSSL(),
		ApplicationSecurity:   mgwSwagger.GetXWSO2ApplicationSecurity(),
		GraphQLSchema:         mgwSwagger.GraphQLSchema,
		GraphqlComplexityInfo: mgwSwagger.GraphQLComplexities.Data.List,
	}
}

// GetEnforcerAPIOperation builds the operation object expected by the proto definition
func GetEnforcerAPIOperation(operation mgw.Operation, isMockedAPI bool) *api.Operation {
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

	var mockedAPIConfig *api.MockedApiConfig
	if isMockedAPI {
		mockedAPIConfig = operation.GetMockedAPIConfig()
	}

	policies := &api.OperationPolicies{
		Request:  castPoliciesToEnforcerPolicies(operation.GetPolicies().Request),
		Response: castPoliciesToEnforcerPolicies(operation.GetPolicies().Response),
		Fault:    castPoliciesToEnforcerPolicies(operation.GetPolicies().Fault),
	}
	apiOperation := api.Operation{
		Method:          operation.GetMethod(),
		Security:        secSchemas,
		Tier:            operation.GetTier(),
		DisableSecurity: operation.GetDisableSecurity(),
		Policies:        policies,
		MockedApiConfig: mockedAPIConfig,
	}
	return &apiOperation
}

func castPoliciesToEnforcerPolicies(policies []model.Policy) []*api.Policy {
	enforcerPolicies := make([]*api.Policy, 0, len(policies))
	for _, policy := range policies {
		if !policy.IsPassToEnforcer {
			// The API Policy do not want support from enforcer to handle the request
			continue
		}
		parameterMap := make(map[string]string)
		if policy.Parameters != nil {
			if params, ok := policy.Parameters.(map[string]interface{}); ok {
				for paramK := range params {
					if paramV, parsed := params[paramK].(string); parsed {
						parameterMap[paramK] = paramV
					} else if paramV, parsed := params[paramK].(bool); parsed {
						parameterMap[paramK] = strconv.FormatBool(paramV)
					}
				}

			}
		}
		enforcerPolicies = append(enforcerPolicies, &api.Policy{
			Action:     policy.Action,
			Parameters: parameterMap,
		})
	}
	return enforcerPolicies
}

func generateRPCEndpointCluster(inputEndpointCluster *mgw.EndpointCluster) *api.EndpointCluster {
	if inputEndpointCluster == nil || len(inputEndpointCluster.Endpoints) == 0 {
		return nil
	}
	urls := []*api.Endpoint{}
	for _, ep := range inputEndpointCluster.Endpoints {
		endpoint := &api.Endpoint{
			Basepath: ep.Basepath,
			Host:     ep.Host,
			Port:     ep.Port,
			URLType:  ep.URLType,
		}
		urls = append(urls, endpoint)
	}

	endpoints := &api.EndpointCluster{
		Urls: urls,
	}
	if inputEndpointCluster.Config != nil {
		// retry config
		var retryConfig *api.RetryConfig
		if inputEndpointCluster.Config.RetryConfig != nil {
			inputRetryConfig := inputEndpointCluster.Config.RetryConfig
			retryConfig = &api.RetryConfig{
				Count:       uint32(inputRetryConfig.Count),
				StatusCodes: inputRetryConfig.StatusCodes,
			}
		}
		// timeout config
		var timeoutConfig *api.TimeoutConfig
		if inputEndpointCluster.Config.TimeoutInMillis != 0 { // if zero, means not set. Then, global timeout is applied via route configs.
			timeoutConfig = &api.TimeoutConfig{
				RouteTimeoutInMillis: uint32(inputEndpointCluster.Config.TimeoutInMillis),
			}
		}
		// Set all endpoint configs
		endpoints.Config = &api.EndpointClusterConfig{
			RetryConfig:   retryConfig,
			TimeoutConfig: timeoutConfig,
		}
	}
	return endpoints
}
