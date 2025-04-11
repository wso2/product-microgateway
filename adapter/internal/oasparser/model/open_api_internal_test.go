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

package model

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/pb33f/libopenapi"
	validator "github.com/pb33f/libopenapi-validator"

	"github.com/pb33f/libopenapi/datamodel/high/base"
	v3 "github.com/pb33f/libopenapi/datamodel/high/v3"
	"github.com/pb33f/libopenapi/orderedmap"
	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
)

func TestSetInfoOpenAPI(t *testing.T) {
	type setInfoTestItem struct {
		input   openapi3.Swagger
		result  MgwSwagger
		message string
	}
	dataItems := []setInfoTestItem{
		{
			openapi3.Swagger{
				OpenAPI: "openApi",
				Info: &openapi3.Info{
					Title:       "petsore",
					Description: "Swagger definition",
					Version:     "1.0",
				},
			},

			MgwSwagger{
				apiType:     "HTTP",
				description: "Swagger definition",
				title:       "petsore",
				version:     "1.0",
			},
			"usual case",
		},
		{
			openapi3.Swagger{
				OpenAPI: "openApi",
				Info:    nil,
			},
			MgwSwagger{
				id:          "",
				apiType:     "HTTP",
				description: "",
				title:       "",
				version:     "",
			},
			"when info section is null",
		},
	}
	for _, item := range dataItems {
		var mgwSwagger MgwSwagger
		err := mgwSwagger.SetInfoOpenAPI(item.input)
		assert.Nil(t, err, "Error should not be present when openAPI v3 definition is converted to a MgwSwagger object")
		assert.Equal(t, item.result, mgwSwagger, item.message)
	}
}

func TestSetResourcesOpenAPI(t *testing.T) {
	type setResourcesTestItem struct {
		input   openapi3.Swagger
		result  []*Resource
		message string
	}

	dataItems := []setResourcesTestItem{
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
							OperationID: "petfindbyid",
						},
						Summary:     "pet find by id",
						Description: "this retrieve data from id",
					},
				},
			},
			[]*Resource{
				{
					path:        "/pet/{petId}",
					methods:     []*Operation{{"", "GET", nil, "", false, nil, ""}},
					description: "this retrieve data from id",
					iD:          "petfindbyid",
					summary:     "pet find by id",
				},
			},
			"usual case",
		},
	}
	for _, item := range dataItems {
		resultResources, err := setResourcesOpenAPI(item.input)
		assert.Nil(t, err, "No error should be encountered when setting resources")
		if item.result != nil {
			assert.Equal(t, item.result[0].path, resultResources[0].GetPath(), item.message)
			resultResources[0].GetMethod()[0].iD = item.result[0].methods[0].iD
			assert.Equal(t, item.result[0].methods, resultResources[0].GetMethod(), item.message)
			assert.Equal(t, item.result[0].description, resultResources[0].description, item.message)
			assert.Equal(t, item.result[0].summary, resultResources[0].summary, item.message)
		} else {
			assert.Equal(t, item.result, resultResources, item.message)
		}
	}
}

func TestGetHostandBasepathandPort(t *testing.T) {
	type setResourcesTestItem struct {
		input   string
		result  *Endpoint
		message string
	}
	fmt.Println(os.Getwd())
	dataItems := []setResourcesTestItem{
		{
			input: "https://petstore.io:8000/api/v2",
			result: &Endpoint{
				Host:     "petstore.io",
				Basepath: "/api/v2",
				Port:     8000,
				URLType:  "https",
				RawURL:   "https://petstore.io:8000/api/v2",
			},
			message: "all the details are provided in the endpoint",
		},
		{
			input: "https://petstore.io:8000/api/v2",
			result: &Endpoint{
				Host:     "petstore.io",
				Basepath: "/api/v2",
				Port:     8000,
				URLType:  "https",
				RawURL:   "https://petstore.io:8000/api/v2",
			},
			message: "when port is not provided", //here should find a way to readi configs in tests
		},
		{
			input: "petstore.io:8000/api/v2",
			result: &Endpoint{
				Host:     "petstore.io",
				Basepath: "/api/v2",
				Port:     8000,
				URLType:  "http",
				RawURL:   "http://petstore.io:8000/api/v2",
			},
			message: "when protocol is not provided",
		},
		{
			input:   "https://{defaultHost}",
			result:  nil,
			message: "when malformed endpoint is provided",
		},
	}
	for _, item := range dataItems {
		resultResources, err := getHTTPEndpoint(item.input)
		assert.Equal(t, item.result, resultResources, item.message)
		if resultResources != nil {
			assert.Nil(t, err, "Error encountered when processing the endpoint")
		} else {
			assert.NotNil(t, err, "Should return an error upon failing to process the endpoint")
		}
	}
}

