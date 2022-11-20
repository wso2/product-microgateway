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
	"context"
	"crypto/sha1"
	"encoding/hex"
	"errors"
	"fmt"
	"math/rand"
	"strings"
	"sync"
	"time"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	envoy_cachev3 "github.com/envoyproxy/go-control-plane/pkg/cache/v3"

	envoy_resource "github.com/envoyproxy/go-control-plane/pkg/resource/v3"
	"github.com/wso2/product-microgateway/adapter/config"
	apiModel "github.com/wso2/product-microgateway/adapter/internal/api/models"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
	oasParser "github.com/wso2/product-microgateway/adapter/internal/oasparser"
	envoyconf "github.com/wso2/product-microgateway/adapter/internal/oasparser/envoyconf"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/svcdiscovery"
	subscription "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
	throttle "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/throttle"
	wso2_cache "github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/cache/v3"
	wso2_resource "github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/resource/v3"
	eventhubTypes "github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

var (
	version int32
	// TODO: (VirajSalaka) Remove Unused mutexes.
	mutexForXdsUpdate   sync.Mutex
	mutexForCacheUpdate sync.Mutex

	mutexForInternalMapUpdate sync.Mutex

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
	apiToVhostsMap           map[string]map[string]struct{} // API_UUID -> VHosts set (for un-deploying APIs from API-CTL)

	orgIDAPIMgwSwaggerMap       map[string]map[string]mgw.MgwSwagger       // organizationID -> Vhost:API_UUID -> MgwSwagger struct map
	orgIDOpenAPIEnvoyMap        map[string]map[string][]string             // organizationID -> Vhost:API_UUID -> Envoy Label Array map
	orgIDOpenAPIRoutesMap       map[string]map[string][]*routev3.Route     // organizationID -> Vhost:API_UUID -> Envoy Routes map
	orgIDOpenAPIClustersMap     map[string]map[string][]*clusterv3.Cluster // organizationID -> Vhost:API_UUID -> Envoy Clusters map
	orgIDOpenAPIEndpointsMap    map[string]map[string][]*corev3.Address    // organizationID -> Vhost:API_UUID -> Envoy Endpoints map
	orgIDOpenAPIEnforcerApisMap map[string]map[string]types.Resource       // organizationID -> Vhost:API_UUID -> API Resource map
	orgIDvHostBasepathMap       map[string]map[string]string               // organizationID -> Vhost:basepath -> Vhost:API_UUID

	reverseAPINameVersionMap map[string]string

	// Envoy Label as map key
	envoyUpdateVersionMap  map[string]int64                       // GW-Label -> XDS version map
	envoyListenerConfigMap map[string][]*listenerv3.Listener      // GW-Label -> Listener Configuration map
	envoyRouteConfigMap    map[string]*routev3.RouteConfiguration // GW-Label -> Routes Configuration map
	envoyClusterConfigMap  map[string][]*clusterv3.Cluster        // GW-Label -> Global Cluster Configuration map
	envoyEndpointConfigMap map[string][]*corev3.Address           // GW-Label -> Global Endpoint Configuration map

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
	isReady        = false
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
	//TODO: (VirajSalaka) Swagger or project should contain the version as a meta information
	envoyUpdateVersionMap = make(map[string]int64)
	envoyListenerConfigMap = make(map[string][]*listenerv3.Listener)
	envoyRouteConfigMap = make(map[string]*routev3.RouteConfiguration)
	envoyClusterConfigMap = make(map[string][]*clusterv3.Cluster)
	envoyEndpointConfigMap = make(map[string][]*corev3.Address)

	orgIDAPIMgwSwaggerMap = make(map[string]map[string]mgw.MgwSwagger)         // organizationID -> Vhost:API_UUID -> MgwSwagger struct map
	orgIDOpenAPIEnvoyMap = make(map[string]map[string][]string)                // organizationID -> Vhost:API_UUID -> Envoy Label Array map
	orgIDOpenAPIRoutesMap = make(map[string]map[string][]*routev3.Route)       // organizationID -> Vhost:API_UUID -> Envoy Routes map
	orgIDOpenAPIClustersMap = make(map[string]map[string][]*clusterv3.Cluster) // organizationID -> Vhost:API_UUID -> Envoy Clusters map
	orgIDOpenAPIEndpointsMap = make(map[string]map[string][]*corev3.Address)   // organizationID -> Vhost:API_UUID -> Envoy Endpoints map
	orgIDOpenAPIEnforcerApisMap = make(map[string]map[string]types.Resource)   // organizationID -> Vhost:API_UUID -> API Resource map
	orgIDvHostBasepathMap = make(map[string]map[string]string)

	reverseAPINameVersionMap = make(map[string]string)

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
	// go watchEnforcerResponse()
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

// GetEnforcerRevokedTokenCache return token cache
func GetEnforcerRevokedTokenCache() wso2_cache.SnapshotCache {
	return enforcerRevokedTokensCache
}

// GetEnforcerThrottleDataCache return throttle data cache
func GetEnforcerThrottleDataCache() wso2_cache.SnapshotCache {
	return enforcerThrottleDataCache
}

// DeployReadinessAPI Method to set the status after the last api is fected and updated in router.
func DeployReadinessAPI(envs []string) {
	logger.LoggerXds.Infof("Finished fetching APIs from the Control Plane. Deploying the readiness endpoint...")
	isReady = true
	for _, env := range envs {
		listeners, clusters, routes, endpoints, apis := GenerateEnvoyResoucesForLabel(env)
		UpdateXdsCacheWithLock(env, endpoints, clusters, routes, listeners)
		UpdateEnforcerApis(env, apis, "")
	}
}

// UpdateAPI updates the Xds Cache when OpenAPI Json content is provided
func UpdateAPI(vHost string, apiProject mgw.ProjectAPI, environments []string) (*notifier.DeployedAPIRevision, error) {
	var mgwSwagger mgw.MgwSwagger
	var deployedRevision *notifier.DeployedAPIRevision
	var err error
	var newLabels []string
	apiYaml := apiProject.APIYaml.Data

	// handle panic
	defer func() {
		if r := recover(); r != nil {
			panic(fmt.Sprintf("Error encountered while applying API %v:%v to %v.", apiYaml.Name, apiYaml.Version, vHost))
		}
	}()

	if len(environments) == 0 {
		environments = []string{config.DefaultGatewayName}
	}

	var apiEnvProps synchronizer.APIEnvProps

	// TODO(amali) under the assumption vhost has one environment at the moment
	if apiEnvPropsV, found := apiProject.APIEnvProps[environments[0]]; found {
		apiEnvProps = apiEnvPropsV
	}

	err = mgwSwagger.PopulateSwaggerFromAPIYaml(apiProject.APIYaml, apiProject.APIType)
	if err != nil {
		logger.LoggerXds.Error("Error while populating swagger from api.yaml. ", err)
		return nil, err
	}

	if apiProject.APIType == mgw.HTTP || apiProject.APIType == mgw.WEBHOOK {
		err = mgwSwagger.GetMgwSwagger(apiProject.OpenAPIJsn)
		if err != nil {
			logger.LoggerXds.Error("Error while populating swagger from api definition. ", err)
			return nil, err
		}
		// the following will be used for APIM specific security config.
		// it will enable folowing securities globally for the API, overriding swagger securities.
		isYamlAPIKey := false
		isYamlOauth := false
		for _, value := range apiYaml.SecurityScheme {
			if value == model.APIMAPIKeyType {
				logger.LoggerXds.Debugf("API key is enabled in api.yaml for API %v:%v", apiYaml.Name, apiYaml.Version)
				isYamlAPIKey = true
			} else if value == model.APIMOauth2Type {
				logger.LoggerXds.Debugf("Oauth2 is enabled in api.yaml for API %v:%v", apiYaml.Name, apiYaml.Version)
				isYamlOauth = true
			}
		}
		mgwSwagger.SanitizeAPISecurity(isYamlAPIKey, isYamlOauth)
		mgwSwagger.SetXWso2AuthHeader(apiYaml.AuthorizationHeader)
	} else if apiProject.APIType != mgw.WS {
		// Unreachable else condition. Added in case previous apiType check fails due to any modifications.
		logger.LoggerXds.Error("API type not currently supported by Choreo Connect")
	}

	conf, _ := config.ReadConfigs()
	if vHost == conf.Adapter.SandboxVhost {
		// Set the Choreo sandbox endpoint as the main endpoint
		mgwSwagger.SetEnvLabelProperties(apiEnvProps, true)
	} else {
		mgwSwagger.SetEnvLabelProperties(apiEnvProps, false)
	}

	if apiYaml.RateLimitLevel != "" {
		mgwSwagger.SetRateLimitPoliciesForOperations(apiYaml.Operations)
	}
	mgwSwagger.SetID(apiYaml.ID)
	mgwSwagger.SetName(apiYaml.Name)
	mgwSwagger.SetVersion(apiYaml.Version)
	mgwSwagger.OrganizationID = apiProject.OrganizationID
	mgwSwagger.APIProvider = apiProject.APIYaml.Data.Provider
	organizationID := apiProject.OrganizationID
	apiHashValue := generateHashValue(apiYaml.Name, apiYaml.Version)

	if mgwSwagger.GetProdEndpoints() != nil {
		mgwSwagger.GetProdEndpoints().SetEndpointsConfig(apiYaml.EndpointConfig.ProductionEndpoints)
		if !mgwSwagger.GetProdEndpoints().SecurityConfig.Enabled && apiYaml.EndpointConfig.APIEndpointSecurity.Production.Enabled {
			mgwSwagger.GetProdEndpoints().SecurityConfig = apiYaml.EndpointConfig.APIEndpointSecurity.Production
		}
	}

	if mgwSwagger.GetSandEndpoints() != nil {
		mgwSwagger.GetSandEndpoints().SetEndpointsConfig(apiYaml.EndpointConfig.SandBoxEndpoints)
		if !mgwSwagger.GetSandEndpoints().SecurityConfig.Enabled && apiYaml.EndpointConfig.APIEndpointSecurity.Sandbox.Enabled {
			mgwSwagger.GetSandEndpoints().SecurityConfig = apiYaml.EndpointConfig.APIEndpointSecurity.Sandbox
		}
	}

	mgwSwagger.SetEnvVariables(apiHashValue)

	validationErr := mgwSwagger.Validate()
	if validationErr != nil {
		logger.LoggerOasparser.Errorf("Validation failed for the API %s:%s of Organization %s",
			apiYaml.Name, apiYaml.Version, organizationID)
		return nil, validationErr
	}

	// -------- Finished updating mgwSwagger struct

	uniqueIdentifier := apiYaml.ID

	if uniqueIdentifier == "" {
		// If API is imported from apictl generate hash as the unique ID
		uniqueIdentifier = GenerateHashedAPINameVersionIDWithoutVhost(apiYaml.Name, apiYaml.Version)
		reverseAPINameVersionMap[GenerateIdentifierForAPIWithoutVhost(apiYaml.Name, apiYaml.Version)] = uniqueIdentifier
	}
	apiIdentifier := GenerateIdentifierForAPIWithUUID(vHost, uniqueIdentifier)

	mutexForInternalMapUpdate.Lock()
	defer mutexForInternalMapUpdate.Unlock()

	// -------- Begin updating maps

	err = addBasepathToMap(mgwSwagger, organizationID, vHost, apiIdentifier)
	if err != nil {
		return nil, err
	}

	// Get the map from organizationID map.
	if _, ok := orgIDAPIMgwSwaggerMap[organizationID]; ok {
		orgIDAPIMgwSwaggerMap[organizationID][apiIdentifier] = mgwSwagger
	} else {
		mgwSwaggerMap := make(map[string]mgw.MgwSwagger)
		mgwSwaggerMap[apiIdentifier] = mgwSwagger
		orgIDAPIMgwSwaggerMap[organizationID] = mgwSwaggerMap
	}

	//TODO: (VirajSalaka) Handle OpenAPIs which does not have label (Current Impl , it will be labelled as default)
	// TODO: commented the following line as the implementation is not supported yet.
	//newLabels = model.GetXWso2Label(openAPIV3Struct.ExtensionProps)
	//:TODO: since currently labels are not taking from x-wso2-label, I have made it to be taken from the method
	// argument.
	newLabels = environments
	logger.LoggerXds.Infof("Added/Updated the content for Organization : %v under OpenAPI Key : %v", organizationID, apiIdentifier)
	logger.LoggerXds.Debugf("Newly added labels for Organization : %v for the OpenAPI Key : %v are %v", organizationID, apiIdentifier, newLabels)
	oldLabels, _ := orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier]
	logger.LoggerXds.Debugf("Already existing labels for the OpenAPI Key : %v are %v", apiIdentifier, oldLabels)

	if _, ok := orgIDOpenAPIEnvoyMap[organizationID]; ok {
		orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier] = newLabels
	} else {
		openAPIEnvoyMap := make(map[string][]string)
		openAPIEnvoyMap[apiIdentifier] = newLabels
		orgIDOpenAPIEnvoyMap[organizationID] = openAPIEnvoyMap
	}
	updateVhostInternalMaps(apiYaml.ID, apiYaml.Name, apiYaml.Version, vHost, newLabels)

	// create cert map for API
	certMap := make(map[string][]byte)
	interceptCertMap := make(map[string][]byte)
	if len(apiProject.EndpointCerts) > 0 && len(apiProject.UpstreamCerts) > 0 {
		for url, certFile := range apiProject.EndpointCerts {
			if certBytes, found := apiProject.UpstreamCerts[certFile]; found {
				certMap[url] = certBytes
				interceptCertMap[url] = certBytes
				delete(apiProject.UpstreamCerts, certFile)
			} else {
				logger.LoggerXds.Errorf("Certificate file %v not found for the url %v", certFile, url)
			}
		}
	}
	newLineByteArray := []byte("\n")
	for _, certBytes := range apiProject.UpstreamCerts {
		certMap["default"] = append(certMap["default"], certBytes...)
		certMap["default"] = append(certMap["default"], newLineByteArray...)
	}
	interceptCertMap["default"] = apiProject.InterceptorCerts

	routes, clusters, endpoints := oasParser.GetRoutesClustersEndpoints(mgwSwagger, certMap,
		interceptCertMap, vHost, organizationID)

	if _, ok := orgIDOpenAPIRoutesMap[organizationID]; ok {
		orgIDOpenAPIRoutesMap[organizationID][apiIdentifier] = routes
	} else {
		routesMap := make(map[string][]*routev3.Route)
		routesMap[apiIdentifier] = routes
		orgIDOpenAPIRoutesMap[organizationID] = routesMap
	}

	if _, ok := orgIDOpenAPIClustersMap[organizationID]; ok {
		orgIDOpenAPIClustersMap[organizationID][apiIdentifier] = clusters
	} else {
		clustersMap := make(map[string][]*clusterv3.Cluster)
		clustersMap[apiIdentifier] = clusters
		orgIDOpenAPIClustersMap[organizationID] = clustersMap
	}

	if _, ok := orgIDOpenAPIEndpointsMap[organizationID]; ok {
		orgIDOpenAPIEndpointsMap[organizationID][apiIdentifier] = endpoints
	} else {
		endpointMap := make(map[string][]*corev3.Address)
		endpointMap[apiIdentifier] = endpoints
		orgIDOpenAPIEndpointsMap[organizationID] = endpointMap
	}

	if _, ok := orgIDOpenAPIEnforcerApisMap[organizationID]; ok {
		orgIDOpenAPIEnforcerApisMap[organizationID][apiIdentifier] = oasParser.GetEnforcerAPI(mgwSwagger,
			apiProject.APILifeCycleStatus, vHost)
	} else {
		enforcerAPIMap := make(map[string]types.Resource)
		enforcerAPIMap[apiIdentifier] = oasParser.GetEnforcerAPI(mgwSwagger, apiProject.APILifeCycleStatus,
			vHost)
		orgIDOpenAPIEnforcerApisMap[organizationID] = enforcerAPIMap
	}

	// TODO: (VirajSalaka) Fault tolerance mechanism implementation
	revisionStatus := updateXdsCacheOnAPIAdd(oldLabels, newLabels)
	if revisionStatus {
		// send updated revision to control plane
		deployedRevision = notifier.UpdateDeployedRevisions(apiYaml.ID, apiYaml.RevisionID, environments,
			vHost)
	}
	if svcdiscovery.IsServiceDiscoveryEnabled {
		startConsulServiceDiscovery(organizationID) //consul service discovery starting point
	}
	return deployedRevision, nil
}

