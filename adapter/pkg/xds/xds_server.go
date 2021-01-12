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
 */

// Package xds contains the implementation for the xds server cache updates
package xds

import (
	"fmt"
	"reflect"
	"sync"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	openAPI3 "github.com/getkin/kin-openapi/openapi3"
	openAPI2 "github.com/go-openapi/spec"
	logger "github.com/wso2/micro-gw/loggers"
	oasParser "github.com/wso2/micro-gw/pkg/oasparser"
	"github.com/wso2/micro-gw/pkg/oasparser/model"
	mgw "github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/operator"
)

var (
	version int32

	cache cachev3.SnapshotCache
	// OpenAPI Name:Version -> openAPI3 struct map
	openAPIV3Map map[string]openAPI3.Swagger
	// OpenAPI Name:Version -> openAPI2 struct map
	openAPIV2Map map[string]openAPI2.Swagger
	// WebsocketAPI Name:Version -> MgwSwagger struct map
	webSocketAPIMap map[string]mgw.MgwSwagger
	// OpenAPI Name:Version -> Envoy Label Array map
	openAPIEnvoyMap map[string][]string
	// OpenAPI Name:Version -> Envoy Routes map
	openAPIRoutesMap map[string][]*routev3.Route
	// OpenAPI Name:Version -> Envoy Clusters map
	openAPIClustersMap map[string][]*clusterv3.Cluster
	// OpenAPI Name:Version -> Envoy Endpoints map
	openAPIEndpointsMap map[string][]*corev3.Address
	// Envoy Label -> XDS version map
	envoyUpdateVersionMap map[string]int64
	// Envoy Label -> Listener Configuration map
	envoyListenerConfigMap map[string]*listenerv3.Listener
	// Envoy Label -> Routes Configuration map
	envoyRouteConfigMap map[string]*routev3.RouteConfiguration
)

// IDHash uses ID field as the node hash.
type IDHash struct{}

// ID uses the node ID field
func (IDHash) ID(node *corev3.Node) string {
	if node == nil {
		return "unknown"
	}
	return node.Id
}

var _ cachev3.NodeHash = IDHash{}

func init() {
	cache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	openAPIV3Map = make(map[string]openAPI3.Swagger)
	openAPIV2Map = make(map[string]openAPI2.Swagger)
	webSocketAPIMap = make(map[string]mgw.MgwSwagger)
	openAPIEnvoyMap = make(map[string][]string)
	openAPIRoutesMap = make(map[string][]*routev3.Route)
	openAPIClustersMap = make(map[string][]*clusterv3.Cluster)
	openAPIEndpointsMap = make(map[string][]*corev3.Address)
	//TODO: (VirajSalaka) Swagger or project should contain the version as a meta information
	envoyUpdateVersionMap = make(map[string]int64)
	envoyListenerConfigMap = make(map[string]*listenerv3.Listener)
	envoyRouteConfigMap = make(map[string]*routev3.RouteConfiguration)
}

// GetXdsCache returns xds server cache.
func GetXdsCache() cachev3.SnapshotCache {
	return cache
}

// UpdateEnvoy updates the Xds Cache when OpenAPI Json content is provided
func UpdateEnvoy(byteArr []byte, upstreamCerts []byte, apiType string) {
	var apiMapKey string
	var newLabels []string

	//TODO: (VirajSalaka) Optimize locking
	var l sync.Mutex
	l.Lock()
	defer l.Unlock()

	if apiType == mgw.HTTP {
		openAPIVersion, jsonContent, err := operator.GetOpenAPIVersionAndJSONContent(byteArr)
		if err != nil {
			logger.LoggerXds.Error("Error while retrieving the openAPI version and Json Content from byte Array.", err)
			return
		}
		logger.LoggerXds.Debugf("OpenAPI version : %v", openAPIVersion)
		if openAPIVersion == "3" {
			openAPIV3Struct, err := operator.GetOpenAPIV3Struct(jsonContent)
			if err != nil {
				logger.LoggerXds.Error("Error while parsing to a OpenAPIv3 struct. ", err)
			}
			apiMapKey = openAPIV3Struct.Info.Title + ":" + openAPIV3Struct.Info.Version
			existingOpenAPI, ok := openAPIV3Map[apiMapKey]
			if ok {
				if reflect.DeepEqual(openAPIV3Struct, existingOpenAPI) {
					//Works as the openAPI already contains the label feature.
					logger.LoggerXds.Infof("No changes to apply for the OpenAPI key : %v", apiMapKey)
					return
				}
			}
			openAPIV3Map[apiMapKey] = openAPIV3Struct
			//TODO: (VirajSalaka) Handle OpenAPIs which does not have label (Current Impl , it will be labelled as default)
			newLabels = model.GetXWso2Label(openAPIV3Struct.ExtensionProps)
		} else {
			openAPIV2Struct, err := operator.GetOpenAPIV2Struct(jsonContent)
			if err != nil {
				logger.LoggerXds.Error("Error while parsing to a OpenAPIv3 struct. ", err)
			}
			apiMapKey = openAPIV2Struct.Info.Title + ":" + openAPIV2Struct.Info.Version
			existingOpenAPI, ok := openAPIV2Map[apiMapKey]
			if ok {
				if reflect.DeepEqual(openAPIV2Struct, existingOpenAPI) {
					//Works as the openAPI already contains the label feature.
					logger.LoggerXds.Infof("No changes to apply for the OpenAPI key : %v", apiMapKey)
					return
				}
			}
			newLabels = operator.GetXWso2Labels(openAPIV2Struct.Extensions)
		}

	} else if apiType == mgw.WS {
		mgwSwagger := operator.GetMgwSwaggerWebSocket(byteArr)
		// TODO - uuid as the key
		apiMapKey = mgwSwagger.GetTitle() + ":" + mgwSwagger.GetVersion()
		existingWebSocketAPI, ok := webSocketAPIMap[apiMapKey]
		if ok {
			if reflect.DeepEqual(mgwSwagger, existingWebSocketAPI) {
				logger.LoggerXds.Infof("No changes to apply for the WebSocketAPI with key: %v", apiMapKey)
				return
			}
		}
		webSocketAPIMap[apiMapKey] = mgwSwagger
		// TODO - add label support
		newLabels = operator.GetXWso2LabelsWebSocket(mgwSwagger)
	} else {
		// Unreachable else condition. Added in case apiType type check fails prior to this function
		// due to any modifications to the code.
		logger.LoggerXds.Info("API type is not cuurently supported by WSO2 micro-gateway")
	}

	logger.LoggerXds.Infof("Added/Updated the content under OpenAPI Key : %v", apiMapKey)
	logger.LoggerXds.Debugf("Newly added labels for the OpenAPI Key : %v are %v", apiMapKey, newLabels)
	oldLabels, _ := openAPIEnvoyMap[apiMapKey]
	logger.LoggerXds.Debugf("Already existing labels for the OpenAPI Key : %v are %v", apiMapKey, oldLabels)
	openAPIEnvoyMap[apiMapKey] = newLabels
	routes, clusters, endpoints := oasParser.GetProductionRoutesClustersEndpoints(byteArr, upstreamCerts, apiType)
	// TODO: (VirajSalaka) Decide if the routes and listeners need their own map since it is not going to be changed based on API at the moment.
	openAPIRoutesMap[apiMapKey] = routes
	// openAPIListenersMap[apiMapKey] = listeners
	openAPIClustersMap[apiMapKey] = clusters
	openAPIEndpointsMap[apiMapKey] = endpoints
	// TODO: (VirajSalaka) Fault tolerance mechanism implementation
	updateXdsCacheOnAPIAdd(oldLabels, newLabels)
}

