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
package utills

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestFindSwaggerVersion(t *testing.T) {

	type findSwaggerVersionTestItem struct {
		inputSwagger   string
		result  string
		message string
	}

	dataItems := []findSwaggerVersionTestItem {
		{
			inputSwagger: `{
							"swagger": "2.0",
							"host": "petstore.io",
							"basepath": "api/v2"
							
							}`,
            result: "2",
            message: "when swagger version is 2",
		},
		{
			inputSwagger: `{
							"openapi": "3.0",
							"host": "petstore.io",
							"basepath": "api/v2"
							
							}`,
			result: "3",
			message: "when openAPi version is 3",
		},
		{
			inputSwagger: `{
							"host": "petstore.io",
							"basepath": "api/v2"
							
							}`,
			result: "2",
			message: "when openAPi version is not provided",
		},
	}

	for _, item := range dataItems{
		apiJsn, _ := ToJSON([]byte(item.inputSwagger))
		resultswaggerVerison:= FindSwaggerVersion(apiJsn)

		assert.Equal(t, item.result, resultswaggerVerison, item.message)
	}
}
