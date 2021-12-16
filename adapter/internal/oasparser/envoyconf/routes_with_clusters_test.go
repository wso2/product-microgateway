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
package envoyconf_test

import (
	"encoding/json"
	"io/ioutil"
	"strings"
	"testing"

	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
	envoy "github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

func TestCreateRoutesWithClustersForOpenAPIWithoutExtensions(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi.yaml"
	commonTestForCreateRoutesWithClusters(t, openapiFilePath, false)
	// TODO: (VirajSalaka) Additional tasks to test
	// OpenAPI version 2
}

func TestCreateRoutesWithClustersForOpenAPIWithExtensionsOnly(t *testing.T) {
	// When the openapi endpoints are only mentioned via the extensions
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_extensions_only.yaml"
	commonTestForCreateRoutesWithClusters(t, openapiFilePath, true)
}

func TestCreateRoutesWithClustersForOpenAPIWithExtensionsServers(t *testing.T) {
	// When the openapi endpoints provided by servers object are overriden via the extensions
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_extensions_servers.yaml"
	commonTestForCreateRoutesWithClusters(t, openapiFilePath, true)
}

// func TestCreateRouteswithClustersWebsocketProdSand(t *testing.T) {
// 	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api.yaml"
// 	testCreateRoutesWithClustersWebsocket(t, apiYamlFilePath)
// 	testCreateRoutesWithClustersWebsocketWithEnvProps(t, apiYamlFilePath)
// }

// func TestCreateRouteswithClustersWebsocketProd(t *testing.T) {
// 	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api_prod.yaml"
// 	testCreateRoutesWithClustersWebsocket(t, apiYamlFilePath)
// 	testCreateRoutesWithClustersWebsocketWithEnvProps(t, apiYamlFilePath)
// }

// func TestCreateRouteswithClustersWebsocketSand(t *testing.T) {
// 	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api_sand.yaml"
// 	testCreateRoutesWithClustersWebsocket(t, apiYamlFilePath)
// 	testCreateRoutesWithClustersWebsocketWithEnvProps(t, apiYamlFilePath)
// }

// commonTestForCreateRoutesWithClusters
// withExtensions - if definition has endpoints x-wso2 extension
func commonTestForCreateRoutesWithClusters(t *testing.T, openapiFilePath string, withExtensions bool) {
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := model.MgwSwagger{}
	err = mgwSwaggerForOpenapi.GetMgwSwagger(openapiByteArr)
	assert.Nil(t, err, "Error should not be present when openAPI definition is converted to a MgwSwagger object")
	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi, nil, nil, "localhost", "carbon.super")

	assert.Equal(t, 2, len(clusters), "Number of production clusters created is incorrect.")
	// As the first cluster is always related to API level cluster
	apiLevelCluster := clusters[0]
	pathLevelCluster := clusters[1]
	assert.Equal(t, apiLevelCluster.GetName(), "carbon.super_clusterProd_localhost_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
	assert.Contains(t, pathLevelCluster.GetName(), "carbon.super_clusterProd_localhost_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")

	apiLevelClusterHost0 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	apiLevelClusterPort0 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	apiLevelClusterPriority0 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].Priority
	apiLevelClusterHost1 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	apiLevelClusterPort1 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	apiLevelClusterPriority1 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[1].Priority

	assert.NotEmpty(t, apiLevelClusterHost0, "API Level Cluster's assigned host should not be null")
	assert.Equal(t, "apiLevelEndpoint", apiLevelClusterHost0, "API Level Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, apiLevelClusterPort0, "API Level Cluster's assigned port should not be null")
	assert.Equal(t, uint32(80), apiLevelClusterPort0, "API Level Cluster's assigned host is incorrect.")
	assert.Equal(t, uint32(0), apiLevelClusterPriority0, "API Level Cluster's assigned Priority is incorrect.")

	assert.NotEmpty(t, apiLevelClusterHost1, "API Level Cluster's second endpoint host should not be null")
	assert.Equal(t, "apiLevelLBEndpoint", apiLevelClusterHost1, "API Level Cluster's second endpoint host is incorrect.")
	assert.NotEmpty(t, apiLevelClusterPort1, "API Level Cluster's second endpoint port should not be null")
	assert.Equal(t, uint32(8080), apiLevelClusterPort1, "API Level Cluster's second endpoint host is incorrect.")
	assert.Equal(t, uint32(0), apiLevelClusterPriority1, "API Level Cluster's second endpoint Priority is incorrect.")

	pathLevelClusterHost0 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	pathLevelClusterPort0 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	pathLevelClusterPriority0 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].Priority
	pathLevelClusterHost1 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	pathLevelClusterPort1 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	pathLevelClusterPriority1 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[1].Priority

	assert.NotEmpty(t, pathLevelClusterHost0, "Path Level Cluster's assigned host should not be null")
	assert.Equal(t, "resourceLevelEndpoint", pathLevelClusterHost0, "Path Level Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, pathLevelClusterPort0, "Path Level Cluster's assigned port should not be null")
	assert.Equal(t, uint32(443), pathLevelClusterPort0, "Path Level Cluster's assigned host is incorrect.")
	assert.Equal(t, uint32(0), pathLevelClusterPriority0, "Path Level Cluster's assigned priority is incorrect.")

	assert.NotEmpty(t, pathLevelClusterHost1, "Path Level Cluster's second endpoint host should not be null")
	assert.Equal(t, "resourceLevelLBEndpoint", pathLevelClusterHost1, "Path Level Cluster's second endpoint host is incorrect.")
	assert.NotEmpty(t, pathLevelClusterPort1, "Path Level Cluster's second endpoint port should not be null")
	assert.Equal(t, uint32(8080), pathLevelClusterPort1, "Path Level Cluster's second endpoint host is incorrect.")
	if withExtensions {
		pathLevelMaxConnections := pathLevelCluster.GetCircuitBreakers().Thresholds[0].MaxConnections
		pathLevelMaxRequests := pathLevelCluster.GetCircuitBreakers().Thresholds[0].MaxRequests
		pathLevelMaxConnectionPools := pathLevelCluster.GetCircuitBreakers().Thresholds[0].MaxConnectionPools

		assert.Empty(t, apiLevelCluster.GetCircuitBreakers(), "API Level Cluster's circuit breaker should be empty.")

		assert.Equal(t, wrapperspb.UInt32(2), pathLevelMaxConnections, "Path Level Cluster's max connection circuit breaker is incorrect.")
		assert.Equal(t, wrapperspb.UInt32(15), pathLevelMaxRequests, "Path Level Cluster's max request circuit breaker is incorrect.")
		// This is to check max connection pool circuit breaker has not set when the config value is -1
		assert.Equal(t, (*wrapperspb.UInt32Value)(nil), pathLevelMaxConnectionPools, "Path Level Cluster's max connection pool circuit breaker is incorrect.")

		assert.Equal(t, uint32(1), pathLevelClusterPriority1, "Path Level Cluster's second endpoint priority is incorrect.")
	} else {
		assert.Equal(t, uint32(0), pathLevelClusterPriority1, "Path Level Cluster's second endpoint priority is incorrect.")
	}
	assert.Equal(t, 2, len(routes), "Created number of routes are incorrect.")
	assert.Contains(t, []string{"^/pets(\\?([^/]+))?$", "^/pets/([^/]+)(\\?([^/]+))?$"}, routes[0].GetMatch().GetSafeRegex().Regex)
	assert.Contains(t, []string{"^/pets(\\?([^/]+))?$", "^/pets/([^/]+)(\\?([^/]+))?$"}, routes[1].GetMatch().GetSafeRegex().Regex)
	assert.NotEqual(t, routes[0].GetMatch().GetSafeRegex().Regex, routes[1].GetMatch().GetSafeRegex().Regex,
		"The route regex for the two routes should not be the same")
}

func TestCreateRoutesWithClustersForEndpointRef(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_endpoint_ref.yaml"
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := model.MgwSwagger{}
	err = mgwSwaggerForOpenapi.GetMgwSwagger(openapiByteArr)
	assert.Nil(t, err, "Error should not be present when openAPI definition is converted to a MgwSwagger object")
	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi, nil, nil, "localhost", "carbon.super")

	assert.Equal(t, 2, len(clusters), "Number of production clusters created is incorrect.")

	cluster1 := clusters[0]
	cluster2 := clusters[1]
	apiLevelCluster := cluster1
	pathLevelCluster := cluster2
	assert.Contains(t, []string{cluster1.GetName(), cluster2.GetName()}, "carbon.super_my-resource-endpoints_xwso2cluster_localhost_SwaggerPetstore1.0.0", "cluster name mismatch")
	if cluster1.GetName() == "carbon.super_my-resource-endpoints_xwso2cluster_localhost_SwaggerPetstore1.0.0" {
		apiLevelCluster = cluster2
		pathLevelCluster = cluster1
	}
	assert.Equal(t, "carbon.super_my-api-endpoints_xwso2cluster_localhost_SwaggerPetstore1.0.0", apiLevelCluster.GetName(), "Resource Level cluster name mismatch")

	apiLevelClusterHost0 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	apiLevelClusterPort0 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	apiLevelClusterPriority0 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].Priority
	apiLevelClusterHost1 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	apiLevelClusterPort1 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	apiLevelClusterPriority1 := apiLevelCluster.GetLoadAssignment().GetEndpoints()[1].Priority

	assert.NotEmpty(t, apiLevelClusterHost0, "API Level Cluster's assigned host should not be null")
	assert.Equal(t, "apiLevelEndpoint", apiLevelClusterHost0, "API Level Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, apiLevelClusterPort0, "API Level Cluster's assigned port should not be null")
	assert.Equal(t, uint32(80), apiLevelClusterPort0, "API Level Cluster's assigned host is incorrect.")
	assert.Equal(t, uint32(0), apiLevelClusterPriority0, "API Level Cluster's assigned Priority is incorrect.")

	assert.NotEmpty(t, apiLevelClusterHost1, "API Level Cluster's second endpoint host should not be null")
	assert.Equal(t, "apiLevelLBEndpoint", apiLevelClusterHost1, "API Level Cluster's second endpoint host is incorrect.")
	assert.NotEmpty(t, apiLevelClusterPort1, "API Level Cluster's second endpoint port should not be null")
	assert.Equal(t, uint32(8080), apiLevelClusterPort1, "API Level Cluster's second endpoint host is incorrect.")
	assert.Equal(t, uint32(0), apiLevelClusterPriority1, "API Level Cluster's second endpoint Priority is incorrect.")

	pathLevelClusterHost0 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	pathLevelClusterPort0 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	pathLevelClusterPriority0 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].Priority
	pathLevelClusterHost1 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	pathLevelClusterPort1 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	pathLevelClusterPriority1 := pathLevelCluster.GetLoadAssignment().GetEndpoints()[1].Priority

	assert.NotEmpty(t, pathLevelClusterHost0, "Path Level Cluster's assigned host should not be null")
	assert.Equal(t, "resourceLevelEndpoint", pathLevelClusterHost0, "Path Level Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, pathLevelClusterPort0, "Path Level Cluster's assigned port should not be null")
	assert.Equal(t, uint32(443), pathLevelClusterPort0, "Path Level Cluster's assigned host is incorrect.")
	assert.Equal(t, uint32(0), pathLevelClusterPriority0, "Path Level Cluster's assigned priority is incorrect.")

	assert.NotEmpty(t, pathLevelClusterHost1, "Path Level Cluster's second endpoint host should not be null")
	assert.Equal(t, "resourceLevelLBEndpoint", pathLevelClusterHost1, "Path Level Cluster's second endpoint host is incorrect.")
	assert.NotEmpty(t, pathLevelClusterPort1, "Path Level Cluster's second endpoint port should not be null")
	assert.Equal(t, uint32(8080), pathLevelClusterPort1, "Path Level Cluster's second endpoint host is incorrect.")
	pathLevelMaxConnections := pathLevelCluster.GetCircuitBreakers().Thresholds[0].MaxConnections
	pathLevelMaxRequests := pathLevelCluster.GetCircuitBreakers().Thresholds[0].MaxRequests
	pathLevelMaxConnectionPools := pathLevelCluster.GetCircuitBreakers().Thresholds[0].MaxConnectionPools

	assert.Empty(t, apiLevelCluster.GetCircuitBreakers(), "API Level Cluster's circuit breaker should be empty.")

	assert.Equal(t, wrapperspb.UInt32(2), pathLevelMaxConnections, "Path Level Cluster's max connection circuit breaker is incorrect.")
	assert.Equal(t, wrapperspb.UInt32(15), pathLevelMaxRequests, "Path Level Cluster's max request circuit breaker is incorrect.")
	// This is to check max connection pool circuit breaker has not set when the config value is -1
	assert.Equal(t, (*wrapperspb.UInt32Value)(nil), pathLevelMaxConnectionPools, "Path Level Cluster's max connection pool circuit breaker is incorrect.")

	assert.Equal(t, uint32(1), pathLevelClusterPriority1, "Path Level Cluster's second endpoint priority is incorrect.")

	assert.Equal(t, 2, len(routes), "Created number of routes are incorrect.")
	assert.Contains(t, []string{"^/pets(\\?([^/]+))?$", "^/pets/([^/]+)(\\?([^/]+))?$"}, routes[0].GetMatch().GetSafeRegex().Regex)
	assert.Contains(t, []string{"^/pets(\\?([^/]+))?$", "^/pets/([^/]+)(\\?([^/]+))?$"}, routes[1].GetMatch().GetSafeRegex().Regex)
	assert.NotEqual(t, routes[0].GetMatch().GetSafeRegex().Regex, routes[1].GetMatch().GetSafeRegex().Regex,
		"The route regex for the two routes should not be the same")
}

