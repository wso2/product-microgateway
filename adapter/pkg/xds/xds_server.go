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
	"github.com/envoyproxy/go-control-plane/wso2/discovery/subscription"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	oasParser "github.com/wso2/micro-gw/pkg/oasparser"
	mgw "github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/operator"
	resourceTypes "github.com/wso2/micro-gw/pkg/resourcetypes"
)

var (
	version           int32
	mutexForXdsUpdate sync.Mutex

	cache                              cachev3.SnapshotCache
	enforcerCache                      cachev3.SnapshotCache
	enforcerSubscriptionCache          cachev3.SnapshotCache
	enforcerApplicationCache           cachev3.SnapshotCache
	enforcerAPICache                   cachev3.SnapshotCache
	enforcerApplicationPolicyCache     cachev3.SnapshotCache
	enforcerSubscriptionPolicyCache    cachev3.SnapshotCache
	enforcerApplicationKeyMappingCache cachev3.SnapshotCache

	// API Name:Version -> MgwSwagger struct map
	apiMgwSwaggerMap map[string]mgw.MgwSwagger
	// API Name:Version -> Envoy Label Array map
	openAPIEnvoyMap map[string][]string
	// API Name:Version -> Envoy Routes map
	openAPIRoutesMap map[string][]*routev3.Route
	// API Name:Version -> Envoy Clusters map
	openAPIClustersMap map[string][]*clusterv3.Cluster
	// API Name:Version -> Envoy Endpoints map
	openAPIEndpointsMap map[string][]*corev3.Address
	// Envoy Label -> XDS version map
	envoyUpdateVersionMap map[string]int64
	// Envoy Label -> Listener Configuration map
	envoyListenerConfigMap map[string]*listenerv3.Listener
	// Envoy Label -> Routes Configuration map
	envoyRouteConfigMap map[string]*routev3.RouteConfiguration

	// Enforcer XDS resource version map
	enforcerCacheVersionMap map[string]int64

	// Enforcer Subscription related resource version maps
	enforcerSubscriptionCacheVersionMap          map[string]int64
	enforcerApplicationCacheVersionMap           map[string]int64
	enforcerAPICacheVersionMap                   map[string]int64
	enforcerApplicationPolicyCacheVersionMap     map[string]int64
	enforcerSubscriptionPolicyCacheVersionMap    map[string]int64
	enforcerApplicationKeyMappingCacheVersionMap map[string]int64

	// Enforcer API XDS resource version map
	enforcerAPIVersionMap map[string]int64
	enforcerApisMap       map[string][]types.Resource
	enforcerConfigMap     map[string][]types.Resource

	enforcerSubscriptionMap          map[string][]types.Resource
	enforcerApplicationMap           map[string][]types.Resource
	enforcerAPIListMap               map[string][]types.Resource
	enforcerApplicationPolicyMap     map[string][]types.Resource
	enforcerSubscriptionPolicyMap    map[string][]types.Resource
	enforcerApplicationKeyMappingMap map[string][]types.Resource
)

const (
	commonEnforcerLabel string = "commonEnforcerLabel"
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
	enforcerSubscriptionCache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	enforcerApplicationCache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	enforcerAPICache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	enforcerApplicationPolicyCache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	enforcerSubscriptionPolicyCache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	enforcerApplicationKeyMappingCache = cachev3.NewSnapshotCache(false, IDHash{}, nil)
	apiMgwSwaggerMap = make(map[string]mgw.MgwSwagger)
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
	enforcerSubscriptionCacheVersionMap = make(map[string]int64)
	enforcerApplicationCacheVersionMap = make(map[string]int64)
	enforcerAPICacheVersionMap = make(map[string]int64)
	enforcerApplicationPolicyCacheVersionMap = make(map[string]int64)
	enforcerSubscriptionPolicyCacheVersionMap = make(map[string]int64)
	enforcerApplicationKeyMappingCacheVersionMap = make(map[string]int64)
	enforcerApisMap = make(map[string][]types.Resource)
	enforcerSubscriptionMap = make(map[string][]types.Resource)
	enforcerApplicationMap = make(map[string][]types.Resource)
	enforcerAPIListMap = make(map[string][]types.Resource)
	enforcerApplicationPolicyMap = make(map[string][]types.Resource)
	enforcerSubscriptionPolicyMap = make(map[string][]types.Resource)
	enforcerApplicationKeyMappingMap = make(map[string][]types.Resource)
}

