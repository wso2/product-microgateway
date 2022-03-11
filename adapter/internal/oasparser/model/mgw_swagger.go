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

package model

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/url"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/go-openapi/spec"
	parser "github.com/mitchellh/mapstructure"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/interceptor"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
	"github.com/wso2/product-microgateway/adapter/internal/svcdiscovery"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

// MgwSwagger represents the object structure holding the information related to the
// openAPI object. The values are populated from the extensions/properties mentioned at
// the root level of the openAPI definition. The pathItem level information is represented
// by the resources array which contains the MgwResource entries.
type MgwSwagger struct {
	id                         string
	UUID                       string
	apiType                    string
	description                string
	title                      string
	version                    string
	vendorExtensions           map[string]interface{}
	productionEndpoints        *EndpointCluster
	sandboxEndpoints           *EndpointCluster
	xWso2Endpoints             map[string]*EndpointCluster
	resources                  []*Resource
	xWso2Basepath              string
	xWso2Cors                  *CorsConfig
	securityScheme             []SecurityScheme
	security                   []map[string][]string
	xWso2ThrottlingTier        string
	xWso2AuthHeader            string
	disableSecurity            bool
	OrganizationID             string
	IsPrototyped               bool
	EndpointType               string
	EndpointImplementationType string
	LifecycleStatus            string
	xWso2RequestBodyPass       bool
	IsDefaultVersion           bool
}

// EndpointCluster represent an upstream cluster
type EndpointCluster struct {
	EndpointPrefix string
	Endpoints      []Endpoint
	// EndpointType enum {failover, loadbalance}. if any other value provided, consider as the default value; which is loadbalance
	EndpointType   string
	Config         *EndpointConfig
	SecurityConfig EndpointSecurity
}

// Endpoint represents the structure of an endpoint.
type Endpoint struct {
	// Host name
	Host string
	// BasePath (which would be added as prefix to the path mentioned in openapi definition)
	// In openAPI v2, it is determined from the basePath property
	// In openAPi v3, it is determined from the server object's suffix
	Basepath string
	// https, http, ws, wss
	// In openAPI v2, it is fetched from the schemes entry
	// In openAPI v3, it is extracted from the server property under servers object
	// only https and http are supported at the moment.
	URLType string
	// Port of the endpoint.
	// If the port is not specified, 80 is assigned if URLType is http
	// 443 is assigned if URLType is https
	Port uint32
	//ServiceDiscoveryQuery consul query for service discovery
	ServiceDiscoveryString string
	RawURL                 string
}

// EndpointConfig holds the configs such as timeout, retry, etc. for the EndpointCluster
type EndpointConfig struct {
	RetryConfig     *RetryConfig     `mapstructure:"retryConfig"`
	TimeoutInMillis uint32           `mapstructure:"timeoutInMillis"`
	CircuitBreakers *CircuitBreakers `mapstructure:"circuitBreakers"`
}

// RetryConfig holds the parameters for retries done by cc to the EndpointCluster
type RetryConfig struct {
	Count       int32    `mapstructure:"count"`
	StatusCodes []uint32 `mapstructure:"statusCodes"`
}

// CircuitBreakers holds the parameters for retries done by cc to the EndpointCluster
type CircuitBreakers struct {
	MaxConnections     int32 `mapstructure:"maxConnections"`
	MaxRequests        int32 `mapstructure:"maxRequests"`
	MaxPendingRequests int32 `mapstructure:"maxPendingRequests"`
	MaxRetries         int32 `mapstructure:"maxRetries"`
	MaxConnectionPools int32 `mapstructure:"maxConnectionPools"`
}

// SecurityScheme represents the structure of an security scheme.
type SecurityScheme struct {
	DefinitionName string // Arbitrary name used to define the security scheme. ex: default, myApikey
	Type           string // Type of the security scheme. Valid: apiKey, api_key, oauth2
	Name           string // Used for API key. Name of header or query. ex: x-api-key, apikey
	In             string // Where the api key found in. Valid: query, header
}

// CorsConfig represents the API level Cors Configuration
type CorsConfig struct {
	Enabled                       bool     `mapstructure:"corsConfigurationEnabled"`
	AccessControlAllowCredentials bool     `mapstructure:"accessControlAllowCredentials,omitempty"`
	AccessControlAllowHeaders     []string `mapstructure:"accessControlAllowHeaders"`
	AccessControlAllowMethods     []string `mapstructure:"accessControlAllowMethods"`
	AccessControlAllowOrigins     []string `mapstructure:"accessControlAllowOrigins"`
	AccessControlExposeHeaders    []string `mapstructure:"accessControlExposeHeaders"`
}

// InterceptEndpoint contains the parameters of endpoint security
type InterceptEndpoint struct {
	Enable          bool
	EndpointCluster EndpointCluster
	ClusterName     string
	ClusterTimeout  time.Duration
	RequestTimeout  time.Duration
	// Level this is an enum allowing only values {api, resource, operation}
	// to indicate from which level interceptor is added
	Level string
	// Includes this is an enum allowing only values in
	// {"request_headers", "request_body", "request_trailer", "response_headers", "response_body", "response_trailer",
	//"invocation_context" }
	Includes *interceptor.RequestInclusions
}

// GetCorsConfig returns the CorsConfiguration Object.
func (swagger *MgwSwagger) GetCorsConfig() *CorsConfig {
	return swagger.xWso2Cors
}

// GetAPIType returns the openapi version
func (swagger *MgwSwagger) GetAPIType() string {
	return swagger.apiType
}

// GetVersion returns the API version
func (swagger *MgwSwagger) GetVersion() string {
	return swagger.version
}

// GetTitle returns the API Title
func (swagger *MgwSwagger) GetTitle() string {
	return swagger.title
}

// GetXWso2Basepath returns the basepath set via the vendor extension.
func (swagger *MgwSwagger) GetXWso2Basepath() string {
	return swagger.xWso2Basepath
}

// GetVendorExtensions returns the map of vendor extensions which are defined
// at openAPI's root level.
func (swagger *MgwSwagger) GetVendorExtensions() map[string]interface{} {
	return swagger.vendorExtensions
}

// GetProdEndpoints returns the array of production endpoints.
func (swagger *MgwSwagger) GetProdEndpoints() *EndpointCluster {
	return swagger.productionEndpoints
}

// GetXWso2Endpoints returns the array of x wso2 endpoints.
func (swagger *MgwSwagger) GetXWso2Endpoints() map[string]*EndpointCluster {
	return swagger.xWso2Endpoints
}