// GetAllEnvironments returns all the environments merging new environments with already deployed environments
// of the given vhost of the API
func GetAllEnvironments(apiUUID, vhost string, newEnvironments []string) []string {
	// allEnvironments represent all the environments the API should be deployed
	allEnvironments := newEnvironments
	if existingEnvs, exists := apiUUIDToGatewayToVhosts[apiUUID]; exists {
		for env, vh := range existingEnvs {
			// update allEnvironments with already existing environments
			if vh == vhost && !arrayContains(allEnvironments, env) {
				allEnvironments = append(allEnvironments, env)
			}
		}
	}
	return allEnvironments
}

// GetVhostOfAPI returns the vhost of API deployed in the given gateway environment
func GetVhostOfAPI(apiUUID, environment string) (vhost string, exists bool) {
	if envToVhost, ok := apiUUIDToGatewayToVhosts[apiUUID]; ok {
		vhost, exists = envToVhost[environment]
		return
	}
	return "", false
}

func addBasepathToMap(mgwSwagger mgw.MgwSwagger, organizationID, vHost, apiIdentifier string) error {
	newBasepath := mgwSwagger.GetXWso2Basepath()

	// Check if the basepath exists
	if existingAPIIdentifier, ok := orgIDvHostBasepathMap[organizationID][vHost+":"+newBasepath]; ok {
		// Check if it is NOT just an update for the already existing API
		if existingAPIIdentifier != apiIdentifier {
			logger.LoggerXds.Errorf("An API exists with the same basepath. Basepath: %v Existing_API: %v New_API: %v orgID: %v VHost: %v",
				newBasepath, existingAPIIdentifier, apiIdentifier, organizationID, vHost)
			err := errors.New("An API exists with the same basepath. Existing_API: " + existingAPIIdentifier + "New_API:" + apiIdentifier +
				" orgID: " + organizationID + " VHost: " + vHost)
			return err
		}
	}

	// Remove the old basepath anyway
	if oldMgwSwagger, ok := orgIDAPIMgwSwaggerMap[organizationID][apiIdentifier]; ok {
		oldBasepath := oldMgwSwagger.GetXWso2Basepath()
		delete(orgIDvHostBasepathMap[organizationID], vHost+":"+oldBasepath)
	}

	// Add the new basepath
	if _, ok := orgIDvHostBasepathMap[organizationID]; ok {
		orgIDvHostBasepathMap[organizationID][vHost+":"+newBasepath] = apiIdentifier
	} else {
		vHostBasepathMap := make(map[string]string)
		vHostBasepathMap[vHost+":"+newBasepath] = apiIdentifier
		orgIDvHostBasepathMap[organizationID] = vHostBasepathMap
	}
	return nil
}

