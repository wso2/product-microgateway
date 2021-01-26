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
	"encoding/json"
	"net/url"
	"strconv"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/google/uuid"
	logger "github.com/wso2/micro-gw/loggers"
)

// SetInfoOpenAPI populates the MgwSwagger object with the properties within the openAPI v3 definition.
// The title, version, description, vendor extension map, endpoints based on servers property,
// and pathItem level information are populated here.
//
// for each pathItem; vendor extensions, endpoints (based on servers object), available http Methods,
// are populated. Each resource corresponding to a pathItem, has the property called iD, which is a
// UUID.
//
// No operation specific information is extracted.
func (swagger *MgwSwagger) SetInfoOpenAPI(swagger3 openapi3.Swagger) {
	if swagger3.Info != nil {
		swagger.description = swagger3.Info.Description
		swagger.title = swagger3.Info.Title
		swagger.version = swagger3.Info.Version
	}

	swagger.vendorExtensible = convertExtensibletoReadableFormat(swagger3.ExtensionProps)
	swagger.resources = setResourcesOpenAPI(swagger3)
	swagger.apiType = HTTP

	if isServerURLIsAvailable(swagger3.Servers) {
		for _, serverEntry := range swagger3.Servers {
			endpoint := getHostandBasepathandPort(serverEntry.URL)
			swagger.productionUrls = append(swagger.productionUrls, endpoint)
		}
	}
}

func setPathInfoOpenAPI(path string, methods []string, pathItem *openapi3.PathItem) Resource {
	var resource Resource
	if pathItem != nil {
		resource = Resource{
			path:    path,
			methods: methods,
			// TODO: (VirajSalaka) This will not solve the actual problem when incremental Xds is introduced (used for cluster names)
			iD:          uuid.New().String(),
			summary:     pathItem.Summary,
			description: pathItem.Description,
			//Schemes: operation.,
			//tags: operation.Tags,
			//Security: operation.Security.,
			vendorExtensible: convertExtensibletoReadableFormat(pathItem.ExtensionProps),
		}
	}
	return resource
}

func setResourcesOpenAPI(openAPI openapi3.Swagger) []Resource {
	var resources []Resource
	if openAPI.Paths != nil {
		for path, pathItem := range openAPI.Paths {
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
				resource := setPathInfoOpenAPI(path, methodsArray, pathItem)
				if isServerURLIsAvailable(pathItem.Servers) {
					for _, serverEntry := range pathItem.Servers {
						endpoint := getHostandBasepathandPort(serverEntry.URL)
						resource.productionUrls = append(resource.productionUrls, endpoint)
					}
				}
				resources = append(resources, resource)
			}
		}
	}
	return resources
}

// getHostandBasepathandPort retrieves host, basepath and port from the endpoint defintion
// from of the production endpoints url entry, combination of schemes and host (in openapi v2)
// or server property.
//
// if no scheme is mentioned before the hostname, urlType would be assigned as http
func getHostandBasepathandPort(rawURL string) Endpoint {
	var (
		basepath string
		host     string
		port     uint32
		urlType  string
	)
	if !strings.Contains(rawURL, "://") {
		rawURL = "http://" + rawURL
	}
	parsedURL, err := url.Parse(rawURL)
	if err != nil {
		logger.LoggerOasparser.Fatal(err)
	}

	host = parsedURL.Hostname()
	basepath = parsedURL.Path
	if parsedURL.Port() != "" {
		u32, err := strconv.ParseUint(parsedURL.Port(), 10, 32)
		if err != nil {
			logger.LoggerOasparser.Error("Error passing port value to mgwSwagger", err)
		}
		port = uint32(u32)
	} else {
		if strings.HasPrefix(rawURL, "https://") {
			port = uint32(443)
			urlType = "https"
		} else {
			port = uint32(80)
			urlType = "http"
		}
	}
	return Endpoint{Host: host, Basepath: basepath, Port: port, URLType: urlType}
}

// isServerURLIsAvailable checks the availability od server url in openApi3
func isServerURLIsAvailable(servers openapi3.Servers) bool {
	if servers != nil {
		if len(servers) > 0 && (servers[0].URL != "") {
			return true
		}
	}
	return false
}

// convertExtensibletoReadableFormat unmarshalls the vendor extensible in open api3.
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

// GetXWso2Label extracts the vendor-extension (openapi v3) property.
//
// Default value is 'default'
func GetXWso2Label(vendorExtensions openapi3.ExtensionProps) []string {
	vendorExtensionsMap := convertExtensibletoReadableFormat(vendorExtensions)
	var labelArray []string
	if y, found := vendorExtensionsMap[xWso2Label]; found {
		if val, ok := y.([]interface{}); ok {
			for _, label := range val {
				labelArray = append(labelArray, label.(string))
			}
			return labelArray
		}
		logger.LoggerOasparser.Errorln("Error while parsing the x-wso2-label")
	}
	return []string{"default"}
}

func getHostandBasepathandPortWebSocket(rawURL string) Endpoint {
	var (
		basepath string
		host     string
		port     uint32
		urlType  string
	)
	if !strings.Contains(rawURL, "://") {
		rawURL = "ws://" + rawURL
	}
	parsedURL, err := url.Parse(rawURL)
	if err != nil {
		logger.LoggerOasparser.Fatal(err)
	}

	host = parsedURL.Hostname()
	if parsedURL.Path == "" {
		basepath = "/"
	} else {
		basepath = parsedURL.Path
	}
	if parsedURL.Port() != "" {
		u32, err := strconv.ParseUint(parsedURL.Port(), 10, 32)
		if err != nil {
			logger.LoggerOasparser.Error("Error passing port value to mgwSwagger", err)
		}
		port = uint32(u32)
	} else {
		if strings.HasPrefix(rawURL, "wss://") {
			port = uint32(443)
		} else {
			port = uint32(80)
		}
	}
	urlType = "ws"
	if strings.HasPrefix(rawURL, "wss://") {
		urlType = "wss"
	}
	return Endpoint{Host: host, Basepath: basepath, Port: port, URLType: urlType}
}