// func testCreateRoutesWithClustersWebsocket(t *testing.T, apiYamlFilePath string) {
// 	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
// 	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
// 	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
// 	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)

// 	var apiYaml model.APIYaml
// 	err = json.Unmarshal(apiJsn, &apiYaml)
// 	apiYaml = model.PopulateEndpointsInfo(apiYaml)
// 	assert.Nil(t, err, "Error occured while parsing api.yaml")
// 	var mgwSwagger model.MgwSwagger
// 	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiYaml, model.WS)
// 	assert.Nil(t, err, "Error while populating the MgwSwagger object for web socket APIs")
// 	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil, nil, "localhost", "carbon.super")

// 	if strings.HasSuffix(apiYamlFilePath, "api.yaml") {
// 		assert.Equal(t, len(clusters), 2, "Number of clusters created incorrect")
// 		productionCluster := clusters[0]
// 		sandBoxCluster := clusters[1]
// 		assert.Equal(t, productionCluster.GetName(), "carbon.super_clusterProd_localhost_EchoWebSocket1.0", "Production cluster name mismatch")
// 		assert.Equal(t, sandBoxCluster.GetName(), "carbon.super_clusterSand_localhost_EchoWebSocket1.0", "Sandbox cluster name mismatch")

// 		productionClusterHost := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
// 		productionClusterPort := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

