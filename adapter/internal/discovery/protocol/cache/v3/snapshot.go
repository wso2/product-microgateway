// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package cache

import (
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	envoy_cache "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	wso2_types "github.com/wso2/micro-gw/internal/discovery/protocol/cache/types"
)

// IndexResourcesByName creates a map from the resource name to the resource.
func IndexResourcesByName(items []types.Resource) map[string]types.Resource {
	indexed := make(map[string]types.Resource, len(items))
	for _, item := range items {
		indexed[GetResourceName(item)] = item
	}
	return indexed
}

// NewResources creates a new resource group.
func NewResources(version string, items []types.Resource) envoy_cache.Resources {
	return envoy_cache.Resources{
		Version: version,
		Items:   IndexResourcesByName(items),
	}
}

// Snapshot is an internally consistent snapshot of xDS resources.
// Consistency is important for the convergence as different resource types
// from the snapshot may be delivered to the proxy in arbitrary order.
type Snapshot struct {
	envoy_cache.Snapshot
	Resources [wso2_types.UnknownType]envoy_cache.Resources
}

// NewSnapshot creates a snapshot from response types and a version.
func NewSnapshot(version string,
	configs []types.Resource,
	apis []types.Resource,
	subcriptionList []types.Resource,
	applicationList []types.Resource,
	apiList []types.Resource,
	applicationPolicyList []types.Resource,
	subscriptionPolicyList []types.Resource,
	applicationKeyMappingList []types.Resource,
	keyManagerConfig []types.Resource) Snapshot {
	out := Snapshot{}
	out.Resources[wso2_types.Config] = NewResources(version, configs)
	out.Resources[wso2_types.API] = NewResources(version, apis)
	out.Resources[wso2_types.SubscriptionList] = NewResources(version, subcriptionList)
	out.Resources[wso2_types.ApplicationList] = NewResources(version, applicationList)
	out.Resources[wso2_types.APIList] = NewResources(version, apiList)
	out.Resources[wso2_types.ApplicationPolicyList] = NewResources(version, applicationPolicyList)
	out.Resources[wso2_types.SubscriptionPolicyList] = NewResources(version, subscriptionPolicyList)
	out.Resources[wso2_types.ApplicationKeyMappingList] = NewResources(version, applicationKeyMappingList)
	out.Resources[wso2_types.KeyManagerConfig] = NewResources(version, keyManagerConfig)
	return out
}

// GetResources selects snapshot resources by type.
func (s *Snapshot) GetResources(typeURL string) map[string]types.Resource {
	if s == nil {
		return nil
	}
	typ := GetResponseType(typeURL)
	if typ == wso2_types.UnknownType {
		return nil
	}
	return s.Resources[typ].Items
}

// GetVersion returns the version for a resource type.
func (s *Snapshot) GetVersion(typeURL string) string {
	if s == nil {
		return ""
	}
	typ := GetResponseType(typeURL)
	if typ == wso2_types.UnknownType {
		return ""
	}
	return s.Resources[typ].Version
}
