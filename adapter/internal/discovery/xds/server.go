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
	"strings"
	"sync"
	"time"

	subscription "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/subscription"
	throttle "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/throttle"

	wso2_cache "github.com/wso2/adapter/internal/discovery/protocol/cache/v3"
	"github.com/wso2/adapter/internal/svcdiscovery"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	envoy_cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/wso2/adapter/config"
	apiModel "github.com/wso2/adapter/internal/api/models"
	eventhubTypes "github.com/wso2/adapter/internal/eventhub/types"
	oasParser "github.com/wso2/adapter/internal/oasparser"
	envoyconf "github.com/wso2/adapter/internal/oasparser/envoyconf"
	mgw "github.com/wso2/adapter/internal/oasparser/model"
	"github.com/wso2/adapter/internal/oasparser/operator"
	logger "github.com/wso2/adapter/loggers"
)

var (
	version             int32
	mutexForXdsUpdate   sync.Mutex
	mutexForCacheUpdate sync.Mutex

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

	// Vhosts entry maps, these maps updated with delta changes (when an API added, only added its entry only)
	// These maps are managed separately for API-CTL and APIM, since when deploying an project from API-CTL there is no API uuid
	apiUUIDToGatewayToVhosts map[string]map[string]string   // API_UUID -> gateway-env -> vhost (for un-deploying APIs from APIM or Choreo)
	apiToVhostsMap           map[string]map[string]struct{} // APIName:Version -> VHosts set (for un-deploying APIs from API-CTL)

	// Vhost:APIName:Version as map key
	apiMgwSwaggerMap       map[string]mgw.MgwSwagger       // Vhost:APIName:Version -> MgwSwagger struct map
	openAPIEnvoyMap        map[string][]string             // Vhost:APIName:Version -> Envoy Label Array map
	openAPIRoutesMap       map[string][]*routev3.Route     // Vhost:APIName:Version -> Envoy Routes map
	openAPIClustersMap     map[string][]*clusterv3.Cluster // Vhost:APIName:Version -> Envoy Clusters map
	openAPIEndpointsMap    map[string][]*corev3.Address    // Vhost:APIName:Version -> Envoy Endpoints map
	openAPIEnforcerApisMap map[string]types.Resource       // Vhost:APIName:Version -> API Resource map

	// Envoy Label as map key
	envoyUpdateVersionMap  map[string]int64                       // GW-Label -> XDS version map
	envoyListenerConfigMap map[string][]*listenerv3.Listener      // GW-Label -> Listener Configuration map
	envoyRouteConfigMap    map[string]*routev3.RouteConfiguration // GW-Label -> Routes Configuration map

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
	enforcerThrottleData             *throttle.ThrottleData

	// KeyManagerList to store data
	KeyManagerList = make([]eventhubTypes.KeyManager, 0)
)

