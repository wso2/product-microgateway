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
	"errors"

	"github.com/go-openapi/spec"
	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// SetInfoSwagger populates the MgwSwagger object with the properties within the openAPI v2
// (swagger) definition.
// The title, version, description, vendor extension map, endpoints based on host and schemes properties,
// and pathItem level information are populated here.
//
// for each pathItem; vendor extensions, available http Methods,
// are populated. Each resource corresponding to a pathItem, has the property called iD, which is a
// UUID.
//
// No operation specific information is extracted.
func (swagger *MgwSwagger) SetInfoSwagger(swagger2 spec.Swagger) error {
	if swagger2.Info != nil {
		swagger.description = swagger2.Info.Description
		swagger.title = swagger2.Info.Title
		swagger.version = swagger2.Info.Version
	}
	swagger.vendorExtensions = swagger2.VendorExtensible.Extensions
	swagger.resources = setResourcesSwagger(swagger2)
	swagger.apiType = HTTP
	swagger.xWso2Basepath = swagger2.BasePath
	swagger.securityScheme = setSecurityDefinitions(swagger2)
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
		endpoint, err := getHostandBasepathandPort(urlScheme + swagger2.Host + swagger2.BasePath)
		if err == nil {
			productionEndpoints := append([]Endpoint{}, *endpoint)
			swagger.productionEndpoints = generateEndpointCluster(xWso2ProdEndpoints, productionEndpoints, LoadBalance)
		} else {
			return errors.New("error encountered when parsing the endpoint")
		}
	}
	return nil
}

// setResourcesSwagger sets swagger (openapi v2) paths as mgwSwagger resources.
func setResourcesSwagger(swagger2 spec.Swagger) []Resource {
	var resources []Resource
	// Check if the "x-wso2-disable-security" vendor ext is present at the API level.
	// If API level vendor ext is present, then the same key:value should be added to
	// resourve level, if it's not present at resource level using "addResourceLevelDisableSecurity"
	disableSecurity, found := swagger2.VendorExtensible.Extensions.GetBool(xWso2DisableSecurity)
	if swagger2.Paths != nil {
		for path, pathItem := range swagger2.Paths.Paths {
			var methodsArray []Operation
			methodFound := false
			if pathItem.Get != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Get.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Get.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Get.Security)
				}
				methodsArray = append(methodsArray, NewOperation("GET", pathItem.Get.Security,
					pathItem.Get.Extensions))
				methodFound = true
			}
			if pathItem.Post != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Post.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Post.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Post.Security)
				}
				methodsArray = append(methodsArray, NewOperation("POST", pathItem.Post.Security,
					pathItem.Post.Extensions))
				methodFound = true
			}
			if pathItem.Put != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Put.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Put.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Put.Security)
				}
				methodsArray = append(methodsArray, NewOperation("PUT", pathItem.Put.Security,
					pathItem.Put.Extensions))
				methodFound = true
			}
			if pathItem.Delete != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Delete.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Delete.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Delete.Security)
				}
				methodsArray = append(methodsArray, NewOperation("DELETE", pathItem.Delete.Security,
					pathItem.Delete.Extensions))
				methodFound = true
			}
			if pathItem.Head != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Head.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Head.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Head.Security)
				}
				methodsArray = append(methodsArray, NewOperation("HEAD", pathItem.Head.Security,
					pathItem.Head.Extensions))
				methodFound = true
			}
			if pathItem.Patch != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Patch.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Patch.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Patch.Security)
				}
				methodsArray = append(methodsArray, NewOperation("PATCH", pathItem.Patch.Security,
					pathItem.Patch.Extensions))
				methodFound = true
			}
			if pathItem.Options != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Options.VendorExtensible, disableSecurity)
				}
				if applicationSecurity, found := pathItem.Options.VendorExtensible.Extensions[xWso2ApplicationSecurity]; found {
					setApplicationSecurity(applicationSecurity, &pathItem.Options.Security)
				}
				methodsArray = append(methodsArray, NewOperation("OPTION", pathItem.Options.Security,
					pathItem.Options.Extensions))
				methodFound = true
			}
			if methodFound {
				resource := setOperationSwagger(path, methodsArray, pathItem)
				resources = append(resources, resource)
			}
		}
	}
	return SortResources(resources)
}

// Sets security definitions defined in swagger 2 format.
func setSecurityDefinitions(swagger2 spec.Swagger) []SecurityScheme {
	var securitySchemes []SecurityScheme
	var isApplicationSecurityOptional = true
	result, ok := swagger2.Extensions[xWso2ApplicationSecurity].(map[string]interface{})
	if ok {
		for key, value := range result {
			if (key == "optional" && value != true) {
				isApplicationSecurityOptional = false
			}
		}
		if !isApplicationSecurityOptional {
			if _, found := result["security-types"]; found {
				if val, ok := result["security-types"].([]interface{}); ok {
					for _, mapValue := range val {
						if mapValue == "api_key" {
							scheme := SecurityScheme{DefinitionName: mapValue.(string), Type: "api_key" , Name: mapValue.(string)}
							securitySchemes = append(securitySchemes, scheme)
						}
					}
				}
			} 
		}
	}

	for key , val := range swagger2.SecurityDefinitions {
		scheme := SecurityScheme{DefinitionName: key, Type: val.Type , Name: val.Name, In: val.In}
		securitySchemes = append(securitySchemes, scheme)
	}
	logger.LoggerOasparser.Debugf("Security schemes in setSecurityDefinitions  %v:",securitySchemes)
	return securitySchemes
}

