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
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"errors"
	"os"
	"strconv"
	"strings"

	"github.com/wso2/product-microgateway/adapter/internal/loggers"
)

const (
	basicAuthSecurity string = "BASIC"
)

// VerifyMandatoryFields check and pupulates the mandatory fields if null
func VerifyMandatoryFields(apiYaml APIYaml) error {
	var errMsg string = ""
	var apiName string = apiYaml.Data.Name
	var apiVersion string = apiYaml.Data.Version

	if apiName == "" {
		apiName = "unknownAPIName"
		errMsg = "API Name "
	}

	if apiVersion == "" {
		apiVersion = "unknownAPIVersion"
		errMsg = errMsg + "API Version "
	}

	if apiYaml.Data.Context == "" {
		errMsg = errMsg + "API Context "
	}

	if len(apiYaml.Data.EndpointConfig.ProductionEndpoints) < 1 &&
		len(apiYaml.Data.EndpointConfig.SandBoxEndpoints) < 1 {
		errMsg = errMsg + "API production and sandbox endpoints "
	}

	if errMsg != "" {
		errMsg = errMsg + "fields cannot be empty for " + apiName + " " + apiVersion
		return errors.New(errMsg)
	}

	for _, ep := range apiYaml.Data.EndpointConfig.ProductionEndpoints {
		if strings.HasPrefix(ep.Endpoint, "/") || len(strings.TrimSpace(ep.Endpoint)) < 1 {
			return errors.New("relative urls or empty values are not supported for API production endpoints")
		}
	}
	for _, ep := range apiYaml.Data.EndpointConfig.SandBoxEndpoints {
		if strings.HasPrefix(ep.Endpoint, "/") || len(strings.TrimSpace(ep.Endpoint)) < 1 {
			return errors.New("relative urls or empty values are not supported for API sandbox endpoints")
		}
	}
	return nil
}

// ExtractAPIInformation reads the values in api.yaml/api.json and populates ProjectAPI struct
func ExtractAPIInformation(apiProject *ProjectAPI, apiYaml APIYaml) {
	apiProject.APIType = strings.ToUpper(apiYaml.Data.APIType)
	apiProject.APILifeCycleStatus = strings.ToUpper(apiYaml.Data.LifeCycleStatus)

	var apiHashValue string = generateHashValue(apiYaml.Data.Name, apiYaml.Data.Version)

	endpointConfig := apiYaml.Data.EndpointConfig
	productionEndpoints, sandboxEndpoints := retrieveEndpointsFromEnv(apiHashValue)
	if len(productionEndpoints) > 0 && len(sandboxEndpoints) > 0 {
		apiProject.ProductionEndpoints = productionEndpoints
		apiProject.SandboxEndpoints = sandboxEndpoints
	}

	// production Endpoint security
	prodEpSecurity, _ := retrieveEndPointSecurityInfo("api_"+apiHashValue,
		endpointConfig.EndpointSecurity.Production, "prod")

	// sandbox Endpoint security
	sandBoxEpSecurity, _ := retrieveEndPointSecurityInfo("api_"+apiHashValue,
		endpointConfig.EndpointSecurity.Sandbox, "sand")

	epSecurity := APIEndpointSecurity{
		Sandbox:    sandBoxEpSecurity,
		Production: prodEpSecurity,
	}

	// organization ID would remain empty string if unassigned
	apiProject.OrganizationID = apiYaml.Data.OrganizationID

	apiProject.EndpointSecurity = epSecurity
}

func retrieveEndpointsFromEnv(apiHashValue string) ([]Endpoint, []Endpoint) {
	var productionEndpoints []Endpoint
	var sandboxEndpoints []Endpoint

	i := 0
	for {
		var productionEndpointURL string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_prod_endpoint_"+strconv.Itoa(i), "")
		if productionEndpointURL == "" {
			break
		}
		productionEndpoint, err := getHostandBasepathandPort(productionEndpointURL)
		if err != nil {
			loggers.LoggerAPI.Errorf("error while reading production endpoint : %v in env variables, %v", productionEndpointURL, err.Error())
		}
		productionEndpoints = append(productionEndpoints, *productionEndpoint)

		// sandbox Endpoints set
		var sandboxEndpointURL string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_sand_endpoint_"+strconv.Itoa(i), "")
		if sandboxEndpointURL == "" {
			break
		}
		sandboxEndpoint, err := getHostandBasepathandPort(sandboxEndpointURL)
		if err != nil {
			loggers.LoggerAPI.Errorf("error while reading production endpoint : %v in env variables, %v", sandboxEndpointURL, err.Error())
		}
		sandboxEndpoints = append(sandboxEndpoints, *sandboxEndpoint)
		i = 1 + 1
	}
	return productionEndpoints, sandboxEndpoints
}

