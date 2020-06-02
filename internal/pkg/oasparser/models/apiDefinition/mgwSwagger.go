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
	constants "github.com/wso2/micro-gw/internal/pkg/oasparser/constants"
	"log"
)

type MgwSwagger struct {
	id               string `json:"id,omitempty"`
	swaggerVersion   string `json:"swagger,omitempty"`
	description      string `json:"description,omitempty"`
	title            string `json:"title,omitempty"`
	version          string `json:"version,omitempty"`
	vendorExtensible map[string]interface{}
	productionUrls   []Endpoint
	sandboxUrls      []Endpoint
	resources        []Resource
	xWso2Basepath    string
}

type Endpoint struct {
	Host     string
	Basepath string
	UrlType  string
	Port     uint32
}

func (endpoint *Endpoint) GetHost() string {
	return endpoint.Host
}
func (endpoint *Endpoint) GetBasepath() string {
	return endpoint.Basepath
}

func (endpoint *Endpoint) GetPort() uint32 {
	return endpoint.Port
}

func (swagger *MgwSwagger) GetSwaggerVersion() string {
	return swagger.swaggerVersion
}

func (swagger *MgwSwagger) GetVersion() string {
	return swagger.version
}

func (swagger *MgwSwagger) GetTitle() string {
	return swagger.title
}

func (swagger *MgwSwagger) GetXWso2Basepath() string {
	return swagger.xWso2Basepath
}

func (swagger *MgwSwagger) GetVendorExtensible() map[string]interface{} {
	return swagger.vendorExtensible
}

func (swagger *MgwSwagger) GetProdEndpoints() []Endpoint {
	return swagger.productionUrls
}

func (swagger *MgwSwagger) GetSandEndpoints() []Endpoint {
	return swagger.sandboxUrls
}

func (swagger *MgwSwagger) GetResources() []Resource {
	return swagger.resources
}

func (swagger *MgwSwagger) SetXWso2Extenstions() {
	swagger.SetXWso2Basepath()
	swagger.SetXWso2PrdoductionEndpoint()
	swagger.SetXWso2SandboxEndpoint()
}

func (swagger *MgwSwagger) SetXWso2PrdoductionEndpoint() {
	xwso2EndpointsApi := GetXWso2Endpoints(swagger.vendorExtensible,constants.PRODUCTION_ENDPOINTS)
	if xwso2EndpointsApi != nil && len(xwso2EndpointsApi) > 0 {
		swagger.productionUrls = xwso2EndpointsApi
	}

	//resources
	for i,resource := range swagger.resources {
		xwso2EndpointsResource := GetXWso2Endpoints(resource.vendorExtensible,constants.PRODUCTION_ENDPOINTS)
		if xwso2EndpointsResource != nil {
			swagger.resources[i].productionUrls = xwso2EndpointsResource
		}
	}
}

func (swagger *MgwSwagger) SetXWso2SandboxEndpoint() {
	xwso2EndpointsApi := GetXWso2Endpoints(swagger.vendorExtensible,constants.SANDBOX_ENDPOINTS)
	if xwso2EndpointsApi != nil && len(xwso2EndpointsApi) > 0 {
		swagger.sandboxUrls = xwso2EndpointsApi
	}

	//resources
	for i,resource := range swagger.resources {
		xwso2EndpointsResource := GetXWso2Endpoints(resource.vendorExtensible,constants.SANDBOX_ENDPOINTS)
		if xwso2EndpointsResource != nil {
			swagger.resources[i].sandboxUrls = xwso2EndpointsResource
		}
	}
}

func GetXWso2Endpoints(vendorExtensible map[string]interface{}, endpointType string) []Endpoint {
	var Endpoints []Endpoint
	var urlType string

	if y, found := vendorExtensible[endpointType]; found {
		if val, ok := y.(map[string]interface{}); ok {
			for ind, val := range val {
				if ind == "type" {
					urlType = val.(string)
				} else if ind == "urls" {
					ainterface := val.([]interface{})
					//urls := make([]string, len(ainterface))
					for _, v := range ainterface {
						endpoint := getHostandBasepathandPort(v.(string))
						endpoint.UrlType = urlType
						Endpoints = append(Endpoints, endpoint)
					}
				}
			}
		} else {
			log.Fatal("X-wso2 production endpoint is not having a correct map structure")
		}

	} else {
		return nil
	}

	return Endpoints
}

func GetXWso2Basepath(vendorExtensible map[string]interface{}) string {
	xWso2basepath := ""
	if y, found := vendorExtensible[constants.XWSO2BASEPATH]; found {
		if val, ok := y.(string); ok {
			xWso2basepath = val
		}
	}
	return xWso2basepath
}

func (swagger *MgwSwagger) SetXWso2Basepath() {
	swagger.xWso2Basepath = GetXWso2Basepath(swagger.vendorExtensible)
}