// GetXdsCache returns xds server cache.
func GetXdsCache() cachev3.SnapshotCache {
	return cache
}

// GetEnforcerCache returns xds server cache.
func GetEnforcerCache() cachev3.SnapshotCache {
	return enforcerCache
}

// GetEnforcerSubscriptionCache returns xds server cache.
func GetEnforcerSubscriptionCache() cachev3.SnapshotCache {
	return enforcerSubscriptionCache
}

// GetEnforcerApplicationCache returns xds server cache.
func GetEnforcerApplicationCache() cachev3.SnapshotCache {
	return enforcerApplicationCache
}

// GetEnforcerAPICache returns xds server cache.
func GetEnforcerAPICache() cachev3.SnapshotCache {
	return enforcerAPICache
}

// GetEnforcerApplicationPolicyCache returns xds server cache.
func GetEnforcerApplicationPolicyCache() cachev3.SnapshotCache {
	return enforcerApplicationPolicyCache
}

// GetEnforcerSubscriptionPolicyCache returns xds server cache.
func GetEnforcerSubscriptionPolicyCache() cachev3.SnapshotCache {
	return enforcerSubscriptionPolicyCache
}

// GetEnforcerApplicationKeyMappingCache returns xds server cache.
func GetEnforcerApplicationKeyMappingCache() cachev3.SnapshotCache {
	return enforcerApplicationKeyMappingCache
}