// DeleteAPIs deletes an API, its resources and updates the caches of given environments
func DeleteAPIs(vhost, apiName, version string, environments []string, organizationID string) error {
	apiNameVersionID := GenerateIdentifierForAPIWithoutVhost(apiName, version)
	apiNameVersionHashedID := reverseAPINameVersionMap[apiNameVersionID]

	mutexForInternalMapUpdate.Lock()
	defer mutexForInternalMapUpdate.Unlock()

	vhosts, found := apiToVhostsMap[apiNameVersionHashedID]
	if !found {
		logger.LoggerXds.Infof("Unable to delete API %v from Organization %v. API does not exist.", apiNameVersionID, organizationID)
		return errors.New(mgw.NotFound)
	}

	if vhost == "" {
		// vhost is not defined, delete all vhosts
		logger.LoggerXds.Infof("No vhost is specified for the API %v in Organizaion %v deleting from all vhosts", apiNameVersionID, organizationID)
		deletedVhosts := make(map[string]struct{})
		for vh := range vhosts {
			apiIdentifier := GenerateIdentifierForAPIWithUUID(vh, apiNameVersionHashedID)
			// TODO: (renuka) optimize to update cache only once after updating all maps
			if err := deleteAPI(apiIdentifier, environments, organizationID); err != nil {
				// Update apiToVhostsMap with already deleted vhosts in the loop
				logger.LoggerXds.Errorf("Error deleting API: %v of organization: %v", apiIdentifier, organizationID)
				logger.LoggerXds.Debugf("Update map apiToVhostsMap with deleting already deleted vhosts for API %v in organization: %v",
					apiIdentifier, organizationID)
				remainingVhosts := make(map[string]struct{})
				for v := range vhosts {
					if _, ok := deletedVhosts[v]; ok {
						remainingVhosts[v] = void
					}
				}
				apiToVhostsMap[apiNameVersionHashedID] = remainingVhosts
				return err
			}
			deletedVhosts[vh] = void

			for val := range deletedVhosts {
				existingLabels := orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier]
				if val == vh && len(existingLabels) == 0 {
					logger.LoggerXds.Infof("Vhost : %v  deleted since there is no gateways assigned to it.", vh)
					delete(apiToVhostsMap[apiNameVersionHashedID], val)
				}
			}
		}
		return nil
	}

	apiIdentifier := GenerateIdentifierForAPIWithUUID(vhost, apiNameVersionHashedID)
	if err := deleteAPI(apiIdentifier, environments, organizationID); err != nil {
		return err
	}

	if _, ok := vhosts[vhost]; ok {
		if len(vhosts) == 1 {
			// if this is the final vhost delete map entry
			logger.LoggerXds.Debugf("The API %v is not exists with any vhost. Hence clean vhost entry from the map 'apiToVhostsMap'",
				apiNameVersionID)
			delete(apiToVhostsMap, apiNameVersionHashedID)
		} else {
			delete(apiToVhostsMap[apiNameVersionHashedID], vhost)
		}
	}
	return nil
}

