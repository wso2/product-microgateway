/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 */

package xds

import (
	"fmt"
	"testing"

	rls_config "github.com/envoyproxy/go-control-plane/ratelimit/config/ratelimit/v3"
	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
)

func TestGetRateLimitUnit(t *testing.T) {
	tests := []struct {
		name         string
		expectsError bool
		unit         rls_config.RateLimitUnit
	}{
		{
			name:         "SECOND",
			expectsError: false,
			unit:         rls_config.RateLimitUnit_SECOND,
		},
		{
			name:         "MINUTE",
			expectsError: false,
			unit:         rls_config.RateLimitUnit_MINUTE,
		},
		{
			name:         "HOUR",
			expectsError: false,
			unit:         rls_config.RateLimitUnit_HOUR,
		},
		{
			name:         "DAY",
			expectsError: false,
			unit:         rls_config.RateLimitUnit_DAY,
		},
		{
			name:         "second",
			expectsError: false,
			unit:         rls_config.RateLimitUnit_SECOND,
		},
		{
			name:         "invalid",
			expectsError: true,
			unit:         rls_config.RateLimitUnit_UNKNOWN,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			unit, err := getRateLimitUnit(test.name)
			assert.Equal(t, test.unit, unit)
			if test.expectsError {
				assert.Error(t, err)
			}
		})
	}
}

func TestAddDeleteAPILevelRateLimitPolicies(t *testing.T) {
	t.Run("Add API level rate limiting", testAddAPILevelRateLimitPolicies)
	t.Run("Delete API level rate limiting", testDeleteAPILevelRateLimitPolicies)
}

func testAddAPILevelRateLimitPolicies(t *testing.T) {
	rateLimitPolicies := map[string]*mgw.APIRateLimitPolicy{
		"5000PerMin":    {PolicyName: "5000PerMin", Count: 5000, SpanUnit: "MINUTE"},
		"2000PerMin":    {PolicyName: "2000PerMin", Count: 2000, SpanUnit: "MINUTE"},
		"100000PerHOUR": {PolicyName: "100000PerHOUR", Count: 100000, SpanUnit: "HOUR"},
		"UNLIMITED":     {PolicyName: "UNLIMITED", Count: -1, SpanUnit: "MINUTE"},
	}

	tests := []struct {
		desc                      string
		apiID                     string
		mgwSwagger                *mgw.MgwSwagger
		policies                  map[string]*mgw.APIRateLimitPolicy
		expectsError              bool
		apiLevelRateLimitPolicies map[string]map[string]map[string][]*rls_config.RateLimitDescriptor
	}{
		{
			desc:                      "Add an API with no Rate Limit policies",
			apiID:                     "vhost1:API1",
			mgwSwagger:                getDummyAPISwagger("1", "", "", "", "", "", ""),
			policies:                  nil,
			expectsError:              false,
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{},
		},
		{
			// Note: Each test case is depend on the earlier test cases
			desc:         "Add an API with API Level Rate Limit Policy",
			apiID:        "vhost1:API2",
			mgwSwagger:   getDummyAPISwagger("2", envoyconf.RateLimitPolicyAPILevel, "5000PerMin", "", "", "", ""),
			policies:     rateLimitPolicies,
			expectsError: false,
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API2": {&rls_config.RateLimitDescriptor{
						Key:   "path",
						Value: "/base-path-2",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "method",
								Value: "ALL",
								RateLimit: &rls_config.RateLimitPolicy{
									Unit:            rls_config.RateLimitUnit_MINUTE,
									RequestsPerUnit: 5000,
								},
							},
						},
					}},
				}},
			},
		},
		{
			// Note: Each test case is depend on the earlier test cases
			desc:         "Add an API with invalid API Level Rate Limit policy",
			apiID:        "vhost1:API3",
			mgwSwagger:   getDummyAPISwagger("3", envoyconf.RateLimitPolicyAPILevel, "6080PerMin", "", "", "", ""),
			policies:     rateLimitPolicies,
			expectsError: true,
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API2": {&rls_config.RateLimitDescriptor{
						Key:   "path",
						Value: "/base-path-2",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "method",
								Value: "ALL",
								RateLimit: &rls_config.RateLimitPolicy{
									Unit:            rls_config.RateLimitUnit_MINUTE,
									RequestsPerUnit: 5000,
								},
							},
						},
					}},
				}},
			},
		},
		{
			// Note: Each test case is depend on the earlier test cases
			desc:         "Add an API with no Rate Limit policies (Unlimited)",
			apiID:        "vhost1:API4",
			mgwSwagger:   getDummyAPISwagger("4", envoyconf.RateLimitDisabled, "UNLIMITED", "", "", "", ""),
			policies:     rateLimitPolicies,
			expectsError: false,
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API2": {&rls_config.RateLimitDescriptor{
						Key:   "path",
						Value: "/base-path-2",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "method",
								Value: "ALL",
								RateLimit: &rls_config.RateLimitPolicy{
									Unit:            rls_config.RateLimitUnit_MINUTE,
									RequestsPerUnit: 5000,
								},
							},
						},
					}},
				}},
			},
		},
		{
			// Note: Each test case is depend on the earlier test cases
			desc:         "Add an API with Operation Level Rate Limit policies",
			apiID:        "vhost1:API5",
			mgwSwagger:   getDummyAPISwagger("5", envoyconf.RateLimitPolicyOperationLevel, "", "100000PerHOUR", "", "", "2000PerMin"),
			policies:     rateLimitPolicies,
			expectsError: false,
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API2": {&rls_config.RateLimitDescriptor{
						Key:   "path",
						Value: "/base-path-2",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "method",
								Value: "ALL",
								RateLimit: &rls_config.RateLimitPolicy{
									Unit:            rls_config.RateLimitUnit_MINUTE,
									RequestsPerUnit: 5000,
								},
							},
						},
					}},
					"vhost1:API5": {
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res1",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "GET",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_HOUR,
										RequestsPerUnit: 100000,
									},
								},
							},
						},
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res2",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "POST",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_MINUTE,
										RequestsPerUnit: 2000,
									},
								},
							},
						},
					},
				}},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.desc, func(t *testing.T) {
			err := rlsPolicyCache.AddAPILevelRateLimitPolicies(test.apiID, test.mgwSwagger, test.policies)
			if test.expectsError {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}
			assert.Equal(t, test.apiLevelRateLimitPolicies, rlsPolicyCache.apiLevelRateLimitPolicies)
		})
	}
}

