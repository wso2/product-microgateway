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
	"os"
	"strconv"

	"github.com/wso2/product-microgateway/adapter/internal/loggers"
)

func retrieveEndpointsFromEnv(apiHashValue string) ([]Endpoint, []Endpoint) {
	var productionEndpoints []Endpoint
	var sandboxEndpoints []Endpoint
	// set production Endpoints
	i := 0
	for {
		var productionEndpointURL string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_prod_endpoint_"+strconv.Itoa(i), "")
		if productionEndpointURL == "" {
			break
		}

		productionEndpoint, err := getHTTPEndpoint(productionEndpointURL)
		if err != nil {
			loggers.LoggerAPI.Errorf("error while reading production endpoint : %v in env variables, %v", productionEndpointURL, err.Error())
		} else if productionEndpoint != nil {
			productionEndpoints = append(productionEndpoints, *productionEndpoint)
		}
		i = i + 1
	}

	// set sandbox Endpoints
	j := 0
	for {
		var sandboxEndpointURL string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_sand_endpoint_"+strconv.Itoa(j), "")
		if sandboxEndpointURL == "" {
			break
		}

		sandboxEndpoint, err := getHTTPEndpoint(sandboxEndpointURL)
		if err != nil {
			loggers.LoggerAPI.Errorf("error while reading sandbox endpoint : %v in env variables, %v", sandboxEndpointURL, err.Error())
		} else if sandboxEndpoint != nil {
			sandboxEndpoints = append(sandboxEndpoints, *sandboxEndpoint)
		}
		j = j + 1
	}

	return productionEndpoints, sandboxEndpoints
}

// RetrieveEndpointBasicAuthCredentialsFromEnv retrieve endpoint security credentials from env variables
func RetrieveEndpointBasicAuthCredentialsFromEnv(apiHashValue string, keyType string, endpointSecurity EndpointSecurity) EndpointSecurity {
	// production Endpoint security
	endpointSecurity.Username = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_"+keyType+
		"_basic_username", endpointSecurity.Username)
	endpointSecurity.Password = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_"+keyType+
		"_basic_password", endpointSecurity.Password)
	return endpointSecurity
}

func resolveEnvValueForEndpointConfig(envKey string, defaultVal string) string {
	envValue, exists := os.LookupEnv(envKey)
	if exists {
		loggers.LoggerAPI.Debugf("resolve env value %v", envValue)
		envUnquoted, err := strconv.Unquote(envValue)
		if err != nil {
			loggers.LoggerAPI.Debugf("Unquoting string %v in env variables has failed. %v", envValue, err.Error())
			// unquoting has failed usually means it was unquoted and in correct format originally
			return envValue
		}
		return envUnquoted
	}
	return defaultVal
}
