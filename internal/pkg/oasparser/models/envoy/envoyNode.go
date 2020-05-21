/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package envoy

import (
	v2 "github.com/envoyproxy/go-control-plane/envoy/api/v2"
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	v2route "github.com/envoyproxy/go-control-plane/envoy/api/v2/route"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
)

type EnvoyNode struct {
	listeners []types.Resource
	clusters  []types.Resource
	routes    []types.Resource
	endpoints []types.Resource
}

func (envoy *EnvoyNode) SetListener(listener *v2.Listener) {
	envoy.listeners = []types.Resource{listener}
}

func (envoy *EnvoyNode) SetClusters(clusters []*v2.Cluster) {
	for _, clusterP := range clusters {
		envoy.clusters = append(envoy.clusters, clusterP)
	}
}

func (envoy *EnvoyNode) SetRoutes(routes []*v2route.Route) {
	for _, routes := range routes {
		envoy.routes = append(envoy.routes, routes)
	}
}

func (envoy *EnvoyNode) SetEndpoints(endpoints []*core.Address) {
	for _, endpoint := range endpoints {
		envoy.endpoints = append(envoy.endpoints, endpoint)
	}
}

func (envoy *EnvoyNode) GetSources() ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	return envoy.listeners, envoy.clusters, envoy.routes, envoy.endpoints
}
