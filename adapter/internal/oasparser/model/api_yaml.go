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
	"errors"
	"os"
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

	if apiYaml.Data.EndpointConfig.ProductionEndpoints.Endpoint == "" &&
		apiYaml.Data.EndpointConfig.SandBoxEndpoints.Endpoint == "" {
		errMsg = errMsg + "API production and sandbox endpoints "
	}

	if errMsg != "" {
		errMsg = errMsg + "fields cannot be empty for " + apiName + " " + apiVersion
		return errors.New(errMsg)
	}

	if strings.HasPrefix(apiYaml.Data.EndpointConfig.ProductionEndpoints.Endpoint, "/") ||
		strings.HasPrefix(apiYaml.Data.EndpointConfig.SandBoxEndpoints.Endpoint, "/") {
		errMsg = "Relative urls are not supported for API production and sandbox endpoints"
		return errors.New(errMsg)
	}
	return nil
}

// ExtractAPIInformation reads the values in api.yaml/api.json and populates ProjectAPI struct
func ExtractAPIInformation(apiProject *ProjectAPI, apiYaml APIYaml) {
	apiProject.APIType = strings.ToUpper(apiYaml.Data.APIType)
	apiProject.APILifeCycleStatus = strings.ToUpper(apiYaml.Data.LifeCycleStatus)

	var apiHashValue string = generateHashValue(apiYaml.Data.Name, apiYaml.Data.Version)

	endpointConfig := apiYaml.Data.EndpointConfig

	// production Endpoints set
	var productionEndpoint string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_prod_endpoint_0",
		endpointConfig.ProductionEndpoints.Endpoint)
	apiProject.ProductionEndpoint = productionEndpoint

	// sandbox Endpoints set
	var sandboxEndpoint string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_sand_endpoint_0",
		endpointConfig.SandBoxEndpoints.Endpoint)
	apiProject.SandboxEndpoint = sandboxEndpoint

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