// UpdateAPI updates the Xds Cache when OpenAPI Json content is provided
func UpdateAPI(byteArr []byte, upstreamCerts []byte, apiType string, environments []string) {
	var apiMapKey string
	var newLabels []string
	var mgwSwagger mgw.MgwSwagger
	vhost := "default" //TODO: (SuKSW) update once vhost feature added

	//TODO: (VirajSalaka) Optimize locking
	var l sync.Mutex
	l.Lock()
	defer l.Unlock()

	if apiType == mgw.HTTP {
		mgwSwagger = operator.GetMgwSwagger(byteArr)
	} else if apiType == mgw.WS {
		mgwSwagger = operator.GetMgwSwaggerWebSocket(byteArr)
	} else {
		// Unreachable else condition. Added in case previous apiType check fails due to any modifications.
		logger.LoggerXds.Error("API type not currently supported with WSO2 Microgateway")
	}
	apiMapKey = vhost + ":" + mgwSwagger.GetTitle() + ":" + mgwSwagger.GetVersion()
	existingMgwSwagger, exists := apiMgwSwaggerMap[apiMapKey]
	if exists {
		if reflect.DeepEqual(mgwSwagger, existingMgwSwagger) {
			logger.LoggerXds.Infof("API %v already exists. No changes to apply.", apiMapKey)
			return
		}
	}
	apiMgwSwaggerMap[apiMapKey] = mgwSwagger
	//TODO: (VirajSalaka) Handle OpenAPIs which does not have label (Current Impl , it will be labelled as default)
	// TODO: commented the following line as the implementation is not supported yet.
	//newLabels = model.GetXWso2Label(openAPIV3Struct.ExtensionProps)
	//:TODO: since currently labels are not taking from x-wso2-label, I have made it to be taken from the method
	// argument.
	newLabels = environments
	logger.LoggerXds.Infof("Added/Updated the content under OpenAPI Key : %v", apiMapKey)
	logger.LoggerXds.Debugf("Newly added labels for the OpenAPI Key : %v are %v", apiMapKey, newLabels)
	oldLabels, _ := openAPIEnvoyMap[apiMapKey]
	logger.LoggerXds.Debugf("Already existing labels for the OpenAPI Key : %v are %v", apiMapKey, oldLabels)
	openAPIEnvoyMap[apiMapKey] = newLabels

	routes, clusters, endpoints := oasParser.GetProductionRoutesClustersEndpoints(mgwSwagger, upstreamCerts)
	enforcerAPI := oasParser.GetEnforcerAPI(mgwSwagger)
	// TODO: (VirajSalaka) Decide if the routes and listeners need their own map since it is not going to be changed based on API at the moment.
	openAPIRoutesMap[apiMapKey] = routes
	// openAPIListenersMap[apiMapKey] = listeners
	openAPIClustersMap[apiMapKey] = clusters
	openAPIEndpointsMap[apiMapKey] = endpoints
	// TODO: (VirajSalaka) Fault tolerance mechanism implementation
	updateXdsCacheOnAPIAdd(oldLabels, newLabels)
	UpdateEnforcerApis(enforcerAPI)
	if svcdiscovery.IsServiceDiscoveryEnabled {
		startConsulServiceDiscovery() //consul service discovery starting point
	}
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
			CertificateFilePath:  issuer.CertificateFilePath,
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
		ApimCredentials: &enforcer.AmCredentials{
			Username: config.Enforcer.ApimCredentials.Username,
			Password: config.Enforcer.ApimCredentials.Password,
		},
		AuthService:    authService,
		JwtTokenConfig: issuers,
		Eventhub: &enforcer.EventHub{
			Enabled:    config.ControlPlane.EventHub.Enabled,
			ServiceUrl: config.ControlPlane.EventHub.ServiceURL,
			JmsConnectionParameters: map[string]string{
				"eventListeningEndpoints": config.ControlPlane.EventHub.JmsConnectionParameters.EventListeningEndpoints,
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
	snap := cachev3.NewSnapshot(fmt.Sprint(version), endpoints, clusters, routes, listeners, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil)
	snap.Consistent()
	err := cache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	envoyUpdateVersionMap[label] = version
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerConfig Sets new update to the enforcer's configuration
func UpdateEnforcerConfig(configFile *config.Config) {
	// TODO: (Praminda) handle labels
	label := commonEnforcerLabel
	configs := []types.Resource{generateEnforcerConfigs(configFile)}
	version, ok := enforcerCacheVersionMap[label]
	if ok {
		version++
	} else {
		version = 1
	}

	apis := enforcerApisMap[label]

	snap := cachev3.NewSnapshot(
		fmt.Sprint(version), nil, nil, nil, nil, nil, nil, configs, apis, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}

	enforcerCacheVersionMap[label] = version
	enforcerConfigMap[label] = configs
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
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

	snap := cachev3.NewSnapshot(
		fmt.Sprint(version), nil, nil, nil, nil, nil, nil, configs, apis, nil, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}

	enforcerCacheVersionMap[label] = version
	enforcerApisMap[label] = apis
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// GenerateSubscriptionList converts the data into SubscriptionList proto type
func GenerateSubscriptionList(subList *resourceTypes.SubscriptionList) *subscription.SubscriptionList {
	subscriptions := []*subscription.Subscription{}

	for _, sb := range subList.List {
		sub := &subscription.Subscription{
			SubscriptionId:    fmt.Sprint(sb.SubscriptionID),
			PolicyId:          sb.PolicyID,
			ApiId:             sb.APIID,
			AppId:             sb.AppID,
			SubscriptionState: sb.SubscriptionState,
			TimeStamp:         sb.TimeStamp,
			TenantId:          sb.TenantID,
			TenantDomain:      sb.TenantDomain,
		}
		subscriptions = append(subscriptions, sub)
	}

	return &subscription.SubscriptionList{
		List: subscriptions,
	}
}

// GenerateApplicationList converts the data into ApplicationList proto type
func GenerateApplicationList(appList *resourceTypes.ApplicationList) *subscription.ApplicationList {
	applications := []*subscription.Application{}

	for _, app := range appList.List {
		application := &subscription.Application{
			Uuid:         app.UUID,
			Id:           app.ID,
			Name:         app.Name,
			SubId:        app.ID,
			SubName:      app.SubName,
			Policy:       app.Policy,
			TokenType:    app.TokenType,
			GroupIds:     app.GroupIds,
			Attributes:   app.Attributes,
			TenantId:     app.TenantID,
			TenantDomain: app.TenantDomain,
			Timestamp:    app.TimeStamp,
		}
		applications = append(applications, application)
	}

	return &subscription.ApplicationList{
		List: applications,
	}
}

// GenerateAPIList converts the data into APIList proto type
func GenerateAPIList(apiList *resourceTypes.APIList) *subscription.APIList {
	apis := []*subscription.APIs{}

	for _, api := range apiList.List {
		newAPI := &subscription.APIs{
			ApiId:            api.APIID,
			Name:             api.Name,
			Provider:         api.Provider,
			Version:          api.Version,
			Context:          api.Context,
			Policy:           api.Policy,
			ApiType:          api.APIType,
			IsDefaultVersion: api.IsDefaultVersion,
		}
		apis = append(apis, newAPI)
	}

	return &subscription.APIList{
		List: apis,
	}
}

// GenerateApplicationPolicyList converts the data into ApplicationPolicyList proto type
func GenerateApplicationPolicyList(appPolicyList *resourceTypes.ApplicationPolicyList) *subscription.ApplicationPolicyList {
	applicationPolicies := []*subscription.ApplicationPolicy{}

	for _, policy := range appPolicyList.List {
		appPolicy := &subscription.ApplicationPolicy{
			Id:        policy.ID,
			TenantId:  policy.TenantID,
			Name:      policy.Name,
			QuotaType: policy.QuotaType,
		}
		applicationPolicies = append(applicationPolicies, appPolicy)
	}

	return &subscription.ApplicationPolicyList{
		List: applicationPolicies,
	}
}

// GenerateSubscriptionPolicyList converts the data into SubscriptionPolicyList proto type
func GenerateSubscriptionPolicyList(subPolicyList *resourceTypes.SubscriptionPolicyList) *subscription.SubscriptionPolicyList {
	subscriptionPolicies := []*subscription.SubscriptionPolicy{}

	for _, policy := range subPolicyList.List {
		subPolicy := &subscription.SubscriptionPolicy{
			Id:                   policy.ID,
			Name:                 policy.Name,
			QuotaType:            policy.QuotaType,
			GraphQLMaxComplexity: policy.GraphQLMaxComplexity,
			GraphQLMaxDepth:      policy.GraphQLMaxDepth,
			RateLimitCount:       policy.RateLimitCount,
			RateLimitTimeUnit:    policy.RateLimitTimeUnit,
			StopOnQuotaReach:     policy.StopOnQuotaReach,
			TenantId:             policy.TenantID,
			TenantDomain:         policy.TenantDomain,
			Timestamp:            policy.TimeStamp,
		}
		subscriptionPolicies = append(subscriptionPolicies, subPolicy)
	}

	return &subscription.SubscriptionPolicyList{
		List: subscriptionPolicies,
	}
}

// GenerateApplicationKeyMappingList converts the data into ApplicationKeyMappingList proto type
func GenerateApplicationKeyMappingList(keyMappingList *resourceTypes.ApplicationKeyMappingList) *subscription.ApplicationKeyMappingList {
	applicationKeyMappings := []*subscription.ApplicationKeyMapping{}

	for _, mapping := range keyMappingList.List {
		keyMapping := &subscription.ApplicationKeyMapping{
			ConsumerKey:   mapping.ConsumerKey,
			KeyType:       mapping.KeyType,
			KeyManager:    mapping.KeyManager,
			ApplicationId: mapping.ApplicationID,
			TenantId:      mapping.TenantID,
			TenantDomain:  mapping.TenantDomain,
			Timestamp:     mapping.TimeStamp,
		}

		applicationKeyMappings = append(applicationKeyMappings, keyMapping)
	}

	return &subscription.ApplicationKeyMappingList{
		List: applicationKeyMappings,
	}
}

// UpdateEnforcerSubscriptions sets new update to the enforcer's Subscriptions
func UpdateEnforcerSubscriptions(subscriptions *subscription.SubscriptionList) {
	//TODO: (Dinusha) check this hardcoded value
	logger.LoggerXds.Debug("Updating Enforcer Subscription Cache")
	label := commonEnforcerLabel
	subscriptionList := enforcerSubscriptionMap[label]
	subscriptionList = append(subscriptionList, subscriptions)

	version, ok := enforcerSubscriptionCacheVersionMap[label]

	if ok {
		version++
	} else {
		version = 1
	}

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, subscriptionList, nil, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerSubscriptionCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerSubscriptionCacheVersionMap[label] = version
	enforcerSubscriptionMap[label] = subscriptionList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApplications sets new update to the enforcer's Applications
func UpdateEnforcerApplications(applications *subscription.ApplicationList) {
	logger.LoggerXds.Debug("Updating Enforcer Application Cache")
	label := commonEnforcerLabel
	applicationList := enforcerApplicationMap[label]
	applicationList = append(applicationList, applications)

	version, ok := enforcerApplicationCacheVersionMap[label]

	if ok {
		version++
	} else {
		version = 1
	}

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, applicationList, nil, nil, nil, nil)
	snap.Consistent()

	err := enforcerApplicationCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationCacheVersionMap[label] = version
	enforcerApplicationMap[label] = applicationList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerAPIList sets new update to the enforcer's Apis
func UpdateEnforcerAPIList(apis *subscription.APIList) {
	logger.LoggerXds.Debug("Updating Enforcer API Cache")
	label := commonEnforcerLabel
	apiList := enforcerAPIListMap[label]
	apiList = append(apiList, apis)

	version, ok := enforcerAPICacheVersionMap[label]

	if ok {
		version++
	} else {
		version = 1
	}

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, apiList, nil, nil, nil)
	snap.Consistent()

	err := enforcerAPICache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerAPICacheVersionMap[label] = version
	enforcerAPIListMap[label] = apiList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApplicationPolicies sets new update to the enforcer's Application Policies
func UpdateEnforcerApplicationPolicies(applicationPolicies *subscription.ApplicationPolicyList) {
	logger.LoggerXds.Debug("Updating Enforcer Application Policy Cache")
	label := commonEnforcerLabel
	applicationPolicyList := enforcerApplicationPolicyMap[label]
	applicationPolicyList = append(applicationPolicyList, applicationPolicies)

	version, ok := enforcerApplicationPolicyCacheVersionMap[label]

	if ok {
		version++
	} else {
		version = 1
	}

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, applicationPolicyList, nil, nil)
	snap.Consistent()

	err := enforcerApplicationPolicyCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationPolicyCacheVersionMap[label] = version
	enforcerApplicationPolicyMap[label] = applicationPolicyList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerSubscriptionPolicies sets new update to the enforcer's Subscription Policies
func UpdateEnforcerSubscriptionPolicies(subscriptionPolicies *subscription.SubscriptionPolicyList) {
	logger.LoggerXds.Debug("Updating Enforcer Subscription Policy Cache")
	label := commonEnforcerLabel
	subscriptionPolicyList := enforcerSubscriptionPolicyMap[label]
	subscriptionPolicyList = append(subscriptionPolicyList, subscriptionPolicies)

	version, ok := enforcerSubscriptionPolicyCacheVersionMap[label]

	if ok {
		version++
	} else {
		version = 1
	}

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, subscriptionPolicyList, nil)
	snap.Consistent()

	err := enforcerSubscriptionPolicyCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerSubscriptionPolicyCacheVersionMap[label] = version
	enforcerSubscriptionPolicyMap[label] = subscriptionPolicyList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateEnforcerApplicationKeyMappings sets new update to the enforcer's Application Key Mappings
func UpdateEnforcerApplicationKeyMappings(applicationKeyMappings *subscription.ApplicationKeyMappingList) {
	logger.LoggerXds.Debug("Updating Application Key Mapping Cache")
	label := commonEnforcerLabel
	applicationKeyMappingList := enforcerApplicationKeyMappingMap[label]
	applicationKeyMappingList = append(applicationKeyMappingList, applicationKeyMappings)

	version, ok := enforcerApplicationKeyMappingCacheVersionMap[label]

	if ok {
		version++
	} else {
		version = 1
	}

	snap := cachev3.NewSnapshot(fmt.Sprint(version), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, applicationKeyMappingList)
	snap.Consistent()

	err := enforcerApplicationKeyMappingCache.SetSnapshot(label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationKeyMappingCacheVersionMap[label] = version
	enforcerApplicationKeyMappingMap[label] = applicationKeyMappingList
	logger.LoggerXds.Infof("New cache update for the label: " + label + " version: " + fmt.Sprint(version))
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
