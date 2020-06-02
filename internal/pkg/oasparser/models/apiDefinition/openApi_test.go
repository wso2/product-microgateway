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

package apiDefinition

import (
	"fmt"
	"github.com/getkin/kin-openapi/openapi3"
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func TestSetInfoOpenApi(t *testing.T) {
	type setInfoTestItem struct {
		input openapi3.Swagger
		result MgwSwagger
		message string
	}
	dataItems := []setInfoTestItem{
		{
			openapi3.Swagger{
				OpenAPI: "openApi",
				Info:    &openapi3.Info{
					Title:       "petsore",
					Description: "Swagger definition",
					Version:     "1.0",
				},
			},

			MgwSwagger{
				swaggerVersion:   "openApi",
				description:      "Swagger definition",
				title:            "petsore",
				version:          "1.0",
			},
			"usual case",
		},
		{
			openapi3.Swagger{
				OpenAPI: "openApi",
				Info: nil,
			},
			MgwSwagger{
				id:               "",
				swaggerVersion:   "openApi",
				description:      "",
				title:            "",
				version:          "",
			},
			"when info section is null",
		},
	}
	for _, item := range dataItems{
		var mgwSwagger MgwSwagger
		mgwSwagger.SetInfoOpenApi(item.input)
		assert.Equal(t, item.result, mgwSwagger, item.message)
	}
}

func TestSetResourcesOpenApi(t *testing.T) {
	type setResourcesTestItem struct {
		input openapi3.Swagger
		result []Resource
		message string
	}
	dataItems := []setResourcesTestItem {
		{
			openapi3.Swagger{
				Paths: nil,
			},
			nil,
			"when paths are nil",
		},
		{
			openapi3.Swagger{
				Paths: openapi3.Paths{
					"/pet/{petId}": &openapi3.PathItem{
						Get: &openapi3.Operation{
							Summary:     "pet find by id",
							Description: "this retrieve data from id",
							OperationID: "petfindbyid",
						},
					},
				},
			},
			[]Resource{
				{
					path: "/pet/{petId}",
					pathtype: "get",
					description: "this retrieve data from id",
					iD:  "petfindbyid",
					summary: "pet find by id",
				},
			},
			"usual case",
		},
	}
	for _, item := range dataItems{
		resultResources := SetResourcesOpenApi(item.input)
		assert.Equal(t, item.result, resultResources, item.message)
	}
}

func TestGetHostandBasepathandPort(t *testing.T) {
	type setResourcesTestItem struct {
		input string
		result Endpoint
		message string
	}
	fmt.Println(os.Getwd())
	dataItems := []setResourcesTestItem {
		{
			input: "https://petstore.io:8000/api/v2",
			result: Endpoint{
				Host:     "petstore.io",
				Basepath: "/api/v2",
				Port:     8000,
			},
			message: "all the details are provided in the endpoint",
		},
		{
			input: "https://petstore.io:8000/api/v2",
			result: Endpoint{
				Host:     "petstore.io",
				Basepath: "/api/v2",
				Port:     8000,
			},
			message: "when port is not provided",  //here should find a way to readi config in tests
		},
		{
			input: "petstore.io:8000/api/v2",
			result: Endpoint{
				Host:     "petstore.io",
				Basepath: "/api/v2",
				Port:     8000,
			},
			message: "when protocol is not provided",
		},
	}
	for _, item := range dataItems{
		resultResources := getHostandBasepathandPort(item.input)
		assert.Equal(t, item.result, resultResources, item.message)
	}
}


