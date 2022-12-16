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
	"context"
	"fmt"
	"math/rand"
	"strings"

	gcp_types "github.com/envoyproxy/go-control-plane/pkg/cache/types"
	gcp_cache "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	gcp_resource "github.com/envoyproxy/go-control-plane/pkg/resource/v3"
	rls_config "github.com/envoyproxy/go-control-plane/ratelimit/config/ratelimit/v3"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
)

var rlsPolicyCache *rateLimitPolicyCache

var rlUnits = rateLimitUnits{
	"SECOND": rls_config.RateLimitUnit_SECOND,
	"MINUTE": rls_config.RateLimitUnit_MINUTE,
	"HOUR":   rls_config.RateLimitUnit_HOUR,
	"DAY":    rls_config.RateLimitUnit_DAY,
}

type rateLimitUnits map[string]rls_config.RateLimitUnit

func (r rateLimitUnits) get(name string) (rls_config.RateLimitUnit, error) {
	n := strings.ToUpper(name)
	if unit, ok := r[n]; ok {
		return unit, nil
	}
	return rls_config.RateLimitUnit_UNKNOWN, fmt.Errorf("Invalid rate limit unit %q", name)
}

type rateLimitPolicyCache struct {
	// xdsCache is the snapshot cache for the
	xdsCache gcp_cache.SnapshotCache

	// org -> vhost -> API-Identifier (i.e. Vhost:API-UUID) -> Rate Limit Configs
	apiLevelRateLimitPolicies map[string]map[string]map[string][]*rls_config.RateLimitDescriptor
}

// AddAPILevelRateLimitPolicies adds inline Rate Limit policies in APIs to be updated in the Rate Limiter service.
func (r *rateLimitPolicyCache) AddAPILevelRateLimitPolicies(apiID string, mgwSwagger *mgw.MgwSwagger, policies map[string]*mgw.APIRateLimitPolicy) error {
	if mgwSwagger.RateLimitLevel == "" {
		return nil
	}
	level := strings.ToUpper(mgwSwagger.RateLimitLevel)

	var rlsConfigs []*rls_config.RateLimitDescriptor
	if level == mgw.RateLimitPolicyAPILevel {
		rlPolicyConfig, err := getRateLimitPolicy(policies, mgwSwagger.RateLimitPolicy)
		if err != nil {
			loggers.LoggerXds.Errorf("Error generating rate limit configuration for API: %q", apiID)
			return err
		}

		rlsConfigs = []*rls_config.RateLimitDescriptor{
			{
				Key:   "path",
				Value: mgwSwagger.GetXWso2Basepath(),
				Descriptors: []*rls_config.RateLimitDescriptor{
					{
						Key:       "method",
						Value:     "ALL",
						RateLimit: rlPolicyConfig,
					},
				},
			},
		}
	} else if level == mgw.RateLimitPolicyOperationLevel {
		// The map apiOperations is used to keep `Pat:HTTPmethod` unique to make sure the Rate Limiter Config to be consistent (not to have duplicate rate limit policies)
		// path -> HTTP method
		apiOperations := make(map[string]map[string]struct{})

		rlsConfigs = make([]*rls_config.RateLimitDescriptor, 0, len(mgwSwagger.GetResources()))
		for _, resource := range mgwSwagger.GetResources() {
			path := mgwSwagger.GetXWso2Basepath() + resource.GetPath()
			if _, ok := apiOperations[path]; ok {
				// Unreachable if the swagger definition is valid
				loggers.LoggerXds.Warnf("Duplicate API resource %q in the swagger definition, skipping rate limit policy for the duplicate resource.", path)
				continue
			}

			apiOperations[path] = make(map[string]struct{})
			operationRlsConfigs := make([]*rls_config.RateLimitDescriptor, 0, len(resource.GetMethod()))
			operationPolicyExists := false
			for _, operation := range resource.GetMethod() {
				method := operation.GetMethod()
				if _, ok := apiOperations[path][method]; ok {
					// Unreachable if the swagger definition is valid
					loggers.LoggerXds.Warnf("Duplicate API resource HTTP method %q %q in the swagger definition, skipping rate limit policy for the duplicate resource.", path, method)
					continue
				}

				if operation.RateLimitPolicy == "" {
					loggers.LoggerXds.Debugf("No Operation level rate limit policy defined for %q %q", path, method)
					continue
				}
				operationPolicyExists = true

				rlPolicyConfig, err := getRateLimitPolicy(policies, operation.RateLimitPolicy)
				if err != nil {
					loggers.LoggerXds.Errorf("Error generating rate limit configuration for API: %q, API operation: %q %q: %v",
						apiID, path, method, err)
					return err
				}

				rlConf := &rls_config.RateLimitDescriptor{
					Key:       "method",
					Value:     method,
					RateLimit: rlPolicyConfig,
				}
				operationRlsConfigs = append(operationRlsConfigs, rlConf)
				apiOperations[path][method] = void
			}

			if operationPolicyExists {
				rlsConfig := &rls_config.RateLimitDescriptor{
					Key:         "path",
					Value:       path,
					Descriptors: operationRlsConfigs,
				}
				rlsConfigs = append(rlsConfigs, rlsConfig)
			}
		}
	} else {
		return fmt.Errorf("invalid rate limit policy level: %q", level)
	}

	org := mgwSwagger.OrganizationID
	vHost := mgwSwagger.VHost

	if _, ok := r.apiLevelRateLimitPolicies[org]; !ok {
		r.apiLevelRateLimitPolicies[org] = make(map[string]map[string][]*rls_config.RateLimitDescriptor)
	}

	if _, ok := r.apiLevelRateLimitPolicies[org][vHost]; !ok {
		r.apiLevelRateLimitPolicies[org][vHost] = make(map[string][]*rls_config.RateLimitDescriptor)
	}

	r.apiLevelRateLimitPolicies[org][vHost][apiID] = rlsConfigs
	return nil
}

