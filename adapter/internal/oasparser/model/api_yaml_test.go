/*
 *  Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package model_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
)

func TestExtractAPIRateLimitPolicies(t *testing.T) {
	apiProjectWithOperationLimit := &model.ProjectAPI{}
	apiProjectWithOperationLimit.APIYaml.Data.Operations = []model.OperationYaml{
		{
			Verb:   "GET",
			Target: "/pet/{petId}",
			ThrottlingLimit: model.ThrottlingLimit{
				RequestCount: 10,
				Unit:         "MINUTE",
			},
		},
	}
	model.ExtractAPIRateLimitPolicies(apiProjectWithOperationLimit)
	policyName := model.GetRLPolicyName(
		apiProjectWithOperationLimit.APIYaml.Data.Operations[0].ThrottlingLimit.RequestCount,
		apiProjectWithOperationLimit.APIYaml.Data.Operations[0].ThrottlingLimit.Unit)
	assert.Equal(t, "10PerMinute", policyName)
	assert.Equal(t, 1, len(apiProjectWithOperationLimit.RateLimitPolicies))
	assert.Equal(t, policyName, apiProjectWithOperationLimit.RateLimitPolicies[policyName].PolicyName)
	assert.Equal(t, "REQUEST_COUNT", apiProjectWithOperationLimit.RateLimitPolicies[policyName].Type)
	assert.Equal(t, uint32(10), apiProjectWithOperationLimit.RateLimitPolicies[policyName].Count)
	assert.Equal(t, "MINUTE", apiProjectWithOperationLimit.RateLimitPolicies[policyName].SpanUnit)
	assert.Equal(t, uint32(1), apiProjectWithOperationLimit.RateLimitPolicies[policyName].Span)

	apiProjectWithoutOperationLimit := &model.ProjectAPI{}
	apiProjectWithoutOperationLimit.APIYaml.Data.Operations = []model.OperationYaml{
		{
			Verb:   "GET",
			Target: "/pet/{petId}",
		},
	}
	model.ExtractAPIRateLimitPolicies(apiProjectWithoutOperationLimit)
	assert.Equal(t, 0, len(apiProjectWithoutOperationLimit.RateLimitPolicies))

	apiProjectWithAPILimit := &model.ProjectAPI{}
	apiProjectWithAPILimit.APIYaml.Data.ThrottlingLimit = model.ThrottlingLimit{
		RequestCount: 10,
		Unit:         "MINUTE",
	}
	apiProjectWithAPILimit.APIYaml.Data.Operations = []model.OperationYaml{
		{
			Verb:   "GET",
			Target: "/pet/{petId}",
			ThrottlingLimit: model.ThrottlingLimit{
				RequestCount: 5,
				Unit:         "HOUR",
			},
		},
		{
			Verb:   "POST",
			Target: "/pet/{petId}",
			ThrottlingLimit: model.ThrottlingLimit{
				RequestCount: 2,
				Unit:         "HOUR",
			},
		},
	}
	model.ExtractAPIRateLimitPolicies(apiProjectWithAPILimit)
	apiPolicyName := model.GetRLPolicyName(
		apiProjectWithOperationLimit.APIYaml.Data.Operations[0].ThrottlingLimit.RequestCount,
		apiProjectWithOperationLimit.APIYaml.Data.Operations[0].ThrottlingLimit.Unit)
	assert.Equal(t, "10PerMinute", apiPolicyName)
	assert.Equal(t, 1, len(apiProjectWithOperationLimit.RateLimitPolicies))
	assert.Equal(t, policyName, apiProjectWithOperationLimit.RateLimitPolicies[apiPolicyName].PolicyName)
	assert.Equal(t, "REQUEST_COUNT", apiProjectWithOperationLimit.RateLimitPolicies[apiPolicyName].Type)
	assert.Equal(t, uint32(10), apiProjectWithOperationLimit.RateLimitPolicies[apiPolicyName].Count)
	assert.Equal(t, "MINUTE", apiProjectWithOperationLimit.RateLimitPolicies[apiPolicyName].SpanUnit)
	assert.Equal(t, uint32(1), apiProjectWithOperationLimit.RateLimitPolicies[apiPolicyName].Span)
}
