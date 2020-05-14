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

//OpenApi version 3
package apiDefinition

import (
	"github.com/getkin/kin-openapi/openapi3"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/config"
	"log"
	"net/url"
	"strconv"
	"strings"
)

func (swagger *MgwSwagger) SetInfoOpenApi(swagger3 openapi3.Swagger) {
	swagger.swaggerVersion = swagger3.OpenAPI
	if swagger3.Info != nil {
		swagger.description = swagger3.Info.Description
		swagger.title = swagger3.Info.Title
		swagger.version = swagger3.Info.Version
	}
	swagger.vendorExtensible = swagger3.Extensions
	swagger.resources = SetResourcesOpenApi3(swagger3)

	if IsServerUrlIsAvailable(swagger3) {
		host,basepath,port := getHostandBasepath(swagger3.Servers[0].URL)
		swagger.basePath = basepath
		swagger.hostUrl = host
		swagger.port = port
	}
}

func setOperationOpenApi(path string, pathtype string, operation *openapi3.Operation) Resource {
	var resource Resource
	resource = Resource{
		path: path,
		pathtype:   pathtype,
		iD:      operation.OperationID,
		summary: operation.Summary,
		//Schemes: operation.,
		tags: operation.Tags,
		//Security: operation.Security.,
		vendorExtensible: operation.Extensions}
	return resource
}


func SetResourcesOpenApi3(openApi openapi3.Swagger) []Resource {
	var resources []Resource

	for path, pathItem := range openApi.Paths {

		var resource Resource
		if pathItem.Get != nil {
			resource = setOperationOpenApi(path, "get", pathItem.Get)
		} else if pathItem.Post != nil {
			resource = setOperationOpenApi(path, "post", pathItem.Post)
		} else if pathItem.Put != nil {
			resource = setOperationOpenApi(path, "put", pathItem.Put)
		} else if pathItem.Delete != nil {
			resource = setOperationOpenApi(path, "delete", pathItem.Delete)
		} else if pathItem.Head != nil {
			resource = setOperationOpenApi(path, "head", pathItem.Head)
		} else if pathItem.Patch != nil {
			resource = setOperationOpenApi(path, "patch", pathItem.Patch)
		} else {
			//resource = setOperation(contxt,"get",pathItem.Get)
		}

		resources = append(resources, resource)
	}
	return resources
}

func getHostandBasepath(rawUrl string) (string, string, uint32) {
	basepath := ""
	host := ""
	port := config.API_DEFAULT_PORT
	u, err := url.Parse(rawUrl)
	if err != nil {
		log.Fatal(err)
	}

	if strings.Contains(rawUrl, "://") {
		host = u.Hostname()
		basepath = u.Path
		if u.Port() != "" {
			u32, err := strconv.ParseUint(u.Port(),10,32)
			if err != nil {
				log.Println("Error passing port value to mgwSwagger",err)
			}
			port = uint32(u32)
		}

	} else {
		if strings.Contains(rawUrl, "/") {
			i := strings.Index(rawUrl, "/")
			host = 	rawUrl[:i]
			basepath = rawUrl[i:]
		} else {
			host = 	rawUrl
		}
	}
	return host,basepath, port
}

func IsServerUrlIsAvailable(swagger3 openapi3.Swagger) bool {
	if swagger3.Servers != nil {
		if len(swagger3.Servers) > 0 && (swagger3.Servers[0].URL != "") {
			return true
		} else {
			return false
		}
	} else {
		return false
	}
}