func retrieveEndPointSecurityInfo(value string, endPointSecurity EndpointSecurity,
	keyType string) (epSecurityInfo EndpointSecurity, err error) {
	var username string
	var password string
	var securityType = endPointSecurity.Type

	if securityType != "" {
		if securityType == basicAuthSecurity {
			username = resolveEnvValueForEndpointConfig(value+"_"+keyType+"_basic_username", endPointSecurity.Username)
			password = resolveEnvValueForEndpointConfig(value+"_"+keyType+"_basic_password", endPointSecurity.Password)

			epSecurityInfo.Username = username
			epSecurityInfo.Password = password
			epSecurityInfo.Type = securityType
			epSecurityInfo.Enabled = endPointSecurity.Enabled
			return epSecurityInfo, nil
		}
		errMsg := securityType + " endpoint security type is not currently supported with" +
			"WSO2 Choreo Connect"
		err = errors.New(errMsg)
		loggers.LoggerAPI.Error(errMsg)
	}
	return epSecurityInfo, err
}

func resolveEnvValueForEndpointConfig(envKey string, defaultVal string) string {
	envValue, exists := os.LookupEnv(envKey)
	if exists {
		loggers.LoggerAPI.Debugf("resolve env value %v", envValue)
		return envValue
	}
	return defaultVal
}

func generateHashValue(apiName string, apiVersion string) string {
	endpointConfigSHValue := sha1.New()
	endpointConfigSHValue.Write([]byte(apiName + ":" + apiVersion))
	return hex.EncodeToString(endpointConfigSHValue.Sum(nil)[:])
}

// PopulateEndpointsInfo this will map sandbox and prod endpoint
// This is done to fix the issue https://github.com/wso2/product-microgateway/issues/2288
func PopulateEndpointsInfo(apiYaml APIYaml) APIYaml {
	rawProdEndpoints := apiYaml.Data.EndpointConfig.RawProdEndpoints
	if rawProdEndpoints != nil {
		if val, ok := rawProdEndpoints.(map[string]interface{}); ok {
			jsonString, _ := json.Marshal(val)
			s := EndpointInfo{}
			json.Unmarshal(jsonString, &s)
			apiYaml.Data.EndpointConfig.ProductionEndpoints = []EndpointInfo{s}
		} else if val, ok := rawProdEndpoints.([]interface{}); ok {
			jsonString, _ := json.Marshal(val)
			s := []EndpointInfo{}
			json.Unmarshal(jsonString, &s)
			apiYaml.Data.EndpointConfig.ProductionEndpoints = s
		} else {
			loggers.LoggerAPI.Warn("No production endpoints provided")
		}
	}
	rawSandEndpoints := apiYaml.Data.EndpointConfig.RawSandboxEndpoints
	if rawSandEndpoints != nil {
		if val, ok := rawSandEndpoints.(map[string]interface{}); ok {
			jsonString, _ := json.Marshal(val)
			s := EndpointInfo{}
			json.Unmarshal(jsonString, &s)
			apiYaml.Data.EndpointConfig.SandBoxEndpoints = []EndpointInfo{s}

		} else if val, ok := rawSandEndpoints.([]interface{}); ok {
			jsonString, _ := json.Marshal(val)
			s := []EndpointInfo{}
			json.Unmarshal(jsonString, &s)
			apiYaml.Data.EndpointConfig.SandBoxEndpoints = s
		} else {
			loggers.LoggerAPI.Warn("No sandbox endpoints provided")
		}
	}
	return apiYaml
}
