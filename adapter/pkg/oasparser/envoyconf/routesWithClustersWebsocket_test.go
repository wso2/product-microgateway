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
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/config"
	envoy "github.com/wso2/micro-gw/pkg/oasparser/envoyconf"
	"github.com/wso2/micro-gw/pkg/oasparser/operator"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
)

func TestMgwSwaggerWebSocketProdAndSand(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api.yaml"
	testGetMgwSwaggerWebSocket(t, apiYamlFilePath)
}

func TestMgwSwaggerWebSocketProd(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api_prod.yaml"
	testGetMgwSwaggerWebSocket(t, apiYamlFilePath)
}

func TestMgwSwaggerWebSocketSand(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api_sand.yaml"
	testGetMgwSwaggerWebSocket(t, apiYamlFilePath)
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

func testGetMgwSwaggerWebSocket(t *testing.T, apiYamlFilePath string) {
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)
	mgwSwagger := operator.GetMgwSwaggerWebSocket(apiJsn)
	if strings.HasSuffix(apiYamlFilePath, "api.yaml") {
		assert.Equal(t, mgwSwagger.GetSwaggerVersion(), "WS", "constant swagger version WS for websocket mismatch")
		assert.Equal(t, mgwSwagger.GetTitle(), "EchoWebSocket", "mgwSwagger title mismatch")
		assert.Equal(t, mgwSwagger.GetVersion(), "1.0", "mgwSwagger version mistmatch")
		assert.Equal(t, mgwSwagger.GetXWso2Basepath(), "/echowebsocket/1.0", "xWso2Basepath mistmatch")
		productionEndpoints := mgwSwagger.GetProdEndpoints()
		productionEndpoint := productionEndpoints[0]
		assert.Equal(t, productionEndpoint.Host, "echo.websocket.org", "mgwSwagger production endpoint host mismatch")
		assert.Equal(t, productionEndpoint.Basepath, "/", "mgwSwagger production endpoint basepath mistmatch")
		assert.Equal(t, productionEndpoint.URLType, "ws", "mgwSwagger production endpoint URLType mismatch")
		var port uint32 = 80
		assert.Equal(t, productionEndpoint.Port, port, "mgwSwagger production endpoint port mismatch")
		sandboxEndpoints := mgwSwagger.GetSandEndpoints()
		sandboxEndpoint := sandboxEndpoints[0]
		assert.Equal(t, sandboxEndpoint.Host, "echo.websocket.org", "mgwSwagger sandbox endpoint host mismatch")
		assert.Equal(t, sandboxEndpoint.Basepath, "/", "mgwSwagger sandbox endpoint basepath mistmatch")
		assert.Equal(t, sandboxEndpoint.URLType, "ws", "mgwSwagger sandbox endpoint URLType mismatch")
		assert.Equal(t, sandboxEndpoint.Port, port, "mgwSwagger sandbox endpoint port mismatch")
	}
	if strings.HasSuffix(apiYamlFilePath, "api_prod.yaml") {
		assert.Equal(t, mgwSwagger.GetSwaggerVersion(), "WS", "constant swagger version WS for websocket mismatch")
		assert.Equal(t, mgwSwagger.GetTitle(), "prodws", "mgwSwagger title mismatch")
		assert.Equal(t, mgwSwagger.GetVersion(), "1.0", "mgwSwagger version mistmatch")
		assert.Equal(t, mgwSwagger.GetXWso2Basepath(), "/echowebsocketprod/1.0", "xWso2Basepath mistmatch")
		productionEndpoints := mgwSwagger.GetProdEndpoints()
		productionEndpoint := productionEndpoints[0]
		var port uint32 = 80
		assert.Equal(t, productionEndpoint.Host, "echo.websocket.org", "mgwSwagger production endpoint host mismatch")
		assert.Equal(t, productionEndpoint.Basepath, "/", "mgwSwagger production endpoint basepath mistmatch")
		assert.Equal(t, productionEndpoint.URLType, "ws", "mgwSwagger production endpoint URLType mismatch")
		assert.Equal(t, productionEndpoint.Port, port, "mgwSwagger production endpoint port mismatch")
		sandboxEndpoints := mgwSwagger.GetSandEndpoints()
		assert.Equal(t, len(sandboxEndpoints), 0, "mgwSwagger sandbox endpoints length mismatch")

	}
	if strings.HasSuffix(apiYamlFilePath, "api_sand.yaml") {
		assert.Equal(t, mgwSwagger.GetSwaggerVersion(), "WS", "constant swagger version WS for websocket mismatch")
		assert.Equal(t, mgwSwagger.GetTitle(), "sandbox", "mgwSwagger title mismatch")
		assert.Equal(t, mgwSwagger.GetVersion(), "1.0", "mgwSwagger version mistmatch")
		assert.Equal(t, mgwSwagger.GetXWso2Basepath(), "/echowebsocketsand/1.0", "xWso2Basepath mistmatch")
		var port uint32 = 80
		sandboxEndpoints := mgwSwagger.GetSandEndpoints()
		sandboxEndpoint := sandboxEndpoints[0]
		assert.Equal(t, sandboxEndpoint.Host, "echo.websocket.org", "mgwSwagger sandbox endpoint host mismatch")
		assert.Equal(t, sandboxEndpoint.Basepath, "/", "mgwSwagger sandbox endpoint basepath mistmatch")
		assert.Equal(t, sandboxEndpoint.URLType, "ws", "mgwSwagger sandbox endpoint URLType mismatch")
		assert.Equal(t, sandboxEndpoint.Port, port, "mgwSwagger sandbox endpoint port mismatch")
		productionEndpoints := mgwSwagger.GetProdEndpoints()
		assert.Equal(t, len(productionEndpoints), 0, "mgwSwagger sandbox endpoints length mismatch")
	}

}

func testCreateRoutesWithClustersWebsocket(t *testing.T, apiYamlFilePath string) {
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)
	mgwSwagger := operator.GetMgwSwaggerWebSocket(apiJsn)
	routes, clusters, _ := envoy.CreateRouteWithClustersWebSocket(mgwSwagger, nil)

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