func TestGetXWso2Label(t *testing.T) {
	// TODO: (Vajira) add more test scenarios
	//newLabels := GetXWso2Label(openAPIV3Struct.ExtensionProps)
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_xwso2label.yaml"
	swagger, err := openapi3.NewSwaggerLoader().LoadSwaggerFromFile(apiYamlFilePath)
	assert.Nil(t, err, "Swagger loader failed")

	assert.Nil(t, swagger.Validate(context.Background()), "Swagger Validation Failed")

	wso2Label := GetXWso2Label(swagger.ExtensionProps)

	assert.NotNil(t, wso2Label, "Lable should at leaset be default")

}

func TestMalformedUrl(t *testing.T) {

	suspectedRawUrls := []string{
		"http://#de.abc.com:80/api",
		"http://&de.abc.com:80/api",
		"http://!de.abc.com:80/api",
		"tcp://http::8900",
		"http://::80",
		"http::80",
		"-",
		"api.worldbank.org-",
		"-api.worldbank.org",
		"",
	}

	for index := range suspectedRawUrls {
		response, _ := getHTTPEndpoint(suspectedRawUrls[index])
		assert.Nil(t, response)
	}

}

func TestSetInfoOpenAPI31(t *testing.T) {
	type setInfoTestItem struct {
		input   v3.Document
		result  MgwSwagger
		message string
	}
	dataItems := []setInfoTestItem{
		{
			v3.Document{
				Info: &base.Info{
					Title:       "petsore",
					Description: "Swagger definition",
					Version:     "1.0",
				},
			},

			MgwSwagger{
				apiType:     "HTTP",
				description: "Swagger definition",
				title:       "petsore",
				version:     "1.0",
			},
			"usual case",
		},
		{
			v3.Document{
				Info: nil,
			},
			MgwSwagger{
				id:          "",
				apiType:     "HTTP",
				description: "",
				title:       "",
				version:     "",
			},
			"when info section is null",
		},
		{
			input: v3.Document{},
			result: MgwSwagger{
				id:          "",
				apiType:     "HTTP",
				description: "",
				title:       "",
				version:     "",
			},
			message: "when openAPI struct is empty",
		},
	}
	for _, item := range dataItems {
		var mgwSwagger MgwSwagger
		err := mgwSwagger.SetInfoOpenAPIV31(item.input)
		assert.Nil(t, err, "Error should not be present when openAPI v3 definition is converted to a MgwSwagger object")
		assert.Equal(t, item.result, mgwSwagger, item.message)
	}
}

func TestSetResourcesOpenAPI31(t *testing.T) {
	type setResourcesTestItem struct {
		input   v3.Document
		result  []*Resource
		message string
	}

	// Setting up a mock path item and path items ordered map.
	// Mock PathItem with GET method
	pathItem1 := &v3.PathItem{
		Get: &v3.Operation{
			OperationId: "petfindbyid",
		},
		Summary:     "pet find by id",
		Description: "this retrieve data from id",
	}
	pathItems1 := orderedmap.New[string, *v3.PathItem]()
	pathItems1.Set("/pet/{petId}", pathItem1)

	// Mock PathItem without any methods
	// OAS 3.1 can have paths without any path items defined inside.
	pathItem2 := &v3.PathItem{
		Summary:     "pet find by id",
		Description: "this retrieve data from id",
	}
	pathItems2 := orderedmap.New[string, *v3.PathItem]()
	pathItems2.Set("/pet/{petId}", pathItem2)

	dataItems := []setResourcesTestItem{
		{
			v3.Document{
				Paths: nil,
			},
			nil,
			"when paths are nil",
		},
		{
			v3.Document{
				Paths: &v3.Paths{
					PathItems:  pathItems1,
					Extensions: nil,
				},
			},
			[]*Resource{
				{
					path:        "/pet/{petId}",
					methods:     []*Operation{{"", "GET", nil, "", false, nil, ""}},
					description: "this retrieve data from id",
					iD:          "petfindbyid",
					summary:     "pet find by id",
				},
			},
			"usual case",
		},
		{
			v3.Document{
				Paths: &v3.Paths{
					PathItems:  pathItems2,
					Extensions: nil,
				},
			},
			[]*Resource{
				{
					path:        "/pet/{petId}",
					methods:     []*Operation{},
					description: "this retrieve data from id",
					summary:     "pet find by id",
				},
			},
			"PathItem without any methods",
		},
	}
	for _, item := range dataItems {
		resultResources, err := setResourcesOpenAPIV31(item.input)
		assert.Nil(t, err, "No error should be encountered when setting resources")
		if item.result != nil {
			assert.Equal(t, item.result[0].path, resultResources[0].GetPath(), item.message)

			if len(resultResources[0].GetMethod()) != 0 {
				resultResources[0].GetMethod()[0].iD = item.result[0].methods[0].iD
			}

			assert.Equal(t, item.result[0].methods, resultResources[0].GetMethod(), item.message)
			assert.Equal(t, item.result[0].description, resultResources[0].description, item.message)
			assert.Equal(t, item.result[0].summary, resultResources[0].summary, item.message)
		} else {
			assert.Equal(t, item.result, resultResources, item.message)
		}
	}
}

