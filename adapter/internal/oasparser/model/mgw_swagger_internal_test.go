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
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
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
				EndpointPrefix: "clusterProd",
				Endpoints: []Endpoint{
					{
						Host:    "www.facebook.com",
						Port:    80,
						URLType: "https",
						RawURL:  "https://www.facebook.com:80",
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
		mgwSwag := MgwSwagger{}
		resultResources, err := mgwSwag.getEndpoints(item.inputVendorExtensions, item.inputEndpointName)
		assert.Nil(t, err, "Error should not be present when extracting endpoints from the vendor extension map")
		assert.Equal(t, item.result, resultResources, item.message)
	}
}

func TestGetXWso2RefEndpoints(t *testing.T) {
	xWso2EPVendorExtension := []interface{}{map[string]interface{}{
		"myep": map[string]interface{}{
			"type": "loadbalance", "urls": []interface{}{"https://www.facebook.com:80"}}}}
	prodEPRefVendorExtension := map[string]interface{}{"x-wso2-production-endpoints": "#/x-wso2-endpoints/myep"}
	result := &EndpointCluster{
		EndpointPrefix: "myep_xwso2cluster",
		Endpoints: []Endpoint{
			{
				Host:    "www.facebook.com",
				Port:    80,
				URLType: "https",
				RawURL:  "https://www.facebook.com:80",
			},
		},
		EndpointType: "loadbalance",
	}

	mgwSwag := MgwSwagger{}
	mgwSwag.vendorExtensions = make(map[string]interface{})
	mgwSwag.vendorExtensions["x-wso2-endpoints"] = xWso2EPVendorExtension
	err := mgwSwag.setXWso2Endpoints()
	assert.Nil(t, err, "Error should not be present when extracting endpoints from the vendor extension map")
	resultResources := mgwSwag.GetXWso2Endpoints()
	epCluster, found := resultResources["myep"]
	assert.Equal(t, true, found, "x-wso2-endpoints vendor extension has not read correctly")
	assert.Equal(t, result, epCluster, "x-wso2-endpoints vendor extension has not read correctly")
	resultEP, err := mgwSwag.getEndpoints(prodEPRefVendorExtension, "x-wso2-production-endpoints")
	assert.Nil(t, err, "Error should not be present when extracting referenced prod endpoints")
	assert.Equal(t, result, resultEP, "x-wso2-endpoints vendor extension has not read correctly")
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
				resources: []*Resource{
					{
						vendorExtensions: nil,
					},
				},
			},
			result: MgwSwagger{
				productionEndpoints: &EndpointCluster{
					EndpointPrefix: "clusterProd",
					Endpoints: []Endpoint{
						{
							Host:     "www.facebook.com",
							Port:     80,
							URLType:  "https",
							Basepath: "/base",
							RawURL:   "https://www.facebook.com:80/base",
						},
					},
					EndpointType: "loadbalance",
				},
				resources: []*Resource{
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
				resources: []*Resource{
					{
						vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
							"type": "loadbalance", "urls": []interface{}{"https://resource.endpoint:80/base"}}},
					},
				},
			},
			result: MgwSwagger{
				productionEndpoints: &EndpointCluster{
					EndpointPrefix: "clusterProd",
					Endpoints: []Endpoint{
						{
							Host:     "www.facebook.com",
							Port:     80,
							URLType:  "https",
							Basepath: "/base",
							RawURL:   "https://www.facebook.com:80/base",
						},
					},
					EndpointType: "loadbalance",
				},
				resources: []*Resource{
					{
						productionEndpoints: &EndpointCluster{
							EndpointPrefix: "clusterProd",
							Endpoints: []Endpoint{
								{
									Host:     "resource.endpoint",
									Port:     80,
									URLType:  "https",
									Basepath: "/base",
									RawURL:   "https://resource.endpoint:80/base",
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
					constants.XWso2Cors: map[string]interface{}{
						"Enabled":                       "true",
						"AccessControlAllowCredentials": "true",
						"AccessControlAllowHeaders":     []interface{}{"Authorization"},
						"AccessControlAllowMethods":     []interface{}{"GET"},
						"AccessControlAllowOrigins":     []interface{}{"http://test123.com", "http://test456.com"},
					},
				},
				resources: []*Resource{
					{
						vendorExtensions: map[string]interface{}{"x-wso2-production-endpoints": map[string]interface{}{
							"type": "loadbalance", "urls": []interface{}{"https://resource.endpoint:80/base"}},
						},
					},
				},
			},
			result: MgwSwagger{
				productionEndpoints: &EndpointCluster{
					EndpointPrefix: "clusterProd",
					Endpoints: []Endpoint{
						{
							Host:     "www.youtube.com",
							Port:     80,
							URLType:  "https",
							Basepath: "/base",
							RawURL:   "https://www.youtube.com:80/base",
						},
					},
					EndpointType: "loadbalance",
				},
				resources: []*Resource{
					{
						productionEndpoints: &EndpointCluster{
							EndpointPrefix: "clusterProd",
							Endpoints: []Endpoint{
								{
									Host:     "resource.endpoint",
									Port:     80,
									URLType:  "https",
									Basepath: "/base",
									RawURL:   "https://resource.endpoint:80/base",
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
		if cors, corsFound := mgwSwag.vendorExtensions[constants.XWso2Cors]; corsFound {
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

func TestValidateBasePath(t *testing.T) {
	type getXWso2BasepathTestItem struct {
		mgwSwagger MgwSwagger
		errorNil   bool
		message    string
	}
	dataItems := []getXWso2BasepathTestItem{
		{
			mgwSwagger: MgwSwagger{xWso2Basepath: "/v1/base"},
			errorNil:   true,
			message:    "valid basepath",
		},
		{
			mgwSwagger: MgwSwagger{xWso2Basepath: "/ERROR-Hello%20W"},
			errorNil:   false,
			message:    "basepath must not include invalid characters",
		},
		{
			mgwSwagger: MgwSwagger{xWso2Basepath: "base"},
			errorNil:   false,
			message:    "basepath must start with /",
		},
		{
			mgwSwagger: MgwSwagger{xWso2Basepath: ""},
			errorNil:   false,
			message:    "basepath must not be empty",
		},
	}
	for _, item := range dataItems {
		err := item.mgwSwagger.validateBasePath()
		assert.Equal(t, item.errorNil, err == nil, item.message)
	}
}

func TestGetAuthorityHeader(t *testing.T) {
	type getXWso2AuthorityHeaderTestItem struct {
		serviceURL      string
		authorityHeader string
		message         string
	}
	dataItems := []getXWso2AuthorityHeaderTestItem{
		{
			serviceURL:      "https://interceptor-service-host:3000",
			authorityHeader: "interceptor-service-host:3000",
			message:         "invalid authority header when port is present",
		},
		{
			serviceURL:      "https://abcd.wxyz.amazonaws.com",
			authorityHeader: "abcd.wxyz.amazonaws.com:443",
			message:         "invalid authority header when port is implicit",
		},
	}
	for _, item := range dataItems {
		endpoint, _ := getHTTPEndpoint(item.serviceURL)
		authHeader := endpoint.GetAuthorityHeader()
		assert.Equal(t, authHeader, item.authorityHeader, item.message)
	}
}

func TestGetEndpointType(t *testing.T) {
	type getEndpointTypeTestItem struct {
		mgwSwagger MgwSwagger
		result     string
		message    string
	}
	dataItems := []getEndpointTypeTestItem{
		{
			mgwSwagger: MgwSwagger{EndpointType: "awslambda"},
			result:     "awslambda",
			message:    "invalid endpoint type",
		},
	}
	for _, item := range dataItems {
		resultEndpointType := item.mgwSwagger.GetEndpointType()
		assert.Equal(t, item.result, resultEndpointType, item.message)
	}
}