func arrayContains(a []string, x string) bool {
	for _, n := range a {
		if x == n {
			return true
		}
	}
	return false
}

func mergeResourceArrays(resourceArrays [][]types.Resource) []types.Resource {
	var totalLength int
	var compositeArray []types.Resource
	for _, resourceArray := range resourceArrays {
		totalLength += len(resourceArray)
	}
	compositeArray = make([]types.Resource, totalLength)
	startingIndex := 0
	lastIndex := 0
	for _, resourceArray := range resourceArrays {
		lastIndex += len(resourceArray)
		copy(compositeArray[startingIndex:lastIndex], resourceArray)
		startingIndex = lastIndex
	}
	return compositeArray
}

// when this method is called, openAPIEnvoy map is updated.
// Old labels refers to the previously assigned labels
// New labels refers to the the updated labels
func updateXdsCacheOnAPIAdd(oldLabels []string, newLabels []string) {

	// TODO: (VirajSalaka) check possible optimizations, Since the number of labels are low by design it should not be an issue
	for _, oldLabel := range oldLabels {
		if !arrayContains(newLabels, oldLabel) {
			listeners, clusters, routes, endpoints := generateEnvoyResoucesForLabel(oldLabel)
			updateXdsCache(oldLabel, endpoints, clusters, routes, listeners)
			logger.LoggerXds.Debugf("Xds Cache is updated for the already existing label : %v", oldLabel)
		}
	}

	for _, newLabel := range newLabels {
		listeners, clusters, routes, endpoints := generateEnvoyResoucesForLabel(newLabel)
		updateXdsCache(newLabel, endpoints, clusters, routes, listeners)
		logger.LoggerXds.Debugf("Xds Cache is updated for the newly added label : %v", newLabel)
	}
}

func generateEnvoyResoucesForLabel(label string) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	var clusterArray []*clusterv3.Cluster
	var routeArray []*routev3.Route
	var endpointArray []*corev3.Address
	// var listenerArrays [][]types.Resource
	for apiKey, labels := range openAPIEnvoyMap {
		if arrayContains(labels, label) {
			clusterArray = append(clusterArray, openAPIClustersMap[apiKey]...)
			routeArray = append(routeArray, openAPIRoutesMap[apiKey]...)
			endpointArray = append(endpointArray, openAPIEndpointsMap[apiKey]...)
			// listenerArrays = append(listenerArrays, openAPIListenersMap[apiKey])
		}
	}
	listener, listenerFound := envoyListenerConfigMap[label]
	routesConfig, routesConfigFound := envoyRouteConfigMap[label]
	if !listenerFound && !routesConfigFound {
		listener, routesConfig = oasParser.GetProductionListenerAndRouteConfig(routeArray)
		envoyListenerConfigMap[label] = listener
		envoyRouteConfigMap[label] = routesConfig
	} else {
		// If the routesConfig exists, the listener exists too
		oasParser.UpdateRoutesConfig(routesConfig, routeArray)
	}
	return oasParser.GetCacheResources(endpointArray, clusterArray, listener, routesConfig)
}

func updateXdsCache(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource, listeners []types.Resource) {
	version, ok := envoyUpdateVersionMap[label]
	if ok {
		version++
	} else {
		// TODO : (VirajSalaka) Fix control plane restart scenario : This is decided to be provided via the openapi file itself
		version = 1
	}
	// TODO: (VirajSalaka) kept same version for all the resources as we are using simple cache implementation.
	// Will be updated once decide to move to incremental XDS
	snap := cachev3.NewSnapshot(fmt.Sprint(version), endpoints, clusters, routes, listeners, nil, nil)
	snap.Consistent()
	err := cache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerMgw.Error(err)
	}
	envoyUpdateVersionMap[label] = version
	logger.LoggerMgw.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}
