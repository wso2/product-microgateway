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
	"fmt"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
)

// APIYaml contains everything necessary to extract api.json/api.yaml file
// To support both api.json and api.yaml we convert yaml to json and then use json.Unmarshal()
// Therefore, the params are defined to support json.Unmarshal()
type APIYaml struct {
	Type    string `yaml:"type" json:"type"`
	Version string `yaml:"version" json:"version"`
	Data    struct {
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
		APIThrottlingPolicy        string   `json:"apiThrottlingPolicy,omitempty"`
		IsDefaultVersion           bool     `json:"isDefaultVersion,omitempty"`
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
		Operations []OperationYaml `json:"Operations,omitempty"`
	} `json:"data"`
}

// APIEndpointSecurity represents the structure of endpoint_security param in api.yaml
type APIEndpointSecurity struct {
	Production EndpointSecurity `json:"production,omitempty"`
	Sandbox    EndpointSecurity `json:"sandbox,omitempty"`
}

// EndpointSecurity contains parameters of endpoint security at api.json
type EndpointSecurity struct {
	Password         string            `json:"password,omitempty" mapstructure:"password"`
	Type             string            `json:"type,omitempty" mapstructure:"type"`
	Enabled          bool              `json:"enabled,omitempty" mapstructure:"enabled"`
	Username         string            `json:"username,omitempty" mapstructure:"username"`
	CustomParameters map[string]string `json:"customparameters,omitempty" mapstructure:"customparameters"`
}

// EndpointInfo holds config values regards to the endpoint
type EndpointInfo struct {
	Endpoint string `json:"url,omitempty"`
	Config   struct {
		ActionDuration string `json:"actionDuration,omitempty"`
		RetryTimeOut   string `json:"retryTimeOut,omitempty"`
	} `json:"config,omitempty"`
}

// OperationYaml holds attributes of APIM operations
type OperationYaml struct {
	Target            string            `json:"target,omitempty"`
	Verb              string            `json:"verb,omitempty"`
	OperationPolicies OperationPolicies `json:"operationPolicies,omitempty"`
}

// OperationPolicies holds policies of the APIM operations
type OperationPolicies struct {
	Request  PolicyList `json:"request,omitempty"`
	Response PolicyList `json:"response,omitempty"`
	Fault    PolicyList `json:"fault,omitempty"`
}

// PolicyList holds list of Polices in a flow of operation
type PolicyList []Policy

// Policy holds APIM policies
type Policy struct {
	PolicyName       string      `json:"policyName,omitempty"`
	PolicyVersion    string      `json:"policyVersion,omitempty"`
	Action           string      `json:"-"` // This is a meta value used in CC, not included in API YAML
	IsPassToEnforcer bool        `json:"-"` // This is a meta value used in CC, not included in API YAML
	Parameters       interface{} `json:"parameters,omitempty"`
}

// GetFullName returns the fully qualified name of the policy
// This should be equal to the policy spec/def file name
func (p *Policy) GetFullName() string {
	return fmt.Sprintf("%s_%s", p.PolicyName, p.PolicyVersion)
}

// NewAPIYaml returns an APIYaml struct after reading and validating api.yaml or api.json
func NewAPIYaml(fileContent []byte) (apiYaml APIYaml, err error) {
	apiJsn, err := utills.ToJSON(fileContent)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error occurred converting api file to json: %v", err.Error())
		return apiYaml, err
	}

	err = json.Unmarshal(apiJsn, &apiYaml)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error occurred while parsing api.yaml or api.json %v", err.Error())
		return apiYaml, err
	}

	apiYaml.FormatAndUpdateInfo()
	if apiYaml.Data.EndpointImplementationType != constants.MockedOASEndpointType {
		apiYaml.PopulateEndpointsInfo()
	}
	err = apiYaml.ValidateMandatoryFields()
	if err != nil {
		loggers.LoggerAPI.Errorf("%v", err)
		return apiYaml, err
	}

	if apiYaml.Data.EndpointImplementationType == constants.InlineEndpointType {
		errmsg := "INLINE endpointImplementationType is not supported with Choreo Connect"
		loggers.LoggerAPI.Warnf(errmsg)
		err = errors.New(errmsg)
		return apiYaml, err
	}
	return apiYaml, nil
}

// FormatAndUpdateInfo formats necessary parameters and update from config if null
func (apiYaml *APIYaml) FormatAndUpdateInfo() {
	apiYaml.Data.APIType = strings.ToUpper(apiYaml.Data.APIType)
	apiYaml.Data.LifeCycleStatus = strings.ToUpper(apiYaml.Data.LifeCycleStatus)

	if apiYaml.Data.OrganizationID == "" {
		apiYaml.Data.OrganizationID = config.GetControlPlaneConnectedTenantDomain()
	}
}

// ValidateMandatoryFields check and populates the mandatory fields if null
func (apiYaml *APIYaml) ValidateMandatoryFields() error {
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

	if apiYaml.Data.EndpointImplementationType != constants.MockedOASEndpointType &&
		len(apiYaml.Data.EndpointConfig.ProductionEndpoints) < 1 &&
		len(apiYaml.Data.EndpointConfig.SandBoxEndpoints) < 1 {
		errMsg = errMsg + "API production and sandbox endpoints "
	}

	if errMsg != "" {
		errMsg = errMsg + "fields cannot be empty for " + apiName + " " + apiVersion
		return errors.New(errMsg)
	}

	if apiYaml.Data.EndpointImplementationType != constants.MockedOASEndpointType {
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

// ValidateAPIType checks if the apiProject is properly assigned with the type.
func (apiYaml APIYaml) ValidateAPIType() (err error) {
	apiType := apiYaml.Data.APIType
	if apiType == "" {
		// If no api.yaml file is included in the zip folder, return with error.
		err = errors.New("could not find api.yaml or api.json")
		return err
	} else if apiType != constants.HTTP && apiType != constants.WS {
		errMsg := "The given API type is currently not supported in Choreo Connect. API type: " + apiType
		err = errors.New(errMsg)
		return err
	}
	return nil
}
