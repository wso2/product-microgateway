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
	"errors"
	"fmt"
	"math/rand"
	"reflect"
	"strings"
	"sync"

	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/subscription"

	wso2_cache "github.com/wso2/micro-gw/internal/discovery/protocol/cache/v3"
	"github.com/wso2/micro-gw/internal/svcdiscovery"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	envoy_cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/wso2/micro-gw/config"
	apiModel "github.com/wso2/micro-gw/internal/api/models"
	eventhubTypes "github.com/wso2/micro-gw/internal/eventhub/types"
	oasParser "github.com/wso2/micro-gw/internal/oasparser"
	envoyconf "github.com/wso2/micro-gw/internal/oasparser/envoyconf"
	mgw "github.com/wso2/micro-gw/internal/oasparser/model"
	"github.com/wso2/micro-gw/internal/oasparser/operator"
	logger "github.com/wso2/micro-gw/loggers"
)

var (
	version           int32
	mutexForXdsUpdate sync.Mutex

	cache                              envoy_cachev3.SnapshotCache
	enforcerCache                      wso2_cache.SnapshotCache
	enforcerSubscriptionCache          wso2_cache.SnapshotCache
	enforcerApplicationCache           wso2_cache.SnapshotCache
	enforcerAPICache                   wso2_cache.SnapshotCache
	enforcerApplicationPolicyCache     wso2_cache.SnapshotCache
	enforcerSubscriptionPolicyCache    wso2_cache.SnapshotCache
	enforcerApplicationKeyMappingCache wso2_cache.SnapshotCache
	enforcerKeyManagerCache            wso2_cache.SnapshotCache
	enforcerRevokedTokensCache         wso2_cache.SnapshotCache
	enforcerThrottleDataCache          wso2_cache.SnapshotCache

	// Vhost:APIName:Version as map key
	apiMgwSwaggerMap       map[string]mgw.MgwSwagger       // MgwSwagger struct map
	openAPIEnvoyMap        map[string][]string             // Envoy Label Array map
	openAPIRoutesMap       map[string][]*routev3.Route     // Envoy Routes map
	openAPIClustersMap     map[string][]*clusterv3.Cluster // Envoy Clusters map
	openAPIEndpointsMap    map[string][]*corev3.Address    // Envoy Endpoints map
	openAPIEnforcerApisMap map[string]types.Resource       // API Resource map

	// Envoy Label as map key
	envoyUpdateVersionMap  map[string]int64                       // XDS version map
	envoyListenerConfigMap map[string]*listenerv3.Listener        // Listener Configuration map
	envoyRouteConfigMap    map[string]*routev3.RouteConfiguration // Routes Configuration map

	// Common Enforcer Label as map key
	enforcerConfigMap                map[string][]types.Resource
	enforcerKeyManagerMap            map[string][]types.Resource
	enforcerSubscriptionMap          map[string][]types.Resource
	enforcerApplicationMap           map[string][]types.Resource
	enforcerAPIListMap               map[string][]types.Resource
	enforcerApplicationPolicyMap     map[string][]types.Resource
	enforcerSubscriptionPolicyMap    map[string][]types.Resource
	enforcerApplicationKeyMappingMap map[string][]types.Resource
	enforcerRevokedTokensMap         map[string][]types.Resource
	enforcerThrottleDataMap          map[string][]types.Resource

	// KeyManagerList to store data
	KeyManagerList = make([]eventhubTypes.KeyManager, 0)
)

