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
	"strconv"
	"strings"

	"github.com/wso2/product-microgateway/adapter/internal/loggers"
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
	// organization ID would remain empty string if unassigned
	apiProject.OrganizationID = apiYaml.Data.OrganizationID
}

// ExtractAPIRateLimitPolicies reads the values in api.yaml/api.json and populates and identifies the
// available rate-limit policies
func ExtractAPIRateLimitPolicies(apiProject *ProjectAPI, parsedAPIYaml APIYaml) {
	apiYamlFromProject := apiProject.APIYaml.Data

	policyMap := map[string]*APIRateLimitPolicy{}
	// revise the if condition
	if apiYamlFromProject.APIThrottlingPolicy != "" {
		throttlingLimit := apiYamlFromProject.ThrottlingLimit
		rlPolicy := getRateLimitPolicy(throttlingLimit)
		policyMap[rlPolicy.PolicyName] = &rlPolicy
		apiProject.RateLimitPolicies = policyMap
		loggers.LoggerAPI.Debugf("API level Rate Limit policies processed")
		return
	}
	for _, operation := range apiYamlFromProject.Operations {
		policyName := GetRLPolicyName(operation.ThrottlingLimit.RequestCount, operation.ThrottlingLimit.Unit)
		_, ok := policyMap[policyName]
		if !ok {
			throttlingLimit := operation.ThrottlingLimit
			rlPolicy := getRateLimitPolicy(throttlingLimit)
			policyMap[rlPolicy.PolicyName] = &rlPolicy
		}
	}
	loggers.LoggerAPI.Debugf("Number of Rate Limit policies received: %v", len(policyMap))
	apiProject.RateLimitPolicies = policyMap
	return
}

func getRateLimitPolicy(throttlingLimit ThrottlingLimit) APIRateLimitPolicy {
	var rlPolicy APIRateLimitPolicy
	policyName := GetRLPolicyName(throttlingLimit.RequestCount, throttlingLimit.Unit)
	var rlType string = "REQUEST_COUNT"
	rlPolicy.PolicyName = policyName
	rlPolicy.Count = uint32(throttlingLimit.RequestCount)
	rlPolicy.Type = rlType
	rlPolicy.Span = 1
	rlPolicy.SpanUnit = throttlingLimit.Unit
	return rlPolicy
}

// GetRLPolicyName from throttlingLimit fields
func GetRLPolicyName(requestCount int, unit string) string {
	return strconv.Itoa(requestCount) + "Per" + strings.Title(strings.ToLower(unit))
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
