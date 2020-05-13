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
	c "github.com/wso2/micro-gw/internal/pkg/oasparser/constants"
)

type MgwSwagger struct {
	id               string `json:"id,omitempty"`
	swaggerVersion   string `json:"swagger,omitempty"`
	description      string `json:"description,omitempty"`
	title            string `json:"title,omitempty"`
	version          string `json:"version,omitempty"`
	basePath         string `json:"basePath,omitempty"`
	hostUrl         string
	vendorExtensible           map[string]interface{}
	productionUrls   []Endpoint
	sandboxUrls      []Endpoint
	resources        []Resource
	//Consumes            []string                    `json:"consumes,omitempty"`
	//Produces            []string                    `json:"produces,omitempty"`
	//Schemes             []string                    `json:"schemes,omitempty"`
	//info                *spec.Info                       `json:"info,omitempty"`
	//Host                string                      `json:"host,omitempty"`
	//Paths               *spec.Paths                      `json:"paths"`
	//Definitions         spec.Definitions            `json:"definitions,omitempty"`
	//Parameters          map[string]spec.Parameter   `json:"parameters,omitempty"`
	//Responses           map[string]spec.Response    `json:"responses,omitempty"`
	//SecurityDefinitions spec.SecurityDefinitions    `json:"securityDefinitions,omitempty"`
	//Security            []map[string][]string       `json:"security,omitempty"`
	//tags                []spec.Tag                  `json:"tags,omitempty"`
	//ExternalDocs        *spec.ExternalDocumentation `json:"externalDocs,omitempty"`

}


type Endpoint struct {
	Host      string
	Basepath  string
	UrlType   string
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


func (swagger *MgwSwagger) SetEndpoints() {
	swagger.SetPrdoductionEndpoint()
	swagger.SetSandboxEndpoint()
}

func (swagger *MgwSwagger) SetPrdoductionEndpoint() {
	xwso2EndpointsApi := GetXWso2Endpoints(swagger.vendorExtensible,c.PRODUCTION_ENDPOINTS)
	if xwso2EndpointsApi != nil && len(xwso2EndpointsApi) > 0 {
		swagger.productionUrls = xwso2EndpointsApi
	} else if swagger.hostUrl != "" {
		endpoint := Endpoint{
			Host: swagger.hostUrl,
			Basepath: swagger.basePath,
		}
		swagger.productionUrls = append(swagger.productionUrls,endpoint)
	} else {

	}


	//resources
	for i,resource := range swagger.resources {
		xwso2EndpointsResource := GetXWso2Endpoints(resource.vendorExtensible,c.PRODUCTION_ENDPOINTS)
		if xwso2EndpointsResource != nil {
			swagger.resources[i].productionUrls = xwso2EndpointsResource
		}
	}

}

func (swagger *MgwSwagger) SetSandboxEndpoint() {
	xwso2EndpointsApi := GetXWso2Endpoints(swagger.vendorExtensible,c.SANDBOX_ENDPOINTS)
	if xwso2EndpointsApi != nil && len(xwso2EndpointsApi) > 0 {
		swagger.sandboxUrls = xwso2EndpointsApi
	}

	//resources
	for i,resource := range swagger.resources {
		xwso2EndpointsResource := GetXWso2Endpoints(resource.vendorExtensible,c.SANDBOX_ENDPOINTS)
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
						host,basepath := getHostandBasepath(v.(string))
						endpoint := Endpoint{
							Host: host,
							Basepath: basepath,
							UrlType: urlType,
						}
						Endpoints = append(Endpoints,endpoint)
					}
				}
			}
		} else {
			fmt.Println("X-wso2 production endpoint is not having a correct structure")
			return nil
		}

	} else {
		return nil
	}

	return Endpoints
}