const (
	commonEnforcerLabel  string = "commonEnforcerLabel"
	maxRandomInt         int    = 999999999
	prototypedAPI        string = "PROTOTYPED"
	apiKeyFieldSeparator string = ":"
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

var _ envoy_cachev3.NodeHash = IDHash{}

func init() {
	cache = envoy_cachev3.NewSnapshotCache(false, IDHash{}, nil)
	enforcerCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerSubscriptionCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerApplicationCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerAPICache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerApplicationPolicyCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerSubscriptionPolicyCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerApplicationKeyMappingCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerKeyManagerCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerRevokedTokensCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)
	enforcerThrottleDataCache = wso2_cache.NewSnapshotCache(false, IDHash{}, nil)

	apiMgwSwaggerMap = make(map[string]mgw.MgwSwagger)
	openAPIEnvoyMap = make(map[string][]string)
	openAPIEnforcerApisMap = make(map[string]types.Resource)
	openAPIRoutesMap = make(map[string][]*routev3.Route)
	openAPIClustersMap = make(map[string][]*clusterv3.Cluster)
	openAPIEndpointsMap = make(map[string][]*corev3.Address)
	//TODO: (VirajSalaka) Swagger or project should contain the version as a meta information
	envoyUpdateVersionMap = make(map[string]int64)
	envoyListenerConfigMap = make(map[string]*listenerv3.Listener)
	envoyRouteConfigMap = make(map[string]*routev3.RouteConfiguration)

	enforcerConfigMap = make(map[string][]types.Resource)
	enforcerKeyManagerMap = make(map[string][]types.Resource)
	enforcerSubscriptionMap = make(map[string][]types.Resource)
	enforcerApplicationMap = make(map[string][]types.Resource)
	enforcerAPIListMap = make(map[string][]types.Resource)
	enforcerApplicationPolicyMap = make(map[string][]types.Resource)
	enforcerSubscriptionPolicyMap = make(map[string][]types.Resource)
	enforcerApplicationKeyMappingMap = make(map[string][]types.Resource)
	enforcerRevokedTokensMap = make(map[string][]types.Resource)
	enforcerThrottleDataMap = make(map[string][]types.Resource)
}

// GetXdsCache returns xds server cache.
func GetXdsCache() envoy_cachev3.SnapshotCache {
	return cache
}

// GetEnforcerCache returns xds server cache.
func GetEnforcerCache() wso2_cache.SnapshotCache {
	return enforcerCache
}

// GetEnforcerSubscriptionCache returns xds server cache.
func GetEnforcerSubscriptionCache() wso2_cache.SnapshotCache {
	return enforcerSubscriptionCache
}

// GetEnforcerApplicationCache returns xds server cache.
func GetEnforcerApplicationCache() wso2_cache.SnapshotCache {
	return enforcerApplicationCache
}

// GetEnforcerAPICache returns xds server cache.
func GetEnforcerAPICache() wso2_cache.SnapshotCache {
	return enforcerAPICache
}

// GetEnforcerApplicationPolicyCache returns xds server cache.
func GetEnforcerApplicationPolicyCache() wso2_cache.SnapshotCache {
	return enforcerApplicationPolicyCache
}

// GetEnforcerSubscriptionPolicyCache returns xds server cache.
func GetEnforcerSubscriptionPolicyCache() wso2_cache.SnapshotCache {
	return enforcerSubscriptionPolicyCache
}

// GetEnforcerApplicationKeyMappingCache returns xds server cache.
func GetEnforcerApplicationKeyMappingCache() wso2_cache.SnapshotCache {
	return enforcerApplicationKeyMappingCache
}

// GetEnforcerKeyManagerCache returns xds server cache.
func GetEnforcerKeyManagerCache() wso2_cache.SnapshotCache {
	return enforcerKeyManagerCache
}

//GetEnforcerRevokedTokenCache return token cache
func GetEnforcerRevokedTokenCache() wso2_cache.SnapshotCache {
	return enforcerRevokedTokensCache
}

//GetEnforcerThrottleDataCache return throttle data cache
func GetEnforcerThrottleDataCache() wso2_cache.SnapshotCache {
	return enforcerThrottleDataCache
}