// 		assert.Equal(t, productionClusterHost, "echo.websocket.org", "Production cluster host mismatch")
// 		assert.Equal(t, productionClusterPort, uint32(80), "Production cluster port mismatch")

// 		sandBoxClusterHost := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
// 		sandBoxClusterPort := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

// 		assert.Equal(t, sandBoxClusterHost, "echo.websocket.org", "Sandbox cluster host mismatch")
// 		assert.Equal(t, sandBoxClusterPort, uint32(80), "Sandbox cluster port mismatch")

// 		assert.Equal(t, 1, len(routes), "Number of routes incorrect")

// 		// TODO: (VirajSalaka) upgrade this properly
// 		// route := routes[0].GetMatch().GetSafeRegex().Regex
// 		// assert.Equal(t, route, "^/echowebsocket/1.0(\\?([^/]+))?$", "route created mismatch")
// 	}
// 	if strings.HasSuffix(apiYamlFilePath, "api_prod.yaml") {
// 		assert.Equal(t, len(clusters), 1, "Number of clusters created incorrect")
// 		productionCluster := clusters[0]
// 		assert.Equal(t, productionCluster.GetName(), "carbon.super_clusterProd_localhost_prodws1.0", "Production cluster name mismatch")

// 		productionClusterHost := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
// 		productionClusterPort := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