func TestGetXWso2LabelOAS31(t *testing.T) {

	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/openapi_with_xwso2label.yaml"

	openapi, err := os.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "OpenAPI specification file loading failed.")

	openapiDocument, err := libopenapi.NewDocument(openapi)
	assert.Nil(t, err, "OpenAPI document creation failed.")

	openapiModel, modelBuildErr := openapiDocument.BuildV3Model()
	assert.Nil(t, modelBuildErr, "OpenAPI model creation failed.")

	openapiValidator, validatorErrs := validator.NewValidator(openapiDocument)
	assert.Nil(t, validatorErrs, "OpenAPI validator creation failed.")

	valid, _ := openapiValidator.ValidateDocument()
	assert.True(t, valid, "OpenAPI document is not valid.")

	wso2Label := GetXWso2LabelV31(openapiModel.Model.Extensions)
	assert.NotNil(t, wso2Label, "Lable should at leaset be default")
}

func TestPopulationFromOpenAPIV31ToMgwSwagger(t *testing.T) {
	apiYamlFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/opeanapi_v31.yaml"

	openapi, err := os.ReadFile(apiYamlFilePath)
	assert.Nil(t, err, "OpenAPI specification file loading failed.")

	openapiDocument, err := libopenapi.NewDocument(openapi)
	assert.Nil(t, err, "OpenAPI document creation failed.")

	openapiv3Model, modelBuildErr := openapiDocument.BuildV3Model()
	assert.Nil(t, modelBuildErr, "OpenAPI model creation failed.")

	openapiValidator, validatorErrs := validator.NewValidator(openapiDocument)
	assert.Nil(t, validatorErrs, "OpenAPI validator creation failed.")

	valid, _ := openapiValidator.ValidateDocument()
	assert.True(t, valid, "OpenAPI document is not valid.")

	openapiModel := openapiv3Model.Model

	wso2Label := GetXWso2LabelV31(openapiModel.Extensions)
	assert.NotNil(t, wso2Label, "Lable should at leaset be default")

	var mgwSwagger MgwSwagger
	mgwSwagger.SetInfoOpenAPIV31(openapiModel)

	assert.Equal(t, mgwSwagger.vendorExtensions["x-wso2-auth-header"], "Authorization",
		"Vendor Extensions are not properly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.title, "Sample 3.1.1",
		"API title is not properly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.description, "This is a sample 3.1.1 openapi definition for testing",
		"API description is not properly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.version, "1.0.0",
		"API version is not properly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.productionEndpoints.Endpoints[0].Host, "www.sample-server.org",
		"API production enpoint URLs are not properly configured in MgwSwagger.")

	assert.Nil(t, mgwSwagger.sandboxEndpoints,
		"API sandbox endpoint URLs are not properly configured in MgwSwagger.")

	assert.Equal(t, len(mgwSwagger.resources), 2,
		"Some API resources are missing from MgwSwagger.")

	assert.Equal(t, len(mgwSwagger.resources[0].methods), 0,
		"Paths without any path items are not properly populated properly in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[0].path, "/pet/findByTags",
		"Paths without any path items are not correctly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[0].summary,
		"Sample path for finding pets by tag", "Summary field is not correctly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[1].methods[0].method, "GET",
		"HTTP methods are not properly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[1].methods[0].vendorExtensions["x-throttling-tier"], "Unlimited",
		"Resource vendor extensions are not correctly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[1].path, "/pet/{petId}",
		"Resource paths are not correctly populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[1].methods[0].security[0]["api_key"], []string(nil),
		"Path Item security is properly not populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.resources[1].methods[0].security[1]["sample_oauth"], []string{"write:pets", "read:pets"},
		"Path Item security is properly not populated in MgwSwagger.")

	assert.Equal(t, mgwSwagger.securityScheme[1].DefinitionName, "api_key",
		"Security Schemes are not populated properly in MgwSwagger.")
}
