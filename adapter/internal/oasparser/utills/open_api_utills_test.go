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

package utills_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
)

func TestFindSwaggerVersion(t *testing.T) {
	type findSwaggerVersionTestItem struct {
		inputSwagger string
		result       string
		message      string
	}
	dataItems := []findSwaggerVersionTestItem{
		{
			inputSwagger: `{
				"swagger": "2.0",
				"host": "petstore.io",
				"basepath": "api/v2"
							
				}`,
			result:  constants.Swagger2,
			message: "when swagger version is 2",
		},
		{
			inputSwagger: `{
				"openapi": "3.0",
				"host": "petstore.io",
				"basepath": "api/v2"
							
				}`,
			result:  constants.OpenAPI3,
			message: "when openAPi version is 3",
		},
		{
			inputSwagger: `{
				"asyncapi": "2.0.0"
				
				}`,
			result:  constants.AsyncAPI2,
			message: "when asyncAPI version is 2",
		},
		{
			inputSwagger: `{
				"asyncapi": "5.0.0"
							
				}`,
			result:  constants.NotSupported,
			message: "when asyncAPI version is not supported",
		},
		{
			inputSwagger: `{
				"host": "petstore.io",
				"basepath": "api/v2"
							
				}`,
			result:  constants.NotDefined,
			message: "when openAPi version is not provided",
		},
	}

	for _, item := range dataItems {
		apiJsn, _ := utills.ToJSON([]byte(item.inputSwagger))
		actualApiVersion := utills.FindAPIDefinitionVersion(apiJsn)

		assert.Equal(t, item.result, actualApiVersion, item.message)
	}
}

func TestFileNameWithoutExtension(t *testing.T) {
	type testItem struct {
		filePath    string
		expFileName string
		message     string
	}

	tests := []testItem{
		{
			filePath:    "/foo/hello.world",
			expFileName: "hello",
			message:     "given path with file extension with dir in path",
		},
		{
			filePath:    "hello.world",
			expFileName: "hello",
			message:     "given path with extensions with no dir in path",
		},
		{
			filePath:    "/foo/hello",
			expFileName: "hello",
			message:     "given path with no extensions",
		},
	}

	for _, test := range tests {
		actualFileName := utills.FileNameWithoutExtension(test.filePath)
		assert.Equal(t, test.expFileName, actualFileName, test.message)
	}
}
