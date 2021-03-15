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
	"io/ioutil"
	"strings"
	"testing"

	"github.com/wso2/micro-gw/internal/oasparser/utills"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/config"

	//envoy "github.com/wso2/micro-gw/internal/oasparser/envoyconf"
	"github.com/wso2/micro-gw/internal/oasparser/operator"
)

func TestCreateRoutesWithClustersForOpenAPIWithoutExtensions(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi.yaml"
	commonTestForCreateRoutesWithClusters(t, openapiFilePath)
	// TODO: (VirajSalaka) Additional tasks to test
	// OpenAPI version 2
}

func TestCreateRoutesWithClustersForOpenAPIWithExtensionsOnly(t *testing.T) {
	// When the openapi endpoints are only mentioned via the extensions
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_extensions_only.yaml"
	commonTestForCreateRoutesWithClusters(t, openapiFilePath)
}

func TestCreateRoutesWithClustersForOpenAPIWithExtensionsServers(t *testing.T) {
	// When the openapi endpoints provided by servers object are overriden via the extensions
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_extensions_servers.yaml"
	commonTestForCreateRoutesWithClusters(t, openapiFilePath)
}

func TestCreateRouteswithClustersWebsocketProdSand(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api.yaml"
	testCreateRoutesWithClustersWebsocket(t, apiYamlFilePath)
}

func TestCreateRouteswithClustersWebsocketProd(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api_prod.yaml"
	testCreateRoutesWithClustersWebsocket(t, apiYamlFilePath)
}

func TestCreateRouteswithClustersWebsocketSand(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api_sand.yaml"
	testCreateRoutesWithClustersWebsocket(t, apiYamlFilePath)
}

