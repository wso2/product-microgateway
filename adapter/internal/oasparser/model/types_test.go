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

func TestPolicyListGetStats(t *testing.T) {
	pl := PolicyList{{PolicyName: "addHeader"}, {PolicyName: "rewriteMethod"}, {PolicyName: "addHeader"}}
	expSt := map[string]policyStats{
		"addHeader":     {firstIndex: 0, count: 2},
		"rewriteMethod": {firstIndex: 1, count: 1},
	}
	st := pl.getStats()
	assert.Equal(t, expSt, st)
}

func TestPolicySpecificationValidatePolicy(t *testing.T) {
	spec := getSampleTestPolicySpec()

	tests := []struct {
		policy     Policy
		flow       PolicyFlow
		stats      map[string]policyStats
		pIndex     int
		isExpError bool
		message    string
	}{
		{
			policy: Policy{
				PolicyName: "fooAddRequestHeader",
				Parameters: map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyInFlow,
			stats:      map[string]policyStats{"fooAddRequestHeader": {firstIndex: 3, count: 2}},
			pIndex:     3,
			isExpError: false,
			message:    "Valid policy should not return error",
		},
		{
			policy: Policy{
				PolicyName: "fooAddRequestHeader",
				Parameters: map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyOutFlow,
			stats:      map[string]policyStats{"fooAddRequestHeader": {firstIndex: 3, count: 2}},
			pIndex:     3,
			isExpError: true,
			message:    "Invalid policy flow should return error",
		},
		{
			policy: Policy{
				PolicyName: "invalidName",
				Parameters: map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyInFlow,
			stats:      map[string]policyStats{"fooAddRequestHeader": {firstIndex: 3, count: 2}},
			pIndex:     3,
			isExpError: true,
			message:    "Invalid policy name should return error",
		},
		{
			policy: Policy{
				PolicyName: "fooAddRequestHeader",
				Parameters: map[string]interface{}{"fooValue": "admin"},
			},
			flow:       policyInFlow,
			stats:      map[string]policyStats{"fooAddRequestHeader": {firstIndex: 3, count: 2}},
			pIndex:     3,
			isExpError: true,
			message:    "Required parameter not found, should return error",
		},
		{
			policy: Policy{
				PolicyName: "fooAddRequestHeader",
				Parameters: map[string]interface{}{"fooName": 2, "fooValue": "admin"},
			},
			flow:       policyInFlow,
			stats:      map[string]policyStats{"fooAddRequestHeader": {firstIndex: 3, count: 2}},
			pIndex:     3,
			isExpError: true,
			message:    "Invalid value type in fooName",
		},
		{
			policy: Policy{
				PolicyName: "fooAddRequestHeader",
				Parameters: map[string]interface{}{"fooName": "user", "fooValue": "admin"},
			},
			flow:       policyInFlow,
			stats:      map[string]policyStats{"fooAddRequestHeader": {firstIndex: 3, count: 2}},
			pIndex:     5,
			isExpError: true,
			message:    "Multiple not allowed and not the first policy in the list, should return error",
		},
	}

	for _, test := range tests {
		err := spec.validatePolicy(test.policy, test.flow, test.stats, test.pIndex)
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
				In: PolicyList{
					{
						PolicyName: "fooAddRequestHeader",
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
	proj := ProjectAPI{
		APIYaml: apiYaml,
		Policies: map[string]PolicyContainer{
			"fooAddRequestHeader": {
				Specification: spec,
				Definition: PolicyDefinition{
					RawData: getSampleTestPolicyDef(),
				},
			},
		},
	}

	expFormattedP := OperationPolicies{
		In: PolicyList{
			{
				PolicyName: "fooAddRequestHeader",
				Action:     "SET_HEADER",
				Parameters: map[string]interface{}{
					"headerName":  "fooHeaderName",
					"headerValue": "fooHeaderValue",
				},
			},
		},
	}
	actualFormattedP := proj.getFormattedOperationalPolicies(apiYaml.Data.Operations[0].OperationPolicies)
	assert.Equal(t, expFormattedP, actualFormattedP, "Converting operational policies to Choreo Connect format failed")
}

func getSampleTestPolicySpec() PolicySpecification {
	spec := PolicySpecification{}
	spec.Data.Name = "fooAddRequestHeader"
	spec.Data.ApplicableFlows = []string{"request"}
	spec.Data.SupportedGateways = []string{"CC"}
	spec.Data.MultipleAllowed = false
	spec.Data.PolicyAttributes = []struct { // redefine struct here, since it is not named, update here if the src changed
		Name            string `yaml:"name"`
		ValidationRegex string `yaml:"validationRegex,omitempty"`
		Type            string `yaml:"type"`
		Required        bool   `yaml:"required,omitempty"`
	}{
		{
			Name:            "fooName",
			ValidationRegex: `/^\S+$/`,
			Type:            "String",
			Required:        true,
		},
		{
			Name:            "fooValue",
			ValidationRegex: `/^\S+$/`,
			Type:            "String",
			Required:        true,
		},
		{
			Name:            "fooNotRequired",
			ValidationRegex: `/^\S+$/`,
			Type:            "String",
			Required:        false,
		},
	}
	return spec
}

func getSampleTestPolicyDef() []byte {
	return []byte(`
type: operation_policy_definition
version: v4.1.0
data:
  action: SET_HEADER
  parameters:
    headerName: {{ .fooName }}
    headerValue: {{ .fooValue }}
`)
}
