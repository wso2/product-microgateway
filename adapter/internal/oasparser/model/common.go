/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// This file contains util methods either common for operation/resource/base levels
// Or common to swagger/OpenAPI/AsyncAPI when populating the mgwSwagger object

import (
	"errors"
	"fmt"
	"net/url"
	"regexp"
	"strconv"
	"strings"

	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
)

func arrayContains(a []string, x string) bool {
	for _, n := range a {
		if x == n {
			return true
		}
	}
	return false
}

// getXWso2AuthHeader extracts the value of xWso2AuthHeader extension.
// if the property is not available, an empty string is returned.
func getXWso2AuthHeader(vendorExtensions map[string]interface{}) string {
	xWso2AuthHeader := ""
	if y, found := vendorExtensions[constants.XAuthHeader]; found {
		if val, ok := y.(string); ok {
			xWso2AuthHeader = val
		}
	}
	return xWso2AuthHeader
}

// getXWso2Basepath extracts the value of xWso2BasePath extension.
// if the property is not available, an empty string is returned.
func getXWso2Basepath(vendorExtensions map[string]interface{}) string {
	xWso2basepath := ""
	if y, found := vendorExtensions[constants.XWso2BasePath]; found {
		if val, ok := y.(string); ok {
			xWso2basepath = val
		}
	}
	return xWso2basepath
}

// ResolveThrottlingTier extracts the value of x-wso2-throttling-tier and
// x-throttling-tier extension. if x-wso2-throttling-tier is available it
// will be prioritized.
// if both the properties are not available, an empty string is returned.
func ResolveThrottlingTier(vendorExtensions map[string]interface{}) string {
	xTier := ""
	if x, found := vendorExtensions[constants.XWso2ThrottlingTier]; found {
		if val, ok := x.(string); ok {
			xTier = val
		}
	} else if y, found := vendorExtensions[constants.XThrottlingTier]; found {
		if val, ok := y.(string); ok {
			xTier = val
		}
	}
	return xTier
}

// ResolveDisableSecurity extracts the value of x-auth-type extension.
// if the property is not available, false is returned.
// If the API definition is fed from API manager, then API definition contains
// x-auth-type as "None" for non secured APIs. Then the return value would be true.
// If the API definition is fed through apictl, the users can use either
// x-wso2-disable-security : true/false to enable and disable security.
func ResolveDisableSecurity(vendorExtensions map[string]interface{}) bool {
	disableSecurity := false
	y, vExtAuthType := vendorExtensions[constants.XAuthType]
	z, vExtDisableSecurity := vendorExtensions[constants.XWso2DisableSecurity]
	if vExtDisableSecurity {
		// If x-wso2-disable-security is present, then disableSecurity = val
		if val, ok := z.(bool); ok {
			disableSecurity = val
		}
	}
	if vExtAuthType && !disableSecurity {
		// If APIs are published through APIM, all resource levels contains x-auth-type
		// vendor extension.
		if val, ok := y.(string); ok {
			// If the x-auth-type vendor ext is None, then the API/resource is considerd
			// to be non secure
			if val == constants.None {
				disableSecurity = true
			}
		}
	}
	return disableSecurity
}

func getHTTPEndpoint(rawURL string) (*Endpoint, error) {
	return getHostandBasepathandPort(constants.HTTP, rawURL)
}

func getWebSocketEndpoint(rawURL string) (*Endpoint, error) {
	return getHostandBasepathandPort(constants.WS, rawURL)
}

func getHostandBasepathandPort(apiType string, rawURL string) (*Endpoint, error) {
	var (
		basepath string
		host     string
		port     uint32
		urlType  string
	)

	// Remove leading and trailing spaces of rawURL
	rawURL = strings.Trim(rawURL, " ")

	if !strings.Contains(rawURL, "://") {
		if apiType == constants.HTTP {
			rawURL = "http://" + rawURL
		} else if apiType == constants.WS {
			rawURL = "ws://" + rawURL
		}
	}

	parsedURL, err := url.Parse(rawURL)
	if err != nil {
		logger.LoggerOasparser.Errorf("Failed to parse the malformed endpoint %v. Error message: %v", rawURL, err)
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
			logger.LoggerOasparser.Error("Endpoint port is not in the expected format.", err)
		}
		port = uint32(u32)
	} else {
		if strings.HasPrefix(rawURL, "https://") || strings.HasPrefix(rawURL, "wss://") {
			port = uint32(443)
		} else {
			port = uint32(80)
		}
	}

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

// unmarshalSwaggerResources used to populate mgwSwagger for both OpenAPI and AsyncAPI
func unmarshalSwaggerResources(path string, methods []*Operation, vendorExtensions map[string]interface{}) Resource {
	return Resource{
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
		vendorExtensions: vendorExtensions,
	}
}

// getRewriteRegexFromPathTemplate returns a regex with capture groups for given rewritePathTemplate
func getRewriteRegexFromPathTemplate(pathTemplate, rewritePathTemplate string) (string, error) {
	rewriteRegex := "/" + strings.TrimSuffix(strings.TrimPrefix(rewritePathTemplate, "/"), "/")
	pathParamToIndexMap := getPathParamToIndexMap(pathTemplate)
	r := regexp.MustCompile(`{uri.var.([^{}]+)}`) // define a capture group to catch the path param
	matches := r.FindAllStringSubmatch(rewritePathTemplate, -1)
	for _, match := range matches {
		// match is slice always with length two (since one capture group is defined in the regex)
		// hence we do not want to explicitly validate the slice length
		templatedParam := match[0]
		param := match[1]
		if index, ok := pathParamToIndexMap[param]; ok {
			rewriteRegex = strings.ReplaceAll(rewriteRegex, templatedParam, fmt.Sprintf(`\%d`, index))
		} else {
			return "", fmt.Errorf("invalid path param %q in rewrite path", param)
		}
	}

	// validate rewriteRegex
	if matched, _ := regexp.MatchString(`^[a-zA-Z0-9~/_.\-\\]*$`, rewriteRegex); !matched {
		logger.LoggerOasparser.Error("Rewrite path includes invalid characters")
		return "", fmt.Errorf("rewrite path regex includes invalid characters, regex %q", rewriteRegex)
	}

	return rewriteRegex, nil
}

// getPathParamToIndexMap returns a map of path params to its index (map of path param -> index)
func getPathParamToIndexMap(pathTemplate string) map[string]int {
	indexMap := make(map[string]int)
	r := regexp.MustCompile(`{([^{}]+)}`) // define a capture group to catch the path param
	matches := r.FindAllStringSubmatch(pathTemplate, -1)
	for i, paramMatches := range matches {
		// paramMatches is slice always with length two (since one capture group is defined in the regex)
		// hence we do not want to explicitly validate the slice length
		indexMap[paramMatches[1]] = i + 1
	}
	return indexMap
}