// This methods adds x-wso2-disable-security vendor extension
// if it's not present in the given vendor extensions.
func addResourceLevelDisableSecurity(v *spec.VendorExtensible, enable bool) {
	if _, found := v.Extensions.GetBool(xWso2DisableSecurity); !found {
		v.AddExtension(xWso2DisableSecurity, enable)
	}
}

// checks whether application level security given by (x-wso2-application-security extension)is optional or not
func getIsApplicationSecurityOptional(applicationSecurity interface{}) bool{
	var isApplicationSecurityOptional = true
	result, ok := applicationSecurity.(map[string]interface{})
	if ok {
		for key, value := range result {
			if (key == "optional" && value != true) {
				isApplicationSecurityOptional = false
			}
		}
	}
	return isApplicationSecurityOptional
}

// sets application level security defined under the x-wso2-application-security extension.
func setApplicationSecurity(applicationSecurity interface{}, pathItemSecurity *[]map[string][]string){
	var isApplicationSecurityOptional = getIsApplicationSecurityOptional(applicationSecurity)
	result, ok := applicationSecurity.(map[string]interface{})
	if ok && !isApplicationSecurityOptional{
		if _, found := result["security-types"]; found {
			if val, ok := result["security-types"].([]interface{}); ok {
				for _, mapValue := range val {
					if mapValue == "api_key" {
						applicationAPIKeyMap := map[string][]string{
							mapValue.(string): {},
						}
						*pathItemSecurity = append(*pathItemSecurity, applicationAPIKeyMap)
					}
				}
			}
		} 
	}
}

func getSwaggerOperationLevelDetails(operation *spec.Operation, method string) Operation {
	var securityData []map[string][]string = operation.Security
	return NewOperation(method, securityData, operation.Extensions)
}

func setOperationSwagger(path string, methods []Operation, pathItem spec.PathItem) Resource {
	var resource Resource
	resource = Resource{
		path:    path,
		methods: methods,
		// TODO: (VirajSalaka) This will not solve the actual problem when incremental Xds is introduced (used for cluster names)
		iD: uuid.New().String(),
		// PathItem object in swagger 2 specification does not contain summary and description properties
		summary:     "",
		description: "",
		//schemes:          operation.Schemes,
		//tags:             operation.Tags,
		//security:         operation.Security,
		vendorExtensions: pathItem.VendorExtensible.Extensions,
	}
	return resource
}

//SetInfoSwaggerWebSocket populates the mgwSwagger object for web sockets
// TODO - (VirajSalaka) read cors config and populate mgwSwagger feild
func (swagger *MgwSwagger) SetInfoSwaggerWebSocket(apiData APIYaml) error {

	data := apiData.Data
	// UUID in the generated api.yaml file is considerd as swagger.id
	swagger.id = data.ID
	// Set apiType as WS for websockets
	swagger.apiType = "WS"
	// name and version in api.yaml corresponds to title and version respectively.
	swagger.title = data.Name
	swagger.version = data.Version
	// context value in api.yaml is assigned as xWso2Basepath
	swagger.xWso2Basepath = data.Context + "/" + swagger.version

	// productionURL & sandBoxURL values are extracted from endpointConfig in api.yaml
	endpointConfig := data.EndpointConfig
	if len(endpointConfig.SandBoxEndpoints) > 0 {
		var endpoints []Endpoint
		endpointType := LoadBalance
		for _, endpointConfig := range endpointConfig.SandBoxEndpoints {
			sandBoxEndpoint, err := getHostandBasepathandPortWebSocket(endpointConfig.Endpoint)
			if err == nil {
				endpoints = append(endpoints, *sandBoxEndpoint)
			} else {
				return err
			}
		}
		if len(endpointConfig.SandboxFailoverEndpoints) > 0 {
			for _, endpointConfig := range endpointConfig.SandboxFailoverEndpoints {
				failoverEndpoint, err := getHostandBasepathandPortWebSocket(endpointConfig.Endpoint)
				if err == nil {
					endpointType = FailOver
					endpoints = append(endpoints, *failoverEndpoint)
				} else {
					return err
				}
			}
		}
		swagger.sandboxEndpoints = generateEndpointCluster(xWso2SandbxEndpoints, endpoints, endpointType)
	}
	if len(endpointConfig.ProductionEndpoints) > 0 {
		var endpoints []Endpoint
		endpointType := LoadBalance
		for _, endpointConfig := range endpointConfig.ProductionEndpoints {
			prodEndpoint, err := getHostandBasepathandPortWebSocket(endpointConfig.Endpoint)
			if err == nil {
				endpoints = append(endpoints, *prodEndpoint)
			} else {
				return err
			}
		}
		if len(endpointConfig.ProductionFailoverEndpoints) > 0 {
			for _, endpointConfig := range endpointConfig.ProductionFailoverEndpoints {
				failoverEndpoint, err := getHostandBasepathandPortWebSocket(endpointConfig.Endpoint)
				if err == nil {
					endpointType = FailOver
					endpoints = append(endpoints, *failoverEndpoint)
				} else {
					return err
				}
			}
		}
		swagger.productionEndpoints = generateEndpointCluster(xWso2ProdEndpoints, endpoints, endpointType)
	}
	return nil
}
