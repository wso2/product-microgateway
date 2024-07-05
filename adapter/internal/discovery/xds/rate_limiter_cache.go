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
	"sync"

	gcp_types "github.com/envoyproxy/go-control-plane/pkg/cache/types"
	gcp_cache "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	gcp_resource "github.com/envoyproxy/go-control-plane/pkg/resource/v3"
	rls_config "github.com/envoyproxy/go-control-plane/ratelimit/config/ratelimit/v3"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
)

var rlsPolicyCache *rateLimitPolicyCache

const (
	subscriptionPolicyType = "subscription"
	organization           = "organization"
)

func getRateLimitUnit(name string) (rls_config.RateLimitUnit, error) {
	switch strings.ToUpper(name) {
	case "SECOND":
		return rls_config.RateLimitUnit_SECOND, nil
	case "MINUTE":
		return rls_config.RateLimitUnit_MINUTE, nil
	case "HOUR":
		return rls_config.RateLimitUnit_HOUR, nil
	case "DAY":
		return rls_config.RateLimitUnit_DAY, nil
	default:
		return rls_config.RateLimitUnit_UNKNOWN, fmt.Errorf("invalid rate limit unit %q", name)
	}
}

func parseRateLimitUnitFromSubscriptionPolicy(name string) (rls_config.RateLimitUnit, error) {
	switch name {
	case "sec":
		return rls_config.RateLimitUnit_SECOND, nil
	case "min":
		return rls_config.RateLimitUnit_MINUTE, nil
	case "hour":
		return rls_config.RateLimitUnit_HOUR, nil
	case "day":
		return rls_config.RateLimitUnit_DAY, nil
	default:
		return rls_config.RateLimitUnit_UNKNOWN, fmt.Errorf("invalid rate limit unit %q", name)
	}
}

type rateLimitPolicyCache struct {
	// xdsCache is the snapshot cache for the rate limiter service
	xdsCache gcp_cache.SnapshotCache

	// TODO: (renuka) move both 'apiLevelRateLimitPolicies' and 'apiLevelMu' to a new struct when doing the App level rate limiting
	// So app level rate limits are in a new struct and refer in this struct.
	// org -> vhost -> API-Identifier (i.e. Vhost:API-UUID) -> Rate Limit Configs
	apiLevelRateLimitPolicies map[string]map[string]map[string][]*rls_config.RateLimitDescriptor
	// metadataBasedPolicies is used to store the rate limit policies which are based on dynamic metadata.
	// metadata related rate limit configs: rate limit type (eg: subscription) -> organization -> policy name (eg: Gold, Silver) -> rate-limit config
	metadataBasedPolicies map[string]map[string]map[string]*rls_config.RateLimitDescriptor
	// mutex for API level
	apiLevelMu sync.RWMutex
}

