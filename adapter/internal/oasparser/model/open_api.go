/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"errors"
	"net/url"
	"regexp"
	"strconv"
	"strings"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/google/uuid"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// hostNameValidator regex is for validate the host name of the URL
// Hostname can have letters, numbers , dots and hypens.But Hostname should not start in a hyphen or a dot.
// ie : http://www.google.com/get  <-- Hostname of the URL is www.google.com
// ie : https://dev.choreo.lk:8899/api/v1  <-- Hostname is dev.choreo.lk
// Above hostNameValidator regex can identify correct hostname from a URL.
// There are 3 character classes defined for check characters of each position.
// First character class for check initial part of the string (^[a-zA-Z0-9]) -
//			--> hostname should start in a letter or a number
// Second class for check middle section of the string ([a-zA-Z0-9-.]*)
//			--> mid of the hostname can contains letters,numbers,hyphens and dots
// Third class for check trailing characters ([0-9a-zA-Z]$)
//			--> hostname should ends with a letter or a number.can`t have any other character
// Wrong URLs as per above regex
// http://#de.abc.com:80/api, http://&de.abc.com:80/api, http://!de.abc.com:80/api, tcp://http::8900, http://::80
// Correct URLs
// https://www.google.com, http://dev.choreo.lk:8899/api/v1, http://127.0.0.1:8080