var void struct{}

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

	apiUUIDToGatewayToVhosts = make(map[string]map[string]string)
	apiToVhostsMap = make(map[string]map[string]struct{})
	apiMgwSwaggerMap = make(map[string]mgw.MgwSwagger)
	openAPIEnvoyMap = make(map[string][]string)
	openAPIEnforcerApisMap = make(map[string]types.Resource)
	openAPIRoutesMap = make(map[string][]*routev3.Route)
	openAPIClustersMap = make(map[string][]*clusterv3.Cluster)
	openAPIEndpointsMap = make(map[string][]*corev3.Address)
	//TODO: (VirajSalaka) Swagger or project should contain the version as a meta information
	envoyUpdateVersionMap = make(map[string]int64)
	envoyListenerConfigMap = make(map[string][]*listenerv3.Listener)
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
	enforcerThrottleData = &throttle.ThrottleData{}
	rand.Seed(time.Now().UnixNano())
	go watchEnforcerResponse()
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
		apiContent.Environments = []string{config.DefaultGatewayName}
	}

	//TODO: (VirajSalaka) Optimize locking
	var l sync.Mutex
	l.Lock()
	defer l.Unlock()

	if apiContent.APIType == mgw.HTTP {
		mgwSwagger = operator.GetMgwSwagger(apiContent.APIDefinition)
		mgwSwagger.SetID(apiContent.UUID)
		mgwSwagger.SetName(apiContent.Name)
		mgwSwagger.SetVersion(apiContent.Version)
		mgwSwagger.SetSecurityScheme(apiContent.SecurityScheme)
		mgwSwagger.SetXWso2AuthHeader(apiContent.AuthHeader)
		mgwSwagger.OrganizationID = apiContent.OrganizationID
	} else if apiContent.APIType == mgw.WS {
		mgwSwagger = operator.GetMgwSwaggerWebSocket(apiContent.APIDefinition)
		mgwSwagger.OrganizationID = apiContent.OrganizationID
	} else {
		// Unreachable else condition. Added in case previous apiType check fails due to any modifications.
		logger.LoggerXds.Error("API type not currently supported with WSO2 Microgateway")
	}

	if (len(mgwSwagger.GetProdEndpoints()) == 0 || mgwSwagger.GetProdEndpoints()[0].Host == "/") &&
		(len(mgwSwagger.GetSandEndpoints()) == 0 || mgwSwagger.GetSandEndpoints()[0].Host == "/") {
		mgwSwagger.SetXWso2ProductionEndpointMgwSwagger(apiContent.ProductionEndpoint)
		mgwSwagger.SetXWso2SandboxEndpointForMgwSwagger(apiContent.SandboxEndpoint)
	}

	validationErr := mgwSwagger.Validate()
	if validationErr != nil {
		logger.LoggerOasparser.Errorf("Validation failed for the API. %s:%s", mgwSwagger.GetTitle(), mgwSwagger.GetVersion())
		return
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
	updateVhostInternalMaps(apiContent, newLabels)

	routes, clusters, endpoints := oasParser.GetProductionRoutesClustersEndpoints(mgwSwagger, apiContent.UpstreamCerts,
		apiContent.VHost)
	// TODO: (VirajSalaka) Decide if the routes and listeners need their own map since it is not going to be changed based on API at the moment.
	openAPIRoutesMap[apiIdentifier] = routes
	// openAPIListenersMap[apiMapKey] = listeners
	openAPIClustersMap[apiIdentifier] = clusters
	openAPIEndpointsMap[apiIdentifier] = endpoints
	openAPIEnforcerApisMap[apiIdentifier] = oasParser.GetEnforcerAPI(mgwSwagger, apiContent.LifeCycleStatus,
		apiContent.EndpointSecurity, apiContent.VHost)
	// TODO: (VirajSalaka) Fault tolerance mechanism implementation
	updateXdsCacheOnAPIAdd(oldLabels, newLabels)
	if svcdiscovery.IsServiceDiscoveryEnabled {
		startConsulServiceDiscovery() //consul service discovery starting point
	}
}

// GetVhostOfAPI returns the vhost of API deployed in the given gateway environment
func GetVhostOfAPI(apiUUID, environment string) (vhost string, exists bool) {
	if envToVhost, ok := apiUUIDToGatewayToVhosts[apiUUID]; ok {
		vhost, exists = envToVhost[environment]
		return
	}
	return "", false
}

