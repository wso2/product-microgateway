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

	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	"github.com/wso2/micro-gw/pkg/svcdiscovery"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/envoyproxy/go-control-plane/wso2/discovery/api"
	"github.com/envoyproxy/go-control-plane/wso2/discovery/config/enforcer"
	openAPI3 "github.com/getkin/kin-openapi/openapi3"
	openAPI2 "github.com/go-openapi/spec"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	oasParser "github.com/wso2/micro-gw/pkg/oasparser"
	"github.com/wso2/micro-gw/pkg/oasparser/model"
	mgw "github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/operator"
)

var (
	version           int32
	mutexForXdsUpdate sync.Mutex

	cache         cachev3.SnapshotCache
	enforcerCache cachev3.SnapshotCache
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

	// Enforcer XDS resource version map
	enforcerCacheVersionMap map[string]int64
	// Enforcer API XDS resource version map
	enforcerAPIVersionMap map[string]int64
	enforcerApisMap       map[string][]types.Resource
	enforcerConfigMap     map[string][]types.Resource
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
	enforcerCache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
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

	enforcerCacheVersionMap = make(map[string]int64)
	enforcerConfigMap = make(map[string][]types.Resource)
	enforcerAPIVersionMap = make(map[string]int64)
	enforcerApisMap = make(map[string][]types.Resource)
}

// GetXdsCache returns xds server cache.
func GetXdsCache() cachev3.SnapshotCache {
	return cache
}

// GetEnforcerCache returns xds server cache.
func GetEnforcerCache() cachev3.SnapshotCache {
	return enforcerCache
}

