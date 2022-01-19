/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package model_test

import (
	"encoding/json"
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
)

func TestSetInfoSwaggerWebSocket(t *testing.T) {

	type setInfoSwaggerWebSocketTestItem struct {
		input   model.MgwSwagger
		apiData map[string]interface{}
	}

	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/api.yaml"
	apiYamlByteArr, err := ioutil.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "Error while reading the api.yaml file : %v"+apiYamlFilePath)
	apiJsn, conversionErr := utills.ToJSON(apiYamlByteArr)
	assert.Nil(t, conversionErr, "YAML to JSON conversion error : %v"+apiYamlFilePath)

	var apiYaml model.APIYaml
	err = json.Unmarshal(apiJsn, &apiYaml)
	assert.Nil(t, err, "Error occured while parsing api.yaml")
	var mgwSwagger model.MgwSwagger
	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiYaml)
	assert.Nil(t, err, "Error while populating the MgwSwagger object for web socket APIs")

	dataItems := []setInfoSwaggerWebSocketTestItem{
		{
			input: mgwSwagger,
			apiData: map[string]interface{}{
				"data": map[string]interface{}{
					"id":      "a65d7b25-96aa-46b0-b635-97bb0731b31c",
					"name":    "EchoWebSocket",
					"version": "1.0",
					"context": "/echowebsocket",
					"endpointConfig": map[string]interface{}{
						"endpoint_type": "http",
						"sandbox_endpoints": map[string]interface{}{
							"url": "ws://echo.websocket.org:80",
						},
						"production_endpoints": map[string]interface{}{
							"url": "ws://echo.websocket.org:80",
						},
					},
				},
			},
		},
	}

	for _, item := range dataItems {
		assert.Nil(t, err, "Error while populating the mgwSwagger object for web sockets")
		assert.Equal(t, item.input.GetID(), item.apiData["data"].(map[string]interface{})["id"], "MgwSwagger id mismatch")
		assert.Equal(t, item.input.GetTitle(), item.apiData["data"].(map[string]interface{})["name"], "MgwSwagger title mismatch")
		assert.Equal(t, item.input.GetVersion(), item.apiData["data"].(map[string]interface{})["version"], "MgwSwagger version mismatch")
		assert.Equal(t, item.input.GetXWso2Basepath(), item.apiData["data"].(map[string]interface{})["context"].(string)+"/"+item.input.GetVersion(), "MgwSwagger XWso2BasePath mismatch")
		// TODO: (Vajira) add assertions for sandbox_endpoints & production_endpoints
	}

}

func TestValidate(t *testing.T) {
	openapiFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_prod_sand_extensions.yaml"
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := model.MgwSwagger{}
	err = mgwSwaggerForOpenapi.GetMgwSwagger(openapiByteArr)
	assert.Nil(t, err, "Error should not be present when openAPI definition is converted to a MgwSwagger object")
	err = mgwSwaggerForOpenapi.Validate()
	assert.Nil(t, err, "Validation Error should not be present when servers URL is properly provided.")

	mgwSwaggerForOpenapi.GetProdEndpoints().Endpoints[0].Host = ("/")
	err = mgwSwaggerForOpenapi.Validate()
	assert.NotNil(t, err, "Validation Error should not be present when production URL is /")

	mgwSwaggerForOpenapi.GetProdEndpoints().Endpoints[0].Host = ("abc.com")
	mgwSwaggerForOpenapi.GetSandEndpoints().Endpoints[0].Host = ("/")
	err = mgwSwaggerForOpenapi.Validate()
	assert.NotNil(t, err, "Validation Error should not be present when sandbox URL is /")

	mgwSwaggerForOpenapi.GetSandEndpoints().Endpoints[0].Host = ("/abc/abc")
	err = mgwSwaggerForOpenapi.Validate()
	assert.NotNil(t, err, "Validation Error should not be present when servers URL is relative URL")
}