// GetSandEndpoints returns the array of sandbox endpoints.
func (swagger *MgwSwagger) GetSandEndpoints() *EndpointCluster {
	return swagger.sandboxEndpoints
}

// GetResources returns the array of resources (openAPI path level info)
func (swagger *MgwSwagger) GetResources() []*Resource {
	return swagger.resources
}

// GetDescription returns the description of the openapi
func (swagger *MgwSwagger) GetDescription() string {
	return swagger.description
}

// GetXWso2ThrottlingTier returns the Throttling tier via the vendor extension.
func (swagger *MgwSwagger) GetXWso2ThrottlingTier() string {
	return swagger.xWso2ThrottlingTier
}

// GetDisableSecurity returns the authType via the vendor extension.
func (swagger *MgwSwagger) GetDisableSecurity() bool {
	return swagger.disableSecurity
}

// GetID returns the Id of the API
func (swagger *MgwSwagger) GetID() string {
	return swagger.id
}

// GetXWso2RequestBodyPass returns boolean value to indicate
// whether it is allowed to pass request body to the enforcer or not.
func (swagger *MgwSwagger) GetXWso2RequestBodyPass() bool {
	return swagger.xWso2RequestBodyPass
}

// SetID set the Id of the API
func (swagger *MgwSwagger) SetID(id string) {
	swagger.id = id
}

// SetName sets the name of the API
func (swagger *MgwSwagger) SetName(name string) {
	swagger.title = name
}

// SetSecurityScheme sets the securityschemes of the API
func (swagger *MgwSwagger) SetSecurityScheme(securityScheme []SecurityScheme) {
	swagger.securityScheme = securityScheme
}

// SetSecurity sets the API level security of the API. These refer to the security schemes
// defined for the same API and would have the structure given below,
//
// security:
//	- PetstoreAuth:
// 		- 'write:pets'
//		- 'read:pets'
//	- ApiKeyAuth: []
//
func (swagger *MgwSwagger) SetSecurity(security []map[string][]string) {
	swagger.security = security
}

// SetVersion sets the version of the API
func (swagger *MgwSwagger) SetVersion(version string) {
	swagger.version = version
}

// SetXWso2AuthHeader sets the authHeader of the API
func (swagger *MgwSwagger) SetXWso2AuthHeader(authHeader string) {
	if swagger.xWso2AuthHeader == "" {
		swagger.xWso2AuthHeader = authHeader
	}
}

// GetXWSO2AuthHeader returns the auth header set via the vendor extension.
func (swagger *MgwSwagger) GetXWSO2AuthHeader() string {
	return swagger.xWso2AuthHeader
}

// GetSecurityScheme returns the securitySchemes of the API
func (swagger *MgwSwagger) GetSecurityScheme() []SecurityScheme {
	return swagger.securityScheme
}

// GetSecurity returns the API level security of the API
func (swagger *MgwSwagger) GetSecurity() []map[string][]string {
	return swagger.security
}

// SetOperationPolicies this will merge operation level policies provided in api yaml
func (swagger *MgwSwagger) SetOperationPolicies(apiProject ProjectAPI) {
	for _, resource := range swagger.resources {
		path := strings.TrimSuffix(resource.path, "/")
		for _, operation := range resource.methods {
			method := operation.method
			for _, yamlOperation := range apiProject.APIYaml.Data.Operations {
				if strings.TrimSuffix(yamlOperation.Target, "/") == path && strings.EqualFold(method, yamlOperation.Verb) {
					operation.policies = apiProject.Policies.GetFormattedOperationalPolicies(yamlOperation.OperationPolicies, swagger)
					break
				}
			}
		}
	}
}

// SanitizeAPISecurity this will validate api level and operation level swagger security
// if apiyaml security is provided swagger security will be removed accordingly
func (swagger *MgwSwagger) SanitizeAPISecurity(isYamlAPIKey bool, isYamlOauth bool) {
	isOverrideSecurityByYaml := isYamlAPIKey || isYamlOauth
	apiSecurityDefinitionNames := []string{}
	overridenAPISecurityDefinitions := []SecurityScheme{}

	if isYamlAPIKey {
		//creating security definition for apikey in header in behalf of apim yaml security
		overridenAPISecurityDefinitions = append(overridenAPISecurityDefinitions,
			SecurityScheme{DefinitionName: constants.APIMAPIKeyInHeader,
				Type: constants.APIKeyTypeInOAS, Name: constants.APIKeyNameWithApim, In: constants.APIKeyInHeaderOAS})

		//creating security definition for apikey in query in behalf of apim yaml security
		overridenAPISecurityDefinitions = append(overridenAPISecurityDefinitions,
			SecurityScheme{DefinitionName: constants.APIMAPIKeyInQuery, Type: constants.APIKeyTypeInOAS,
				Name: constants.APIKeyNameWithApim, In: constants.APIKeyInQueryOAS})
	}
	for _, securityDef := range swagger.securityScheme {
		//read default oauth2 security with scopes when oauth2 enabled
		if isYamlOauth && securityDef.DefinitionName == constants.APIMDefaultOauth2Security {
			overridenAPISecurityDefinitions = append(overridenAPISecurityDefinitions, securityDef)
			apiSecurityDefinitionNames = append(apiSecurityDefinitionNames, securityDef.DefinitionName)
		} else if !isOverrideSecurityByYaml {
			apiSecurityDefinitionNames = append(apiSecurityDefinitionNames, securityDef.DefinitionName)
		}
	}
	if isOverrideSecurityByYaml {
		logger.LoggerXds.Debugf("Security definitions are overriden according to api.yaml for API %v:%v",
			swagger.title, swagger.version)
		swagger.SetSecurityScheme(overridenAPISecurityDefinitions)
	}
	logger.LoggerXds.Debugf("Security definitions for API %v:%v : %v", swagger.title, swagger.version,
		swagger.securityScheme)

	//sanitize API level security
	sanitizedAPISecurity := []map[string][]string{}
	for _, security := range swagger.security {
		for securityDefName := range security {
			if arrayContains(apiSecurityDefinitionNames, securityDefName) {
				sanitizedAPISecurity = append(sanitizedAPISecurity, security)
			} else {
				logger.LoggerXds.Warnf("A security definition for %v has not found in API %v:%v",
					securityDefName, swagger.title, swagger.version)
			}
		}
	}
	// Adding api level security when api.yaml apikey security is provided
	if isYamlAPIKey {
		sanitizedAPISecurity = append(sanitizedAPISecurity, map[string][]string{constants.APIMAPIKeyInHeader: {}})
		sanitizedAPISecurity = append(sanitizedAPISecurity, map[string][]string{constants.APIMAPIKeyInQuery: {}})
	}
	swagger.security = sanitizedAPISecurity

	//sanitize operation level security
	for _, resource := range swagger.resources {
		for _, operation := range resource.GetMethod() {
			sanitizedOperationSecurity := []map[string][]string{}
			for _, security := range operation.GetSecurity() {
				for securityDefName := range security {
					if arrayContains(apiSecurityDefinitionNames, securityDefName) {
						sanitizedOperationSecurity = append(sanitizedOperationSecurity, security)
					} else {
						logger.LoggerXds.Warnf("A security definition for %v has not found in API %v:%v",
							securityDefName, swagger.title, swagger.version)
					}
				}
			}
			// has do the following to enable apikey as well when default oauth2 security is in operation level
			if isOverrideSecurityByYaml && isYamlOauth && isYamlAPIKey {
				sanitizedOperationSecurity = append(sanitizedOperationSecurity, map[string][]string{constants.APIMAPIKeyInHeader: {}})
				sanitizedOperationSecurity = append(sanitizedOperationSecurity, map[string][]string{constants.APIMAPIKeyInQuery: {}})
			}
			operation.SetSecurity(sanitizedOperationSecurity)
		}
	}
}