// DeleteAPIs deletes an API, its resources and updates the caches of given environments
func DeleteAPIs(vhost, apiName, version string, environments []string) error {
	apiNameVersionID := GenerateIdentifierForAPIWithoutVhost(apiName, version)
	vhosts, found := apiToVhostsMap[apiNameVersionID]
	if !found {
		logger.LoggerXds.Infof("Unable to delete API %v. API does not exist.", apiNameVersionID)
		return errors.New(mgw.NotFound)
	}

	if vhost == "" {
		// vhost is not defined, delete all vhosts
		logger.LoggerXds.Infof("No vhost is specified for the API %v deleting from all vhosts", apiNameVersionID)
		deletedVhosts := make(map[string]struct{})
		for vh := range vhosts {
			apiIdentifier := GenerateIdentifierForAPI(vh, apiName, version)
			// TODO: (renuka) optimize to update cache only once after updating all maps
			if err := deleteAPI(apiIdentifier, environments); err != nil {
				// Update apiToVhostsMap with already deleted vhosts in the loop
				logger.LoggerXds.Errorf("Error deleting API: %v", apiIdentifier)
				logger.LoggerXds.Debugf("Update map apiToVhostsMap with deleting already deleted vhosts for API %v",
					apiIdentifier)
				remainingVhosts := make(map[string]struct{})
				for v := range vhosts {
					if _, ok := deletedVhosts[v]; ok {
						remainingVhosts[v] = void
					}
				}
				apiToVhostsMap[apiNameVersionID] = remainingVhosts
				return err
			}
			deletedVhosts[vh] = void
		}
		delete(apiToVhostsMap, apiNameVersionID)
		return nil
	}

	apiIdentifier := GenerateIdentifierForAPI(vhost, apiName, version)
	if err := deleteAPI(apiIdentifier, environments); err != nil {
		return err
	}

	if _, ok := vhosts[vhost]; ok {
		if len(vhosts) == 1 {
			// if this is the final vhost delete map entry
			logger.LoggerXds.Debugf("The API %v is not exists with any vhost. Hence clean vhost entry from the map 'apiToVhostsMap'",
				apiNameVersionID)
			delete(apiToVhostsMap, apiNameVersionID)
		} else {
			delete(apiToVhostsMap[apiNameVersionID], vhost)
		}
	}
	return nil
}

// DeleteAPIWithAPIMEvent deletes API with the given UUID from the given gw environments
func DeleteAPIWithAPIMEvent(uuid, name, version string, environments []string) {
	apiIdentifiers := make(map[string]struct{})
	for gw, vhost := range apiUUIDToGatewayToVhosts[uuid] {
		// delete from only specified environments
		if arrayContains(environments, gw) {
			id := GenerateIdentifierForAPI(vhost, name, version)
			apiIdentifiers[id] = void
		}
	}
	for apiIdentifier := range apiIdentifiers {
		if err := deleteAPI(apiIdentifier, environments); err != nil {
			logger.LoggerXds.Errorf("Error undeploying API %v from environments %v", apiIdentifier, environments)
		} else {
			// if no error, update internal vhost maps
			// error only happens when API not found in deleteAPI func
			logger.LoggerXds.Debugf("Successfully undeployed API %v from environments %v", apiIdentifier, environments)
			for _, environment := range environments {
				// delete environment if exists
				delete(apiUUIDToGatewayToVhosts[uuid], environment)
			}
		}
	}
}

// deleteAPI deletes an API, its resources and updates the caches of given environments
func deleteAPI(apiIdentifier string, environments []string) error {
	_, exists := apiMgwSwaggerMap[apiIdentifier]
	if !exists {
		logger.LoggerXds.Infof("Unable to delete API " + apiIdentifier + ". Does not exist.")
		return errors.New(mgw.NotFound)
	}

	existingLabels := openAPIEnvoyMap[apiIdentifier]
	toBeDelEnvs, toBeKeptEnvs := getEnvironmentsToBeDeleted(existingLabels, environments)

	if len(existingLabels) != len(toBeDelEnvs) {
		// do not delete from all environments, hence do not clear routes, clusters, endpoints, enforcerAPIs
		updateXdsCacheOnAPIAdd(toBeDelEnvs, []string{})
		logger.LoggerXds.Infof("Deleted API. %v", apiIdentifier)
		openAPIEnvoyMap[apiIdentifier] = toBeKeptEnvs
		return nil
	}

	//clean maps of routes, clusters, endpoints, enforcerAPIs
	delete(openAPIRoutesMap, apiIdentifier)
	delete(openAPIClustersMap, apiIdentifier)
	delete(openAPIEndpointsMap, apiIdentifier)
	delete(openAPIEnforcerApisMap, apiIdentifier)

	//updateXdsCacheOnAPIAdd is called after cleaning maps of routes, clusters, endpoints, enforcerAPIs.
	//Therefore resources that belongs to the deleting API do not exist. Caches updated only with
	//resources that belongs to the remaining APIs
	updateXdsCacheOnAPIAdd(toBeDelEnvs, []string{})

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

	for _, newLabel := range newLabels {
		_, _, _, _, apis := GenerateEnvoyResoucesForLabel(newLabel)
		UpdateEnforcerApis(newLabel, apis, "")
		logger.LoggerXds.Debugf("Xds Cache is updated for the newly added label : %v", newLabel)
	}
	for _, oldLabel := range oldLabels {
		if !arrayContains(newLabels, oldLabel) {
			_, _, _, _, apis := GenerateEnvoyResoucesForLabel(oldLabel)
			UpdateEnforcerApis(oldLabel, apis, "")
			logger.LoggerXds.Debugf("Xds Cache is updated for the already existing label : %v", oldLabel)
		}
	}

}