// UpdateAPI updates the Xds Cache when OpenAPI Json content is provided
func UpdateAPI(apiContent config.APIContent) {
	var newLabels []string
	var mgwSwagger mgw.MgwSwagger
	if len(apiContent.Environments) == 0 {
		apiContent.Environments = append(apiContent.Environments, "default")
	}

	//TODO: (VirajSalaka) Optimize locking
	var l sync.Mutex
	l.Lock()
	defer l.Unlock()

	if apiContent.APIType == mgw.HTTP {
		mgwSwagger = operator.GetMgwSwagger(apiContent.APIDefinition)
		mgwSwagger.SetName(apiContent.Name)
		mgwSwagger.SetVersion(apiContent.Version)
		mgwSwagger.SetSecurityScheme(apiContent.SecurityScheme)
	} else if apiContent.APIType == mgw.WS {
		mgwSwagger = operator.GetMgwSwaggerWebSocket(apiContent.APIDefinition)
	} else {
		// Unreachable else condition. Added in case previous apiType check fails due to any modifications.
		logger.LoggerXds.Error("API type not currently supported with WSO2 Microgateway")
	}

	if apiContent.LifeCycleStatus == prototypedAPI {
		mgwSwagger.SetXWso2ProductionEndpointMgwSwagger(apiContent.ProductionEndpoint)
		mgwSwagger.SetXWso2SandboxEndpointForMgwSwagger(apiContent.SandboxEndpoint)
	}

	apiIdentifier := GenerateIdentifierForAPI(apiContent.VHost, apiContent.Name, apiContent.Version)
	//TODO: (SuKSW) Uncomment the below section depending on MgwSwagger.Resource ids
	//TODO: (SuKSW) Update the existing API if the basepath already exists
	//existingMgwSwagger, exists := apiMgwSwaggerMap[apiIdentifier]
	// if exists {
	// 	if reflect.DeepEqual(mgwSwagger, existingMgwSwagger) {
	// 		logger.LoggerXds.Infof("API %v already exists. No changes to apply.", apiIdentifier)
	// 		return
	// 	}
	// }
	apiMgwSwaggerMap[apiIdentifier] = mgwSwagger
	//TODO: (VirajSalaka) Handle OpenAPIs which does not have label (Current Impl , it will be labelled as default)
	// TODO: commented the following line as the implementation is not supported yet.
	//newLabels = model.GetXWso2Label(openAPIV3Struct.ExtensionProps)
	//:TODO: since currently labels are not taking from x-wso2-label, I have made it to be taken from the method
	// argument.
	newLabels = apiContent.Environments
	logger.LoggerXds.Infof("Added/Updated the content under OpenAPI Key : %v", apiIdentifier)
	logger.LoggerXds.Debugf("Newly added labels for the OpenAPI Key : %v are %v", apiIdentifier, newLabels)
	oldLabels, _ := openAPIEnvoyMap[apiIdentifier]
	logger.LoggerXds.Debugf("Already existing labels for the OpenAPI Key : %v are %v", apiIdentifier, oldLabels)
	openAPIEnvoyMap[apiIdentifier] = newLabels

	routes, clusters, endpoints := oasParser.GetProductionRoutesClustersEndpoints(mgwSwagger, apiContent.UpstreamCerts,
		apiContent.VHost)
	// TODO: (VirajSalaka) Decide if the routes and listeners need their own map since it is not going to be changed based on API at the moment.
	openAPIRoutesMap[apiIdentifier] = routes
	// openAPIListenersMap[apiMapKey] = listeners
	openAPIClustersMap[apiIdentifier] = clusters
	openAPIEndpointsMap[apiIdentifier] = endpoints
	openAPIEnforcerApisMap[apiIdentifier] = oasParser.GetEnforcerAPI(mgwSwagger, apiContent.LifeCycleStatus,
		apiContent.VHost)
	// TODO: (VirajSalaka) Fault tolerance mechanism implementation
	updateXdsCacheOnAPIAdd(oldLabels, newLabels)
	if svcdiscovery.IsServiceDiscoveryEnabled {
		startConsulServiceDiscovery() //consul service discovery starting point
	}
}

// DeleteAPI deletes an API, its resources and updates the caches
func DeleteAPI(vhost, apiName, version string) error {
	apiIdentifier := GenerateIdentifierForAPI(vhost, apiName, version)
	_, exists := apiMgwSwaggerMap[apiIdentifier]
	if !exists {
		logger.LoggerXds.Infof("Unable to delete API " + apiIdentifier + ". Does not exist.")
		return errors.New(mgw.NotFound)
	}
	//clean maps of routes, clusters, endpoints, enforcerAPIs
	delete(openAPIRoutesMap, apiIdentifier)
	delete(openAPIClustersMap, apiIdentifier)
	delete(openAPIEndpointsMap, apiIdentifier)
	delete(openAPIEnforcerApisMap, apiIdentifier)

	existingLabels := openAPIEnvoyMap[apiIdentifier]
	//updateXdsCacheOnAPIAdd is called after cleaning maps of routes, clusters, endpoints, enforcerAPIs.
	//Therefore resources that belongs to the deleting API do not exist. Caches updated only with
	//resources that belongs to the remaining APIs
	updateXdsCacheOnAPIAdd(existingLabels, []string{})

	delete(openAPIEnvoyMap, apiIdentifier)  //delete labels
	delete(apiMgwSwaggerMap, apiIdentifier) //delete mgwSwagger
	//TODO: (SuKSW) clean any remaining in label wise maps, if this is the last API of that label
	logger.LoggerXds.Infof("Deleted API. %v", apiIdentifier)
	return nil
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
			listeners, clusters, routes, endpoints, apis := GenerateEnvoyResoucesForLabel(oldLabel)
			UpdateXdsCacheWithLock(oldLabel, endpoints, clusters, routes, listeners)
			UpdateEnforcerApis(oldLabel, apis)
			logger.LoggerXds.Debugf("Xds Cache is updated for the already existing label : %v", oldLabel)
		}
	}

	for _, newLabel := range newLabels {
		listeners, clusters, routes, endpoints, apis := GenerateEnvoyResoucesForLabel(newLabel)
		UpdateXdsCacheWithLock(newLabel, endpoints, clusters, routes, listeners)
		UpdateEnforcerApis(newLabel, apis)
		logger.LoggerXds.Debugf("Xds Cache is updated for the newly added label : %v", newLabel)
	}
}