// DeleteAPIsWithUUID deletes an API, its resources and updates the caches of given environments
func DeleteAPIsWithUUID(vhost, uuid string, environments []string, organizationID string) error {

	mutexForInternalMapUpdate.Lock()
	defer mutexForInternalMapUpdate.Unlock()

	vhosts, found := apiToVhostsMap[uuid]
	if !found {
		logger.LoggerXds.Infof("Unable to delete API with UUID %v from Organization %v. API does not exist.", uuid, organizationID)
		return errors.New(mgw.NotFound)
	}

	if vhost == "" {
		// vhost is not defined, delete all vhosts
		logger.LoggerXds.Infof("No vhost is specified for the API with UUID %v in Organizaion %v deleting from all vhosts", uuid, organizationID)
		deletedVhosts := make(map[string]struct{})
		for vh := range vhosts {
			apiIdentifier := GenerateIdentifierForAPIWithUUID(vh, uuid)
			// TODO: (renuka) optimize to update cache only once after updating all maps
			if err := deleteAPI(apiIdentifier, environments, organizationID); err != nil {
				// Update apiToVhostsMap with already deleted vhosts in the loop
				logger.LoggerXds.Errorf("Error deleting API: %v of organization: %v", apiIdentifier, organizationID)
				logger.LoggerXds.Debugf("Update map apiToVhostsMap with deleting already deleted vhosts for API %v in organization: %v",
					apiIdentifier, organizationID)
				remainingVhosts := make(map[string]struct{})
				for v := range vhosts {
					if _, ok := deletedVhosts[v]; ok {
						remainingVhosts[v] = void
					}
				}
				apiToVhostsMap[uuid] = remainingVhosts
				return err
			}
			deletedVhosts[vh] = void
		}
		delete(apiToVhostsMap, uuid)
		return nil
	}

	apiIdentifier := GenerateIdentifierForAPIWithUUID(vhost, uuid)
	if err := deleteAPI(apiIdentifier, environments, organizationID); err != nil {
		return err
	}

	if _, ok := vhosts[vhost]; ok {
		if len(vhosts) == 1 {
			// if this is the final vhost delete map entry
			logger.LoggerXds.Debugf("The API with UUID %v is not exists with any vhost. Hence clean vhost entry from the map 'apiToVhostsMap'",
				uuid)
			delete(apiToVhostsMap, uuid)
		} else {
			delete(apiToVhostsMap[uuid], vhost)
		}
	}
	return nil
}

