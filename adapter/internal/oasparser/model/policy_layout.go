/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"errors"
	"fmt"

	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
)

// supportedPoliciesMap maps (policy action name) -> (policy layout)
var supportedPoliciesMap = map[string]policyLayout{
	"SET_HEADER": {
		RequiredParams:   []string{"headerName", "headerValue"},
		IsPassToEnforcer: true,
	},
	"REMOVE_HEADER": {
		RequiredParams:   []string{"headerName"},
		IsPassToEnforcer: true,
	},
	"ADD_QUERY": {
		RequiredParams:   []string{"queryParamName", "queryParamValue"},
		IsPassToEnforcer: true,
	},
	constants.InterceptorServiceAction: {
		RequiredParams:   []string{constants.InterceptorServiceURL, constants.InterceptorServiceIncludes},
		IsPassToEnforcer: false,
	},
	constants.RewriteMethodAction: {
		RequiredParams:   []string{"currentMethod", "updatedMethod"},
		IsPassToEnforcer: true,
	},
	constants.RewritePathAction: {
		RequiredParams:   []string{constants.RewritePathResourcePath, constants.IncludeQueryParams},
		IsPassToEnforcer: true,
	},
	"OPA": {
		// Following parameters are not required (optional)
		// "rule", token", "additionalProperties", "sendAccessToken", "maxOpenConnections", "maxPerRoute"
		// "connectionTimeout", "requestGenerator"
		RequiredParams:   []string{"serverURL", "policy"},
		IsPassToEnforcer: true,
	},
}

// PolicyLayout holds the layout of policy that support by Choreo Connect
type policyLayout struct {
	RequiredParams   []string
	IsPassToEnforcer bool
}

// validatePolicyAction validates policy against the policy definition that supported by Choreo Connect
func validatePolicyAction(policy *Policy) error {
	if layout, ok := supportedPoliciesMap[policy.Action]; ok {
		for _, requiredParam := range layout.RequiredParams {
			if params, isMap := policy.Parameters.(map[string]interface{}); isMap {
				if _, ok := params[requiredParam]; !ok {
					return fmt.Errorf("required parameter %q not found for the policy action %q", requiredParam, policy.Action)
				}
			} else {
				return errors.New("policy params required in map format")
			}
		}
		policy.IsPassToEnforcer = layout.IsPassToEnforcer
	} else {
		return fmt.Errorf("policy action %q not supported by Choreo Connect gateway", policy.Action)
	}
	return nil
}
