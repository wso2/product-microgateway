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
)

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

	if swagger2.Host != "" {
		endpoint := getHostandBasepathandPort(swagger2.Host + swagger2.BasePath)
		swagger.productionUrls = append(swagger.productionUrls,endpoint)
	}
}

func SetResourcesSwagger(swagger2 spec.Swagger) []Resource {
	var resources []Resource
	for path, _ := range swagger2.Paths.Paths {
		var pathItem = swagger2.Paths.Paths[path].PathItemProps
		var resource Resource
		if pathItem.Get != nil {
			resource = setOperationSwagger(path, "get", pathItem.Get)
		} else if pathItem.Post != nil {
			resource = setOperationSwagger(path, "post", pathItem.Post)
		} else if pathItem.Put != nil {
			resource = setOperationSwagger(path, "put", pathItem.Put)
		} else if pathItem.Delete != nil {
			resource = setOperationSwagger(path, "delete", pathItem.Delete)
		} else if pathItem.Head != nil {
			resource = setOperationSwagger(path, "head", pathItem.Head)
		} else if pathItem.Patch != nil {
			resource = setOperationSwagger(path, "patch", pathItem.Patch)
		} else {
			//resource = setOperation(contxt,"get",pathItem.Get)
		}
		resources = append(resources, resource)
	}
	return resources
}

func setOperationSwagger(path string, pathtype string, operation *spec.Operation) Resource {
	var resource Resource
	resource = Resource{
		path:          path,
		pathtype:            pathtype,
		iD:               operation.ID,
		summary:          operation.Summary,
		schemes:          operation.Schemes,
		tags:             operation.Tags,
		security:         operation.Security,
		vendorExtensible: operation.VendorExtensible.Extensions}
	return resource
}