// DeleteAPIWithAPIMEvent deletes API with the given UUID from the given gw environments
func DeleteAPIWithAPIMEvent(uuid, organizationID string, environments []string, revisionUUID string) {
	apiIdentifiers := make(map[string]struct{})
	mutexForInternalMapUpdate.Lock()
	defer mutexForInternalMapUpdate.Unlock()

	for gw, vhost := range apiUUIDToGatewayToVhosts[uuid] {
		// delete from only specified environments
		if arrayContains(environments, gw) {
			id := GenerateIdentifierForAPIWithUUID(vhost, uuid)
			apiIdentifiers[id] = void
		}
	}
	for apiIdentifier := range apiIdentifiers {
		if err := deleteAPI(apiIdentifier, environments, organizationID); err != nil {
			logger.LoggerXds.Errorf("Error undeploying API %v of Organization %v from environments %v", apiIdentifier, organizationID, environments)
		} else {
			// if no error, update internal vhost maps
			// error only happens when API not found in deleteAPI func
			logger.LoggerXds.Infof("Successfully undeployed the revision %v of API %v under Organization %v and environment %v ", revisionUUID, apiIdentifier, organizationID, environments)
			for _, environment := range environments {
				// delete environment if exists
				delete(apiUUIDToGatewayToVhosts[uuid], environment)
				notifier.SendRevisionUndeploy(uuid, revisionUUID, environment)
			}
		}
	}
}

