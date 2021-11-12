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
	"errors"

	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	envoy_cache "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	wso2_types "github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/cache/types"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/resource/v3"
)

// Snapshot is an internally consistent snapshot of xDS resources.
// Consistency is important for the convergence as different resource types
// from the snapshot may be delivered to the proxy in arbitrary order.
type Snapshot struct {
	envoy_cache.Snapshot
	Resources [wso2_types.UnknownType]envoy_cache.Resources
	// Only used for delta XDS. Hence it remains unused for adapter implementation.
	VersionMap map[string]map[string]string
}

// NewSnapshot creates a snapshot from response types and a version.
// The resources map is keyed off the type URL of a resource, followed by the slice of resource objects.
func NewSnapshot(version string, resources map[resource.Type][]types.Resource) (Snapshot, error) {
	out := Snapshot{}

	for typ, resource := range resources {
		index := GetResponseType(typ)
		if index == wso2_types.UnknownType {
			return out, errors.New("unknown resource type: " + typ)
		}

		out.Resources[index] = NewResources(version, resource)
	}

	return out, nil
}

// // GetResources selects snapshot resources by type, returning the map of resources.
// func (s *Snapshot) GetResources(typeURL resource.Type) map[string]types.Resource {
// 	resources := s.GetResourcesAndTTL(typeURL)
// 	if resources == nil {
// 		return nil
// 	}

// 	withoutTTL := make(map[string]types.Resource, len(resources))

// 	for k, v := range resources {
// 		withoutTTL[k] = v.Resource
// 	}

// 	return withoutTTL
// }

// GetResourcesAndTTL selects snapshot resources by type, returning the map of resources and the associated TTL.
func (s *Snapshot) GetResourcesAndTTL(typeURL resource.Type) map[string]types.ResourceWithTTL {
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

// IndexResourcesByName creates a map from the resource name to the resource.
func IndexResourcesByName(items []types.ResourceWithTTL) map[string]types.ResourceWithTTL {
	indexed := make(map[string]types.ResourceWithTTL)
	for _, item := range items {
		indexed[GetResourceName(item.Resource)] = item
	}
	return indexed
}

// NewResources creates a new resource group.
func NewResources(version string, items []types.Resource) envoy_cache.Resources {
	itemsWithTTL := []types.ResourceWithTTL{}
	for _, item := range items {
		itemsWithTTL = append(itemsWithTTL, types.ResourceWithTTL{Resource: item})
	}
	return NewResourcesWithTTL(version, itemsWithTTL)
}

// NewResourcesWithTTL creates a new resource group.
func NewResourcesWithTTL(version string, items []types.ResourceWithTTL) envoy_cache.Resources {
	return envoy_cache.Resources{
		Version: version,
		Items:   IndexResourcesByName(items),
	}
}