func testDeleteAPILevelRateLimitPolicies(t *testing.T) {
	tests := []struct {
		desc                      string
		org                       string
		vHost                     string
		apiID                     string
		apiLevelRateLimitPolicies map[string]map[string]map[string][]*rls_config.RateLimitDescriptor
	}{
		{
			desc:  "Delete API with API level rate limits: vhost1:API2",
			org:   "org1",
			vHost: "vhost1",
			apiID: "vhost1:API2",
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API5": {
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res1",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "GET",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_HOUR,
										RequestsPerUnit: 100000,
									},
								},
							},
						},
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res2",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "POST",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_MINUTE,
										RequestsPerUnit: 2000,
									},
								},
							},
						},
					},
				}},
			},
		},
		{
			desc:  "Delete API with no API level rate limits: vhost1:API1",
			org:   "org1",
			vHost: "vhost1",
			apiID: "vhost1:API3",
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API5": {
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res1",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "GET",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_HOUR,
										RequestsPerUnit: 100000,
									},
								},
							},
						},
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res2",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "POST",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_MINUTE,
										RequestsPerUnit: 2000,
									},
								},
							},
						},
					},
				}},
			},
		},
		{
			desc:  "Delete API with operation level rate limits: vhost1:API5",
			org:   "org1",
			vHost: "vhost1",
			apiID: "vhost1:API5",
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {}},
			},
		},
		{
			desc:  "Delete API in an Org that has no APIs with rate limits",
			org:   "org1",
			vHost: "vhost1",
			apiID: "vhost1:API4",
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {}},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.desc, func(t *testing.T) {
			rlsPolicyCache.DeleteAPILevelRateLimitPolicies(test.org, test.vHost, test.apiID)
			assert.Equal(t, test.apiLevelRateLimitPolicies, rlsPolicyCache.apiLevelRateLimitPolicies)
		})
	}
}

