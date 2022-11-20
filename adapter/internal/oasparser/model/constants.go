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
	SecurityConfig        string = "securityConfig"
)

// Constants for OpenAPI vendor extension keys
const (
	xWso2ProdEndpoints   string = "x-wso2-production-endpoints"
	xWso2SandbxEndpoints string = "x-wso2-sandbox-endpoints"
	xWso2endpoints       string = "x-wso2-endpoints"
	xWso2BasePath        string = "x-wso2-basePath"
	xWso2Label           string = "x-wso2-label"
	xWso2Cors            string = "x-wso2-cors"
	// xThrottlingTier      string = "x-throttling-tier"
	// xWso2ThrottlingTier  string = "x-wso2-throttling-tier"
	xAuthHeader          string = "x-wso2-auth-header"
	xAuthType            string = "x-auth-type"
	xWso2DisableSecurity string = "x-wso2-disable-security"
	None                 string = "None"
	DefaultSecurity      string = "default"
)

// cluster name prefixes
const (
	sandClustersConfigNamePrefix    string = "clusterSand"
	prodClustersConfigNamePrefix    string = "clusterProd"
	xWso2EPClustersConfigNamePrefix string = "xwso2cluster"
)

// sub-property values and keys relevant for x-wso2-application security extension
const (
	APIMAPIKeyType            string = "api_key"
	APIKeyNameWithApim        string = "apikey"
	APIKeyTypeInOAS           string = "apiKey"
	APIMOauth2Type            string = "oauth2"
	APIMDefaultOauth2Security string = "default"
	APIKeyInHeaderOAS         string = "header"
	APIKeyInQueryOAS          string = "query"
	APIMAPIKeyInHeader        string = "api_key_header"
	APIMAPIKeyInQuery         string = "api_key_query"
)

// sub-property keys mentioned under x-wso2-request-interceptor and x-wso2-response-interceptor
const (
	serviceURL                string = "serviceURL"
	clusterTimeout            string = "clusterTimeout"
	requestTimeout            string = "requestTimeout"
	includes                  string = "includes"
	OperationLevelInterceptor string = "operation"
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
