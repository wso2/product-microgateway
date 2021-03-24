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
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/config"
	model "github.com/wso2/micro-gw/internal/oasparser/model"
	"github.com/wso2/micro-gw/internal/oasparser/operator"
	"github.com/wso2/micro-gw/internal/oasparser/utills"
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
	mgwSwagger := operator.GetMgwSwaggerWebSocket(apiJsn)

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
		item.input.SetInfoSwaggerWebSocket(item.apiData)
		assert.Equal(t, item.input.GetID(), item.apiData["data"].(map[string]interface{})["id"], "MgwSwagger id mismatch")
		assert.Equal(t, item.input.GetTitle(), item.apiData["data"].(map[string]interface{})["name"], "MgwSwagger title mismatch")
		assert.Equal(t, item.input.GetVersion(), item.apiData["data"].(map[string]interface{})["version"], "MgwSwagger version mismatch")
		assert.Equal(t, item.input.GetXWso2Basepath(), item.apiData["data"].(map[string]interface{})["context"].(string)+"/"+item.input.GetVersion(), "MgwSwagger XWso2BasePath mismatch")
		// TODO: (Vajira) add assertions for sandbox_endpoints & production_endpoints
	}

}
