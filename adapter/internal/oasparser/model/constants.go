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

// sub-property keys mentioned under x-wso2-production-endpoints
const (
	urls                  string = "urls"
	typeConst             string = "type"
	LoadBalance           string = "loadbalance"
	FailOver              string = "failover"
	AdvanceEndpointConfig string = "advanceEndpointConfig"
)

// Constants for OpenAPI vendor extension keys
const (
	xWso2ProdEndpoints       string = "x-wso2-production-endpoints"
	xWso2SandbxEndpoints     string = "x-wso2-sandbox-endpoints"
	xWso2endpoints           string = "x-wso2-endpoints"
	xWso2BasePath            string = "x-wso2-basePath"
	xWso2Label               string = "x-wso2-label"
	xWso2Cors                string = "x-wso2-cors"
	xThrottlingTier          string = "x-throttling-tier"
	xWso2ThrottlingTier      string = "x-wso2-throttling-tier"
	xAuthHeader              string = "x-wso2-auth-header"
	xAuthType                string = "x-auth-type"
	xWso2DisableSecurity     string = "x-wso2-disable-security"
	xWso2ApplicationSecurity string = "x-wso2-application-security"
	None                     string = "None"
	DefaultSecurity          string = "default"
)

// sub-property values and keys relevant for x-wso2-application security extension
const (
	Optional                 string = "optional"
	SecurityTypes            string = "security-types"
	APIKeyInAppLevelSecurity string = "api_key"
	APIKeyTypeInOAS          string = "apiKey"
)

// sub-property keys mentioned under x-wso2-request-interceptor and x-wso2-response-interceptor
const (
	serviceURL     string = "serviceURL"
	clusterTimeout string = "clusterTimeout"
	requestTimeout string = "requestTimeout"
	includes       string = "includes"
)

const (
	// HTTP - API type for http/https APIs
	HTTP string = "HTTP"
	// WS - API type for websocket APIs
	WS string = "WS"
	// WEBHOOK - API type for WEBHOOK APIs
	WEBHOOK string = "WEBHOOK"
)

// Constants to represent errors
const (
	AlreadyExists string = "ALREADY_EXISTS"
	NotFound      string = "NOT_FOUND"
)