// GenerateEnvoyResoucesForLabel generates envoy resources for a given label
func GenerateEnvoyResoucesForLabel(label string) ([]types.Resource, []types.Resource, []types.Resource,
	[]types.Resource, []types.Resource) {
	var clusterArray []*clusterv3.Cluster
	var vhostToRouteArrayMap = make(map[string][]*routev3.Route)
	var endpointArray []*corev3.Address
	var apis []types.Resource

	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}
	port := conf.Envoy.ListenerPort

	for apiKey, labels := range openAPIEnvoyMap {
		if arrayContains(labels, label) {
			vhost := fmt.Sprintf("%v:%v", ExtractVhostFromAPIIdentifier(apiKey), port)
			clusterArray = append(clusterArray, openAPIClustersMap[apiKey]...)
			vhostToRouteArrayMap[vhost] = append(vhostToRouteArrayMap[vhost], openAPIRoutesMap[apiKey]...)
			endpointArray = append(endpointArray, openAPIEndpointsMap[apiKey]...)
			apis = append(apis, openAPIEnforcerApisMap[apiKey])
			// listenerArrays = append(listenerArrays, openAPIListenersMap[apiKey])
		}
	}

	enableJwtIssuer := conf.Enforcer.JwtIssuer.Enabled
	if enableJwtIssuer {
		routeToken := envoyconf.CreateTokenRoute()
		vhostToRouteArrayMap["*"] = append(vhostToRouteArrayMap["*"], routeToken)
	}
	listener, listenerFound := envoyListenerConfigMap[label]
	routesConfig, routesConfigFound := envoyRouteConfigMap[label]
	if !listenerFound && !routesConfigFound {
		listener, routesConfig = oasParser.GetProductionListenerAndRouteConfig(vhostToRouteArrayMap)
		envoyListenerConfigMap[label] = listener
		envoyRouteConfigMap[label] = routesConfig
	} else {
		// If the routesConfig exists, the listener exists too
		oasParser.UpdateRoutesConfig(routesConfig, vhostToRouteArrayMap)
	}
	endpoints, clusters, listeners, routeConfigs := oasParser.GetCacheResources(endpointArray, clusterArray, listener, routesConfig)
	return endpoints, clusters, listeners, routeConfigs, apis
}

//use UpdateXdsCacheWithLock to avoid race conditions
func updateXdsCache(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource, listeners []types.Resource) {
	version := rand.Intn(maxRandomInt)
	// TODO: (VirajSalaka) kept same version for all the resources as we are using simple cache implementation.
	// Will be updated once decide to move to incremental XDS
	snap := envoy_cachev3.NewSnapshot(fmt.Sprint(version), endpoints, clusters, routes, listeners, nil, nil)
	snap.Consistent()
	err := cache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerConfig Sets new update to the enforcer's configuration
func UpdateEnforcerConfig(configFile *config.Config) {
	// TODO: (Praminda) handle labels
	label := commonEnforcerLabel
	configs := []types.Resource{MarshalConfig(configFile)}
	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), configs, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}

	enforcerConfigMap[label] = configs
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApis Sets new update to the enforcer's Apis
func UpdateEnforcerApis(label string, apis []types.Resource) {

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, apis, nil, nil, nil, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}

	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerSubscriptions sets new update to the enforcer's Subscriptions
