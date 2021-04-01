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

package envoyconf

import (
	"github.com/wso2/micro-gw/internal/oasparser/model"
)

// routeCreateParams is the DTO used to provide information to the envoy route create function
type routeCreateParams struct {
	title             string
	version           string
	apiType           string
	xWSO2BasePath     string
	vHost             string
	endpointBasePath  string
	resourcePathParam string
	resourceMethods   []string
	prodClusterName   string
	sandClusterName   string
	AuthHeader        string
	corsPolicy        *model.CorsConfig
}