// 		assert.Equal(t, productionClusterHost, "echo.websocket.org", "Production cluster host mismatch")
// 		assert.Equal(t, productionClusterPort, uint32(80), "Production cluster port mismatch")

// 		assert.Equal(t, 1, len(routes), "Number of routes incorrect")

// 		// route := routes[0].GetMatch().GetSafeRegex().Regex
// 		// assert.Equal(t, route, "^/echowebsocketprod/1.0(\\?([^/]+))?$", "route created mismatch")

// 	}
// 	if strings.HasSuffix(apiYamlFilePath, "api_sand.yaml") {
// 		assert.Equal(t, len(clusters), 1, "Number of clusters created incorrect")
// 		sandBoxCluster := clusters[0]
// 		assert.Equal(t, sandBoxCluster.GetName(), "carbon.super_clusterSand_localhost_sandbox1.0", "Sandbox cluster name mismatch")

// 		sandBoxClusterHost := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
// 		sandBoxClusterPort := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

// 		assert.Equal(t, sandBoxClusterHost, "echo.websocket.org", "Production cluster host mismatch")
// 		assert.Equal(t, sandBoxClusterPort, uint32(80), "Production cluster port mismatch")

// 		assert.Equal(t, 1, len(routes), "Number of routes incorrect")

