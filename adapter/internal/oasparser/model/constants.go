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
	urls      string = "urls"
	typeConst string = "type"
)

// Constants for OpenAPI vendor extension keys
const (
	productionEndpoints  string = "x-wso2-production-endpoints"
	sandboxEndpoints     string = "x-wso2-sandbox-endpoints"
	xWso2BasePath        string = "x-wso2-basePath"
	xWso2Label           string = "x-wso2-label"
	xWso2Cors            string = "x-wso2-cors"
	xThrottlingTier      string = "x-throttling-tier"
	xAuthType            string = "x-auth-type"
	xWso2DisableSecurity string = "x-wso2-disable-security"
	None                 string = "None"
)

const (
	// HTTP - API type for http/https APIs
	HTTP string = "HTTP"
	// WS - API type for websocket APIs
	WS string = "WS"
)

// Constants to represent errors
const (
	AlreadyExists string = "ALREADY_EXISTS"
	NotFound      string = "NOT_FOUND"
)
