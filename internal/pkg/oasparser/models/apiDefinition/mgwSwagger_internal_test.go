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
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestGetXWso2Endpoints(t *testing.T) {
	type getXWso2EndpointsTestItem struct {
		inputVendorExtensible map[string]interface{}
		inputEndpointType  string
		result  []Endpoint
		message string
	}
	dataItems := []getXWso2EndpointsTestItem {
		{
			inputEndpointType: "x-wso2-production-endpoints",
			inputVendorExtensible: map[string]interface{}{"x-wso2-production-endpoints":map[string]interface{}{
				"type":"https", "urls":[]interface{}{"https://www.facebook.com:80"}}},
			result: []Endpoint {
				{
					Host: "www.facebook.com",
					Port: 80,
					UrlType: "https",
				},
			},
			message: "usual case",
		},
		{
			inputEndpointType: "x-wso2-production-endpoints",
			inputVendorExtensible: map[string]interface{}{"x-wso2-production-endpoints+++":map[string]interface{}{
				"type":"https", "urls":[]interface{}{"https://www.facebook.com:80/base"}}},
			result: nil,
			message: "when having incorrect extenstion name",
		},
	}
	for _, item := range dataItems{
		resultResources := GetXWso2Endpoints(item.inputVendorExtensible,item.inputEndpointType)
		assert.Equal(t, item.result, resultResources, item.message)
	}
}

func TestGetXWso2Basepath(t *testing.T) {
	type getXWso2BasepathTestItem struct {
		inputVendorExtensible map[string]interface{}
		result  string
		message string
	}
	dataItems := []getXWso2BasepathTestItem {
		{
			inputVendorExtensible: map[string]interface{}{"x-wso2-basepath": "/base"},
			result: "/base",
			message: "usual case",
		},
		{
			inputVendorExtensible: map[string]interface{}{"x-wso2-basepath+++": "/base"},
			result: "",
			message: "when having incorrect structure",
		},
	}
	for _, item := range dataItems{
		resultResources := GetXWso2Basepath(item.inputVendorExtensible)
		assert.Equal(t, item.result, resultResources, item.message)
	}
}

func TestSetXWso2PrdoductionEndpoint(t *testing.T) {
	type setXWso2PrdoductionEndpointTestItem struct {
		input MgwSwagger
		result  MgwSwagger
		message string
	}
	dataItems := []setXWso2PrdoductionEndpointTestItem {
		{
			input: MgwSwagger{
				vendorExtensible: map[string]interface{}{"x-wso2-production-endpoints":map[string]interface{}{
					"type":"https", "urls":[]interface{}{"https://www.facebook.com:80/base"}}},
				resources: []Resource {
					{
						vendorExtensible: nil,
					},
			     },
		    },
		    result: MgwSwagger{
				productionUrls: []Endpoint {
					{
						Host: "www.facebook.com",
						Port: 80,
						UrlType: "https",
						Basepath: "/base",
					},
				},
				resources: []Resource{
					{
						productionUrls: nil,
					},
				},
			},
			message: "when resource level endpoints doesn't exist",
		},
		{
			input: MgwSwagger{
				vendorExtensible: map[string]interface{}{"x-wso2-production-endpoints":map[string]interface{}{
					"type":"https", "urls":[]interface{}{"https://www.facebook.com:80/base"}}},
				resources: []Resource {
					{
						vendorExtensible: map[string]interface{}{"x-wso2-production-endpoints":map[string]interface{}{
							"type":"https", "urls":[]interface{}{"https://resource.endpoint:80/base"}}},
					},
				},
			},
			result: MgwSwagger{
				productionUrls: []Endpoint {
					{
						Host: "www.facebook.com",
						Port: 80,
						UrlType: "https",
						Basepath: "/base",
					},
				},
				resources: []Resource {
					{
						productionUrls: []Endpoint {
							{
								Host: "resource.endpoint",
								Port: 80,
								UrlType: "https",
								Basepath: "/base",
							},
						},
					},
				},
			},
			message: "when resource level endpoints exist",
		},
	}

	for _, item := range dataItems{
		mgwSwag := item.input
		mgwSwag.SetXWso2Extenstions()
		assert.Equal(t, item.result.productionUrls, mgwSwag.productionUrls, item.message)
		if mgwSwag.resources != nil {
			assert.Equal(t, item.result.resources[0].productionUrls, mgwSwag.resources[0].productionUrls, item.message)
		}
	}
}