// 		// route := routes[0].GetMatch().GetSafeRegex().Regex
// 		// assert.Equal(t, route, "^/echowebsocketsand/1.0(\\?([^/]+))?$", "route created mismatch")

// 	}

// }

// this tests env props getting overriden for api.yaml provided values for websocket apis
func testCreateRoutesWithClustersWebsocketWithEnvProps(t *testing.T, apiYamlFilePath string) {
	envProps := synchronizer.APIEnvProps{
		EnvID: "some id",
		APIConfigs: synchronizer.APIConfigs{
			ProductionEndpoint: "ws://env.websocket.org:443",
			SandBoxEndpoint:    "ws://env.websocket.org:443",
		},
	}

	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v", apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v", apiYamlFilePath)

	var apiYaml model.APIYaml
	err = json.Unmarshal(apiJsn, &apiYaml)
	apiYaml = model.PopulateEndpointsInfo(apiYaml)
	assert.Nil(t, err, "Error occured while parsing api.yaml")
	var mgwSwagger model.MgwSwagger
	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiYaml, model.WS)
	mgwSwagger.SetEnvLabelProperties(envProps)
	assert.Nil(t, err, "Error while populating the MgwSwagger object for web socket APIs")
	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil, nil, "localhost", "carbon.super")

	assert.Equal(t, len(clusters), 2, "Number of clusters created incorrect")
	productionCluster := clusters[0]
	sandBoxCluster := clusters[1]

	productionClusterHost := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	productionClusterPort := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

	assert.Equal(t, productionClusterHost, "env.websocket.org", "Production cluster host mismatch")
	assert.Equal(t, productionClusterPort, uint32(443), "Production cluster port mismatch")

	sandBoxClusterHost := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	sandBoxClusterPort := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

	assert.Equal(t, sandBoxClusterHost, "env.websocket.org", "Sandbox cluster host mismatch")
	assert.Equal(t, sandBoxClusterPort, uint32(443), "Sandbox cluster port mismatch")
	assert.Equal(t, 1, len(routes), "Number of routes incorrect")

}

func TestCreateHealthEndpoint(t *testing.T) {
	route := envoy.CreateHealthEndpoint()
	assert.NotNil(t, route, "Health Endpoint Route should not be null.")
	assert.Equal(t, "/health", route.Name, "Health Route Name is incorrect.")
	assert.Equal(t, "/health", route.GetMatch().GetPath(), "Health route path is incorrect.")
	assert.Equal(t, "{\"status\": \"healthy\"}", route.GetDirectResponse().GetBody().GetInlineString(), "Health response message is incorrect.")
	assert.Equal(t, uint32(200), route.GetDirectResponse().GetStatus(), "Health response status is incorrect.")
}

