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
	"encoding/json"
	"net/url"
	"strconv"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/google/uuid"
	logger "github.com/wso2/micro-gw/loggers"
)

/**
 * Set openApi3 data to mgwSwagger  Instance.
 *
 * @param swagger3  OpenApi3 unmarshalled data
 */
func (swagger *MgwSwagger) SetInfoOpenApi(swagger3 openapi3.Swagger) {
	swagger.swaggerVersion = swagger3.OpenAPI
	if swagger3.Info != nil {
		swagger.description = swagger3.Info.Description
		swagger.title = swagger3.Info.Title
		swagger.version = swagger3.Info.Version
	}

	swagger.vendorExtensible = convertExtensibletoReadableFormat(swagger3.ExtensionProps)
	swagger.resources = SetResourcesOpenApi(swagger3)

	if IsServerUrlIsAvailable(swagger3) {
		for i, _ := range swagger3.Servers {
			endpoint := getHostandBasepathandPort(swagger3.Servers[i].URL)
			swagger.productionUrls = append(swagger.productionUrls, endpoint)
		}
	}
}

/**
 * Set swagger3 resource path details to mgwSwagger  Instance.
 *
 * @param path  Resource path
 * @param methods  Path types as an array (Get, Post ... )
 * @param pathItem  PathItem entity
 * @return Resource  MgwSwagger resource instance
 */
func setOperationOpenApi(path string, methods []string, pathItem *openapi3.PathItem) Resource {
	var resource Resource
	if pathItem != nil {
		resource = Resource{
			path:    path,
			methods: methods,
			//TODO: (VirajSalaka) This will not solve the actual problem when incremental Xds is introduced (used for cluster names)
			iD:          uuid.New().String(),
			summary:     pathItem.Summary,
			description: pathItem.Description,
			//Schemes: operation.,
			//tags: operation.Tags,
			//Security: operation.Security.,
			vendorExtensible: convertExtensibletoReadableFormat(pathItem.ExtensionProps)}
	}
	return resource
}

/**
 * Set swagger3 all resource to mgwSwagger resources.
 *
 * @param openApi  Swagger3 unmarshalled data
 * @return []Resource  MgwSwagger resource array
 */
func SetResourcesOpenApi(openApi openapi3.Swagger) []Resource {
	var resources []Resource
	if openApi.Paths != nil {
		for path, pathItem := range openApi.Paths {
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
				methodsArray = append(methodsArray, "OPTIONS")
				methodFound = true
			}
			if methodFound {
				resource := setOperationOpenApi(path, methodsArray, pathItem)
				resources = append(resources, resource)
			}
		}
	}
	return resources
}

/**
 * Retrieve host, basepath and port from the endpoint defintion from of the swaggers.
 *
 * @param rawUrl  RawUrl defintion
 * @return Endpoint  Endpoint instance
 */
func getHostandBasepathandPort(rawUrl string) Endpoint {
	var (
		basepath string
		host     string
		port     uint32
	)
	if !strings.Contains(rawUrl, "://") {
		rawUrl = "https://" + rawUrl
	}
	parsedUrl, err := url.Parse(rawUrl)
	if err != nil {
		logger.LoggerOasparser.Fatal(err)
	}

	host = parsedUrl.Hostname()
	basepath = parsedUrl.Path
	if parsedUrl.Port() != "" {
		u32, err := strconv.ParseUint(parsedUrl.Port(), 10, 32)
		if err != nil {
			logger.LoggerOasparser.Error("Error passing port value to mgwSwagger", err)
		}
		port = uint32(u32)
	} else {
		if strings.HasPrefix(rawUrl, "https://") {
			port = uint32(443)
		} else {
			port = uint32(80)
		}
	}
	return Endpoint{Host: host, Basepath: basepath, Port: port}
}

/**
 * Check the availability od server url in openApi3
 *
 * @param swagger3  Swagger3 unmarshalled data
 * @return bool  Bool value of availability
 */
func IsServerUrlIsAvailable(swagger3 openapi3.Swagger) bool {
	if swagger3.Servers != nil {
		if len(swagger3.Servers) > 0 && (swagger3.Servers[0].URL != "") {
			return true
		}
	}
	return false
}

/**
 * Unmarshall the vendo extensible in open api3.
 *
 * @param vendorExtensible  VendorExtensible data of open api3
 * @return map[string]interface{}  Map of the vendorExtensible
 */
func convertExtensibletoReadableFormat(vendorExtensible openapi3.ExtensionProps) map[string]interface{} {
	jsnRawExtensible := vendorExtensible.Extensions
	b, err := json.Marshal(jsnRawExtensible)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling vendor extenstions: ", err)
	}

	var extensible map[string]interface{}
	err = json.Unmarshal(b, &extensible)
	if err != nil {
		logger.LoggerOasparser.Error("Error unmarsheling vendor extenstions:", err)
	}
	return extensible
}

func GetXWso2Label(vendorExtensions openapi3.ExtensionProps) []string {
	vendorExtensionsMap := convertExtensibletoReadableFormat(vendorExtensions)
	var labelArray []string
	if y, found := vendorExtensionsMap["x-wso2-label"]; found {
		if val, ok := y.([]interface{}); ok {
			for _, label := range val {
				labelArray = append(labelArray, label.(string))
			}
			return labelArray
		} else {
			logger.LoggerOasparser.Errorln("Error while parsing the x-wso2-label")
		}
	}
	return []string{"default"}
}
