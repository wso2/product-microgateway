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
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/config"
	envoy "github.com/wso2/micro-gw/pkg/oasparser/envoyconf"
	"github.com/wso2/micro-gw/pkg/oasparser/operator"
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

func commonTestForCreateRoutesWithClusters(t *testing.T, openapiFilePath string) {
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := operator.GetMgwSwagger(openapiByteArr)
	// TODO: (VirajSalaka) Test Sandbox endpoints.
	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi)

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

func TestCreateRoutesWithClustersProdSandEp(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_prod_sand_extensions.yaml"
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := operator.GetMgwSwagger(openapiByteArr)
	routes, clusters, _ := envoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi)

	assert.Equal(t, 4, len(clusters), "Number of production clusters created is incorrect.")
	assert.Equal(t, 2, len(routes), "Created number of routes are incorrect.")

	if len(clusters) != 4 || len(routes) != 2 {
		return
	}
	apiLevelProdCluster := clusters[0]
	apiLevelSandCluster := clusters[1]
	pathLevelProdCluster := clusters[2]
	pathLevelSandCluster := clusters[3]

	assert.Equal(t, apiLevelProdCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
	assert.Contains(t, pathLevelProdCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")
	assert.Equal(t, apiLevelSandCluster.GetName(), "clusterSand_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
	assert.Contains(t, pathLevelSandCluster.GetName(), "clusterSand_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")

	apiLevelSandClusterHost := apiLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	apiLevelSandClusterPort := apiLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	assert.NotEmpty(t, apiLevelSandClusterHost, "API Level Sandbox Cluster's assigned host should not be null")
	assert.Equal(t, "apiLevelSandEndpoint", apiLevelSandClusterHost, "API Level Sandbox Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, apiLevelSandClusterPort, "API Level Sandbox Cluster's assigned port should not be null")
	assert.Equal(t, uint32(80), apiLevelSandClusterPort, "API Level Sandbox Cluster's assigned host is incorrect.")

	pathLevelSandClusterHost := pathLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetAddress()
	pathLevelSandClusterPort := pathLevelSandCluster.GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().
		GetAddress().GetSocketAddress().GetPortValue()
	assert.NotEmpty(t, pathLevelSandClusterHost, "Path Level Sandbox Cluster's assigned host should not be null")
	assert.Equal(t, "resourceLevelSandEndpoint", pathLevelSandClusterHost, "Path Level Sandbox Cluster's assigned host is incorrect.")
	assert.NotEmpty(t, pathLevelSandClusterPort, "Path Level Sandbox Cluster's assigned port should not be null")
	assert.Equal(t, uint32(443), pathLevelSandClusterPort, "Path Level Sandbox Cluster's assigned host is incorrect.")
}
