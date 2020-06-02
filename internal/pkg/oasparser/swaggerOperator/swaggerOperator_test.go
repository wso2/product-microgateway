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
package swaggerOperator

import (
	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/models/apiDefinition"
	"testing"
)

func TestGetMgwSwagger(t *testing.T) {
	type getMgwSwaggerTestItem struct {
		inputSwagger   string
		resultApiProdEndpoints []apiDefinition.Endpoint
		resultResourceProdEndpoints []apiDefinition.Endpoint
		message string
	}
	dataItems := []getMgwSwaggerTestItem {
		{
			inputSwagger: `{
							"swagger": "2.0",
							"host": "petstore.io:80",
							"basepath": "/api/v2"
							
                           }`,
			resultApiProdEndpoints: []apiDefinition.Endpoint{
				{
					Host: "petstore.io",
					Basepath: "/api/v2",
					Port: 80,
				},
			},
			resultResourceProdEndpoints: nil,
			message: "api level endpoint provided as swagger 2 standard",
		},
		{
			inputSwagger: `openapi: "3.0.0"
servers:
  - url: http://petstore.io:80/api/v2`,
			resultApiProdEndpoints: []apiDefinition.Endpoint{
				{
					Host: "petstore.io",
					Basepath: "/api/v2",
					Port: 80,
				},
			},
			resultResourceProdEndpoints: nil,
			message: "api level endpoint provided as openApi3 standard",
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
			resultApiProdEndpoints: []apiDefinition.Endpoint{
				{
					Host: "petstore.io",
					Basepath: "/api/v2",
					Port: 80,
				},
			},
			resultResourceProdEndpoints: nil,
			message: "api level endpoint provided as x-wso2 format and swagger2 standard",
		},
		{
			inputSwagger: `openapi: "3.0.0"
servers:
  - url: http://petstore.io:80/api/v2

x-wso2-production-endpoints:
  urls:
    - 'https://petstorecorrect.swagger.io:90/api/v3'
  type: https`,
			resultApiProdEndpoints: []apiDefinition.Endpoint{
				{
					Host: "petstorecorrect.swagger.io",
					Basepath: "/api/v3",
					Port: 90,
					UrlType: "https",
				},
			},
			resultResourceProdEndpoints: nil,
			message: "api level endpoint provided as x-wso2 format and openApi3 standard",
		},
	}

	for _, item := range dataItems{
		resultMgwSagger := GetMgwSwagger([]byte(item.inputSwagger))

		assert.Equal(t, item.resultApiProdEndpoints, resultMgwSagger.GetProdEndpoints(), item.message)
		if resultMgwSagger.GetResources() != nil {
			assert.Equal(t, item.resultResourceProdEndpoints, resultMgwSagger.GetResources()[0].GetProdEndpoints(), item.message)
		}
	}
}