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
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
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
	swagger.securityScheme = getSecurityDefinitions(swagger2)
	swagger.security = swagger2.Security
	swagger.resources = getResourcesSwagger(swagger2)
	swagger.apiType = constants.HTTP
	swagger.xWso2Basepath = swagger2.BasePath
	// According to the definition, multiple schemes can be mentioned. Since the microgateway can assign only one scheme
	// https is prioritized over http. If it is ws or wss, the microgateway will print an error.
	// If the schemes property is not mentioned at all, http will be assigned. (Only swagger 2 version has this property)
	// For prototyped APIs, the prototype endpoint is only assinged from api.Yaml. Hence,
	// an exception is made where host property is not processed when the API is prototyped.
	if swagger2.Host != "" && !swagger.IsProtoTyped {
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
		endpoint, err := getHTTPEndpoint(urlScheme + swagger2.Host + swagger2.BasePath)
		if err == nil {
			productionEndpoints := append([]Endpoint{}, *endpoint)
			swagger.productionEndpoints = generateEndpointCluster(constants.ProdClustersConfigNamePrefix,
				productionEndpoints, constants.LoadBalance)
			swagger.sandboxEndpoints = nil
		} else {
			return errors.New("error encountered when parsing the endpoint")
		}
	}
	return nil
}

// getResourcesSwagger sets swagger (openapi v2) paths as mgwSwagger resources.
func getResourcesSwagger(swagger2 spec.Swagger) []*Resource {
	var resources []*Resource
	// Check if the "x-wso2-disable-security" vendor ext is present at the API level.
	// If API level vendor ext is present, then the same key:value should be added to
	// resourve level, if it's not present at resource level using "addResourceLevelDisableSecurity"
	if swagger2.Paths != nil {
		for path, pathItem := range swagger2.Paths.Paths {
			disableSecurity, found := swagger2.VendorExtensible.Extensions.GetBool(constants.XWso2DisableSecurity)
			// Checks for resource level security, if security is disabled in resource level,
			// below code segment will override above two variable values (disableSecurity & found)
			disableResourceLevelSecurity, foundInResourceLevel := pathItem.Extensions.GetBool(constants.XWso2DisableSecurity)
			if foundInResourceLevel {
				logger.LoggerOasparser.Infof("x-wso2-disable-security extension is available in the API: %v %v's resource %v.",
					swagger2.Info.Title, swagger2.Info.Version, path)
				disableSecurity = disableResourceLevelSecurity
				found = true
			}
			var methodsArray []*Operation
			methodFound := false
			if pathItem.Get != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Get.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("GET", pathItem.Get.Security,
					pathItem.Get.Extensions))
				methodFound = true
			}
			if pathItem.Post != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Post.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("POST", pathItem.Post.Security,
					pathItem.Post.Extensions))
				methodFound = true
			}
			if pathItem.Put != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Put.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("PUT", pathItem.Put.Security,
					pathItem.Put.Extensions))
				methodFound = true
			}
			if pathItem.Delete != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Delete.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("DELETE", pathItem.Delete.Security,
					pathItem.Delete.Extensions))
				methodFound = true
			}
			if pathItem.Head != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Head.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("HEAD", pathItem.Head.Security,
					pathItem.Head.Extensions))
				methodFound = true
			}
			if pathItem.Patch != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Patch.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("PATCH", pathItem.Patch.Security,
					pathItem.Patch.Extensions))
				methodFound = true
			}
			if pathItem.Options != nil {
				if found {
					addResourceLevelDisableSecurity(&pathItem.Options.VendorExtensible, disableSecurity)
				}
				methodsArray = append(methodsArray, NewOperation("OPTION", pathItem.Options.Security,
					pathItem.Options.Extensions))
				methodFound = true
			}
			if methodFound {
				resource := generateResource(path, methodsArray, pathItem.Extensions)
				resources = append(resources, &resource)
			}
		}
	}
	return SortResources(resources)
}

// Sets security definitions defined in swagger 2 format.
func getSecurityDefinitions(swagger2 spec.Swagger) []SecurityScheme {
	var securitySchemes []SecurityScheme

	for key, val := range swagger2.SecurityDefinitions {
		scheme := SecurityScheme{DefinitionName: key, Type: val.Type, Name: val.Name, In: val.In}
		securitySchemes = append(securitySchemes, scheme)
	}
	return securitySchemes
}

// This methods adds x-wso2-disable-security vendor extension
// if it's not present in the given vendor extensions.
func addResourceLevelDisableSecurity(v *spec.VendorExtensible, enable bool) {
	if _, found := v.Extensions.GetBool(constants.XWso2DisableSecurity); !found {
		v.AddExtension(constants.XWso2DisableSecurity, enable)
	}
}