// TODO: (VirajSalaka) Fix the cause for the intermittent failure
// func TestCreateRoutesWithClustersProdSandEp(t *testing.T) {
// 	// Tested Features
// 	// 1. Production Sandbox endpoint assignment for the routes.
// 	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_prod_sand_extensions.yaml"
// 	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
// 	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
// 	mgwSwaggerForOpenapi := operator.GetMgwSwagger(openapiByteArr)
// 	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi, nil)
//
// 	assert.Equal(t, 4, len(clusters), "Number of production clusters created is incorrect.")
// 	assert.Equal(t, 2, len(routes), "Created number of routes are incorrect.")
//
// 	if len(clusters) != 4 || len(routes) != 2 {
// 		return
// 	}
// 	apiLevelProdCluster := clusters[0]
// 	apiLevelSandCluster := clusters[1]
// 	pathLevelProdCluster := clusters[2]
// 	pathLevelSandCluster := clusters[3]
//
// 	assert.Equal(t, apiLevelProdCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
// 	assert.Contains(t, pathLevelProdCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")
// 	assert.Equal(t, apiLevelSandCluster.GetName(), "clusterSand_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
// 	assert.Contains(t, pathLevelSandCluster.GetName(), "clusterSand_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")
//
// 	apiLevelSandClusterHost := apiLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
// 		GetAddress().GetSocketAddress().GetAddress()
// 	apiLevelSandClusterPort := apiLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
// 		GetAddress().GetSocketAddress().GetPortValue()
// 	assert.NotEmpty(t, apiLevelSandClusterHost, "API Level Sandbox Cluster's assigned host should not be null")
// 	assert.Equal(t, "apiLevelSandEndpoint", apiLevelSandClusterHost, "API Level Sandbox Cluster's assigned host is incorrect.")
// 	assert.NotEmpty(t, apiLevelSandClusterPort, "API Level Sandbox Cluster's assigned port should not be null")
// 	assert.Equal(t, uint32(80), apiLevelSandClusterPort, "API Level Sandbox Cluster's assigned host is incorrect.")
//
// 	pathLevelSandClusterHost := pathLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
// 		GetAddress().GetSocketAddress().GetAddress()
// 	pathLevelSandClusterPort := pathLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
// 		GetAddress().GetSocketAddress().GetPortValue()
// 	assert.NotEmpty(t, pathLevelSandClusterHost, "Path Level Sandbox Cluster's assigned host should not be null")
// 	assert.Equal(t, "resourceLevelSandEndpoint", pathLevelSandClusterHost, "Path Level Sandbox Cluster's assigned host is incorrect.")
// 	assert.NotEmpty(t, pathLevelSandClusterPort, "Path Level Sandbox Cluster's assigned port should not be null")
// 	assert.Equal(t, uint32(443), pathLevelSandClusterPort, "Path Level Sandbox Cluster's assigned host is incorrect.")
//
// 	resourceLevelEndpointRoute := routes[0]
// 	apiLevelEndpointRoute := routes[1]
//
// 	extAuthPerRouteConfigAPILevel := &extAuthService.ExtAuthzPerRoute{}
// 	err = ptypes.UnmarshalAny(apiLevelEndpointRoute.
// 		TypedPerFilterConfig[wellknown.HTTPExternalAuthorization],
// 		extAuthPerRouteConfigAPILevel)
// 	assert.Nil(t, err, "Error while parsing ExtAuthzPerRouteConfig")
// 	assert.NotEmpty(t, extAuthPerRouteConfigAPILevel.GetCheckSettings(), "Check Settings per ext authz route should not be empty")
// 	assert.NotEmpty(t, extAuthPerRouteConfigAPILevel.GetCheckSettings().ContextExtensions,
// 		"ContextExtensions per ext authz route should not be empty")
//
// 	contextExtensionMapAPI := extAuthPerRouteConfigAPILevel.GetCheckSettings().ContextExtensions
// 	assert.Equal(t, apiLevelProdCluster.GetName(), contextExtensionMapAPI["prodClusterName"],
// 		"Production Cluster mismatch in route ext authz context. (API Level Endpoints)")
// 	assert.Equal(t, apiLevelSandCluster.GetName(), contextExtensionMapAPI["sandClusterName"],
// 		"Sandbox Cluster mismatch in route ext authz context. (API Level Endpoints)")
//
// 	extAuthPerRouteConfigPathLevel := &extAuthService.ExtAuthzPerRoute{}
// 	err = ptypes.UnmarshalAny(resourceLevelEndpointRoute.
// 		TypedPerFilterConfig[wellknown.HTTPExternalAuthorization],
// 		extAuthPerRouteConfigPathLevel)
// 	assert.Nil(t, err, "Error while parsing ExtAuthzPerRouteConfig")
// 	assert.NotEmpty(t, extAuthPerRouteConfigPathLevel.GetCheckSettings(), "Check Settings per ext authz route should not be empty")
// 	assert.NotEmpty(t, extAuthPerRouteConfigPathLevel.GetCheckSettings().ContextExtensions,
// 		"ContextExtensions per ext authz route should not be empty")
//
// 	contextExtensionMapPath := extAuthPerRouteConfigPathLevel.GetCheckSettings().ContextExtensions
// 	assert.Contains(t, pathLevelProdCluster.GetName(), contextExtensionMapPath["prodClusterName"],
// 		"Production Cluster mismatch in route ext authz context. (Path Level Endpoints)")
// 	assert.Contains(t, pathLevelSandCluster.GetName(), contextExtensionMapPath["sandClusterName"],
// 		"Sandbox Cluster mismatch in route ext authz context. (Path Level Endpoints)")
// }

