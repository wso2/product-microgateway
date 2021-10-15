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

package model

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetXWso2Endpoints(t *testing.T) {
	type getXWso2EndpointsTestItem struct {
		inputVendorExtensions map[string]interface{}
		inputEndpointName     string
		result                *EndpointCluster
		message               string
	}
	dataItems := []getXWso2EndpointsTestItem{
		{
			inputEndpointName: "x-wso2-production-endpoints",
			inputVendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
				"type": "loadbalance", "urls": []interface{}{"https://www.facebook.com:80"}}},
			result: &EndpointCluster{
				EndpointName: "x-wso2-production-endpoints",
				Endpoints: []Endpoint{
					{
						Host:    "www.facebook.com",
						Port:    80,
						URLType: "https",
					},
				},
				EndpointType: "loadbalance",
			},
			message: "usual case",
		},
		{
			inputEndpointName: "x-wso2-production-endpoints",
			inputVendorExtensions: map[string]interface{}{"x-wso2-production-endpoints+++": map[string]interface{}{
				"type": "loadbalance", "urls": []interface{}{"https://www.facebook.com:80/base"}}},
			result:  nil,
			message: "when having incorrect extenstion name",
		},
	}
	for _, item := range dataItems {
		resultResources, err := getEndpoints(item.inputVendorExtensions, item.inputEndpointName)
		assert.Nil(t, err, "Error should not be present when extracting endpoints from the vendor extension map")
		assert.Equal(t, item.result, resultResources, item.message)
	}
}

func TestGetXWso2Basepath(t *testing.T) {
	type getXWso2BasepathTestItem struct {
		inputVendorExtensions map[string]interface{}
		result                string
		message               string
	}
	dataItems := []getXWso2BasepathTestItem{
		{
			inputVendorExtensions: map[string]interface{}{"x-wso2-basePath": "/base"},
			result:                "/base",
			message:               "usual case",
		},
		{
			inputVendorExtensions: map[string]interface{}{"x-wso2-basepath+++": "/base"},
			result:                "",
			message:               "when having incorrect structure",
		},
	}
	for _, item := range dataItems {
		resultResources := getXWso2Basepath(item.inputVendorExtensions)
		assert.Equal(t, item.result, resultResources, item.message)
	}
}

func TestSetXWso2ProductionEndpoint(t *testing.T) {
	type setXWso2ProductionEndpointTestItem struct {
		input   MgwSwagger
		result  MgwSwagger
		message string
	}
	dataItems := []setXWso2ProductionEndpointTestItem{
		{
			input: MgwSwagger{
				vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
					"type": "loadbalance", "urls": []interface{}{"https://www.facebook.com:80/base"}}},
				resources: []Resource{
					{
						vendorExtensions: nil,
					},
				},
			},
			result: MgwSwagger{
				productionEndpoints: &EndpointCluster{
					EndpointName: "x-wso2-production-endpoints",
					Endpoints: []Endpoint{
						{
							Host:     "www.facebook.com",
							Port:     80,
							URLType:  "https",
							Basepath: "/base",
						},
					},
					EndpointType: "loadbalance",
				},
				resources: []Resource{
					{
						productionEndpoints: nil,
					},
				},
			},
			message: "when resource level endpoints doesn't exist",
		},
		{
			input: MgwSwagger{
				vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
					"type": "loadbalance", "urls": []interface{}{"https://www.facebook.com:80/base"}}},
				resources: []Resource{
					{
						vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
							"type": "loadbalance", "urls": []interface{}{"https://resource.endpoint:80/base"}}},
					},
				},
			},
			result: MgwSwagger{
				productionEndpoints: &EndpointCluster{
					EndpointName: "x-wso2-production-endpoints",
					Endpoints: []Endpoint{
						{
							Host:     "www.facebook.com",
							Port:     80,
							URLType:  "https",
							Basepath: "/base",
						},
					},
					EndpointType: "loadbalance",
				},
				resources: []Resource{
					{
						productionEndpoints: &EndpointCluster{
							EndpointName: "x-wso2-production-endpoints",
							Endpoints: []Endpoint{
								{
									Host:     "resource.endpoint",
									Port:     80,
									URLType:  "https",
									Basepath: "/base",
								},
							},
							EndpointType: "loadbalance",
						},
					},
				},
			},
			message: "when resource level endpoints exist",
		},

		{
			input: MgwSwagger{
				vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
					"type": "loadbalance", "urls": []interface{}{"https://www.youtube.com:80/base"}},
					xWso2Cors: map[string]interface{}{
						"Enabled":                       "true",
						"AccessControlAllowCredentials": "true",
						"AccessControlAllowHeaders":     []interface{}{"Authorization"},
						"AccessControlAllowMethods":     []interface{}{"GET"},
						"AccessControlAllowOrigins":     []interface{}{"http://test123.com", "http://test456.com"},
					},
				},
				resources: []Resource{
					{
						vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
							"type": "loadbalance", "urls": []interface{}{"https://resource.endpoint:80/base"}},
						},
					},
				},
			},
			result: MgwSwagger{
				productionEndpoints: &EndpointCluster{
					EndpointName: "x-wso2-production-endpoints",
					Endpoints: []Endpoint{
						{
							Host:     "www.youtube.com",
							Port:     80,
							URLType:  "https",
							Basepath: "/base",
						},
					},
					EndpointType: "loadbalance",
				},
				resources: []Resource{
					{
						productionEndpoints: &EndpointCluster{
							EndpointName: "x-wso2-production-endpoints",
							Endpoints: []Endpoint{
								{
									Host:     "resource.endpoint",
									Port:     80,
									URLType:  "https",
									Basepath: "/base",
								},
							},
							EndpointType: "loadbalance",
						},
					},
				},
			},
			message: "when resource level endpoints exist",
		},
	}

	for _, item := range dataItems {
		mgwSwag := item.input
		if cors, corsFound := mgwSwag.vendorExtensions[xWso2Cors]; corsFound {
			assert.NotNil(t, cors, "cors should not be empty")
		}
		err := mgwSwag.SetXWso2Extensions()
		assert.Nil(t, err, "Should not encounter an error when setting vendor extensions")
		assert.Equal(t, item.result.productionEndpoints, mgwSwag.productionEndpoints, item.message)
		if mgwSwag.resources != nil {
			assert.Equal(t, item.result.resources[0].productionEndpoints, mgwSwag.resources[0].productionEndpoints, item.message)
		}
	}
}