// SetXWso2Extensions set the MgwSwagger object with the properties
// extracted from vendor extensions.
// xWso2Basepath, xWso2ProductionEndpoints, and xWso2SandboxEndpoints are assigned
// based on the vendor extensions.
//
// Resource level properties (xwso2ProductionEndpoints and xWso2SandboxEndpoints are
// also populated at the same time).
func (swagger *MgwSwagger) SetXWso2Extensions() error {
	swagger.setXWso2Basepath()

	xWso2EPErr := swagger.setXWso2Endpoints()
	if xWso2EPErr != nil {
		logger.LoggerOasparser.Error("Error while adding x-wso2-endpoints. ", xWso2EPErr)
		return xWso2EPErr
	}

	apiLevelProdEPFound, productionEndpointErr := swagger.setXWso2ProductionEndpoint()
	if productionEndpointErr != nil {
		logger.LoggerOasparser.Error("Error while adding x-wso2-production-endpoints. ", productionEndpointErr)
		return productionEndpointErr
	}

	apiLevelSandEPFound, sandboxEndpointErr := swagger.setXWso2SandboxEndpoint()
	if sandboxEndpointErr != nil {
		logger.LoggerOasparser.Error("Error while adding x-wso2-sandbox-endpoints. ", sandboxEndpointErr)
		return sandboxEndpointErr
	}

	// to remove swagger server/host urls being added when x-wso2-sandbox-endpoints is given
	if !apiLevelProdEPFound && apiLevelSandEPFound && swagger.productionEndpoints != nil &&
		len(swagger.productionEndpoints.Endpoints) > 0 {
		swagger.productionEndpoints = nil
	}

	swagger.setXWso2Cors()
	swagger.setXWso2ThrottlingTier()
	swagger.setDisableSecurity()
	swagger.setXWso2AuthHeader()

	// Error nil for successful execution
	return nil
}

// SetEnvLabelProperties sets environment specific values
func (swagger *MgwSwagger) SetEnvLabelProperties(envProps synchronizer.APIEnvProps) {
	var productionUrls []Endpoint
	var sandboxUrls []Endpoint

	if envProps.APIConfigs.ProductionEndpoint != "" {
		logger.LoggerOasparser.Infof("Production endpoints are found in env properties for %v : %v",
			swagger.title, swagger.version)
		endpoint, err := getHTTPEndpoint(envProps.APIConfigs.ProductionEndpoint)
		if err == nil {
			productionUrls = append(productionUrls, *endpoint)
		} else {
			logger.LoggerOasparser.Errorf("error encountered when parsing the production endpoints in env properties for %v : %v",
				swagger.title, swagger.version)
		}
	}

	if len(productionUrls) > 0 {
		logger.LoggerOasparser.Infof("Production endpoints is overridden by env properties %v : %v", swagger.title, swagger.version)
		swagger.productionEndpoints = generateEndpointCluster(constants.ProdClustersConfigNamePrefix, productionUrls, constants.LoadBalance)
	}

	if envProps.APIConfigs.SandBoxEndpoint != "" {
		logger.LoggerOasparser.Infof("Sandbox endpoints are found in env properties %v : %v", swagger.title, swagger.version)
		endpoint, err := getHTTPEndpoint(envProps.APIConfigs.SandBoxEndpoint)
		if err == nil {
			sandboxUrls = append(sandboxUrls, *endpoint)
		} else {
			logger.LoggerOasparser.Errorf("error encountered when parsing the production endpoints in env properties %v : %v",
				swagger.title, swagger.version)
		}
	}

	if len(sandboxUrls) > 0 {
		logger.LoggerOasparser.Infof("Sandbox endpoints is overridden by env properties %v : %v", swagger.title, swagger.version)
		swagger.sandboxEndpoints = generateEndpointCluster(constants.SandClustersConfigNamePrefix, sandboxUrls, constants.LoadBalance)
	}
}

// SetEnvVariables sets environment specific values to the mgwswagger
func (swagger *MgwSwagger) SetEnvVariables(apiHashValue string) {
	productionEndpoints, sandboxEndpoints := retrieveEndpointsFromEnv(apiHashValue)
	if len(productionEndpoints) > 0 {
		logger.LoggerOasparser.Infof("Applying production endpoints provided in env variables for API %v : %v", swagger.title, swagger.version)
		swagger.productionEndpoints.EndpointPrefix = constants.ProdClustersConfigNamePrefix
		swagger.productionEndpoints.Endpoints = productionEndpoints
		swagger.productionEndpoints.EndpointType = constants.LoadBalance

	}
	if len(sandboxEndpoints) > 0 {
		logger.LoggerOasparser.Infof("Applying sandbox endpoints provided in env variables for API %v : %v", swagger.title, swagger.version)
		swagger.sandboxEndpoints.EndpointPrefix = constants.SandClustersConfigNamePrefix
		swagger.sandboxEndpoints.Endpoints = sandboxEndpoints
		swagger.sandboxEndpoints.EndpointType = constants.LoadBalance
	}

	// retrieving security credentials from environment variables
	if swagger.productionEndpoints != nil && swagger.productionEndpoints.SecurityConfig.Enabled {
		swagger.productionEndpoints.SecurityConfig = RetrieveEndpointBasicAuthCredentialsFromEnv(apiHashValue,
			"prod", swagger.productionEndpoints.SecurityConfig)
	}
	if swagger.sandboxEndpoints != nil && swagger.sandboxEndpoints.SecurityConfig.Enabled {
		swagger.sandboxEndpoints.SecurityConfig = RetrieveEndpointBasicAuthCredentialsFromEnv(apiHashValue, "sand",
			swagger.sandboxEndpoints.SecurityConfig)
	}
}