func TestCreateRoutesWithClusters(t *testing.T) {

	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api.yaml"
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)

	var apiYaml model.APIYaml
	err = json.Unmarshal(apiJsn, &apiYaml)
	apiYaml = model.PopulateEndpointsInfo(apiYaml)
	assert.Nil(t, err, "Error occured while parsing api.yaml")
	var mgwSwagger model.MgwSwagger
	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiYaml, model.WS)
	assert.Nil(t, err, "Error while populating the MgwSwagger object for web socket APIs")
	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil, nil, "localhost", "carbon.super")
	assert.NotNil(t, routes, "CreateRoutesWithClusters failed: returned routes nil")
	assert.NotNil(t, clusters, "CreateRoutesWithClusters failed: returned clusters nil")
}

func TestLoadBalancedCluster(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/ws_api_loadbalance.yaml"
	commonTestForClusterPrioritiesInWebSocketAPI(t, openapiFilePath)
	commonTestForClusterPrioritiesInWebSocketAPIWithEnvProps(t, openapiFilePath)
}
func TestFailoverCluster(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/ws_api_failover.yaml"
	commonTestForClusterPrioritiesInWebSocketAPI(t, openapiFilePath)
	commonTestForClusterPrioritiesInWebSocketAPIWithEnvProps(t, openapiFilePath)
}

