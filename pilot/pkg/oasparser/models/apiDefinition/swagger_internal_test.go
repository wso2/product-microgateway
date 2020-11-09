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
	"testing"

	"github.com/go-openapi/spec"
	"github.com/stretchr/testify/assert"
)

func TestSetInfoSwagger(t *testing.T) {
	type setInfoTestItem struct {
		input   spec.Swagger
		result  MgwSwagger
		message string
	}
	dataItems := []setInfoTestItem{
		{
			spec.Swagger{
				VendorExtensible: spec.VendorExtensible{},
				SwaggerProps: spec.SwaggerProps{
					ID:      "v1",
					Swagger: "swagger",
					Info: &spec.Info{
						InfoProps: spec.InfoProps{
							Description: "Swagger definition",
							Title:       "petsore",
							Version:     "1.0",
						},
					},
				},
			},

			MgwSwagger{
				id:             "v1",
				swaggerVersion: "swagger",
				description:    "Swagger definition",
				title:          "petsore",
				version:        "1.0",
			},
			"usual case",
		},
		{
			spec.Swagger{
				VendorExtensible: spec.VendorExtensible{},
				SwaggerProps: spec.SwaggerProps{
					ID:      "",
					Swagger: "swagger",
					Info:    nil,
				},
			},
			MgwSwagger{
				id:             "",
				swaggerVersion: "swagger",
				description:    "",
				title:          "",
				version:        "",
			},
			"when info section is null",
		},
	}
	for _, item := range dataItems {
		var mgwSwagger MgwSwagger
		mgwSwagger.SetInfoSwagger(item.input)
		assert.Equal(t, item.result, mgwSwagger, item.message)
	}
}

func TestSetResourcesSwagger(t *testing.T) {
	type setResourcesTestItem struct {
		input   spec.Swagger
		result  []Resource
		message string
	}
	dataItems := []setResourcesTestItem{
		{
			spec.Swagger{
				SwaggerProps: spec.SwaggerProps{
					Paths: nil,
				},
			},
			nil,
			"when paths are nil",
		},
		{
			spec.Swagger{
				SwaggerProps: spec.SwaggerProps{
					Paths: &spec.Paths{
						Paths: map[string]spec.PathItem{"/pet/{petId}": {
							PathItemProps: spec.PathItemProps{
								Get: &spec.Operation{
									OperationProps: spec.OperationProps{
										Description: "this retrieve data from id",
										Summary:     "pet find by id",
										ID:          "petfindbyid",
									},
								},
							},
						}},
					},
				},
			},
			[]Resource{
				{
					path:        "/pet/{petId}",
					methods:     []string{"GET"},
					description: "this retrieve data from id",
					iD:          "petfindbyid",
					summary:     "pet find by id",
				},
			},
			"usual case",
		},
	}
	for _, item := range dataItems {
		resultResources := SetResourcesSwagger(item.input)
		if item.result != nil {
			assert.Equal(t, item.result[0].path, resultResources[0].GetPath(), item.message)
			assert.Equal(t, item.result[0].methods, resultResources[0].GetMethod(), item.message)
		} else {
			assert.Equal(t, item.result, resultResources, item.message)
		}
	}
}