// SetSandboxEndpoints set the MgwSwagger object with the SandboxEndpoint when
// it is not populated by SetXWso2Extensions
func (swagger *MgwSwagger) SetSandboxEndpoints(sandboxEndpoints []Endpoint) {
	swagger.sandboxEndpoints = generateEndpointCluster(constants.SandClustersConfigNamePrefix, sandboxEndpoints, constants.LoadBalance)
}

// SetProductionEndpoints set the MgwSwagger object with the productionEndpoint when
// it is not populated by SetXWso2Extensions
func (swagger *MgwSwagger) SetProductionEndpoints(productionEndpoints []Endpoint) {
	swagger.productionEndpoints = generateEndpointCluster(constants.ProdClustersConfigNamePrefix, productionEndpoints, constants.LoadBalance)
}

func (swagger *MgwSwagger) setXWso2ProductionEndpoint() (bool, error) {
	apiLevelEPFound := false
	xWso2APIEndpoints, err := swagger.getEndpoints(swagger.vendorExtensions, constants.XWso2ProdEndpoints)
	if xWso2APIEndpoints != nil {
		swagger.productionEndpoints = xWso2APIEndpoints
		apiLevelEPFound = true
	} else if err != nil {
		return apiLevelEPFound, errors.New("error encountered when extracting endpoints. " + err.Error())
	}

	//resources
	for i, resource := range swagger.resources {
		xwso2ResourceEndpoints, err := swagger.getEndpoints(resource.vendorExtensions, constants.XWso2ProdEndpoints)
		if err != nil {
			return apiLevelEPFound, errors.New("error encountered when extracting resource endpoints for API with basepath: " +
				swagger.xWso2Basepath + ". " + err.Error())
		} else if xwso2ResourceEndpoints != nil {
			swagger.resources[i].productionEndpoints = xwso2ResourceEndpoints
		}
	}

	return apiLevelEPFound, nil
}

func (swagger *MgwSwagger) setXWso2SandboxEndpoint() (bool, error) {
	apiLevelEPFound := false
	xWso2APIEndpoints, err := swagger.getEndpoints(swagger.vendorExtensions, constants.XWso2SandbxEndpoints)
	if xWso2APIEndpoints != nil {
		swagger.sandboxEndpoints = xWso2APIEndpoints
		apiLevelEPFound = true
	} else if err != nil {
		return apiLevelEPFound, errors.New("error encountered when extracting endpoints")
	}

	// resources
	for i, resource := range swagger.resources {
		xwso2ResourceEndpoints, err := swagger.getEndpoints(resource.vendorExtensions, constants.XWso2SandbxEndpoints)
		if err != nil {
			return apiLevelEPFound, errors.New("error encountered when extracting resource endpoints for API with basepath: " +
				swagger.xWso2Basepath + ". " + err.Error())
		} else if xwso2ResourceEndpoints != nil {
			swagger.resources[i].sandboxEndpoints = xwso2ResourceEndpoints
		}
	}
	return apiLevelEPFound, nil
}

// GetXWso2Endpoints get x-wso2-endpoints
func (swagger *MgwSwagger) setXWso2Endpoints() error {
	endpointClusters := make(map[string]*EndpointCluster)
	if val, found := swagger.vendorExtensions[constants.XWso2endpoints]; found {
		if val1, ok := val.([]interface{}); ok {
			for _, val2 := range val1 { // loop thorough multiple endpoints
				if eps, ok := val2.(map[string]interface{}); ok {
					for epName := range eps { // epName is endpoint's name
						endpointCluster, err := swagger.getEndpoints(eps, epName)
						if err == nil && endpointCluster != nil {
							endpointClusters[epName] = endpointCluster
						} else if err != nil {
							return errors.New("error encountered when extracting x-wso2-endpoints")
						}
					}
				}
			}
			swagger.xWso2Endpoints = endpointClusters
			return nil
		}
		return errors.New("error while parsing x-wso2-endpoints extension")
	}
	return nil
}

// SetEndpointsConfig set configs for Endpoints sent by api.yaml
func (endpointCluster *EndpointCluster) SetEndpointsConfig(endpointInfos []EndpointInfo) error {
	if len(endpointInfos) == 0 {
		return nil
	}
	if endpointCluster.Config == nil {
		endpointCluster.Config = &EndpointConfig{}
	}
	// Set timeout
	if endpointCluster.Config.TimeoutInMillis == 0 {
		timeout := endpointInfos[0].Config.ActionDuration
		if timeout != "" {
			routeTimeout, err := strconv.ParseInt(timeout, 10, 32)
			if err != nil {
				return err
			}
			endpointCluster.Config.TimeoutInMillis = uint32(routeTimeout)
		}
	}

	// retry
	if endpointCluster.Config.RetryConfig == nil {
		retryCount := endpointInfos[0].Config.RetryTimeOut
		if retryCount != "" {
			count, err := strconv.ParseInt(retryCount, 10, 32)
			if err != nil {
				return err
			}
			conf, _ := config.ReadConfigs()
			retryConfig := &RetryConfig{
				Count:       int32(count),
				StatusCodes: conf.Envoy.Upstream.Retry.StatusCodes,
			}
			endpointCluster.Config.RetryConfig = retryConfig
		}
	}
	return nil
}

func (swagger *MgwSwagger) setXWso2ThrottlingTier() {
	tier := ResolveThrottlingTier(swagger.vendorExtensions)
	if tier != "" {
		swagger.xWso2ThrottlingTier = tier
	}
}

// SetXWSO2AuthHeader sets the AuthHeader of the API
func (swagger *MgwSwagger) setXWso2AuthHeader() {
	authorizationHeader := getXWso2AuthHeader(swagger.vendorExtensions)
	if authorizationHeader != "" {
		swagger.xWso2AuthHeader = authorizationHeader
	}
}

func (swagger *MgwSwagger) setDisableSecurity() {
	swagger.disableSecurity = ResolveDisableSecurity(swagger.vendorExtensions)
}