// deleteAPI deletes an API, its resources and updates the caches of given environments
func deleteAPI(apiIdentifier string, environments []string, organizationID string) error {
	_, exists := orgIDAPIMgwSwaggerMap[organizationID][apiIdentifier]
	if !exists {
		logger.LoggerXds.Infof("Unable to delete API: %v from Organization: %v. API Does not exist.", apiIdentifier, organizationID)
		return errors.New(mgw.NotFound)
	}

	existingLabels := orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier]
	toBeDelEnvs, toBeKeptEnvs := getEnvironmentsToBeDeleted(existingLabels, environments)

	for _, val := range toBeDelEnvs {
		isAllowedToDelete := arrayContains(existingLabels, val)
		if isAllowedToDelete {
			// do not delete from all environments, hence do not clear routes, clusters, endpoints, enforcerAPIs
			orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier] = toBeKeptEnvs
			updateXdsCacheOnAPIAdd(toBeDelEnvs, []string{})
			existingLabels = orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier]
			if len(existingLabels) != 0 {
				return nil
			}
			logger.LoggerXds.Infof("API identifier: %v does not have any gateways. Hence deleting the API.", apiIdentifier)
			cleanMapResources(apiIdentifier, organizationID, toBeDelEnvs)
			return nil
		}
	}

	//clean maps of routes, clusters, endpoints, enforcerAPIs
	if len(environments) == 0 {
		cleanMapResources(apiIdentifier, organizationID, toBeDelEnvs)
	}
	return nil
}

func cleanMapResources(apiIdentifier string, organizationID string, toBeDelEnvs []string) {
	delete(orgIDOpenAPIRoutesMap[organizationID], apiIdentifier)
	delete(orgIDOpenAPIClustersMap[organizationID], apiIdentifier)
	delete(orgIDOpenAPIEndpointsMap[organizationID], apiIdentifier)
	delete(orgIDOpenAPIEnforcerApisMap[organizationID], apiIdentifier)

	//updateXdsCacheOnAPIAdd is called after cleaning maps of routes, clusters, endpoints, enforcerAPIs.
	//Therefore resources that belongs to the deleting API do not exist. Caches updated only with
	//resources that belongs to the remaining APIs
	updateXdsCacheOnAPIAdd(toBeDelEnvs, []string{})

	deleteBasepathForVHost(organizationID, apiIdentifier)
	delete(orgIDOpenAPIEnvoyMap[organizationID], apiIdentifier)  //delete labels
	delete(orgIDAPIMgwSwaggerMap[organizationID], apiIdentifier) //delete mgwSwagger
	//TODO: (SuKSW) clean any remaining in label wise maps, if this is the last API of that label
	logger.LoggerXds.Infof("Deleted API %v of organization %v", apiIdentifier, organizationID)
}