func UpdateEnforcerSubscriptions(subscriptions *subscription.SubscriptionList) {
	//TODO: (Dinusha) check this hardcoded value
	logger.LoggerXds.Debug("Updating Enforcer Subscription Cache")
	label := commonEnforcerLabel
	subscriptionList := enforcerSubscriptionMap[label]
	subscriptionList = append(subscriptionList, subscriptions)

	// TODO: (VirajSalaka) Decide if a map is required to keep version (just to avoid having the same version)
	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, subscriptionList, nil, nil, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerSubscriptionCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerSubscriptionMap[label] = subscriptionList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApplications sets new update to the enforcer's Applications
func UpdateEnforcerApplications(applications *subscription.ApplicationList) {
	logger.LoggerXds.Debug("Updating Enforcer Application Cache")
	label := commonEnforcerLabel
	applicationList := enforcerApplicationMap[label]
	applicationList = append(applicationList, applications)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, applicationList, nil, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerApplicationCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationMap[label] = applicationList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerAPIList sets new update to the enforcer's Apis
func UpdateEnforcerAPIList(label string, apis *subscription.APIList) {
	logger.LoggerXds.Debug("Updating Enforcer API Cache")
	apiList := enforcerAPIListMap[label]
	apiList = append(apiList, apis)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, apiList, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerAPICache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerAPIListMap[label] = apiList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApplicationPolicies sets new update to the enforcer's Application Policies
func UpdateEnforcerApplicationPolicies(applicationPolicies *subscription.ApplicationPolicyList) {
	logger.LoggerXds.Debug("Updating Enforcer Application Policy Cache")
	label := commonEnforcerLabel
	applicationPolicyList := enforcerApplicationPolicyMap[label]
	applicationPolicyList = append(applicationPolicyList, applicationPolicies)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, applicationPolicyList, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerApplicationPolicyCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationPolicyMap[label] = applicationPolicyList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerSubscriptionPolicies sets new update to the enforcer's Subscription Policies
func UpdateEnforcerSubscriptionPolicies(subscriptionPolicies *subscription.SubscriptionPolicyList) {
	logger.LoggerXds.Debug("Updating Enforcer Subscription Policy Cache")
	label := commonEnforcerLabel
	subscriptionPolicyList := enforcerSubscriptionPolicyMap[label]
	subscriptionPolicyList = append(subscriptionPolicyList, subscriptionPolicies)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, subscriptionPolicyList, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerSubscriptionPolicyCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerSubscriptionPolicyMap[label] = subscriptionPolicyList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApplicationKeyMappings sets new update to the enforcer's Application Key Mappings
func UpdateEnforcerApplicationKeyMappings(applicationKeyMappings *subscription.ApplicationKeyMappingList) {
	logger.LoggerXds.Debug("Updating Application Key Mapping Cache")
	label := commonEnforcerLabel
	applicationKeyMappingList := enforcerApplicationKeyMappingMap[label]
	applicationKeyMappingList = append(applicationKeyMappingList, applicationKeyMappings)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, applicationKeyMappingList, nil, nil, nil)
	snap.Consistent()

	err := enforcerApplicationKeyMappingCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationKeyMappingMap[label] = applicationKeyMappingList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateXdsCacheWithLock uses mutex and lock to avoid different go routines updating XDS at the same time
func UpdateXdsCacheWithLock(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource,
	listeners []types.Resource) {
	mutexForXdsUpdate.Lock()
	defer mutexForXdsUpdate.Unlock()
	updateXdsCache(label, endpoints, clusters, routes, listeners)
}

func startConsulServiceDiscovery() {
	//label := "default"
	for apiKey, clusterList := range openAPIClustersMap {
		for _, cluster := range clusterList {
			if consulSyntax, ok := svcdiscovery.ClusterConsulKeyMap[cluster.Name]; ok {
				svcdiscovery.InitConsul() //initialize consul client and load certs
				query, errConSyn := svcdiscovery.ParseQueryString(consulSyntax)
				if errConSyn != nil {
					logger.LoggerXds.Error("consul syntax parse error ", errConSyn)
					return
				}
				logger.LoggerXds.Debugln(query)
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
				logger.LoggerXds.Debugln("closed the result channel for cluster name: ", clusterName)
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
				listeners, clusters, routes, endpoints, _ := GenerateEnvoyResoucesForLabel(label)
				UpdateXdsCacheWithLock(label, endpoints, clusters, routes, listeners)
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

// ListApis returns a list of objects that holds info about each API
func ListApis(apiType string, limit *int64) *apiModel.APIMeta {
	var limitValue int
	if limit == nil {
		limitValue = len(apiMgwSwaggerMap)
	} else {
		limitValue = int(*limit)
	}
	var apisArray []*apiModel.APIMetaListItem
	i := 0
	for apiIdentifier, mgwSwagger := range apiMgwSwaggerMap {
		if i == limitValue {
			break
		}
		if apiType == "" || mgwSwagger.GetAPIType() == apiType {
			var apiMetaListItem apiModel.APIMetaListItem
			apiMetaListItem.APIName = mgwSwagger.GetTitle()
			apiMetaListItem.Version = mgwSwagger.GetVersion()
			apiMetaListItem.APIType = mgwSwagger.GetAPIType()
			apiMetaListItem.Context = mgwSwagger.GetXWso2Basepath()
			apiMetaListItem.GatewayEnvs = openAPIEnvoyMap[apiIdentifier]
			apisArray = append(apisArray, &apiMetaListItem)
			i++
		}
	}
	var apiMetaObject apiModel.APIMeta
	apiMetaObject.Total = int64(len(apiMgwSwaggerMap))
	apiMetaObject.Count = int64(len(apisArray))
	apiMetaObject.List = apisArray
	return &apiMetaObject
}

// IsAPIExist returns whether a given API exists
func IsAPIExist(vhost, name, version string) (exists bool) {
	apiIdentifier := GenerateIdentifierForAPI(vhost, name, version)
	_, exists = apiMgwSwaggerMap[apiIdentifier]
	return exists
}

// GenerateIdentifierForAPI generates an identifier unique to the API
func GenerateIdentifierForAPI(vhost, name, version string) string {
	return vhost + apiKeyFieldSeparator + name + apiKeyFieldSeparator + version
}

// ExtractVhostFromAPIIdentifier extracts vhost from the API identifier
func ExtractVhostFromAPIIdentifier(id string) string {
	elem := strings.Split(id, apiKeyFieldSeparator)
	if len(elem) == 3 {
		return elem[0]
	}
	// TODO: (renuka) check this to have handle (return error?)
	return "*"
}

// GenerateAndUpdateKeyManagerList converts the data into KeyManager proto type
func GenerateAndUpdateKeyManagerList() {
	var keyManagerConfigList = make([]types.Resource, 0)
	for _, keyManager := range KeyManagerList {
		kmConfig := MarshalKeyManager(&keyManager)
		if kmConfig != nil {
			keyManagerConfigList = append(keyManagerConfigList, kmConfig)
		}
	}
	UpdateEnforcerKeyManagers(keyManagerConfigList)
}

// UpdateEnforcerKeyManagers Sets new update to the enforcer's configuration
func UpdateEnforcerKeyManagers(keyManagerConfigList []types.Resource) {
	logger.LoggerXds.Debug("Updating Key Manager Cache")
	label := commonEnforcerLabel

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, keyManagerConfigList, nil, nil)
	snap.Consistent()

	err := enforcerKeyManagerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerKeyManagerMap[label] = keyManagerConfigList
	logger.LoggerXds.Infof("New key manager cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerRevokedTokens method update the revoked tokens
// in the enforcer
func UpdateEnforcerRevokedTokens(revokedTokens []types.Resource) {
	logger.LoggerXds.Debug("Updating enforcer cache for revoked tokens")
	label := commonEnforcerLabel
	tokens := enforcerRevokedTokensMap[label]
	tokens = append(tokens, revokedTokens...)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, tokens, nil)
	snap.Consistent()

	err := enforcerRevokedTokensCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerRevokedTokensMap[label] = tokens
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerThrottleData update the key template and blocking conditions
// data in the enforcer
func UpdateEnforcerThrottleData(throttleData []types.Resource) {
	logger.LoggerXds.Debug("Updating enforcer cache for throttle data")
	label := commonEnforcerLabel
	data := enforcerThrottleDataMap[label]
	data = append(data, throttleData...)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, data)
	snap.Consistent()

	err := enforcerThrottleDataCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerThrottleDataMap[label] = data
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}