// Validate method confirms that the mgwSwagger has all required fields in the required format.
// This needs to be checked prior to generate router/enforcer related resources.
func (swagger *MgwSwagger) Validate() error {
	if swagger.EndpointImplementationType != constants.MockedOASEndpointType {
		if (swagger.productionEndpoints == nil || len(swagger.productionEndpoints.Endpoints) == 0) &&
			(swagger.sandboxEndpoints == nil || len(swagger.sandboxEndpoints.Endpoints) == 0) {

			logger.LoggerOasparser.Errorf("No Endpoints are provided for the API %s:%s",
				swagger.title, swagger.version)
			return errors.New("no endpoints are provided for the API")
		}
		err := swagger.productionEndpoints.validateEndpointCluster("API level production")
		if err != nil {
			logger.LoggerOasparser.Errorf("Error while parsing the production endpoints of the API %s:%s - %v",
				swagger.title, swagger.version, err)
			return err
		}
		err = swagger.sandboxEndpoints.validateEndpointCluster("API level sandbox")
		if err != nil {
			logger.LoggerOasparser.Errorf("Error while parsing the sandbox endpoints of the API %s:%s - %v",
				swagger.title, swagger.version, err)
			return err
		}

		for _, res := range swagger.resources {
			err := res.productionEndpoints.validateEndpointCluster("Resource level production")
			if err != nil {
				logger.LoggerOasparser.Errorf("Error while parsing the production endpoints of the API %s:%s - %v",
					swagger.title, swagger.version, err)
				return err
			}
			err = res.sandboxEndpoints.validateEndpointCluster("Resource level sandbox")
			if err != nil {
				logger.LoggerOasparser.Errorf("Error while parsing the sandbox endpoints of the API %s:%s - %v",
					swagger.title, swagger.version, err)
				return err
			}
		}
	}
	err := swagger.validateBasePath()
	if err != nil {
		logger.LoggerOasparser.Errorf("Error while parsing the API %s:%s - %v", swagger.title, swagger.version, err)
		return err
	}
	return nil
}

func (swagger *MgwSwagger) validateBasePath() error {
	if swagger.xWso2Basepath == "" {
		return errors.New("empty Basepath is provided. Provide a non empty context either using the x-wso2-basePath extension," +
			" or else 'basePath' (if OpenAPI v2) or a 'servers' entry (if OpenAPI v3)")
	} else if match, _ := regexp.MatchString("^[/][a-zA-Z0-9~/_.-]*$", swagger.xWso2Basepath); !match {
		return errors.New("invalid basepath. Does not start with / or includes invalid characters")
	}
	return nil
}

func (endpoint *Endpoint) validateEndpoint() error {
	if len(endpoint.ServiceDiscoveryString) > 0 {
		return nil
	}
	if endpoint.Port == 0 || endpoint.Port > 65535 {
		return errors.New("endpoint port value should be between 0 and 65535")
	}
	if len(endpoint.Host) == 0 {
		return errors.New("empty Hostname is provided")
	}
	if strings.HasPrefix(endpoint.Host, "/") {
		return errors.New("relative paths are not supported as endpoint URLs")
	}
	urlString := endpoint.URLType + "://" + endpoint.Host
	_, err := url.ParseRequestURI(urlString)
	return err
}

func (retryConfig *RetryConfig) validateRetryConfig() {
	conf, _ := config.ReadConfigs()
	maxConfigurableCount := conf.Envoy.Upstream.Retry.MaxRetryCount
	if retryConfig.Count > int32(maxConfigurableCount) || retryConfig.Count < 0 {
		logger.LoggerOasparser.Errorf("Retry count for the API must be within the range 0 - %v."+
			"Reconfiguring retry count as %v", maxConfigurableCount, maxConfigurableCount)
		retryConfig.Count = int32(maxConfigurableCount)
	}
	var validStatusCodes []uint32
	for _, statusCode := range retryConfig.StatusCodes {
		if statusCode > 598 || statusCode < 401 {
			logger.LoggerOasparser.Errorf("Given status code for the API retry config is invalid." +
				"Must be in the range 401 - 598. Dropping the status code.")
		} else {
			validStatusCodes = append(validStatusCodes, statusCode)
		}
	}
	if len(validStatusCodes) < 1 {
		validStatusCodes = append(validStatusCodes, conf.Envoy.Upstream.Retry.StatusCodes...)
	}
	retryConfig.StatusCodes = validStatusCodes
}

func (endpointCluster *EndpointCluster) validateEndpointCluster(endpointName string) error {
	if endpointCluster != nil && len(endpointCluster.Endpoints) > 0 {
		var err error
		for _, endpoint := range endpointCluster.Endpoints {
			err = endpoint.validateEndpoint()
			if err != nil {
				logger.LoggerOasparser.Errorf("Error while parsing the %s endpoints. %v",
					endpointName, err)
				return err
			}
		}

		if endpointCluster.Config != nil {
			// Validate retry
			if endpointCluster.Config.RetryConfig != nil {
				endpointCluster.Config.RetryConfig.validateRetryConfig()
			}
			// Validate timeout
			conf, _ := config.ReadConfigs()
			maxTimeoutInMillis := conf.Envoy.Upstream.Timeouts.MaxRouteTimeoutInSeconds * 1000
			if endpointCluster.Config.TimeoutInMillis > maxTimeoutInMillis {
				endpointCluster.Config.TimeoutInMillis = maxTimeoutInMillis
			}
		}
	}
	return nil
}

