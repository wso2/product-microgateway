/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package xds

import (
	"sync"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	oasModel "github.com/wso2/adapter/internal/oasparser/model"
)

// RequestEvent is the event that is published by the xds callback (onStreamRequest)
type RequestEvent struct {
	IsError    bool
	Version    string
	Node       string
	IsResponse bool
}

// EnforcerAPIState Stores the last success state of the enforcer apis
type EnforcerAPIState struct {
	Apis                     map[string]oasModel.MgwSwagger
	OpenAPIEnforcerApisMap   map[string]types.Resource
	APIUUIDToGatewayToVhosts map[string]map[string]string
	APIToVhostsMap           map[string]map[string]struct{}
}

// RouterResourceState Stores the last successful state of the router resources
type RouterResourceState struct {
	APIRoutesMap           map[string][]*routev3.Route     // Envoy Routes map
	APIClustersMap         map[string][]*clusterv3.Cluster // Envoy Clusters map
	APIEndpointsMap        map[string][]*corev3.Address
	EnvoyListenerConfigMap map[string][]*listenerv3.Listener // Listener Configuration map
	EnvoyRouteConfigMap    map[string]*routev3.RouteConfiguration
}

var (
	lockForStateUpdate sync.Mutex
	once               sync.Once
	// RequestEventChannel is the channel which communicates between callback and the server
	RequestEventChannel chan RequestEvent

	// enforcerState is to store the current success state of the enforcer
	enforcerState = make(map[string]EnforcerAPIState)

	// routerState is to store the current success state of the router
	routerState = make(map[string]RouterResourceState)
)

// NewRequestEvent create new change event
func NewRequestEvent() RequestEvent {
	return RequestEvent{false, "", "", false}
}

// GetRequestEventChannel returns the state change channel.
// RequestEventChannel should be a singleton object
func GetRequestEventChannel() chan RequestEvent {
	once.Do(func() {
		RequestEventChannel = make(chan RequestEvent)
	})
	return RequestEventChannel
}

// SetSuccessState set the last successful state of enforcer and router
func SetSuccessState(label string, enforerSuccessState EnforcerAPIState, routerSuccessState RouterResourceState) {
	lockForStateUpdate.Lock()
	defer lockForStateUpdate.Unlock()
	enforcerState[label] = enforerSuccessState
	routerState[label] = routerSuccessState

}

// GetLastSuccessState returns the successful enforcer state and router state which belongs to the provided label
func GetLastSuccessState(label string) (EnforcerAPIState, RouterResourceState) {
	var stateEnforcer EnforcerAPIState
	var stateRouter RouterResourceState

	if successState, ok := enforcerState[label]; ok {
		stateEnforcer = successState
	}
	if successState, ok := routerState[label]; ok {
		stateRouter = successState
	}
	return stateEnforcer, stateRouter
}
