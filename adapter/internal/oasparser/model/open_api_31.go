/*
 *  Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"errors"
	"strconv"
	"strings"

	"github.com/google/uuid"
	"github.com/pb33f/libopenapi/datamodel/high/base"
	"github.com/pb33f/libopenapi/datamodel/high/v3"
	"github.com/pb33f/libopenapi/orderedmap"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"gopkg.in/yaml.v3"
)

// SetInfoOpenAPI31 populates the MgwSwagger object with the properties within the openAPI v3.1 definition.
// The title, version, description, vendor extension map, endpoints based on servers property,
// and pathItem level information are populated here.
//
// for each pathItem; vendor extensions, endpoints (based on servers object), available http Methods,
// are populated. Each resource corresponding to a pathItem, has the property called iD, which is a
// UUID.
//
// No operation specific information is extracted.
func (swagger *MgwSwagger) SetInfoOpenAPI31(swagger31 v3.Document) error {
	var err error
	if swagger31.Info != nil {
		swagger.description = swagger31.Info.Description
		swagger.title = swagger31.Info.Title
		swagger.version = swagger31.Info.Version
	}

	swagger.vendorExtensions = convertExtensibletoReadableFormatOAS31(swagger31.Extensions)
	swagger.securityScheme = setSecuritySchemesOpenAPI31(swagger31)
	for _, security := range swagger31.Security {
		securityMap := make(map[string][]string, security.Requirements.Len())
		for key, val := range security.Requirements.FromOldest() {
			securityMap[key] = val
		}
		swagger.security = append(swagger.security, securityMap)
	}
	swagger.resources, err = setResourcesOpenAPI31(swagger31)
	if err != nil {
		return err
	}

	swagger.apiType = HTTP
	var productionUrls []Endpoint
	// For prototyped APIs, the prototype endpoint is only assinged from api.Yaml. Hence,
	// an exception is made where servers url is not processed when the API is prototyped.
	if isServerURLIsAvailableOAS31(swagger31.Servers) && !swagger.IsProtoTyped {
		for _, serverEntry := range swagger31.Servers {
			if len(serverEntry.URL) == 0 || strings.HasPrefix(serverEntry.URL, "/") {
				continue
			}
			endpoint, err := getHTTPEndpoint(serverEntry.URL)
			if err == nil {
				productionUrls = append(productionUrls, *endpoint)
				swagger.xWso2Basepath = endpoint.Basepath
			} else {
				logger.LoggerOasparser.Info("Not considering the URL in servers object as parsing has failed. ", err)
			}
		}
		if len(productionUrls) > 0 {
			swagger.productionEndpoints = generateEndpointCluster(prodClustersConfigNamePrefix, productionUrls, LoadBalance)
			swagger.sandboxEndpoints = nil
		}
	}
	return nil
}

// setPathInfoOpenAPI31 extracts the path information from the openAPI v3.1 definition and returns a Resource struct.
func setPathInfoOpenAPI31(path string, methods []*Operation, pathItem *v3.PathItem) Resource {
	var resource Resource
	if pathItem != nil {
		resource = Resource{
			path:             path,
			methods:          methods,
			iD:               uuid.New().String(),
			summary:          pathItem.Summary,
			description:      pathItem.Description,
			vendorExtensions: convertExtensibletoReadableFormatOAS31(pathItem.Extensions),
		}
	}
	return resource
}

// setResourcesOpenAPI31 extracts the resource information from the openAPI v3.1 definition and returns a slice of Resource structs.
func setResourcesOpenAPI31(openAPI v3.Document) ([]*Resource, error) {
	var resources []*Resource

	// Check the disable security vendor ext at API level.
	// If it's present, then the same value should be added to the
	// resource level if vendor ext is not present at each resource level.
	val, found := resolveDisableSecurityOAS31(openAPI.Extensions)
	if openAPI.Paths != nil {
		conf, _ := config.ReadConfigs()
		for path, pathItem := range openAPI.Paths.PathItems.FromOldest() {
			if conf.Envoy.MaximumResourcePathLengthInKB != -1 &&
				isResourcePathLimitExceeds(path, int(conf.Envoy.MaximumResourcePathLengthInKB)) {
				return nil, errors.New("path: " + path + " exceeds maximum allowed length")
			}
			// Checks for resource level security. (security is disabled in resource level using x-wso2-disable-security extension)
			isResourceLvlSecurityDisabled, foundInResourceLevel := resolveDisableSecurityOAS31(pathItem.Extensions)
			methodsArray := make([]*Operation, pathItem.GetOperations().Len())
			var arrayIndex int = 0
			for httpMethod, operation := range pathItem.GetOperations().FromOldest() {
				if operation != nil {
					if foundInResourceLevel {
						operation.Extensions = addDisableSecurityIfNotPresentOAS31(operation.Extensions, isResourceLvlSecurityDisabled)
					} else if found {
						operation.Extensions = addDisableSecurityIfNotPresentOAS31(operation.Extensions, val)
					}
					methodsArray[arrayIndex] = getOperationLevelDetailsOAS31(operation, strings.ToUpper(httpMethod))
					arrayIndex++
				}
			}

			resource := setPathInfoOpenAPI31(path, methodsArray, pathItem)
			var productionUrls []Endpoint
			if isServerURLIsAvailableOAS31(pathItem.Servers) {
				for _, serverEntry := range pathItem.Servers {
					if len(serverEntry.URL) == 0 || strings.HasPrefix(serverEntry.URL, "/") {
						continue
					}
					endpoint, err := getHTTPEndpoint(serverEntry.URL)
					if err == nil {
						productionUrls = append(productionUrls, *endpoint)

					} else {
						logger.LoggerOasparser.Info("Not considering the URL in servers object as parsing has failed. ", err)
					}

				}
				if len(productionUrls) > 0 {
					resource.productionEndpoints = generateEndpointCluster(prodClustersConfigNamePrefix, productionUrls, LoadBalance)
				}
			}
			resources = append(resources, &resource)

		}
	}
	return SortResources(resources), nil
}

// setSecuritySchemesOpenAPI31 extracts the security schemes from the openAPI v3.1 definition and returns a slice of SecurityScheme structs.
func setSecuritySchemesOpenAPI31(openAPI v3.Document) []SecurityScheme {
	var securitySchemes []SecurityScheme
	if openAPI.Components != nil {
		for key, val := range openAPI.Components.SecuritySchemes.FromOldest() {
			scheme := SecurityScheme{DefinitionName: key, Type: val.Type, Name: val.Name, In: val.In}
			securitySchemes = append(securitySchemes, scheme)
		}
	}
	logger.LoggerOasparser.Debugf("Security schemes in setSecuritySchemesOpenAPI method %v:", securitySchemes)
	return securitySchemes
}

func getOperationLevelDetailsOAS31(operation *v3.Operation, method string) *Operation {
	extensions := convertExtensibletoReadableFormatOAS31(operation.Extensions)

	if operation.Security == nil {
		return NewOperation(method, nil, extensions)
	}

	var securityData []*base.SecurityRequirement = operation.Security
	var securityArray = make([]map[string][]string, len(securityData))
	for i, security := range securityData {
		securityMap := make(map[string][]string, security.Requirements.Len())
		for key, val := range security.Requirements.FromOldest() {
			securityMap[key] = val
		}
		securityArray[i] = securityMap
	}
	logger.LoggerOasparser.Debugf("Security array %v", securityArray)
	return NewOperation(method, securityArray, extensions)

}

// isServerURLIsAvailableOAS31 checks if the servers object is present and has a valid URL.
func isServerURLIsAvailableOAS31(servers []*v3.Server) bool {
	if servers != nil {
		if len(servers) > 0 && (servers[0].URL != "") {
			return true
		}
	}
	return false
}

// convertExtensibletoReadableFormatOAS31 converts the vendor extensions from the ordered map to a readable format.
func convertExtensibletoReadableFormatOAS31(vendorExtensions *orderedmap.Map[string, *yaml.Node]) map[string]interface{} {
	b, err := yaml.Marshal(vendorExtensions)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshalling vendor extensions: ", err)
	}
	var extensible map[string]interface{}
	err = yaml.Unmarshal(b, &extensible)
	if err != nil {
		logger.LoggerOasparser.Error("Error unmarshalling vendor extensions: ", err)
	}
	return extensible
}

// This method check if the x-wso2-disable-security vendor extension present in the given
// openapi extension prop set.
// If found, it will return two bool values which are the following in order.
// 1st bool represnt the value of the vendor extension.
// 2nd bool represent if the vendor extension present.
func resolveDisableSecurityOAS31(vendorExtensions *orderedmap.Map[string, *yaml.Node]) (bool, bool) {
	extensions := convertExtensibletoReadableFormatOAS31(vendorExtensions)
	if y, found := extensions[xWso2DisableSecurity]; found {
		if val, ok := y.(bool); ok {
			return val, found
		}
		logger.LoggerOasparser.Errorln("Error while parsing the x-wso2-label")
	}
	return false, false
}

// This method add the disable security to given vendor extensions, if it's not present.
func addDisableSecurityIfNotPresentOAS31(vendorExtensions *orderedmap.Map[string, *yaml.Node], val bool) *orderedmap.Map[string, *yaml.Node] {
	if _, found := vendorExtensions.Get(xWso2DisableSecurity); !found {
		node := &yaml.Node{
			Kind:  8,
			Tag:   "!!bool",
			Value: strconv.FormatBool(val),
		}
		vendorExtensions.Set(xWso2DisableSecurity, node)
	}
	return vendorExtensions
}

// GetXWso2LabelOAS31 extracts the vendor-extension (openapi v3.1) property.
//
// Default value is 'default'
func GetXWso2LabelOAS31(vendorExtensions *orderedmap.Map[string, *yaml.Node]) []string {
	vendorExtensionsMap := convertExtensibletoReadableFormatOAS31(vendorExtensions)
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