// getEndpoints extracts and generate the EndpointCluster Object from any yaml map that has the following structure
//   endpoint-name:
// 		urls:
// 			- <endpoint-URL-1>
// 			- <endpoint-URL-2>
//		type: <loadbalance or failover>
//		advanceEndpointConfig:
//			<the configs>
func (swagger *MgwSwagger) getEndpoints(vendorExtensions map[string]interface{}, endpointName string) (*EndpointCluster, error) {

	// TODO: (VirajSalaka) x-wso2-production-endpoint 's type does not represent http/https, instead it indicates loadbalance and failover
	if endpointClusterYaml, found := vendorExtensions[endpointName]; found {
		if endpointClusterMap, ok := endpointClusterYaml.(map[string]interface{}); ok {
			endpointPrefix := endpointName + "_" + constants.XWso2EPClustersConfigNamePrefix
			if strings.EqualFold(endpointName, constants.XWso2ProdEndpoints) {
				endpointPrefix = constants.ProdClustersConfigNamePrefix
			} else if strings.EqualFold(endpointName, constants.XWso2SandbxEndpoints) {
				endpointPrefix = constants.SandClustersConfigNamePrefix
			}
			endpointCluster := EndpointCluster{
				EndpointPrefix: endpointPrefix,
			}
			// Set URLs
			if urlsProperty, found := endpointClusterMap[constants.Urls]; found {
				if urlsArray, ok := urlsProperty.([]interface{}); ok {
					endpoints, err := processEndpointUrls(urlsArray)
					if err != nil {
						return nil, err
					}
					endpointCluster.Endpoints = endpoints
					endpointCluster.EndpointType = constants.LoadBalance
				} else {
					return nil, errors.New("Error while parsing array of urls in " + endpointName)
				}
			} else {
				// TODO: (VirajSalaka) Throw an error and catch from an upper layer where the API name is visible.
				errMsg := "urls property is not provided with the " + endpointName + " extension"
				logger.LoggerOasparser.Error(errMsg)
				return nil, errors.New(errMsg)
			}

			// Update Endpoint Cluster type
			if epType, found := endpointClusterMap[constants.Type]; found {
				if endpointType, ok := epType.(string); ok {
					endpointCluster.EndpointType = endpointType
				}
			}
			// Set Endpoint Config
			if advanceEndpointConfig, found := endpointClusterMap[constants.AdvanceEndpointConfig]; found {
				if configMap, ok := advanceEndpointConfig.(map[string]interface{}); ok {
					var endpointConfig EndpointConfig
					err := parser.Decode(configMap, &endpointConfig)
					if err != nil {
						return nil, errors.New("Invalid schema for advanceEndpointConfig in " + endpointName)
					}
					endpointCluster.Config = &endpointConfig
				} else {
					return nil, errors.New("Invalid structure for advanceEndpointConfig in " + endpointName)
				}
			}
			// Set Endpoint Config
			if securityConfig, found := endpointClusterMap[constants.SecurityConfig]; found {
				if configMap, ok := securityConfig.(map[string]interface{}); ok {
					var epSecurity EndpointSecurity
					err := parser.Decode(configMap, &epSecurity)
					if err != nil {
						return nil, errors.New("Invalid schema for securityConfig in API " + swagger.title +
							" : " + swagger.version + "for " + endpointName)
					}
					if !strings.EqualFold("BASIC", epSecurity.Type) {
						return nil, errors.New("endpoint security type : " + epSecurity.Type +
							" is not currently supported with WSO2 Choreo Connect")
					}
					epSecurity.Enabled = true
					endpointCluster.SecurityConfig = epSecurity
				}
			}
			return &endpointCluster, nil
		} else if endpointRef, ok := endpointClusterYaml.(string); ok &&
			(strings.EqualFold(endpointName, constants.XWso2ProdEndpoints) || strings.EqualFold(endpointName, constants.XWso2SandbxEndpoints)) {
			refPrefix := "#/" + constants.XWso2endpoints + "/"
			if strings.HasPrefix(endpointRef, refPrefix) {
				epName := strings.TrimPrefix(endpointRef, refPrefix)
				if _, found := swagger.xWso2Endpoints[epName]; found {
					return swagger.xWso2Endpoints[epName], nil
				}
				return nil, errors.New("Invalid endpoint reference " + endpointRef)
			}

		}
		logger.LoggerOasparser.Errorf("%v OpenAPI extension does not adhere with the schema", endpointName)
		return nil, errors.New("invalid map structure detected")
	}
	return nil, nil // the vendor extension for prod or sandbox just isn't present
}

func processEndpointUrls(urlsArray []interface{}) ([]Endpoint, error) {
	var endpoints []Endpoint
	for _, v := range urlsArray {
		if svcdiscovery.IsServiceDiscoveryEnabled && svcdiscovery.IsDiscoveryServiceEndpoint(v.(string)) {
			logger.LoggerOasparser.Debug("Consul query syntax found: ", v.(string))
			queryString, defHost, err := svcdiscovery.ParseConsulSyntax(v.(string))
			if err != nil {
				logger.LoggerOasparser.Error("Consul syntax parse error ", err)
				continue
			}
			endpoint, err := getHTTPEndpoint(defHost)
			if err == nil {
				endpoint.ServiceDiscoveryString = queryString
				endpoints = append(endpoints, *endpoint)
			} else {
				return nil, err
			}
		} else {
			endpoint, err := getHTTPEndpoint(v.(string))
			if err == nil {
				endpoints = append(endpoints, *endpoint)
			} else {
				return nil, err
			}
		}
	}
	return endpoints, nil
}

func (swagger *MgwSwagger) setXWso2Basepath() {
	extBasepath := getXWso2Basepath(swagger.vendorExtensions)
	if extBasepath != "" {
		swagger.xWso2Basepath = extBasepath
	}
}

func (swagger *MgwSwagger) setXWso2Cors() {
	if cors, corsFound := swagger.vendorExtensions[constants.XWso2Cors]; corsFound {
		logger.LoggerOasparser.Debugf("%v configuration is available", constants.XWso2Cors)
		if parsedCors, parsedCorsOk := cors.(map[string]interface{}); parsedCorsOk {
			//Default CorsConfiguration
			corsConfig := &CorsConfig{
				Enabled: true,
			}
			err := parser.Decode(parsedCors, &corsConfig)
			if err != nil {
				logger.LoggerOasparser.Errorf("Error while parsing %v: "+err.Error(), constants.XWso2Cors)
				return
			}
			if corsConfig.Enabled {
				logger.LoggerOasparser.Debugf("API Level Cors Configuration is applied : %+v\n", corsConfig)
				swagger.xWso2Cors = corsConfig
				return
			}
			swagger.xWso2Cors = generateGlobalCors()
			return
		}
		logger.LoggerOasparser.Errorf("Error while parsing %v .", constants.XWso2Cors)
	} else {
		swagger.xWso2Cors = generateGlobalCors()
	}
}

func generateEndpointCluster(endpointPrefix string, endpoints []Endpoint, endpointType string) *EndpointCluster {
	if len(endpoints) > 0 {
		endpointCluster := EndpointCluster{
			EndpointPrefix: endpointPrefix,
			Endpoints:      endpoints,
			EndpointType:   endpointType,
		}
		return &endpointCluster
	}
	return nil
}

func generateGlobalCors() *CorsConfig {
	conf, _ := config.ReadConfigs()
	logger.LoggerOasparser.Debug("CORS policy is applied from global configuration.")
	return &CorsConfig{
		Enabled:                       conf.Envoy.Cors.Enabled,
		AccessControlAllowCredentials: conf.Envoy.Cors.AllowCredentials,
		AccessControlAllowOrigins:     conf.Envoy.Cors.AllowOrigins,
		AccessControlAllowHeaders:     conf.Envoy.Cors.AllowHeaders,
		AccessControlAllowMethods:     conf.Envoy.Cors.AllowMethods,
		AccessControlExposeHeaders:    conf.Envoy.Cors.ExposeHeaders,
	}
}

