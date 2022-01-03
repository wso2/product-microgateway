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
	"strings"

	"github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// APIYaml contains everything necessary to extract api.json/api.yaml file
// To support both api.json and api.yaml we convert yaml to json and then use json.Unmarshal()
// Therefore, the params are defined to support json.Unmarshal()
type APIYaml struct {
	ApimMeta
	Data struct {
		ID                         string   `json:"Id,omitempty"`
		Name                       string   `json:"name,omitempty"`
		Context                    string   `json:"context,omitempty"`
		Version                    string   `json:"version,omitempty"`
		RevisionID                 int      `json:"revisionId,omitempty"`
		APIType                    string   `json:"type,omitempty"`
		LifeCycleStatus            string   `json:"lifeCycleStatus,omitempty"`
		EndpointImplementationType string   `json:"endpointImplementationType,omitempty"`
		AuthorizationHeader        string   `json:"authorizationHeader,omitempty"`
		SecurityScheme             []string `json:"securityScheme,omitempty"`
		OrganizationID             string   `json:"organizationId,omitempty"`
		EndpointConfig             struct {
			EndpointType                 string              `json:"endpoint_type,omitempty"`
			LoadBalanceAlgo              string              `json:"algoCombo,omitempty"`
			LoadBalanceSessionManagement string              `json:"sessionManagement,omitempty"`
			LoadBalanceSessionTimeOut    string              `json:"sessionTimeOut,omitempty"`
			APIEndpointSecurity          APIEndpointSecurity `json:"endpoint_security,omitempty"`
			RawProdEndpoints             interface{}         `json:"production_endpoints,omitempty"`
			ProductionEndpoints          []EndpointInfo
			ProductionFailoverEndpoints  []EndpointInfo `json:"production_failovers,omitempty"`
			RawSandboxEndpoints          interface{}    `json:"sandbox_endpoints,omitempty"`
			SandBoxEndpoints             []EndpointInfo
			SandboxFailoverEndpoints     []EndpointInfo `json:"sandbox_failovers,omitempty"`
			ImplementationStatus         string         `json:"implementation_status,omitempty"`
		} `json:"endpointConfig,omitempty"`
	} `json:"data"`
}

// APIEndpointSecurity represents the structure of endpoint_security param in api.yaml
type APIEndpointSecurity struct {
	Production EndpointSecurity `json:"production,omitempty"`
	Sandbox    EndpointSecurity `json:"sandbox,omitempty"`
}

// EndpointInfo holds config values regards to the endpoint
type EndpointInfo struct {
	Endpoint string `json:"url,omitempty"`
	Config   struct {
		ActionDuration string `json:"actionDuration,omitempty"`
		RetryTimeOut   string `json:"retryTimeOut,omitempty"`
	} `json:"config,omitempty"`
}

// VerifyMandatoryFields check and populates the mandatory fields if null
func (apiYaml *APIYaml) VerifyMandatoryFields() error {
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

// PopulateEndpointsInfo this will map sandbox and prod endpoint
// This is done to fix the issue https://github.com/wso2/product-microgateway/issues/2288
func (apiYaml *APIYaml) PopulateEndpointsInfo() {
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
}
