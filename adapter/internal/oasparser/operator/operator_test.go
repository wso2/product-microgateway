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
package operator_test

import (
	"context"
	"encoding/json"
	"io/ioutil"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/internal/oasparser/model"
	"github.com/wso2/micro-gw/internal/oasparser/operator"
	"github.com/wso2/micro-gw/internal/oasparser/utills"
)

func TestGetMgwSwagger(t *testing.T) {
	type getMgwSwaggerTestItem struct {
		inputSwagger                string
		resultApiProdEndpoints      []model.Endpoint
		resultResourceProdEndpoints []model.Endpoint
		message                     string
	}
	dataItems := []getMgwSwaggerTestItem{
		{
			inputSwagger: `{
				"swagger": "2.0",
				"host": "petstore.io:80",
				"basepath": "/api/v2"
							
							}`,
			resultApiProdEndpoints: []model.Endpoint{
				{
					Host:     "petstore.io",
					Basepath: "/api/v2",
					Port:     80,
					URLType:  "http",
				},
			},
			resultResourceProdEndpoints: nil,
			message:                     "api level endpoint provided as swagger 2 standard",
		},
		{
			inputSwagger: `openapi: "3.0.0"
servers:
  - url: http://petstore.io:80/api/v2`,
			resultApiProdEndpoints: []model.Endpoint{
				{
					Host:     "petstore.io",
					Basepath: "/api/v2",
					Port:     80,
					URLType:  "http",
				},
			},
			resultResourceProdEndpoints: nil,
			message:                     "api level endpoint provided as openApi3 standard",
		},
		{
			inputSwagger: `{
				"swagger": "2.0",
				"host": "facebook.io:80",
				"basepath": "/api/v2",
				"x-wso2-production-endpoints": {
				"urls": [
						"https://petstore.io:80/api/v2"
						],
						"type": "https"
				}
							
                           }`,
			resultApiProdEndpoints: []model.Endpoint{
				{
					Host:     "petstore.io",
					Basepath: "/api/v2",
					Port:     80,
					URLType:  "https",
				},
			},
			resultResourceProdEndpoints: nil,
			message:                     "api level endpoint provided as x-wso2 format and swagger2 standard",
		},
		{
			inputSwagger: `openapi: "3.0.0"
servers:
  - url: http://petstore.io:80/api/v2

x-wso2-production-endpoints:
  urls:
    - 'https://petstorecorrect.swagger.io:90/api/v3'
  type: https`,
			resultApiProdEndpoints: []model.Endpoint{
				{
					Host:     "petstorecorrect.swagger.io",
					Basepath: "/api/v3",
					Port:     90,
					URLType:  "https",
				},
			},
			resultResourceProdEndpoints: nil,
			message:                     "api level endpoint provided as x-wso2 format and openApi3 standard",
		},
	}

	for _, item := range dataItems {
		resultMgwSagger := operator.GetMgwSwagger([]byte(item.inputSwagger))

		assert.Equal(t, item.resultApiProdEndpoints, resultMgwSagger.GetProdEndpoints(), item.message)
		if resultMgwSagger.GetResources() != nil {
			assert.Equal(t, item.resultResourceProdEndpoints, resultMgwSagger.GetResources()[0].GetProdEndpoints(), item.message)
		}
	}
}

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

//Test execution for GetOpenAPIVersionAndJSONContent
func TestGetOpenAPIVersionAndJSONContent(t *testing.T) {

	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen"
	files, err := ioutil.ReadDir(apiYamlFilePath)

	if err != nil {
		t.Errorf("Unable to read test-resources")
	}

	for _, f := range files {
		if strings.HasSuffix(f.Name(), ".yaml") {
			testGetOpenAPIVersionAndJSONContent(t, apiYamlFilePath+"/"+f.Name())
		}
	}

}

//helper function to test GetOpenAPIVersionAndJSONContent
func testGetOpenAPIVersionAndJSONContent(t *testing.T, apiYamlFilePath string) {

	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the yaml file : %v"+apiYamlFilePath)
	swaggerVerison, apiJsn, err := operator.GetOpenAPIVersionAndJSONContent(apiYamlByteArr)

	assert.NotNil(t, swaggerVerison, "Swagger version should not be empty")
	assert.NotNil(t, apiJsn, "Swagger version should not be empty")

	// Check for swagger version
	if strings.HasSuffix(apiYamlFilePath, "/openapi.yaml") {
		assert.Equal(t, swaggerVerison, "3", "OpenAPI swagger version mismatch")
	}

	if strings.HasSuffix(apiYamlFilePath, "/api.yaml") {
		assert.Equal(t, swaggerVerison, "2", "Default swaggerVersion should be 2")
	}

	if strings.HasSuffix(apiYamlFilePath, "/openapi_with_prod_sand_extensions.yaml") {
		assert.Equal(t, swaggerVerison, "2", "swaggerVersion mismatch")
	}

	//validate apiJsn
	var v interface{}
	jsnerr := json.Unmarshal(apiJsn, &v)

	assert.Nil(t, jsnerr, "JSONcontent validation failed")

}

//Test execution for TestGetOpenAPIV3Struct
func TestGetOpenAPIV3Struct(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen"
	files, err := ioutil.ReadDir(apiYamlFilePath)

	if err != nil {
		t.Errorf("Unable to read test-resources")
	}

	for _, f := range files {
		if strings.HasSuffix(f.Name(), ".yaml") {
			testGetOpenAPIV3Struct(t, apiYamlFilePath+"/"+f.Name())
		}
	}
}

//helper function for TestGetOpenAPIV3Struct
func testGetOpenAPIV3Struct(t *testing.T, apiYamlFilePath string) {
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)

	mgwSwagger, _ := operator.GetOpenAPIV3Struct(apiJsn)

	assert.Nil(t, mgwSwagger.Validate(context.Background()), "MgwSwagger validation failed for : %v", apiYamlFilePath)

}

func testGetMgwSwaggerWebSocket(t *testing.T, apiYamlFilePath string) {
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)
	mgwSwagger := operator.GetMgwSwaggerWebSocket(apiJsn)
	if strings.HasSuffix(apiYamlFilePath, "api.yaml") {
		assert.Equal(t, mgwSwagger.GetAPIType(), "WS", "API type for websocket mismatch")
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
		assert.Equal(t, mgwSwagger.GetAPIType(), "WS", "API type for websocket mismatch")
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
		assert.Equal(t, mgwSwagger.GetAPIType(), "WS", "API type for websocket mismatch")
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
