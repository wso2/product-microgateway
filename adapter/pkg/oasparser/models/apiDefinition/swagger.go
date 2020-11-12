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

//swagger version 2
package apiDefinition

import (
	"github.com/go-openapi/spec"
	"github.com/google/uuid"
	logger "github.com/wso2/micro-gw/loggers"
)

/**
 * Set swagger2 data to mgwSwagger  Instance.
 *
 * @param swagger2  Swagger2 unmarshalled data
 */
func (swagger *MgwSwagger) SetInfoSwagger(swagger2 spec.Swagger) {
	swagger.id = swagger2.ID
	swagger.swaggerVersion = swagger2.Swagger
	if swagger2.Info != nil {
		swagger.description = swagger2.Info.Description
		swagger.title = swagger2.Info.Title
		swagger.version = swagger2.Info.Version
	}
	swagger.vendorExtensible = swagger2.VendorExtensible.Extensions
	swagger.resources = SetResourcesSwagger(swagger2)

	// According to the definition, multiple schemes can be mentioned. Since the microgateway can assign only one scheme
	// https is prioritized over http. If it is ws or wss, the microgateway will print an error.
	// If the schemes property is not mentioned at all, http will be assigned. (Only swagger 2 version has this property)
	if swagger2.Host != "" {
		urlScheme := ""
		for _, scheme := range swagger2.Schemes {
			//TODO: (VirajSalaka) Introduce Constants
			if scheme == "https" {
				urlScheme = "https://"
				break
			} else if scheme == "http" {
				urlScheme = "http://"
			} else {
				//TODO: (VirajSalaka) Throw an error and stop processing
				logger.LoggerOasparser.Errorf("The scheme : %v for the swagger definition %v:%v is not supported", scheme,
					swagger2.Info.Title, swagger2.Info.Version)
			}
		}
		endpoint := getHostandBasepathandPort(urlScheme + swagger2.Host + swagger2.BasePath)
		swagger.productionUrls = append(swagger.productionUrls, endpoint)
	}
}

/**
 * Set swagger2 all resource to mgwSwagger resources.
 *
 * @param swagger2  Swagger2 unmarshalled data
 * @return []Resource  MgwSwagger resource array
 */
func SetResourcesSwagger(swagger2 spec.Swagger) []Resource {
	var resources []Resource
	if swagger2.Paths != nil {
		for path, pathItem := range swagger2.Paths.Paths {
			var methodsArray []string
			methodFound := false
			if pathItem.Get != nil {
				methodsArray = append(methodsArray, "GET")
				methodFound = true
			}
			if pathItem.Post != nil {
				methodsArray = append(methodsArray, "POST")
				methodFound = true
			}
			if pathItem.Put != nil {
				methodsArray = append(methodsArray, "PUT")
				methodFound = true
			}
			if pathItem.Delete != nil {
				methodsArray = append(methodsArray, "DELETE")
				methodFound = true
			}
			if pathItem.Head != nil {
				methodsArray = append(methodsArray, "HEAD")
				methodFound = true
			}
			if pathItem.Patch != nil {
				methodsArray = append(methodsArray, "HEAD")
				methodFound = true
			}
			if pathItem.Options != nil {
				methodsArray = append(methodsArray, "GET")
				methodFound = true
			}
			if methodFound {
				resource := setOperationSwagger(path, methodsArray, pathItem)
				resources = append(resources, resource)
			}
		}
	}

	return resources
}

/**
 * Set swagger2 resource path details to mgwSwagger  Instance.
 *
 * @param path  Resource path
 * @param method  Http methods array (Get, Post ... )
 * @param operation  Operation type
 * @return Resource  MgwSwagger resource instance
 */
func setOperationSwagger(path string, methods []string, pathItem spec.PathItem) Resource {
	var resource Resource
	resource = Resource{
		path:    path,
		methods: methods,
		//TODO: (VirajSalaka) This will not solve the actual problem when incremental Xds is introduced (used for cluster names)
		iD: uuid.New().String(),
		//PathItem object in swagger 2 specification does not contain summary and description properties
		summary:     "",
		description: "",
		//schemes:          operation.Schemes,
		//tags:             operation.Tags,
		//security:         operation.Security,
		vendorExtensible: pathItem.VendorExtensible.Extensions,
	}
	return resource
}