func TestGenerateRateLimitConfig(t *testing.T) {
	tests := []struct {
		desc                      string
		orgIDOpenAPIEnvoyMap      map[string]map[string][]string
		apiLevelRateLimitPolicies map[string]map[string]map[string][]*rls_config.RateLimitDescriptor
		rlsConfig                 *rls_config.RateLimitConfig
	}{
		{
			desc: "Test config with multiple labels",
			orgIDOpenAPIEnvoyMap: map[string]map[string][]string{
				"org1": {
					"vhost1:API2": []string{"Default"},
					"vhost1:API3": []string{"Dev"},
					"vhost1:API5": []string{"Default"},
				},
			},
			apiLevelRateLimitPolicies: map[string]map[string]map[string][]*rls_config.RateLimitDescriptor{
				"org1": {"vhost1": {
					"vhost1:API2": {&rls_config.RateLimitDescriptor{
						Key:   "path",
						Value: "/base-path-2",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "method",
								Value: "ALL",
								RateLimit: &rls_config.RateLimitPolicy{
									Unit:            rls_config.RateLimitUnit_MINUTE,
									RequestsPerUnit: 5000,
								},
							},
						},
					}},
					"vhost1:API3": {&rls_config.RateLimitDescriptor{
						Key:   "path",
						Value: "/base-path-2",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "method",
								Value: "ALL",
								RateLimit: &rls_config.RateLimitPolicy{
									Unit:            rls_config.RateLimitUnit_MINUTE,
									RequestsPerUnit: 5000,
								},
							},
						},
					}},
					"vhost1:API5": {
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res1",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "GET",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_HOUR,
										RequestsPerUnit: 100000,
									},
								},
								{
									Key:   "method",
									Value: "POST",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_MINUTE,
										RequestsPerUnit: 1000,
									},
								},
							},
						},
						&rls_config.RateLimitDescriptor{
							Key:   "path",
							Value: "/base-path-5/res2",
							Descriptors: []*rls_config.RateLimitDescriptor{
								{
									Key:   "method",
									Value: "POST",
									RateLimit: &rls_config.RateLimitPolicy{
										Unit:            rls_config.RateLimitUnit_MINUTE,
										RequestsPerUnit: 2000,
									},
								},
							},
						},
					},
				}},
			},
			rlsConfig: &rls_config.RateLimitConfig{
				Name:   "Default",
				Domain: "Default",
				Descriptors: []*rls_config.RateLimitDescriptor{
					{
						Key:   "org",
						Value: "org1",
						Descriptors: []*rls_config.RateLimitDescriptor{
							{
								Key:   "vhost",
								Value: "vhost1",
								Descriptors: []*rls_config.RateLimitDescriptor{
									{
										Key:   "path",
										Value: "/base-path-2",
										Descriptors: []*rls_config.RateLimitDescriptor{
											{
												Key:   "method",
												Value: "ALL",
												RateLimit: &rls_config.RateLimitPolicy{
													Unit:            rls_config.RateLimitUnit_MINUTE,
													RequestsPerUnit: 5000,
												},
											},
										},
									},
									{
										Key:   "path",
										Value: "/base-path-5/res1",
										Descriptors: []*rls_config.RateLimitDescriptor{
											{
												Key:   "method",
												Value: "GET",
												RateLimit: &rls_config.RateLimitPolicy{
													Unit:            rls_config.RateLimitUnit_HOUR,
													RequestsPerUnit: 100000,
												},
											},
											{
												Key:   "method",
												Value: "POST",
												RateLimit: &rls_config.RateLimitPolicy{
													Unit:            rls_config.RateLimitUnit_MINUTE,
													RequestsPerUnit: 1000,
												},
											},
										},
									},
									{
										Key:   "path",
										Value: "/base-path-5/res2",
										Descriptors: []*rls_config.RateLimitDescriptor{
											{
												Key:   "method",
												Value: "POST",
												RateLimit: &rls_config.RateLimitPolicy{
													Unit:            rls_config.RateLimitUnit_MINUTE,
													RequestsPerUnit: 2000,
												},
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.desc, func(t *testing.T) {
			orgIDOpenAPIEnvoyMap = test.orgIDOpenAPIEnvoyMap
			c := &rateLimitPolicyCache{
				apiLevelRateLimitPolicies: test.apiLevelRateLimitPolicies,
			}
			actualConf := c.generateRateLimitConfig("Default")
			// Construct "expected" and "actual" here, since the diff gen by assert is bit difficult to read.
			valuesAsStr := fmt.Sprintf("expected: %v\nactual: %v", test.rlsConfig, actualConf)

			// Test descriptors inside Org1, vHost1 (because the order of the elements can not be guaranteed)
			assert.ElementsMatch(t, test.rlsConfig.Descriptors[0].Descriptors[0].Descriptors,
				actualConf.Descriptors[0].Descriptors[0].Descriptors, valuesAsStr)

			// Test other parts of the config
			test.rlsConfig.Descriptors[0].Descriptors[0] = nil
			actualConf.Descriptors[0].Descriptors[0] = nil
			assert.Equal(t, test.rlsConfig, actualConf)
		})
	}
}

func getDummyAPISwagger(apiID, level, apiPolicy, res1GetPolicy, res1PostPolicy, res2GetPolicy, res2PostPolicy string) *mgw.MgwSwagger {
	// res1 GET
	res1GetOp := mgw.NewOperation("GET", nil, nil)
	res1GetOp.RateLimitPolicy = res1GetPolicy

	// res1 POST
	res1PostOp := mgw.NewOperation("POST", nil, nil)
	res1PostOp.RateLimitPolicy = res1PostPolicy

	// res2 GET
	res2GetOp := mgw.NewOperation("GET", nil, nil)
	res2GetOp.RateLimitPolicy = res2GetPolicy

	// res2 POST
	res2PostOp := mgw.NewOperation("POST", nil, nil)
	res2PostOp.RateLimitPolicy = res2PostPolicy

	res1 := mgw.CreateMinimalDummyResourceForTests("/res1", []*mgw.Operation{res1GetOp, res1PostOp}, "id1", nil, nil)
	res2 := mgw.CreateMinimalDummyResourceForTests("/res2", []*mgw.Operation{res2GetOp, res2PostOp}, "id2", nil, nil)

	mgwSwagger := mgw.CreateDummyMgwSwaggerForTests(fmt.Sprintf("API-%s", apiID), "v1.0.0", fmt.Sprintf("/base-path-%s", apiID), []*mgw.Resource{
		&res1, &res2,
	})
	mgwSwagger.RateLimitLevel = level
	mgwSwagger.RateLimitPolicy = apiPolicy
	mgwSwagger.OrganizationID = "org1"
	mgwSwagger.VHost = "vhost1"
	return mgwSwagger
}