// AddAPILevelRateLimitPolicies adds inline Rate Limit policies in APIs to be updated in the Rate Limiter service.
func (r *rateLimitPolicyCache) AddAPILevelRateLimitPolicies(apiID string, mgwSwagger *mgw.MgwSwagger, policies map[string]*mgw.APIRateLimitPolicy) error {
	if mgwSwagger.RateLimitLevel == "" || mgwSwagger.RateLimitLevel == envoyconf.Unlimited {
		return nil
	}
	level := strings.ToUpper(mgwSwagger.RateLimitLevel)
	loggers.LoggerXds.Infof("Rate-limiting enabled for the API: %q, level: %q", apiID, level)

	var rlsConfigs []*rls_config.RateLimitDescriptor
	if level == envoyconf.RateLimitPolicyAPILevel {
		rlPolicyConfig, err := getRateLimitPolicy(policies, mgwSwagger.RateLimitPolicy)
		if err != nil {
			loggers.LoggerXds.Errorf("Error generating API level rate limit configuration for API: %q", apiID)
			return err
		}

		rlsConfigs = []*rls_config.RateLimitDescriptor{
			{
				Key:   envoyconf.DescriptorKeyForPath,
				Value: mgwSwagger.GetXWso2Basepath(),
				Descriptors: []*rls_config.RateLimitDescriptor{
					{
						Key:       envoyconf.DescriptorKeyForMethod,
						Value:     envoyconf.DescriptorValueForAPIMethod,
						RateLimit: rlPolicyConfig,
					},
				},
			},
		}
	} else if level == envoyconf.RateLimitPolicyOperationLevel {
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
					Key:       envoyconf.DescriptorKeyForMethod,
					Value:     method,
					RateLimit: rlPolicyConfig,
				}
				operationRlsConfigs = append(operationRlsConfigs, rlConf)
				apiOperations[path][method] = void
			}

			if operationPolicyExists {
				rlsConfig := &rls_config.RateLimitDescriptor{
					Key:         envoyconf.DescriptorKeyForPath,
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

	r.apiLevelMu.Lock()
	defer r.apiLevelMu.Unlock()
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
	r.apiLevelMu.Lock()
	defer r.apiLevelMu.Unlock()
	delete(r.apiLevelRateLimitPolicies[org][vHost], apiID)
}

func getRateLimitPolicy(policies map[string]*mgw.APIRateLimitPolicy, policyName string) (*rls_config.RateLimitPolicy, error) {
	policy, ok := policies[policyName]
	if !ok {
		return nil, fmt.Errorf("rate limit policy %q not defined", policyName)
	}

	unit, err := getRateLimitUnit(policy.SpanUnit)
	if err != nil {
		return nil, err
	}

	return &rls_config.RateLimitPolicy{
		Unit:            unit,
		RequestsPerUnit: (uint32(policy.Count)),
	}, nil
}

func (r *rateLimitPolicyCache) generateRateLimitConfig(label string) *rls_config.RateLimitConfig {
	var orgDescriptors []*rls_config.RateLimitDescriptor
	var metadataDescriptors []*rls_config.RateLimitDescriptor

	r.apiLevelMu.RLock()
	defer r.apiLevelMu.RUnlock()

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
				Key:         envoyconf.DescriptorKeyForVhost,
				Value:       vHost,
				Descriptors: apiDescriptors,
			}
			vHostDescriptors = append(vHostDescriptors, vHostDescriptor)
		}
		orgDescriptor := &rls_config.RateLimitDescriptor{
			Key:         envoyconf.DescriptorKeyForOrg,
			Value:       org,
			Descriptors: vHostDescriptors,
		}
		orgDescriptors = append(orgDescriptors, orgDescriptor)
	}

	// Iterate through the subscription policies and append it to the orgDescriptors
	// Sample Rate Limiter config:
	// 	domain: Default
	// 	descriptors:
	//   	- key: organisation
	//        value: org001
	//         - key: subscription
	//           descriptors:
	//            - key: policy
	//              value: gold
	//              rate_limit:
	//                requests_per_unit: 1000
	//                unit: minute
	//            - key: policy
	//              value: silver
	//              rate_limit:
	//                requests_per_unit: 200
	//                unit: minute
	if subscriptionPoliciesList, ok := r.metadataBasedPolicies[subscriptionPolicyType]; ok {
		for orgUUID := range subscriptionPoliciesList {
			var metadataDescriptor *rls_config.RateLimitDescriptor
			var policyDescriptors []*rls_config.RateLimitDescriptor
			metadataDescriptor = &rls_config.RateLimitDescriptor{
				Key:   organization,
				Value: orgUUID,
			}
			subscriptionIDDescriptor := &rls_config.RateLimitDescriptor{
				Key: subscriptionPolicyType,
			}
			for policyName := range subscriptionPoliciesList[orgUUID] {
				policyDescriptors = append(policyDescriptors, subscriptionPoliciesList[orgUUID][policyName])
			}
			subscriptionIDDescriptor.Descriptors = policyDescriptors
			metadataDescriptor.Descriptors = append(metadataDescriptor.Descriptors, subscriptionIDDescriptor)

			metadataDescriptors = append(metadataDescriptors, metadataDescriptor)
		}
	}

	allDescriptors := append(orgDescriptors, metadataDescriptors...)
	return &rls_config.RateLimitConfig{
		Name:        envoyconf.RateLimiterDomain,
		Domain:      envoyconf.RateLimiterDomain,
		Descriptors: allDescriptors,
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
		loggers.LoggerXds.Error("Error while updating the rate limit snapshot: ", err)
		return false
	}
	if err := snap.Consistent(); err != nil {
		loggers.LoggerXds.Error("Inconsistent rate limiter snapshot: ", err)
		return false
	}

	if err := r.xdsCache.SetSnapshot(context.Background(), label, snap); err != nil {
		loggers.LoggerXds.Error("Error while updating the rate limit snapshot: ", err)
		return false
	}
	loggers.LoggerXds.Infof("New rate limit cache updated for the label: %q version: %q", label, version)
	loggers.LoggerXds.Debug("Updated rate limit config: ", rlsConf)
	return true
}