// commonTestForClusterPriorities use to test loadbalance/failover in WS apis
func commonTestForClusterPrioritiesInWebSocketAPI(t *testing.T, apiYamlFilePath string) {
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)

	var apiYaml model.APIYaml
	err = json.Unmarshal(apiJsn, &apiYaml)
	apiYaml = model.PopulateEndpointsInfo(apiYaml)
	assert.Nil(t, err, "Error occured while parsing api.yaml")
	var mgwSwagger model.MgwSwagger
	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiYaml, model.WS)
	assert.Nil(t, err, "Error while populating the MgwSwagger object for web socket APIs")
	_, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil, nil, "localhost", "carbon.super")

	assert.Equal(t, len(clusters), 2, "Number of clusters created incorrect")
	productionCluster := clusters[0]
	sandBoxCluster := clusters[1]

	productionClusterHost0 := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	productionClusterPort0 := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()
	productionClusterPriority0 := productionCluster.GetLoadAssignment().GetEndpoints()[0].Priority
	productionClusterHost1 := productionCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	productionClusterPort1 := productionCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()
	productionClusterPriority1 := productionCluster.GetLoadAssignment().GetEndpoints()[1].Priority

	sandBoxClusterHost0 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	sandBoxClusterPort0 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()
	sandBoxClusterPriority0 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].Priority
	sandBoxClusterHost1 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	sandBoxClusterPort1 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[1].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()
	sandBoxClusterPriority1 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[1].Priority

	assert.Equal(t, "primary.websocket.org", productionClusterHost0, "Production endpoint host mismatch")
	assert.Equal(t, uint32(443), productionClusterPort0, "Production endpoint port mismatch")
	assert.Equal(t, uint32(0), productionClusterPriority0, "Production endpoint priority mismatch")

	assert.Equal(t, "echo.websocket.org", productionClusterHost1, "Second production endpoint host mismatch")
	assert.Equal(t, uint32(80), productionClusterPort1, "Second production endpoint port mismatch")

	assert.Equal(t, sandBoxClusterHost0, "primary.websocket.org", "Sandbox cluster host mismatch")
	assert.Equal(t, sandBoxClusterPort0, uint32(443), "Sandbox cluster port mismatch")
	assert.Equal(t, uint32(0), sandBoxClusterPriority0, "Sandbox endpoint priority mismatch")

	assert.Equal(t, sandBoxClusterHost1, "echo.websocket.org", "Sandbox cluster host mismatch")
	assert.Equal(t, sandBoxClusterPort1, uint32(80), "Second sandbox cluster port mismatch")

	if strings.HasSuffix(apiYamlFilePath, "ws_api_loadbalance.yaml") {
		assert.Equal(t, uint32(0), productionClusterPriority1, "Second production endpoint port mismatch")
		assert.Equal(t, uint32(0), sandBoxClusterPriority1, "Second sandbox endpoint priority mismatch")
	}

	if strings.HasSuffix(apiYamlFilePath, "ws_api_failover.yaml") {
		assert.Equal(t, uint32(1), productionClusterPriority1, "Second production endpoint port mismatch")
		assert.Equal(t, uint32(1), sandBoxClusterPriority1, "Second sandbox endpoint priority mismatch")
	}
}

// commonTestForClusterPriorities use to test loadbalance/failover in WS apis
func commonTestForClusterPrioritiesInWebSocketAPIWithEnvProps(t *testing.T, apiYamlFilePath string) {
	envProps := synchronizer.APIEnvProps{
		EnvID: "some id",
		APIConfigs: synchronizer.APIConfigs{
			ProductionEndpoint: "ws://env.prod.websocket.org:80",
			SandBoxEndpoint:    "ws://env.sand.websocket.org:80",
		},
	}

	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v", apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v", apiYamlFilePath)

	var apiYaml model.APIYaml
	err = json.Unmarshal(apiJsn, &apiYaml)
	apiYaml = model.PopulateEndpointsInfo(apiYaml)
	assert.Nil(t, err, "Error occured while parsing api.yaml")
	var mgwSwagger model.MgwSwagger
	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiYaml, model.WS)
	mgwSwagger.SetEnvLabelProperties(envProps)
	assert.Nil(t, err, "Error while populating the MgwSwagger object for web socket APIs")
	_, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil, nil, "localhost", "carbon.super")

	assert.Equal(t, len(clusters), 2, "Number of clusters created incorrect")
	productionCluster := clusters[0]
	sandBoxCluster := clusters[1]

	productionClusterHost0 := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	productionClusterPort0 := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

	sandBoxClusterHost0 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
	sandBoxClusterPort0 := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

	assert.Equal(t, "env.prod.websocket.org", productionClusterHost0, "Production endpoint host mismatch")
	assert.Equal(t, uint32(80), productionClusterPort0, "Production endpoint port mismatch")

	assert.Equal(t, sandBoxClusterHost0, "env.sand.websocket.org", "Sandbox cluster host mismatch")
	assert.Equal(t, sandBoxClusterPort0, uint32(80), "Sandbox cluster port mismatch")
}
