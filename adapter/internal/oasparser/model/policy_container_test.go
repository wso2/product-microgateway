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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPolicySpecificationValidatePolicy(t *testing.T) {
	spec := getSampleTestPolicySpec()

	tests := []struct {
		policy     Policy
		flow       PolicyFlow
		isExpError bool
		message    string
	}{
		{
			policy: Policy{
				PolicyName:    "fooAddRequestHeader",
				PolicyVersion: "v1",
				Parameters:    map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyInFlow,
			isExpError: false,
			message:    "Valid policy should not return error",
		},
		{
			policy: Policy{
				PolicyName:    "fooAddRequestHeader",
				PolicyVersion: "v1",
				Parameters:    map[string]interface{}{"fooName": "$%invalid name%$", "fooValue": "admin"},
			},
			flow:       policyInFlow,
			isExpError: true,
			message:    "Invalid value for policy parameter should return error",
		},
		{
			policy: Policy{
				PolicyName:    "fooAddRequestHeader",
				PolicyVersion: "v1",
				Parameters:    map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyOutFlow,
			isExpError: true,
			message:    "Invalid policy flow should return error",
		},
		{
			policy: Policy{
				PolicyName:    "invalidName",
				PolicyVersion: "v1",
				Parameters:    map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyInFlow,
			isExpError: true,
			message:    "Invalid policy name should return error",
		},
		{
			policy: Policy{
				PolicyName:    "fooAddRequestHeader",
				PolicyVersion: "v1",
				Parameters:    map[string]interface{}{"fooValue": "admin"},
			},
			flow:       policyInFlow,
			isExpError: true,
			message:    "Required parameter not found, should return error",
		},
	}

	for _, test := range tests {
		err := spec.validatePolicy(test.policy, test.flow)
		if test.isExpError {
			assert.Error(t, err, test.message)
		} else {
			assert.True(t, err == nil, test.message+", err: %v", err)
		}
	}
}

func TestAPIProjectGetFormattedPolicyFromTemplated(t *testing.T) {
	apiYaml := APIYaml{}
	apiYaml.Data.Operations = []OperationYaml{
		{
			Target: "/pets",
			Verb:   "POST",
			OperationPolicies: OperationPolicies{
				Request: PolicyList{
					{
						PolicyName:    "fooAddRequestHeader",
						PolicyVersion: "v1",
						Parameters: map[string]interface{}{
							"fooName":  "fooHeaderName",
							"fooValue": "fooHeaderValue",
						},
					},
				},
			},
		},
	}

	spec := getSampleTestPolicySpec()
	specInvalid1 := getSampleTestPolicySpec()
	specInvalid1.Data.Name = "fooAddRequestHeaderInvalid1"
	specInvalid2 := getSampleTestPolicySpec()
	specInvalid2.Data.Name = "fooAddRequestHeaderInvalid2"

	proj := ProjectAPI{
		APIYaml: apiYaml,
		Policies: map[string]PolicyContainer{
			"fooAddRequestHeader_v1": {
				Specification: spec,
				Definition: PolicyDefinition{
					RawData: getSampleTestPolicyDef(),
				},
			},
			"fooAddRequestHeaderInvalid1_v1": {
				Specification: specInvalid1,
				Definition: PolicyDefinition{
					RawData: getSampleInvalidTestPolicyDef1(),
				},
			},
			"fooAddRequestHeaderInvalid2_v1": {
				Specification: specInvalid2,
				Definition: PolicyDefinition{
					RawData: getSampleInvalidTestPolicyDef2(),
				},
			},
		},
	}

	expFormattedP := OperationPolicies{
		Request: PolicyList{
			{
				PolicyName:       "fooAddRequestHeader",
				PolicyVersion:    "v1",
				Action:           "SET_HEADER",
				IsPassToEnforcer: true,
				Parameters: map[string]interface{}{
					"headerName":  "fooHeaderName",
					"headerValue": "fooHeaderValue",
				},
			},
		},
	}
	actualFormattedP := proj.Policies.GetFormattedOperationalPolicies(apiYaml.Data.Operations[0].OperationPolicies, &MgwSwagger{})
	assert.Equal(t, expFormattedP, actualFormattedP, "Converting operational policies to Choreo Connect format failed")
}

func getSampleTestPolicySpec() PolicySpecification {
	spec := PolicySpecification{}
	spec.Data.Name = "fooAddRequestHeader"
	spec.Data.Version = "v1"
	spec.Data.ApplicableFlows = []string{"request"}
	spec.Data.SupportedGateways = []string{"ChoreoConnect"}
	spec.Data.PolicyAttributes = []struct { // redefine struct here, since it is not named, update here if the src changed
		Name            string `yaml:"name"`
		ValidationRegex string `yaml:"validationRegex,omitempty"`
		Type            string `yaml:"type"`
		DefaultValue    string `yaml:"defaultValue"`
		Required        bool   `yaml:"required,omitempty"`
	}{
		{
			Name:            "fooName",
			ValidationRegex: `^([a-zA-Z_][a-zA-Z\\d_\\-\\ ]*)$`,
			Type:            "String",
			Required:        true,
		},
		{
			Name:            "fooValue",
			ValidationRegex: `.*`,
			Type:            "String",
			Required:        true,
		},
		{
			Name:            "fooNotRequired",
			ValidationRegex: `^\S+$`,
			Type:            "String",
			Required:        false,
		},
	}
	return spec
}

func getSampleTestPolicyDef() []byte {
	return []byte(`
definition:
  action: SET_HEADER
  parameters:
    headerName: {{ .fooName }}
    headerValue: {{ .fooValue }}
`)
}

func getSampleInvalidTestPolicyDef1() []byte {
	return []byte(`
definition:
  action: SET_HEADER_INVALID_ACTION
  parameters:
    headerName: {{ .fooName }}
    headerValue: {{ .fooValue }}
`)
}

func getSampleInvalidTestPolicyDef2() []byte {
	return []byte(`
definition:
  action: SET_HEADER
  parameters:
    headerNameInvalidParam: {{ .fooName }}
    headerValue: {{ .fooValue }}
`)
}