const (
	hostNameValidator = "^[a-zA-Z0-9][a-zA-Z0-9-.]*[0-9a-zA-Z]$"
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
func (swagger *MgwSwagger) SetInfoOpenAPI(swagger3 openapi3.Swagger) error {
	var err error
	if swagger3.Info != nil {
		swagger.description = swagger3.Info.Description
		swagger.title = swagger3.Info.Title
		swagger.version = swagger3.Info.Version
	}

	swagger.vendorExtensions = convertExtensibletoReadableFormat(swagger3.ExtensionProps)
	swagger.securityScheme = setSecuritySchemesOpenAPI(swagger3)
	for _, security := range swagger3.Security {
		swagger.security = append(swagger.security, security)
	}
	swagger.resources, err = setResourcesOpenAPI(swagger3)
	if err != nil {
		return err
	}

	swagger.apiType = HTTP
	var productionUrls []Endpoint
	// For prototyped APIs, the prototype endpoint is only assinged from api.Yaml. Hence,
	// an exception is made where servers url is not processed when the API is prototyped.
	if isServerURLIsAvailable(swagger3.Servers) && !swagger.IsProtoTyped {
		for _, serverEntry := range swagger3.Servers {
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

func setPathInfoOpenAPI(path string, methods []*Operation, pathItem *openapi3.PathItem) Resource {
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
			//security: pathItem.operation.Security.,
			vendorExtensions: convertExtensibletoReadableFormat(pathItem.ExtensionProps),
		}
	}
	return resource
}

func setResourcesOpenAPI(openAPI openapi3.Swagger) ([]*Resource, error) {
	var resources []*Resource

	// Check the disable security vendor ext at API level.
	// If it's present, then the same value should be added to the
	// resource level if vendor ext is not present at each resource level.
	val, found := resolveDisableSecurity(openAPI.ExtensionProps)
	if openAPI.Paths != nil {
		conf, _ := config.ReadConfigs()
		for path, pathItem := range openAPI.Paths {
			if conf.Envoy.MaximumResourcePathLengthInKB != -1 &&
				isResourcePathLimitExceeds(path, int(conf.Envoy.MaximumResourcePathLengthInKB)) {
				return nil, errors.New("path: " + path + " exceeds maximum allowed length")
			}
			// Checks for resource level security. (security is disabled in resource level using x-wso2-disable-security extension)
			isResourceLvlSecurityDisabled, foundInResourceLevel := resolveDisableSecurity(pathItem.ExtensionProps)
			methodsArray := make([]*Operation, len(pathItem.Operations()))
			var arrayIndex int = 0
			for httpMethod, operation := range pathItem.Operations() {
				if operation != nil {
					if foundInResourceLevel {
						operation.ExtensionProps = addDisableSecurityIfNotPresent(operation.ExtensionProps, isResourceLvlSecurityDisabled)
					} else if found {
						operation.ExtensionProps = addDisableSecurityIfNotPresent(operation.ExtensionProps, val)
					}
					methodsArray[arrayIndex] = getOperationLevelDetails(operation, httpMethod)
					arrayIndex++
				}
			}

			resource := setPathInfoOpenAPI(path, methodsArray, pathItem)
			var productionUrls []Endpoint
			if isServerURLIsAvailable(pathItem.Servers) {
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

func setSecuritySchemesOpenAPI(openAPI openapi3.Swagger) []SecurityScheme {
	var securitySchemes []SecurityScheme
	for key, val := range openAPI.Components.SecuritySchemes {
		scheme := SecurityScheme{DefinitionName: key, Type: val.Value.Type, Name: val.Value.Name, In: val.Value.In}
		securitySchemes = append(securitySchemes, scheme)
	}
	logger.LoggerOasparser.Debugf("Security schemes in setSecuritySchemesOpenAPI method %v:", securitySchemes)
	return securitySchemes
}

func getOperationLevelDetails(operation *openapi3.Operation, method string) *Operation {
	extensions := convertExtensibletoReadableFormat(operation.ExtensionProps)

	if operation.Security == nil {
		return NewOperation(method, nil, extensions)
	}

	var securityData []openapi3.SecurityRequirement = *(operation.Security)
	var securityArray = make([]map[string][]string, len(securityData))
	for i, security := range securityData {
		securityArray[i] = security
	}
	logger.LoggerOasparser.Debugf("Security array %v", securityArray)
	return NewOperation(method, securityArray, extensions)

}

func getHTTPEndpoint(rawURL string) (*Endpoint, error) {
	return getHostandBasepathandPort(HTTP, rawURL)
}

func getWebSocketEndpoint(rawURL string) (*Endpoint, error) {
	return getHostandBasepathandPort(WS, rawURL)
}

// getHostandBasepathandPort retrieves host, basepath and port from the endpoint defintion
// from of the production endpoints url entry, combination of schemes and host (in openapi v2)
// or server property.
//
// if no scheme is mentioned before the hostname, urlType would be assigned as http
func getHostandBasepathandPort(apiType string, rawURL string) (*Endpoint, error) {
	var (
		basepath string
		host     string
		port     uint32
		urlType  string
	)

	// Remove leading and trailing spaces of rawURL
	rawURL = strings.TrimSpace(rawURL)

	if !strings.Contains(rawURL, "://") {
		if apiType == HTTP || apiType == GRAPHQL || apiType == WEBHOOK {
			rawURL = "http://" + rawURL
		} else if apiType == WS {
			rawURL = "ws://" + rawURL
		}
	}
	parsedURL, err := url.Parse(rawURL)
	if err != nil {
		logger.LoggerOasparser.Debugf("Failed to parse the malformed endpoint %v. Error message: %v", rawURL, err)
		return nil, err
	}

	// Hostname validation
	if err == nil && !regexp.MustCompile(hostNameValidator).MatchString(parsedURL.Hostname()) {
		logger.LoggerOasparser.Error("Malformed endpoint detected (Invalid host name) : ", rawURL)
		return nil, errors.New("malformed endpoint detected (Invalid host name) : " + rawURL)
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
		if strings.HasPrefix(rawURL, "https://") || strings.HasPrefix(rawURL, "wss://") {
			port = uint32(443)
		} else {
			port = uint32(80)
		}
	}

	urlType = "http"
	if strings.HasPrefix(rawURL, "https://") {
		urlType = "https"
	} else if strings.HasPrefix(rawURL, "http://") {
		urlType = "http"
	} else if strings.HasPrefix(rawURL, "wss://") {
		urlType = "wss"
	} else if strings.HasPrefix(rawURL, "ws://") {
		urlType = "ws"
	}

	return &Endpoint{Host: host, Basepath: basepath, Port: port, URLType: urlType, RawURL: rawURL}, nil
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
func convertExtensibletoReadableFormat(vendorExtensions openapi3.ExtensionProps) map[string]interface{} {
	jsnRawExtensible := vendorExtensions.Extensions
	b, err := json.Marshal(jsnRawExtensible)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshalling vendor extensions: ", err)
	}

	var extensible map[string]interface{}
	err = json.Unmarshal(b, &extensible)
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
func resolveDisableSecurity(vendorExtensions openapi3.ExtensionProps) (bool, bool) {
	extensions := convertExtensibletoReadableFormat(vendorExtensions)
	if y, found := extensions[xWso2DisableSecurity]; found {
		if val, ok := y.(bool); ok {
			return val, found
		}
		logger.LoggerOasparser.Errorln("Error while parsing the x-wso2-label")
	}
	return false, false
}

// This method add the disable security to given vendor extensions, if it's not present.
func addDisableSecurityIfNotPresent(vendorExtensions openapi3.ExtensionProps, val bool) openapi3.ExtensionProps {
	_, found := resolveDisableSecurity(vendorExtensions)
	if !found {
		vendorExtensions.Extensions[xWso2DisableSecurity] = val
	}
	return vendorExtensions
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