func commonTestForCreateRoutesWithClusters(t *testing.T, openapiFilePath string) {
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := operator.GetMgwSwagger(openapiByteArr)
	//routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi, nil)
	routes, clusters, _ := CreateRoutesWithClusters(mgwSwaggerForOpenapi, nil)

	assert.Equal(t, 2, len(clusters), "Number of production clusters created is incorrect.")
	// As the first cluster is always related to API level cluster
	apiLevelCluster := clusters[0]
	pathLevelCluster := clusters[1]
	assert.Equal(t, apiLevelCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
	assert.Contains(t, pathLevelCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")

	apiLevelClusterHost := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	apiLevelClusterPort := apiLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	assert.NotEmpty(t, apiLevelClusterHost, "API Level Cluster's assigned host should not be null")
	assert.Equal(t, "apiLevelEndpoint", apiLevelClusterHost, "API Level Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, apiLevelClusterPort, "API Level Cluster's assigned port should not be null")
	assert.Equal(t, uint32(80), apiLevelClusterPort, "API Level Cluster's assigned host is incorrect.")

	pathLevelClusterHost := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	pathLevelClusterPort := pathLevelCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	assert.NotEmpty(t, pathLevelClusterHost, "Path Level Cluster's assigned host should not be null")
	assert.Equal(t, "resourceLevelEndpoint", pathLevelClusterHost, "Path Level Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, pathLevelClusterPort, "Path Level Cluster's assigned port should not be null")
	assert.Equal(t, uint32(443), pathLevelClusterPort, "Path Level Cluster's assigned host is incorrect.")

	assert.Equal(t, 2, len(routes), "Created number of routes are incorrect.")
	assert.Contains(t, []string{"^/pets(\\?([^/]+))?$", "^/pets/([^/]+)(\\?([^/]+))?$"}, routes[0].GetMatch().GetSafeRegex().Regex)
	assert.Contains(t, []string{"^/pets(\\?([^/]+))?$", "^/pets/([^/]+)(\\?([^/]+))?$"}, routes[1].GetMatch().GetSafeRegex().Regex)
	assert.NotEqual(t, routes[0].GetMatch().GetSafeRegex().Regex, routes[1].GetMatch().GetSafeRegex().Regex,
		"The route regex for the two routes should not be the same")
	routeRegexMatchesFound := false
	// route entity creation is tested separately. In here, it checks the connection between the route and the cluster
	for _, route := range routes {
		if route.GetMatch().GetSafeRegex().Regex == "^/pets(\\?([^/]+))?$" {
			routeRegexMatchesFound = true
			assert.Equal(t, pathLevelCluster.GetName(), route.GetRoute().GetCluster(), "Path level cluster is not set correctly.")
		}
		if route.GetMatch().GetSafeRegex().Regex == "^/pets/([^/]+)(\\?([^/]+))?$" {
			routeRegexMatchesFound = true
			assert.Equal(t, apiLevelCluster.GetName(), route.GetRoute().GetCluster(), "API level cluster is not set correctly.")
		}
	}
	assert.Equal(t, true, routeRegexMatchesFound, "Generated route regex is incorrect.")
}

func testCreateRoutesWithClustersWebsocket(t *testing.T, apiYamlFilePath string) {
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)
	mgwSwagger := operator.GetMgwSwaggerWebSocket(apiJsn)
	//routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil)
	routes, clusters, _ := CreateRoutesWithClusters(mgwSwagger, nil)

	if strings.HasSuffix(apiYamlFilePath, "api.yaml") {
		assert.Equal(t, len(clusters), 2, "Number of clusters created incorrect")
		productionCluster := clusters[0]
		sandBoxCluster := clusters[1]
		assert.Equal(t, productionCluster.GetName(), "clusterProd_EchoWebSocket1.0", "Production cluster name mismatch")
		assert.Equal(t, sandBoxCluster.GetName(), "clusterSand_EchoWebSocket1.0", "Sandbox cluster name mismatch")

		productionClusterHost := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
		productionClusterPort := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

		assert.Equal(t, productionClusterHost, "echo.websocket.org", "Production cluster host mismatch")
		assert.Equal(t, productionClusterPort, uint32(80), "Production cluster port mismatch")

		sandBoxClusterHost := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
		sandBoxClusterPort := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

		assert.Equal(t, sandBoxClusterHost, "echo.websocket.org", "Sandbox cluster host mismatch")
		assert.Equal(t, sandBoxClusterPort, uint32(80), "Sandbox cluster port mismatch")

		assert.Equal(t, 1, len(routes), "Number of routes incorrect")

		route := routes[0].GetMatch().GetSafeRegex().Regex
		assert.Equal(t, route, "^/echowebsocket/1.0(\\?([^/]+))?$", "route created mismatch")
	}
	if strings.HasSuffix(apiYamlFilePath, "api_prod.yaml") {
		assert.Equal(t, len(clusters), 1, "Number of clusters created incorrect")
		productionCluster := clusters[0]
		assert.Equal(t, productionCluster.GetName(), "clusterProd_prodws1.0", "Production cluster name mismatch")

		productionClusterHost := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
		productionClusterPort := productionCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

		assert.Equal(t, productionClusterHost, "echo.websocket.org", "Production cluster host mismatch")
		assert.Equal(t, productionClusterPort, uint32(80), "Production cluster port mismatch")

		assert.Equal(t, 1, len(routes), "Number of routes incorrect")

		route := routes[0].GetMatch().GetSafeRegex().Regex
		assert.Equal(t, route, "^/echowebsocketprod/1.0(\\?([^/]+))?$", "route created mismatch")

	}
	if strings.HasSuffix(apiYamlFilePath, "api_sand.yaml") {
		assert.Equal(t, len(clusters), 1, "Number of clusters created incorrect")
		sandBoxCluster := clusters[0]
		assert.Equal(t, sandBoxCluster.GetName(), "clusterSand_sandbox1.0", "Production cluster name mismatch")

		sandBoxClusterHost := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress()
		sandBoxClusterPort := sandBoxCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue()

		assert.Equal(t, sandBoxClusterHost, "echo.websocket.org", "Production cluster host mismatch")
		assert.Equal(t, sandBoxClusterPort, uint32(80), "Production cluster port mismatch")

		assert.Equal(t, 1, len(routes), "Number of routes incorrect")

		route := routes[0].GetMatch().GetSafeRegex().Regex
		assert.Equal(t, route, "^/echowebsocketsand/1.0(\\?([^/]+))?$", "route created mismatch")

	}

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
	mgwSwagger := operator.GetMgwSwaggerWebSocket(apiJsn)
	//routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwagger, nil)
	routes, clusters, _ := CreateRoutesWithClusters(mgwSwagger, nil)
	t.Log(routes)
	t.Log(clusters)
}

// func TestCreateCluster()