//GetOperationInterceptors returns operation interceptors
func (swagger *MgwSwagger) GetOperationInterceptors(apiInterceptor InterceptEndpoint, resourceInterceptor InterceptEndpoint, operations []*Operation, isIn bool) map[string]InterceptEndpoint {
	interceptorOperationMap := make(map[string]InterceptEndpoint)

	for _, op := range operations {
		extensionName := constants.XWso2RequestInterceptor
		// first get operational policies
		operationInterceptor := op.GetCallInterceptorService(isIn)
		// if operational policy interceptor not given check operational level swagger extension
		if !operationInterceptor.Enable {
			if !isIn {
				extensionName = constants.XWso2ResponseInterceptor
			}
			operationInterceptor = swagger.GetInterceptor(op.GetVendorExtensions(), extensionName, constants.OperationLevelInterceptor)
		}
		operationInterceptor.ClusterName = op.iD
		// if operation interceptor not given
		if !operationInterceptor.Enable {
			// assign resource level interceptor
			if resourceInterceptor.Enable {
				operationInterceptor = resourceInterceptor
			} else if apiInterceptor.Enable {
				// if resource interceptor not given add api level interceptor
				operationInterceptor = apiInterceptor
			}
		}
		// add operation to the list only if an interceptor is enabled for the operation
		if operationInterceptor.Enable {
			interceptorOperationMap[strings.ToUpper(op.method)] = operationInterceptor
		}
	}
	return interceptorOperationMap

}

//GetInterceptor returns interceptors
func (swagger *MgwSwagger) GetInterceptor(vendorExtensions map[string]interface{}, extensionName string, level string) InterceptEndpoint {
	var endpointCluster EndpointCluster
	conf, _ := config.ReadConfigs()
	clusterTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
	requestTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
	includesV := &interceptor.RequestInclusions{}

	if x, found := vendorExtensions[extensionName]; found {
		if val, ok := x.(map[string]interface{}); ok {
			//serviceURL mandatory
			if v, found := val[constants.ServiceURL]; found {
				serviceURLV := v.(string)
				endpoint, err := getHTTPEndpoint(serviceURLV)
				if err != nil {
					logger.LoggerOasparser.Error("Error reading interceptors service url value", err)
					return InterceptEndpoint{}
				}
				if endpoint.Basepath != "" {
					logger.LoggerOasparser.Warnf("Interceptor serviceURL basepath is given as %v but it will be ignored",
						endpoint.Basepath)
				}
				endpointCluster.Endpoints = []Endpoint{*endpoint}

			} else {
				logger.LoggerOasparser.Error("Error reading interceptors service url value")
				return InterceptEndpoint{}
			}
			//clusterTimeout optional
			if v, found := val[constants.ClusterTimeout]; found {
				p, err := strconv.ParseInt(fmt.Sprint(v), 0, 0)
				if err == nil {
					clusterTimeoutV = time.Duration(p)
				} else {
					logger.LoggerOasparser.Errorf("Error reading interceptors %v value : %v", constants.ClusterTimeout, err.Error())
				}
			}
			//requestTimeout optional
			if v, found := val[constants.RequestTimeout]; found {
				p, err := strconv.ParseInt(fmt.Sprint(v), 0, 0)
				if err == nil {
					requestTimeoutV = time.Duration(p)
				} else {
					logger.LoggerOasparser.Errorf("Error reading interceptors %v value : %v", constants.RequestTimeout, err.Error())
				}
			}
			//includes optional
			if v, found := val[constants.Includes]; found {
				includes := v.([]interface{})
				if len(includes) > 0 {
					// convert type of includes from "[]interface{}" to "[]string"
					includesStr := make([]string, len(includes))
					for i, v := range includes {
						includesStr[i] = v.(string)
					}
					includesV = GenerateInterceptorIncludes(includesStr)
				}
			}

			return InterceptEndpoint{
				Enable:          true,
				EndpointCluster: endpointCluster,
				ClusterTimeout:  clusterTimeoutV,
				RequestTimeout:  requestTimeoutV,
				Includes:        includesV,
				Level:           level,
			}
		}
		logger.LoggerOasparser.Error("Error parsing response interceptors values to mgwSwagger")
	}
	return InterceptEndpoint{}
}

//GenerateInterceptorIncludes generate includes
func GenerateInterceptorIncludes(includes []string) *interceptor.RequestInclusions {
	includesV := &interceptor.RequestInclusions{}
	for _, include := range includes {
		switch strings.TrimSpace(include) {
		case "request_headers":
			includesV.RequestHeaders = true
		case "request_body":
			includesV.RequestBody = true
		case "request_trailers":
			includesV.RequestTrailer = true
		case "response_headers":
			includesV.ResponseHeaders = true
		case "response_body":
			includesV.ResponseBody = true
		case "response_trailers":
			includesV.ResponseTrailers = true
		case "invocation_context":
			includesV.InvocationContext = true
		}
	}
	return includesV
}

// GetMgwSwagger converts the openAPI v3, v2 and asyncAPI content
// To MgwSwagger objects
func (swagger *MgwSwagger) GetMgwSwagger(apiContent []byte) error {

	definitionJsn, err := utills.ToJSON(apiContent)
	if err != nil {
		logger.LoggerOasparser.Error("Error converting api file to json", err)
		return err
	}
	definitionVersion := utills.FindAPIDefinitionVersion(definitionJsn)

	if definitionVersion == constants.Swagger2 {
		var swaggerSpec spec.Swagger
		err = json.Unmarshal(definitionJsn, &swaggerSpec)
		if err == nil {
			err = swagger.SetInfoSwagger(swaggerSpec)
		}
	} else if definitionVersion == constants.OpenAPI3 {
		var openAPISpec openapi3.Swagger
		err = json.Unmarshal(definitionJsn, &openAPISpec)
		if err == nil {
			err = swagger.SetInfoOpenAPI(openAPISpec)
		}
	} else if definitionVersion == constants.AsyncAPI2 {
		var asyncAPISpec AsyncAPI
		err = json.Unmarshal(definitionJsn, &asyncAPISpec)
		if err == nil {
			err = asyncAPISpec.unmarshallAPILevelVendorExtensions(definitionJsn)
			if err == nil {
				err = swagger.SetInfoAsyncAPI(asyncAPISpec)
			}
		}
	} else {
		return errors.New("API version not specified or not supported")
	}

	if err != nil {
		logger.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error occurred while extracting the API definition to MgwSwagger. %s", err.Error()),
			Severity:  logging.MINOR,
			ErrorCode: 2208,
		})
		return err
	}
	err = swagger.SetXWso2Extensions()
	if err != nil {
		logger.LoggerOasparser.Error("Error occurred while setting x-wso2 extensions for ",
			swagger.GetTitle(), " ", err)
		return err
	}
	return nil
}