// GenerateEnvoyResoucesForLabel generates envoy resources for a given label
func GenerateEnvoyResoucesForLabel(label string) ([]types.Resource, []types.Resource, []types.Resource,
	[]types.Resource, []types.Resource) {
	var clusterArray []*clusterv3.Cluster
	var vhostToRouteArrayMap = make(map[string][]*routev3.Route)
	var endpointArray []*corev3.Address
	var apis []types.Resource

	for apiKey, labels := range openAPIEnvoyMap {
		if arrayContains(labels, label) {
			vhost, err := ExtractVhostFromAPIIdentifier(apiKey)
			if err != nil {
				logger.LoggerXds.Errorf("Error extracting vhost from API identifier: %v. Ignore deploying the API",
					err.Error())
				continue
			}
			clusterArray = append(clusterArray, openAPIClustersMap[apiKey]...)
			vhostToRouteArrayMap[vhost] = append(vhostToRouteArrayMap[vhost], openAPIRoutesMap[apiKey]...)
			endpointArray = append(endpointArray, openAPIEndpointsMap[apiKey]...)
			enfocerAPI, ok := openAPIEnforcerApisMap[apiKey]
			if ok {
				apis = append(apis, enfocerAPI)
			}
			// listenerArrays = append(listenerArrays, openAPIListenersMap[apiKey])
		}
	}

	// If the token endpoint is enabled, the token endpoint also needs to be added.
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}
	enableJwtIssuer := conf.Enforcer.JwtIssuer.Enabled
	systemHost := conf.Envoy.SystemHost
	if enableJwtIssuer {
		routeToken := envoyconf.CreateTokenRoute()
		vhostToRouteArrayMap[systemHost] = append(vhostToRouteArrayMap[systemHost], routeToken)
	}

	// Add health endpoint
	routeHealth := envoyconf.CreateHealthEndpoint()
	vhostToRouteArrayMap[systemHost] = append(vhostToRouteArrayMap[systemHost], routeHealth)

	listenerArray, listenerFound := envoyListenerConfigMap[label]
	routesConfig, routesConfigFound := envoyRouteConfigMap[label]
	if !listenerFound && !routesConfigFound {
		listenerArray, routesConfig = oasParser.GetProductionListenerAndRouteConfig(vhostToRouteArrayMap)
		envoyListenerConfigMap[label] = listenerArray
		envoyRouteConfigMap[label] = routesConfig
	} else {
		// If the routesConfig exists, the listener exists too
		oasParser.UpdateRoutesConfig(routesConfig, vhostToRouteArrayMap)
	}
	endpoints, clusters, listeners, routeConfigs := oasParser.GetCacheResources(endpointArray, clusterArray, listenerArray, routesConfig)
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
	logger.LoggerXds.Infof("New Router cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New Config cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApis Sets new update to the enforcer's Apis
func UpdateEnforcerApis(label string, apis []types.Resource, version string) {

	if version == "" {
		version = fmt.Sprint(rand.Intn(maxRandomInt))
	}

	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, apis, nil, nil, nil, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	logger.LoggerXds.Infof("New API cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New Subscription cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New Application cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New API List cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New Application Policy cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New Subscription Policy cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
	logger.LoggerXds.Infof("New Application Key Mapping cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateXdsCacheWithLock uses mutex and lock to avoid different go routines updating XDS at the same time
func UpdateXdsCacheWithLock(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource,
	listeners []types.Resource) {
	mutexForXdsUpdate.Lock()
	defer mutexForXdsUpdate.Unlock()
	updateXdsCache(label, endpoints, clusters, routes, listeners)
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
			vhost := "ERROR"
			if vh, err := ExtractVhostFromAPIIdentifier(apiIdentifier); err == nil {
				vhost = vh
			}
			apiMetaListItem.Vhost = vhost
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
	return fmt.Sprint(vhost, apiKeyFieldSeparator, name, apiKeyFieldSeparator, version)
}

// GenerateIdentifierForAPIWithoutVhost generates an identifier unique to the API name and version
func GenerateIdentifierForAPIWithoutVhost(name, version string) string {
	return fmt.Sprint(name, apiKeyFieldSeparator, version)
}

// ExtractVhostFromAPIIdentifier extracts vhost from the API identifier
func ExtractVhostFromAPIIdentifier(id string) (string, error) {
	elem := strings.Split(id, apiKeyFieldSeparator)
	if len(elem) == 3 {
		return elem[0], nil
	}
	err := fmt.Errorf("invalid API identifier: %v", id)
	return "", err
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
	logger.LoggerXds.Infof("New Revoked token cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerThrottleData update the key template and blocking conditions
// data in the enforcer
func UpdateEnforcerThrottleData(throttleData *throttle.ThrottleData) {
	logger.LoggerXds.Debug("Updating enforcer cache for throttle data")
	label := commonEnforcerLabel
	var data []types.Resource

	// Set new throttle data content based on the already available content in the cache DTO
	// and the new data being requested to add.
	// ex: keytemplates being pressent in the `throttleData` means this method was called
	// after downloading key templates. That means we should populate keytemplates property
	// in the cache DTO, keeping the other properties as it is. This is done this way to avoid
	// the need of two xds services to push keytemplates and blocking conditions.
	templates := throttleData.KeyTemplates
	conditions := throttleData.BlockingConditions
	ipConditions := throttleData.IpBlockingConditions
	if templates == nil {
		templates = enforcerThrottleData.KeyTemplates
	}
	if conditions == nil {
		conditions = enforcerThrottleData.BlockingConditions
	}
	if ipConditions == nil {
		ipConditions = enforcerThrottleData.IpBlockingConditions
	}

	t := &throttle.ThrottleData{
		KeyTemplates:         templates,
		BlockingConditions:   conditions,
		IpBlockingConditions: ipConditions,
	}
	data = append(data, t)

	version := rand.Intn(maxRandomInt)
	snap := wso2_cache.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, data)
	snap.Consistent()

	err := enforcerThrottleDataCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerThrottleData = t
	logger.LoggerXds.Infof("New Throttle Data cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// buildAndStoreSuccessState method builds the latest successful enforcer and router resources and returns as
// state structs.
func buildAndStoreSuccessState(label string, version string) {
	// Copy the current status maps to new maps, since assigning maps is by reference.

	// Enforcer resources
	mutexForCacheUpdate.Lock()
	defer mutexForCacheUpdate.Unlock()
	newAPIMgwSwaggerMap := make(map[string]mgw.MgwSwagger)
	for k, v := range apiMgwSwaggerMap {
		newAPIMgwSwaggerMap[k] = v
	}
	newOpenAPIEnforcerApisMap := make(map[string]types.Resource)
	for k, v := range openAPIEnforcerApisMap {
		newOpenAPIEnforcerApisMap[k] = v
	}
	newAPIToVhostsMap := make(map[string]map[string]struct{})
	for k, v := range apiToVhostsMap {
		intermediate := make(map[string]struct{})
		for k1, v1 := range v {
			intermediate[k1] = v1
		}
		newAPIToVhostsMap[k] = intermediate
	}
	newAPIUUIDToGatewayToVhosts := make(map[string]map[string]string)
	for k, v := range apiUUIDToGatewayToVhosts {
		intermediate := make(map[string]string)
		for k1, v1 := range v {
			intermediate[k1] = v1
		}
		newAPIUUIDToGatewayToVhosts[k] = intermediate
	}

	// Router resources
	newOpenAPIRoutesMap := make(map[string][]*routev3.Route)
	for k, v := range openAPIRoutesMap {
		newOpenAPIRoutesMap[k] = v
	}
	newOpenAPIClustersMap := make(map[string][]*clusterv3.Cluster)
	for k, v := range openAPIClustersMap {
		newOpenAPIClustersMap[k] = v
	}
	newOpenAPIEndpointsMap := make(map[string][]*corev3.Address)
	for k, v := range openAPIEndpointsMap {
		newOpenAPIEndpointsMap[k] = v
	}
	newEnvoyListenerConfigMap := make(map[string][]*listenerv3.Listener)
	for k, v := range envoyListenerConfigMap {
		newEnvoyListenerConfigMap[k] = v
	}
	newEnvoyRouteConfigMap := make(map[string]*routev3.RouteConfiguration)
	for k, v := range envoyRouteConfigMap {
		newEnvoyRouteConfigMap[k] = v
	}
	enforcerNewState := EnforcerAPIState{
		Apis:                     newAPIMgwSwaggerMap,
		OpenAPIEnforcerApisMap:   newOpenAPIEnforcerApisMap,
		APIToVhostsMap:           newAPIToVhostsMap,
		APIUUIDToGatewayToVhosts: newAPIUUIDToGatewayToVhosts,
		Version:                  version,
	}
	routerNewState := RouterResourceState{
		APIRoutesMap:           newOpenAPIRoutesMap,
		APIClustersMap:         newOpenAPIClustersMap,
		APIEndpointsMap:        newOpenAPIEndpointsMap,
		EnvoyListenerConfigMap: newEnvoyListenerConfigMap,
		EnvoyRouteConfigMap:    newEnvoyRouteConfigMap,
		Version:                version,
	}
	SetSuccessState(label, enforcerNewState, routerNewState)
}

// restorePreviousState retrive the last successful state from the state cache and restore the date to the maps.
func restorePreviousState(label string) string {
	// Get the last known successful state of enforcer and router from the state cache
	enforcerOldState, routerOldState := GetLastSuccessState(label)
	// Initialize new maps and populate the success state from the retrieved state cache.

	// Enforcer resources
	apiMgwSwaggerMap = enforcerOldState.Apis
	openAPIEnforcerApisMap = enforcerOldState.OpenAPIEnforcerApisMap
	apiToVhostsMap = enforcerOldState.APIToVhostsMap
	apiUUIDToGatewayToVhosts = enforcerOldState.APIUUIDToGatewayToVhosts

	// Router resources
	openAPIRoutesMap = routerOldState.APIRoutesMap
	openAPIClustersMap = routerOldState.APIClustersMap
	openAPIEndpointsMap = routerOldState.APIEndpointsMap
	envoyListenerConfigMap = routerOldState.EnvoyListenerConfigMap
	envoyRouteConfigMap = routerOldState.EnvoyRouteConfigMap
	return enforcerOldState.Version
}

/** This method will listen to the callback messages from the xds protocol.
* A message will be published for each xds client request (for API resources only)
* Based on the message (error/ success) the current state of the resources will be changed.
* If failure ==> restore the last success state
* If success ==> store the current state as the last success state
* Flow -->
* 1. Update the enforcer
* 2. Listen for the discovery request from client.
* 3. If success, set the current state as the success state. Otherwise, restore the previous state.
* 4. Update the router
**/
func watchEnforcerResponse() {
	for {
		requestEvent := <-GetRequestEventChannel()
		logger.LoggerXds.Debugf("xds Request from client version : %s", requestEvent.Version)
		if requestEvent.IsError {
			logger.LoggerXds.Infof("Applying config failed. Last success version of enforcer : %s", requestEvent.Version)
			lastSuccessVersion := restorePreviousState(requestEvent.Node)
			_, _, _, _, apis := GenerateEnvoyResoucesForLabel(requestEvent.Node)
			UpdateEnforcerApis(requestEvent.Node, apis, lastSuccessVersion)
		} else {
			logger.LoggerXds.Infof("Successfully updated APIs in Client for version: %s", requestEvent.Version)

			// Successful message, set the current state of the enforcer apis to the state cache.
			buildAndStoreSuccessState(requestEvent.Node, requestEvent.Version)

			// Generate the router resources and update the router.
			listeners, clusters, routes, endpoints, _ := GenerateEnvoyResoucesForLabel(requestEvent.Node)
			UpdateXdsCacheWithLock(requestEvent.Node, endpoints, clusters, routes, listeners)
		}
	}
}