// UpdateAPI updates the Xds Cache when OpenAPI Json content is provided
func UpdateAPI(byteArr []byte, upstreamCerts []byte, apiType string) {
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
				logger.LoggerXds.Error("Error while parsing to a OpenAPIv2 struct. ", err)
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
			openAPIV2Map[apiMapKey] = openAPIV2Struct
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

	routes, clusters, endpoints, mgwSwagger := oasParser.GetProductionRoutesClustersEndpoints(byteArr, upstreamCerts, apiType)
	enforcerAPI := oasParser.GetEnforcerAPI(mgwSwagger)
	// TODO: (VirajSalaka) Decide if the routes and listeners need their own map since it is not going to be changed based on API at the moment.
	openAPIRoutesMap[apiMapKey] = routes
	// openAPIListenersMap[apiMapKey] = listeners
	openAPIClustersMap[apiMapKey] = clusters
	openAPIEndpointsMap[apiMapKey] = endpoints
	// TODO: (VirajSalaka) Fault tolerance mechanism implementation
	updateXdsCacheOnAPIAdd(oldLabels, newLabels)
	UpdateEnforcerApis(enforcerAPI)
	startConsulServiceDiscovery() //consul service discovery starting point
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
			updateXdsCacheWithLock(oldLabel, endpoints, clusters, routes, listeners)
			logger.LoggerXds.Debugf("Xds Cache is updated for the already existing label : %v", oldLabel)
		}
	}

	for _, newLabel := range newLabels {
		listeners, clusters, routes, endpoints := generateEnvoyResoucesForLabel(newLabel)
		updateXdsCacheWithLock(newLabel, endpoints, clusters, routes, listeners)
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

func generateEnforcerConfigs(config *config.Config) *enforcer.Config {
	issuers := []*enforcer.Issuer{}
	for _, issuer := range config.Enforcer.JwtTokenConfig {
		jwtConfig := &enforcer.Issuer{
			CertificateAlias:     issuer.CertificateAlias,
			ConsumerKeyClaim:     issuer.ConsumerKeyClaim,
			Issuer:               issuer.Issuer,
			Name:                 issuer.Name,
			ValidateSubscription: issuer.ValidateSubscription,
			JwksURL:              issuer.JwksURL,
		}
		issuers = append(issuers, jwtConfig)
	}

	authService := &enforcer.AuthService{
		KeepAliveTime:  config.Enforcer.AuthService.KeepAliveTime,
		MaxHeaderLimit: config.Enforcer.AuthService.MaxHeaderLimit,
		MaxMessageSize: config.Enforcer.AuthService.MaxMessageSize,
		Port:           config.Enforcer.AuthService.Port,
		ThreadPool: &enforcer.ThreadPool{
			CoreSize:      config.Enforcer.AuthService.ThreadPool.CoreSize,
			KeepAliveTime: config.Enforcer.AuthService.ThreadPool.KeepAliveTime,
			MaxSize:       config.Enforcer.AuthService.ThreadPool.MaxSize,
			QueueSize:     config.Enforcer.AuthService.ThreadPool.QueueSize,
		},
	}

	return &enforcer.Config{
		Truststore: &enforcer.CertStore{
			Location: config.Enforcer.Truststore.Location,
			Password: config.Enforcer.Truststore.Password,
			Type:     config.Enforcer.Truststore.StoreType,
		},
		Keystore: &enforcer.CertStore{
			Location: config.Enforcer.Keystore.Location,
			Password: config.Enforcer.Keystore.Password,
			Type:     config.Enforcer.Keystore.StoreType,
		},
		ApimCredentials: &enforcer.AmCredentials{
			Username: config.Enforcer.ApimCredentials.Username,
			Password: config.Enforcer.ApimCredentials.Password,
		},
		AuthService:    authService,
		JwtTokenConfig: issuers,
		Eventhub: &enforcer.EventHub{
			Enabled:    config.Enforcer.EventHub.Enabled,
			ServiceUrl: config.Enforcer.EventHub.ServiceURL,
			JmsConnectionParameters: map[string]string{
				"eventListeningEndpoints": config.Enforcer.EventHub.JmsConnectionParameters.EventListeningEndpoints,
			},
		},
	}
}

//use updateXdsCacheWithLock to avoid race conditions
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
	snap := cachev3.NewSnapshot(fmt.Sprint(version), endpoints, clusters, routes, listeners, nil, nil, nil, nil)
	snap.Consistent()
	err := cache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerMgw.Error(err)
	}
	envoyUpdateVersionMap[label] = version
	logger.LoggerMgw.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerConfig Sets new update to the enforcer's configuration
func UpdateEnforcerConfig(configFile *config.Config) {
	// TODO: (Praminda) handle labels
	label := "enforcer"
	configs := []types.Resource{generateEnforcerConfigs(configFile)}
	version, ok := enforcerCacheVersionMap[label]
	if ok {
		version++
	} else {
		version = 1
	}

	apis := enforcerApisMap[label]

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, configs, apis)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerMgw.Error(err)
	}

	enforcerCacheVersionMap[label] = version
	enforcerConfigMap[label] = configs
	logger.LoggerMgw.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApis Sets new update to the enforcer's Apis
func UpdateEnforcerApis(api *api.Api) {
	//TODO: (Praminda) Use same cache and the version for both API and envoy xds resources
	label := "enforcer"
	apis := enforcerApisMap[label]
	apis = append(apis, api)
	version, ok := enforcerCacheVersionMap[label]
	if ok {
		version++
	} else {
		version = 1
	}
	configs := enforcerConfigMap[label]

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, configs, apis)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerMgw.Error(err)
	}

	enforcerCacheVersionMap[label] = version
	enforcerApisMap[label] = apis
	logger.LoggerMgw.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

//different go routines could update XDS at the same time. To avoid this we use a mutex and lock
func updateXdsCacheWithLock(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource,
	listeners []types.Resource) {
	mutexForXdsUpdate.Lock()
	defer mutexForXdsUpdate.Unlock()
	updateXdsCache(label, endpoints, clusters, routes, listeners)
}

