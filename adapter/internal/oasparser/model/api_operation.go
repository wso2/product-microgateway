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

// Package model contains the implementation of DTOs to convert OpenAPI/Swagger files
// and create a common model which can represent both types.
package model

import (
	"strings"

	"github.com/google/uuid"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/interceptor"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
)

// Operation type object holds data about each http method in the REST API.
type Operation struct {
	iD               string
	method           string
	security         []map[string][]string
	tier             string
	disableSecurity  bool
	vendorExtensions map[string]interface{}
	policies         OperationPolicies
	mockedAPIConfig  MockedAPIConfig    
}

// MockedAPIConfig holds configurations defined for a mocked API operation result
type MockedAPIConfig struct {
	In          string                    `json:"in,omitempty"`
	Name        string                    `json:"name,omitempty"`
	Responses   []MockedResponseConfig    `json:"responses,omitempty"`
}

// MockedResponseConfig holds response configurations in the mocked API operation
type MockedResponseConfig struct {
	Value 	string                        `json:"value,omitempty"`
	Headers []MockedHeaderConfig          `json:"headers,omitempty"`
	Code	int                           `json:"code,omitempty"`	
	Payload MockedPayloadConfig           `json:"payload,omitempty"`
}

// MockedHeaderConfig holds header configurations in the mocked API operation
type MockedHeaderConfig struct {
	Name  string                          `json:"name,omitempty"`
	Value string                          `json:"value,omitempty"`
}

// MockedPayloadConfig holds mocked payload configurations in the mocked API operation
type MockedPayloadConfig struct {
	ApplicationJSON string                `json:"application/json,omitempty"`
	ApplicationXML 	string                `json:"application/xml,omitempty"`
}

// GetMethod returns the http method name of the give API operation
func (operation *Operation) GetMethod() string {
	return operation.method
}

// GetDisableSecurity returns if the resouce is secured.
func (operation *Operation) GetDisableSecurity() bool {
	return operation.disableSecurity
}

// GetPolicies returns if the resouce is secured.
func (operation *Operation) GetPolicies() *OperationPolicies {
	return &operation.policies
}

// GetSecurity returns the security schemas defined for the http opeartion
func (operation *Operation) GetSecurity() []map[string][]string {
	return operation.security
}

// SetSecurity sets the security schemas for the http opeartion
func (operation *Operation) SetSecurity(security []map[string][]string) {
	operation.security = security
}

// GetTier returns the operation level throttling tier
func (operation *Operation) GetTier() string {
	return operation.tier
}

// GetMockedAPIConfig returns the operation level mocked API implementation configs
func (operation *Operation) GetMockedAPIConfig() MockedAPIConfig {
	return operation.mockedAPIConfig
}

// GetVendorExtensions returns vendor extensions which are explicitly defined under
// a given resource.
func (operation *Operation) GetVendorExtensions() map[string]interface{} {
	return operation.vendorExtensions
}

// GetID returns the id of a given resource.
// This is a randomly generated UUID
func (operation *Operation) GetID() string {
	return operation.iD
}

// GetCallInterceptorService returns the interceptor configs for a given operation.
func (operation *Operation) GetCallInterceptorService(isIn bool) InterceptEndpoint {
	var policies []Policy
	if isIn {
		policies = operation.policies.In
	} else {
		policies = operation.policies.Out
	}
	if len(policies) > 0 {
		for _, policy := range policies {
			if strings.EqualFold(constants.InterceptorServiceTemplate, policy.TemplateName) {
				if paramMap, isMap := policy.Parameters.(map[string]interface{}); isMap {
					urlValue, urlFound := paramMap[constants.InterceptorServiceURL]
					includesValue, includesFound := paramMap[constants.InterceptorServiceIncludes]
					if urlFound {
						url, isString := urlValue.(string)
						if isString && url != "" {
							endpoint, err := getHostandBasepathandPort(url)
							if err == nil {
								conf, _ := config.ReadConfigs()
								clusterTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
								requestTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
								includesV := &interceptor.RequestInclusions{}
								if includesFound {
									includes, isList := includesValue.([]interface{})
									if isList && len(includes) > 0 {
										includesV = GenerateInterceptorIncludes(includes)
									}
								}
								if err == nil {
									return InterceptEndpoint{
										Enable:          true,
										EndpointCluster: EndpointCluster{Endpoints: []Endpoint{*endpoint}},
										ClusterTimeout:  clusterTimeoutV,
										RequestTimeout:  requestTimeoutV,
										Includes:        includesV,
										Level:           constants.OperationLevelInterceptor,
									}
								}
							}
						}
					}
				}
			}
		}
	}
	return InterceptEndpoint{}
}

// NewOperation Creates and returns operation type object
func NewOperation(method string, security []map[string][]string, extensions map[string]interface{}, 
	mockedAPIConfig MockedAPIConfig) *Operation {
	tier := ResolveThrottlingTier(extensions)
	disableSecurity := ResolveDisableSecurity(extensions)
	id := uuid.New().String()
	return &Operation{id, method, security, tier, disableSecurity, extensions, OperationPolicies{}, mockedAPIConfig}
}