func deleteBasepathForVHost(organizationID, apiIdentifier string) {
	// Remove the basepath from map (that is used to avoid duplicate basepaths)
	if oldMgwSwagger, ok := orgIDAPIMgwSwaggerMap[organizationID][apiIdentifier]; ok {
		s := strings.Split(apiIdentifier, apiKeyFieldSeparator)
		vHost := s[0]
		oldBasepath := oldMgwSwagger.GetXWso2Basepath()
		delete(orgIDvHostBasepathMap[organizationID], vHost+":"+oldBasepath)
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
func updateXdsCacheOnAPIAdd(oldLabels []string, newLabels []string) bool {
	revisionStatus := false
	// TODO: (VirajSalaka) check possible optimizations, Since the number of labels are low by design it should not be an issue
	for _, newLabel := range newLabels {
		listeners, clusters, routes, endpoints, apis := GenerateEnvoyResoucesForLabel(newLabel)
		UpdateEnforcerApis(newLabel, apis, "")
		success := UpdateXdsCacheWithLock(newLabel, endpoints, clusters, routes, listeners)
		logger.LoggerXds.Debugf("Xds Cache is updated for the newly added label : %v", newLabel)
		if success {
			// if even one label was updated with latest revision, we take the revision as deployed.
			// (other labels also will get updated successfully)
			revisionStatus = success
			continue
		}
	}
	for _, oldLabel := range oldLabels {
		if !arrayContains(newLabels, oldLabel) {
			listeners, clusters, routes, endpoints, apis := GenerateEnvoyResoucesForLabel(oldLabel)
			UpdateEnforcerApis(oldLabel, apis, "")
			UpdateXdsCacheWithLock(oldLabel, endpoints, clusters, routes, listeners)
			logger.LoggerXds.Debugf("Xds Cache is updated for the already existing label : %v", oldLabel)
		}
	}
	return revisionStatus
}

// GenerateEnvoyResoucesForLabel generates envoy resources for a given label
// This method will list out all APIs mapped to the label. and generate envoy resources for all of these APIs.
func GenerateEnvoyResoucesForLabel(label string) ([]types.Resource, []types.Resource, []types.Resource,
	[]types.Resource, []types.Resource) {
	var clusterArray []*clusterv3.Cluster
	var vhostToRouteArrayMap = make(map[string][]*routev3.Route)
	var endpointArray []*corev3.Address
	var apis []types.Resource

	for organizationID, entityMap := range orgIDOpenAPIEnvoyMap {
		for apiKey, labels := range entityMap {
			if arrayContains(labels, label) {
				vhost, err := ExtractVhostFromAPIIdentifier(apiKey)
				if err != nil {
					logger.LoggerXds.Errorf("Error extracting vhost from API identifier: %v for Organization %v. Ignore deploying the API",
						err.Error(), organizationID)
					continue
				}
				clusterArray = append(clusterArray, orgIDOpenAPIClustersMap[organizationID][apiKey]...)
				vhostToRouteArrayMap[vhost] = append(vhostToRouteArrayMap[vhost], orgIDOpenAPIRoutesMap[organizationID][apiKey]...)
				endpointArray = append(endpointArray, orgIDOpenAPIEndpointsMap[organizationID][apiKey]...)
				enfocerAPI, ok := orgIDOpenAPIEnforcerApisMap[organizationID][apiKey]
				if ok {
					apis = append(apis, enfocerAPI)
				}
				// listenerArrays = append(listenerArrays, openAPIListenersMap[apiKey])
			}
		}
	}

	// If the token endpoint is enabled, the token endpoint also needs to be added.
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}
	systemHost := conf.Envoy.SystemHost

	// Add testkey and JWKS endpoints
	if conf.Enforcer.JwtIssuer.Enabled {
		routeToken := envoyconf.CreateTokenRoute()
		vhostToRouteArrayMap[systemHost] = append(vhostToRouteArrayMap[systemHost], routeToken)
	}
	if conf.Enforcer.JwtGenerator.Enabled {
		routeJwks := envoyconf.CreateJwksEndpoint()
		vhostToRouteArrayMap[systemHost] = append(vhostToRouteArrayMap[systemHost], routeJwks)
	}

	// Add health endpoint
	routeHealth := envoyconf.CreateHealthEndpoint()
	vhostToRouteArrayMap[systemHost] = append(vhostToRouteArrayMap[systemHost], routeHealth)

	// Add the readiness endpoint. isReady flag will be set to true once all the apis are fetched from the control plane
	if isReady {
		readynessEndpoint := envoyconf.CreateReadyEndpoint()
		vhostToRouteArrayMap[systemHost] = append(vhostToRouteArrayMap[systemHost], readynessEndpoint)
	}

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
	clusterArray = append(clusterArray, envoyClusterConfigMap[label]...)
	endpointArray = append(endpointArray, envoyEndpointConfigMap[label]...)
	endpoints, clusters, listeners, routeConfigs := oasParser.GetCacheResources(endpointArray, clusterArray, listenerArray, routesConfig)
	return endpoints, clusters, listeners, routeConfigs, apis
}

// GenerateGlobalClusters generates the globally available clusters and endpoints.
func GenerateGlobalClusters(label string) {
	clusters, endpoints := oasParser.GetGlobalClusters()
	envoyClusterConfigMap[label] = clusters
	envoyEndpointConfigMap[label] = endpoints
}

// use UpdateXdsCacheWithLock to avoid race conditions
func updateXdsCache(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource, listeners []types.Resource) bool {
	version := rand.Intn(maxRandomInt)
	// TODO: (VirajSalaka) kept same version for all the resources as we are using simple cache implementation.
	// Will be updated once decide to move to incremental XDS
	snap, err := envoy_cachev3.NewSnapshot(fmt.Sprint(version), map[envoy_resource.Type][]types.Resource{
		envoy_resource.EndpointType: endpoints,
		envoy_resource.ClusterType:  clusters,
		envoy_resource.ListenerType: listeners,
		envoy_resource.RouteType:    routes,
	})
	if err != nil {
		logger.LoggerXds.Errorf("Error while updating the snapshot : %v", err.Error())
		return false
	}
	err = snap.Consistent()
	//TODO: (VirajSalaka) check
	err = cache.SetSnapshot(context.Background(), label, snap)
	if err != nil {
		logger.LoggerXds.Errorf("Error while updating the snapshot : %v", err.Error())
		return false
	}
	logger.LoggerXds.Infof("New Router cache updated for the label: " + label + " version: " + fmt.Sprint(version))
	return true
}

// UpdateEnforcerConfig Sets new update to the enforcer's configuration
func UpdateEnforcerConfig(configFile *config.Config) {
	// TODO: (Praminda) handle labels
	label := commonEnforcerLabel
	configs := []types.Resource{MarshalConfig(configFile)}
	version := rand.Intn(maxRandomInt)
	snap, err := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.ConfigType: configs,
	})
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	snap.Consistent()

	err = enforcerCache.SetSnapshot(context.Background(), label, snap)
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

	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.APIType: apis,
	})
	snap.Consistent()

	err := enforcerCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.SubscriptionListType: subscriptionList,
	})
	snap.Consistent()

	err := enforcerSubscriptionCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.ApplicationListType: applicationList,
	})
	snap.Consistent()

	err := enforcerApplicationCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.APIListType: apiList,
	})
	snap.Consistent()

	err := enforcerAPICache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.ApplicationPolicyListType: applicationPolicyList,
	})
	snap.Consistent()

	err := enforcerApplicationPolicyCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.SubscriptionPolicyListType: subscriptionPolicyList,
	})
	snap.Consistent()

	err := enforcerSubscriptionPolicyCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.ApplicationKeyMappingListType: applicationKeyMappingList,
	})
	snap.Consistent()

	err := enforcerApplicationKeyMappingCache.SetSnapshot(context.Background(), label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerApplicationKeyMappingMap[label] = applicationKeyMappingList
	logger.LoggerXds.Infof("New Application Key Mapping cache update for the label: " + label + " version: " + fmt.Sprint(version))
}