// DeleteAPILevelRateLimitPolicies deletes inline Rate Limit policies added with the API.
func (r *rateLimitPolicyCache) DeleteAPILevelRateLimitPolicies(org, vHost, apiID string) {
	delete(r.apiLevelRateLimitPolicies[org][vHost], apiID)
}

func getRateLimitPolicy(policies map[string]*mgw.APIRateLimitPolicy, policyName string) (*rls_config.RateLimitPolicy, error) {
	policy, ok := policies[policyName]
	if !ok {
		return nil, fmt.Errorf("rate limit policy %q not defined", policyName)
	}

	unit, err := rlUnits.get(policy.SpanUnit)
	if err != nil {
		return nil, err
	}

	return &rls_config.RateLimitPolicy{
		Unit:            unit,
		RequestsPerUnit: policy.Count,
	}, nil
}

func (r *rateLimitPolicyCache) generateRateLimitConfig(label string) *rls_config.RateLimitConfig {
	var orgDescriptors []*rls_config.RateLimitDescriptor

	// Generate API level rate limit configurations
	for org, orgPolicies := range r.apiLevelRateLimitPolicies {
		var vHostDescriptors []*rls_config.RateLimitDescriptor
		for vHost, vHostPolicies := range orgPolicies {
			var apiDescriptors []*rls_config.RateLimitDescriptor
			for apiID, apiPolicies := range vHostPolicies {
				// Configure API Level rate limit policies only if, the API is deployed to the gateway label
				// Check API deployed to the gateway label
				if arrayContains(orgIDOpenAPIEnvoyMap[org][apiID], label) {
					apiDescriptors = append(apiDescriptors, apiPolicies...)
				}
			}
			vHostDescriptor := &rls_config.RateLimitDescriptor{
				Key:         "vhost",
				Value:       vHost,
				Descriptors: apiDescriptors,
			}
			vHostDescriptors = append(vHostDescriptors, vHostDescriptor)
		}
		orgDescriptor := &rls_config.RateLimitDescriptor{
			Key:         "org",
			Value:       org,
			Descriptors: vHostDescriptors,
		}
		orgDescriptors = append(orgDescriptors, orgDescriptor)
	}

	return &rls_config.RateLimitConfig{
		Name:        "Default",
		Domain:      "Default",
		Descriptors: orgDescriptors,
	}
}

func (r *rateLimitPolicyCache) updateXdsCache(label string) bool {
	rlsConf := r.generateRateLimitConfig(label)

	version := fmt.Sprint(rand.Intn(maxRandomInt))
	snap, err := gcp_cache.NewSnapshot(version, map[gcp_resource.Type][]gcp_types.Resource{
		gcp_resource.RateLimitConfigType: {
			rlsConf,
		},
	})
	if err != nil {
		loggers.LoggerXds.Error("Error while updating the rate limit snapshot", err)
		return false
	}
	if err := snap.Consistent(); err != nil {
		loggers.LoggerXds.Error("Inconsistent rate limiter snapshot", err)
		return false
	}

	if err := r.xdsCache.SetSnapshot(context.Background(), label, snap); err != nil {
		loggers.LoggerXds.Error("Error while updating the rate limit snapshot", err)
		return false
	}
	loggers.LoggerXds.Infof("New rate limit cache updated for the label: %q version: %q", label, version)
	loggers.LoggerXds.Trace("Updated rate limit config", rlsConf)
	return true
}

func init() {
	rlsPolicyCache = &rateLimitPolicyCache{
		xdsCache:                  gcp_cache.NewSnapshotCache(false, IDHash{}, nil),
		apiLevelRateLimitPolicies: make(map[string]map[string]map[string][]*rls_config.RateLimitDescriptor),
	}
}