// AddSubscriptionLevelRateLimitPolicies adds a subscription level rate limit policies to the cache.
func AddSubscriptionLevelRateLimitPolicies(policyList *types.SubscriptionPolicyList) error {
	// Check if rlsPolicyCache.metadataBasedPolicies[Subscription] exists and create a new map if not
	if _, ok := rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType]; !ok {
		rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType] = make(map[string]map[string]*rls_config.RateLimitDescriptor)
	}
	for _, policy := range policyList.List {
		// Needs to skip on async policies.
		if policy.DefaultLimit == nil || policy.DefaultLimit.QuotaType != "requestCount" || policy.DefaultLimit.RequestCount == nil {
			continue
		}

		// Need not to add the Unauthenticated and Unlimited policies to the rate limiter service
		if (policy.Organization == "carbon.super" && policy.Name == "Unauthenticated") || policy.DefaultLimit.RequestCount.RequestCount <= 0 {
			continue
		}
		AddSubscriptionLevelRateLimitPolicy(policy);
		loggers.LoggerXds.Debugf("Rate-limiter cache map updated with subscription policy: %s belonging to the organization: %s", policy.Name, policy.Organization)
	}
	return nil
}

// RemoveSubscriptionRateLimitPolicy removes a subscription level rate limit policy from the rate-limit cache.
func RemoveSubscriptionRateLimitPolicy(policy types.SubscriptionPolicy) {
	rlsPolicyCache.apiLevelMu.Lock()
	defer rlsPolicyCache.apiLevelMu.Unlock()
	if policiesForOrg , ok := rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType][policy.Organization]; ok {
		delete(policiesForOrg, policy.Name)
	}
}

// UpdateSubscriptionRateLimitPolicy updates a subscription level rate limit policy to the rate-limit cache.
func UpdateSubscriptionRateLimitPolicy(policy types.SubscriptionPolicy) {
	rlsPolicyCache.apiLevelMu.Lock()
	defer rlsPolicyCache.apiLevelMu.Unlock()
	if policiesForOrg , ok := rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType][policy.Organization]; ok {
		delete(policiesForOrg, policy.Name)
	}
	error := AddSubscriptionLevelRateLimitPolicy(policy)
	if error != nil {
		loggers.LoggerXds.Errorf("Error occurred while updating subscription policy: %s for the orgnanization %s. Error: %v",
				policy.Name, policy.Organization, error)
	}
}

// AddSubscriptionLevelRateLimitPolicy adds a subscription level rate limit policy to the rate-limit cache.
func AddSubscriptionLevelRateLimitPolicy(policy types.SubscriptionPolicy) error {
	rateLimitUnit, err := parseRateLimitUnitFromSubscriptionPolicy(policy.DefaultLimit.RequestCount.TimeUnit)
	if err != nil {
		loggers.LoggerXds.Error("Error while getting the rate limit unit: ", err)
		return err
	}
	rlPolicyConfig := rls_config.RateLimitPolicy{
		Unit:            rateLimitUnit,
		RequestsPerUnit: uint32(policy.DefaultLimit.RequestCount.RequestCount),
	}
	descriptor := &rls_config.RateLimitDescriptor{
		Key:       "policy",
		Value:     policy.Name,
		RateLimit: &rlPolicyConfig,
		ShadowMode: !policy.StopOnQuotaReach,
	}
	if _, ok := rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType][policy.Organization]; !ok {
		rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType][policy.Organization] = make(map[string]*rls_config.RateLimitDescriptor)
	}

	if policy.RateLimitCount > 0 && policy.RateLimitTimeUnit != "" {
		burstCtrlUnit, err := parseRateLimitUnitFromSubscriptionPolicy(policy.RateLimitTimeUnit)
		if err != nil {
			loggers.LoggerXds.Error("Error while getting the burst control time unit", err)
			return err
		}
		burstCtrlPolicyConfig := rls_config.RateLimitPolicy{
			Unit:            burstCtrlUnit,
			RequestsPerUnit: uint32(policy.RateLimitCount),
		}
		burstCtrlDescriptor := &rls_config.RateLimitDescriptor{
			Key: "burst",
			Value: "enabled",
			RateLimit: &burstCtrlPolicyConfig,
		}
		descriptor.Descriptors = append(descriptor.Descriptors, burstCtrlDescriptor)
	}
	rlsPolicyCache.metadataBasedPolicies[subscriptionPolicyType][policy.Organization][policy.Name] = descriptor
	return nil
}

func init() {
	rlsPolicyCache = &rateLimitPolicyCache{
		xdsCache:                  gcp_cache.NewSnapshotCache(false, IDHash{}, nil),
		apiLevelRateLimitPolicies: make(map[string]map[string]map[string][]*rls_config.RateLimitDescriptor),
		metadataBasedPolicies:     make(map[string]map[string]map[string]*rls_config.RateLimitDescriptor),
	}
}