// UpdateXdsCacheWithLock uses mutex and lock to avoid different go routines updating XDS at the same time
func UpdateXdsCacheWithLock(label string, endpoints []types.Resource, clusters []types.Resource, routes []types.Resource,
	listeners []types.Resource) bool {
	mutexForXdsUpdate.Lock()
	defer mutexForXdsUpdate.Unlock()
	return updateXdsCache(label, endpoints, clusters, routes, listeners)
}

// ListApis returns a list of objects that holds info about each API
func ListApis(apiType string, organizationID string, limit *int64) *apiModel.APIMeta {
	var limitValue int
	if limit == nil {
		limitValue = len(orgIDAPIMgwSwaggerMap[organizationID])
	} else {
		limitValue = int(*limit)
	}
	var apisArray []*apiModel.APIMetaListItem
	i := 0
	for apiIdentifier, mgwSwagger := range orgIDAPIMgwSwaggerMap[organizationID] {
		if i == limitValue {
			break
		}
		if apiType == "" || mgwSwagger.GetAPIType() == apiType {
			var apiMetaListItem apiModel.APIMetaListItem
			apiMetaListItem.APIName = mgwSwagger.GetTitle()
			apiMetaListItem.Version = mgwSwagger.GetVersion()
			apiMetaListItem.APIType = mgwSwagger.GetAPIType()
			apiMetaListItem.Context = mgwSwagger.GetXWso2Basepath()
			apiMetaListItem.GatewayEnvs = orgIDOpenAPIEnvoyMap[organizationID][apiIdentifier]
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
	apiMetaObject.Total = int64(len(orgIDAPIMgwSwaggerMap[organizationID]))
	apiMetaObject.Count = int64(len(apisArray))
	apiMetaObject.List = apisArray
	return &apiMetaObject
}

// IsAPIExist returns whether a given API exists
func IsAPIExist(vhost, uuid, organizationID string) (exists bool) {
	apiIdentifier := GenerateIdentifierForAPIWithUUID(vhost, uuid)
	_, exists = orgIDAPIMgwSwaggerMap[organizationID][apiIdentifier]
	return exists
}

// GenerateIdentifierForAPI generates an identifier unique to the API
func GenerateIdentifierForAPI(vhost, name, version string) string {
	return fmt.Sprint(vhost, apiKeyFieldSeparator, name, apiKeyFieldSeparator, version)
}

// GenerateIdentifierForAPIWithUUID generates an identifier unique to the API
func GenerateIdentifierForAPIWithUUID(vhost, uuid string) string {
	return fmt.Sprint(vhost, apiKeyFieldSeparator, uuid)
}

// GenerateIdentifierForAPIWithoutVhost generates an identifier unique to the API name and version
func GenerateIdentifierForAPIWithoutVhost(name, version string) string {
	return fmt.Sprint(name, apiKeyFieldSeparator, version)
}

// GenerateHashedAPINameVersionIDWithoutVhost generates a hashed identifier unique to the API Name and Version
func GenerateHashedAPINameVersionIDWithoutVhost(name, version string) string {
	return generateHashValue(name, version)
}

func generateHashValue(apiName string, apiVersion string) string {
	apiNameVersionHash := sha1.New()
	apiNameVersionHash.Write([]byte(apiName + ":" + apiVersion))
	return hex.EncodeToString(apiNameVersionHash.Sum(nil)[:])
}

// ExtractVhostFromAPIIdentifier extracts vhost from the API identifier
func ExtractVhostFromAPIIdentifier(id string) (string, error) {
	elem := strings.Split(id, apiKeyFieldSeparator)
	if len(elem) == 2 {
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.KeyManagerType: keyManagerConfigList,
	})
	snap.Consistent()

	err := enforcerKeyManagerCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.RevokedTokensType: revokedTokens,
	})
	snap.Consistent()

	err := enforcerRevokedTokensCache.SetSnapshot(context.Background(), label, snap)
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
	snap, _ := wso2_cache.NewSnapshot(fmt.Sprint(version), map[wso2_resource.Type][]types.Resource{
		wso2_resource.ThrottleDataType: data,
	})
	snap.Consistent()

	err := enforcerThrottleDataCache.SetSnapshot(context.Background(), label, snap)
	if err != nil {
		logger.LoggerXds.Error(err)
	}
	enforcerThrottleData = t
	logger.LoggerXds.Infof("New Throttle Data cache update for the label: " + label + " version: " + fmt.Sprint(version))
}