func startConsulServiceDiscovery() {
	//label := "default"
	for apiKey, clusterList := range openAPIClustersMap {
		for _, cluster := range clusterList {
			logger.LoggerXds.Info(cluster.Name)
			if consulSyntax, ok := svcdiscovery.ClusterConsulKeyMap[cluster.Name]; ok {
				svcdiscovery.InitConsul() //initialize consul client and load certs
				query, errConSyn := svcdiscovery.ParseQueryString(consulSyntax)
				if errConSyn != nil {
					logger.LoggerXds.Error("consul syntax parse error ", errConSyn)
					return
				}
				logger.LoggerXds.Info(query)
				go getServiceDiscoveryData(query, cluster.Name, apiKey)
			}
		}
	}
}

func getServiceDiscoveryData(query svcdiscovery.Query, clusterName string, apiKey string) {
	doneChan := make(chan bool)
	svcdiscovery.ClusterConsulDoneChanMap[clusterName] = doneChan
	resultChan := svcdiscovery.ConsulClientInstance.Poll(query, doneChan)
	for {
		select {
		case queryResultsList, ok := <-resultChan:
			if !ok { //ok==false --> result chan is closed
				logger.LoggerXds.Info("closed the result channel for cluster name: ", clusterName)
				return
			}
			if val, ok := svcdiscovery.ClusterConsulResultMap[clusterName]; ok {
				if !reflect.DeepEqual(val, queryResultsList) {
					svcdiscovery.ClusterConsulResultMap[clusterName] = queryResultsList
					//update the envoy cluster
					updateRoute(apiKey, clusterName, queryResultsList)
				}
			} else {
				logger.LoggerXds.Debugln("updating cluster from the consul service registry, removed the default host")
				svcdiscovery.ClusterConsulResultMap[clusterName] = queryResultsList
				updateRoute(apiKey, clusterName, queryResultsList)
			}
		}
	}
}

func updateRoute(apiKey string, clusterName string, queryResultsList []svcdiscovery.Upstream) {
	if clusterList, available := openAPIClustersMap[apiKey]; available {
		for i := range clusterList {
			if clusterList[i].Name == clusterName {
				var lbEndpointList []*endpointv3.LbEndpoint
				for _, result := range queryResultsList {
					address := &corev3.Address{Address: &corev3.Address_SocketAddress{
						SocketAddress: &corev3.SocketAddress{
							Address:  result.Address,
							Protocol: corev3.SocketAddress_TCP,
							PortSpecifier: &corev3.SocketAddress_PortValue{
								PortValue: uint32(result.ServicePort),
							},
						},
					}}

					lbEndPoint := &endpointv3.LbEndpoint{
						HostIdentifier: &endpointv3.LbEndpoint_Endpoint{
							Endpoint: &endpointv3.Endpoint{
								Address: address,
							},
						},
					}
					lbEndpointList = append(lbEndpointList, lbEndPoint)
				}
				clusterList[i].LoadAssignment = &endpointv3.ClusterLoadAssignment{
					ClusterName: clusterName,
					Endpoints: []*endpointv3.LocalityLbEndpoints{
						{
							LbEndpoints: lbEndpointList,
						},
					},
				}
				updateXDSRouteCacheForServiceDiscovery(apiKey)
			}
		}
	}
}

func updateXDSRouteCacheForServiceDiscovery(apiKey string) {
	for key, envoyLabelList := range openAPIEnvoyMap {
		if key == apiKey {
			for _, label := range envoyLabelList {
				listeners, clusters, routes, endpoints := generateEnvoyResoucesForLabel(label)
				updateXdsCacheWithLock(label, endpoints, clusters, routes, listeners)
				logger.LoggerXds.Info("Updated XDS cache by consul service discovery")
			}
		}
	}
}

func stopConsulDiscoveryFor(clusterName string) {
	if doneChan, available := svcdiscovery.ClusterConsulDoneChanMap[clusterName]; available {
		close(doneChan)
	}
}