//PopulateFromAPIYaml populates the mgwSwagger object for APIs using API.yaml
// TODO - (VirajSalaka) read cors config and populate mgwSwagger feild
func (swagger *MgwSwagger) PopulateFromAPIYaml(apiYaml APIYaml) error {

	data := apiYaml.Data
	// UUID in the generated api.yaml file is considered as swagger.id
	swagger.id = data.ID
	swagger.apiType = data.APIType
	// name and version in api.yaml corresponds to title and version respectively.
	swagger.title = data.Name
	swagger.version = data.Version
	// context value in api.yaml is assigned as xWso2Basepath
	swagger.xWso2Basepath = data.Context + "/" + swagger.version
	swagger.LifecycleStatus = data.LifeCycleStatus
	swagger.IsDefaultVersion = data.IsDefaultVersion

	// Added with both HTTP and WS APIs. x-throttling-tier is not used with WS.
	swagger.xWso2ThrottlingTier = data.APIThrottlingPolicy

	// productionURL & sandBoxURL values are extracted from endpointConfig in api.yaml
	endpointConfig := data.EndpointConfig

	if endpointConfig.ImplementationStatus == constants.Prototyped {
		swagger.IsPrototyped = true
	}

	swagger.EndpointType = endpointConfig.EndpointType
	swagger.EndpointImplementationType = data.EndpointImplementationType

	// from here onwards it will process endpoint info
	// So discontinue if the implementation type is mocked_oas
	if data.EndpointImplementationType == constants.MockedOASEndpointType {
		return nil
	}

	if len(endpointConfig.ProductionEndpoints) > 0 {
		var endpoints []Endpoint
		endpointType := constants.LoadBalance
		var unProcessedURLs []interface{}
		for _, endpointConfig := range endpointConfig.ProductionEndpoints {
			if swagger.apiType == constants.WS {
				prodEndpoint, err := getWebSocketEndpoint(endpointConfig.Endpoint)
				if err == nil {
					endpoints = append(endpoints, *prodEndpoint)
				} else {
					return err
				}
			} else {
				unProcessedURLs = append(unProcessedURLs, endpointConfig.Endpoint)
			}
		}
		if len(endpointConfig.ProductionFailoverEndpoints) > 0 {
			endpointType = constants.FailOver
			for _, endpointConfig := range endpointConfig.ProductionFailoverEndpoints {
				if swagger.apiType == constants.WS {
					failoverEndpoint, err := getWebSocketEndpoint(endpointConfig.Endpoint)
					if err == nil {
						endpoints = append(endpoints, *failoverEndpoint)
					} else {
						return err
					}
				} else {
					unProcessedURLs = append(unProcessedURLs, endpointConfig.Endpoint)
				}
			}
		}
		if swagger.apiType != constants.WS {
			productionEndpoints, err := processEndpointUrls(unProcessedURLs)
			if err == nil {
				endpoints = append(endpoints, productionEndpoints...)
			} else {
				return err
			}
		}
		swagger.productionEndpoints = generateEndpointCluster(constants.ProdClustersConfigNamePrefix, endpoints, endpointType)
	}

	if len(endpointConfig.SandBoxEndpoints) > 0 {
		var endpoints []Endpoint
		endpointType := constants.LoadBalance
		var unProcessedURLs []interface{}
		for _, endpointConfig := range endpointConfig.SandBoxEndpoints {
			if swagger.apiType == constants.WS {
				sandBoxEndpoint, err := getWebSocketEndpoint(endpointConfig.Endpoint)
				if err == nil {
					endpoints = append(endpoints, *sandBoxEndpoint)
				} else {
					return err
				}
			} else {
				unProcessedURLs = append(unProcessedURLs, endpointConfig.Endpoint)
			}
		}
		if len(endpointConfig.SandboxFailoverEndpoints) > 0 {
			endpointType = constants.FailOver
			for _, endpointConfig := range endpointConfig.SandboxFailoverEndpoints {
				if swagger.apiType == constants.WS {
					failoverEndpoint, err := getWebSocketEndpoint(endpointConfig.Endpoint)
					if err == nil {
						endpoints = append(endpoints, *failoverEndpoint)
					} else {
						return err
					}
				} else {
					unProcessedURLs = append(unProcessedURLs, endpointConfig.Endpoint)
				}
			}
		}
		if swagger.apiType != constants.WS {
			sandboxEndpoints, err := processEndpointUrls(unProcessedURLs)
			if err == nil {
				endpoints = append(endpoints, sandboxEndpoints...)
			} else {
				return err
			}
		}
		swagger.sandboxEndpoints = generateEndpointCluster(constants.SandClustersConfigNamePrefix, endpoints, endpointType)
	}

	// if yaml has production security, setting it
	if swagger.productionEndpoints != nil && endpointConfig.APIEndpointSecurity.Production.Enabled {
		if endpointConfig.APIEndpointSecurity.Production.Type != "" && strings.EqualFold("BASIC", endpointConfig.APIEndpointSecurity.Production.Type) {
			swagger.productionEndpoints.SecurityConfig = endpointConfig.APIEndpointSecurity.Production
		} else {
			endpointConfig.APIEndpointSecurity.Production.Enabled = false
			logger.LoggerXds.Errorf("endpoint security type given in api.yaml : %v is not currently supported with WSO2 Choreo Connect",
				endpointConfig.APIEndpointSecurity.Production.Type)
		}
	}
	// if yaml has sandbox security, setting it
	if swagger.sandboxEndpoints != nil && endpointConfig.APIEndpointSecurity.Sandbox.Enabled {
		if endpointConfig.APIEndpointSecurity.Sandbox.Type != "" && strings.EqualFold("BASIC", endpointConfig.APIEndpointSecurity.Sandbox.Type) {
			swagger.sandboxEndpoints.SecurityConfig = endpointConfig.APIEndpointSecurity.Sandbox
		} else {
			endpointConfig.APIEndpointSecurity.Sandbox.Enabled = false
			logger.LoggerXds.Errorf("endpoint security type given in api.yaml : %v is not currently supported with WSO2 Choreo Connect",
				endpointConfig.APIEndpointSecurity.Sandbox.Type)
		}
	}
	return nil
}
