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

package constants

// sub-property keys mentioned under x-wso2-production-endpoints
const (
	Urls                  string = "urls"
	Type                  string = "type"
	LoadBalance           string = "load_balance"
	FailOver              string = "failover"
	AdvanceEndpointConfig string = "advanceEndpointConfig"
	SecurityConfig        string = "securityConfig"
)

// Constants for OpenAPI vendor extension keys
const (
	XWso2ProdEndpoints   string = "x-wso2-production-endpoints"
	XWso2SandbxEndpoints string = "x-wso2-sandbox-endpoints"
	XWso2endpoints       string = "x-wso2-endpoints"
	XWso2BasePath        string = "x-wso2-basePath"
	XWso2Label           string = "x-wso2-label"
	XWso2Cors            string = "x-wso2-cors"
	XThrottlingTier      string = "x-throttling-tier"
	XWso2ThrottlingTier  string = "x-wso2-throttling-tier"
	XAuthHeader          string = "x-wso2-auth-header"
	XAuthType            string = "x-auth-type"
	XWso2DisableSecurity string = "x-wso2-disable-security"
	None                 string = "None"
	DefaultSecurity      string = "default"
)

// cluster name prefixes
const (
	SandClustersConfigNamePrefix    string = "clusterSand"
	ProdClustersConfigNamePrefix    string = "clusterProd"
	XWso2EPClustersConfigNamePrefix string = "xwso2cluster"
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
	ServiceURL                string = "serviceURL"
	ClusterTimeout            string = "clusterTimeout"
	RequestTimeout            string = "requestTimeout"
	Includes                  string = "includes"
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
