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
	"errors"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	parser "github.com/mitchellh/mapstructure"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/interceptor"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/svcdiscovery"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

// MgwSwagger represents the object structure holding the information related to the
// openAPI object. The values are populated from the extensions/properties mentioned at
// the root level of the openAPI definition. The pathItem level information is represented
// by the resources array which contains the MgwResource entries.
type MgwSwagger struct {
	id                  string
	UUID                string
	apiType             string
	description         string
	title               string
	version             string
	vendorExtensions    map[string]interface{}
	productionEndpoints *EndpointCluster
	sandboxEndpoints    *EndpointCluster
	resources           []Resource
	xWso2Basepath       string
	xWso2Cors           *CorsConfig
	securityScheme      []SecurityScheme
	xWso2ThrottlingTier string
	xWso2AuthHeader     string
	disableSecurity     bool
	OrganizationID      string
}

// EndpointCluster represent an upstream cluster
type EndpointCluster struct {
	EndpointName string
	Endpoints    []Endpoint
	// EndpointType enum {failover, loadbalance}. if any other value provided, consider as the default value; which is loadbalance
	EndpointType string
	Config       *EndpointConfig
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
	// Arbitrary name used to define security scheme
	// ex: default, x-api-key etc.
	DefinitionName string

	// Type of the security scheme
	// Possible values are apiKey, oauth2
	Type string

	// Name of the security scheme
	// User can define a specific name for above types
	Name string

	// Location of the api key
	// Valid values are query, header
	In string
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

// GetSandEndpoints returns the array of sandbox endpoints.
func (swagger *MgwSwagger) GetSandEndpoints() *EndpointCluster {
	return swagger.sandboxEndpoints
}

// GetResources returns the array of resources (openAPI path level info)
func (swagger *MgwSwagger) GetResources() []Resource {
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

// SetXWso2Extensions set the MgwSwagger object with the properties
// extracted from vendor extensions.
// xWso2Basepath, xWso2ProductionEndpoints, and xWso2SandboxEndpoints are assigned
// based on the vendor extensions.
//
// Resource level properties (xwso2ProductionEndpoints and xWso2SandboxEndpoints are
// also populated at the same time).
func (swagger *MgwSwagger) SetXWso2Extensions() error {
	swagger.setXWso2Basepath()

	productionEndpointErr := swagger.setXWso2ProductionEndpoint()
	if productionEndpointErr != nil {
		return productionEndpointErr
	}

	sandboxEndpointErr := swagger.setXWso2SandboxEndpoint()
	if sandboxEndpointErr != nil {
		return sandboxEndpointErr
	}

	swagger.setXWso2Cors()
	swagger.setXWso2ThrottlingTier()
	swagger.setDisableSecurity()
	swagger.setXWso2AuthHeader()

	// Error nil for successful execution
	return nil
}

// SetEnvProperties sets environment specific values
func (swagger *MgwSwagger) SetEnvProperties(envProps synchronizer.APIEnvProps) {
	var productionUrls []Endpoint
	var sandboxUrls []Endpoint

	if envProps.APIConfigs.ProductionEndpoint != "" {
		logger.LoggerOasparser.Infof("Production endpoints are found in env properties for %v : %v",
			swagger.title, swagger.version)
		endpoint, err := getHostandBasepathandPort(envProps.APIConfigs.ProductionEndpoint)
		if err == nil {
			productionUrls = append(productionUrls, *endpoint)
		} else {
			logger.LoggerOasparser.Errorf("error encountered when parsing the production endpoints in env properties for %v : %v",
				swagger.title, swagger.version)
		}
	}

	if len(productionUrls) > 0 {
		logger.LoggerOasparser.Infof("Production endpoints is overridden by env properties %v : %v", swagger.title, swagger.version)
		swagger.productionEndpoints = generateEndpointCluster(xWso2ProdEndpoints, productionUrls, LoadBalance)
	}

	if envProps.APIConfigs.SandBoxEndpoint != "" {
		logger.LoggerOasparser.Infof("Sandbox endpoints are found in env properties %v : %v", swagger.title, swagger.version)
		endpoint, err := getHostandBasepathandPort(envProps.APIConfigs.SandBoxEndpoint)
		if err == nil {
			sandboxUrls = append(sandboxUrls, *endpoint)
		} else {
			logger.LoggerOasparser.Errorf("error encountered when parsing the production endpoints in env properties %v : %v",
				swagger.title, swagger.version)
		}
	}

	if len(sandboxUrls) > 0 {
		logger.LoggerOasparser.Infof("Sandbox endpoints is overridden by env properties %v : %v", swagger.title, swagger.version)
		swagger.sandboxEndpoints = generateEndpointCluster(xWso2SandbxEndpoints, sandboxUrls, LoadBalance)
	}
}

// SetSandboxEndpoints set the MgwSwagger object with the SandboxEndpoint when
// it is not populated by SetXWso2Extensions
func (swagger *MgwSwagger) SetSandboxEndpoints(sandboxEndpoints []Endpoint) {
	swagger.sandboxEndpoints = generateEndpointCluster(xWso2SandbxEndpoints, sandboxEndpoints, LoadBalance)
}

// SetProductionEndpoints set the MgwSwagger object with the productionEndpoint when
// it is not populated by SetXWso2Extensions
func (swagger *MgwSwagger) SetProductionEndpoints(productionEndpoints []Endpoint) {
	swagger.productionEndpoints = generateEndpointCluster(xWso2ProdEndpoints, productionEndpoints, LoadBalance)
}

func (swagger *MgwSwagger) setXWso2ProductionEndpoint() error {
	xWso2APIEndpoints, err := getEndpoints(swagger.vendorExtensions, xWso2ProdEndpoints)
	if xWso2APIEndpoints != nil {
		swagger.productionEndpoints = xWso2APIEndpoints
	} else if err != nil {
		return errors.New("error encountered when extracting endpoints. " + err.Error())
	}

	//resources
	for i, resource := range swagger.resources {
		xwso2ResourceEndpoints, err := getEndpoints(resource.vendorExtensions, xWso2ProdEndpoints)
		if err != nil {
			return errors.New("error encountered when extracting resource endpoints for API with basepath: " +
				swagger.xWso2Basepath + ". " + err.Error())
		} else if xwso2ResourceEndpoints != nil {
			swagger.resources[i].productionEndpoints = xwso2ResourceEndpoints
		}
	}

	return nil
}

func (swagger *MgwSwagger) setXWso2SandboxEndpoint() error {
	xWso2APIEndpoints, err := getEndpoints(swagger.vendorExtensions, xWso2SandbxEndpoints)
	if xWso2APIEndpoints != nil {
		swagger.sandboxEndpoints = xWso2APIEndpoints
	} else if err != nil {
		return errors.New("error encountered when extracting endpoints")
	}

	// resources
	for i, resource := range swagger.resources {
		xwso2ResourceEndpoints, err := getEndpoints(resource.vendorExtensions, xWso2SandbxEndpoints)
		if err != nil {
			return errors.New("error encountered when extracting resource endpoints for API with basepath: " +
				swagger.xWso2Basepath + ". " + err.Error())
		} else if xwso2ResourceEndpoints != nil {
			swagger.resources[i].sandboxEndpoints = xwso2ResourceEndpoints
		}
	}

	return nil
}

// GetXWso2Endpoints get x-wso2-endpoints
func (swagger *MgwSwagger) GetXWso2Endpoints() ([]*EndpointCluster, error) {
	var endpointClusters []*EndpointCluster
	if val, found := swagger.vendorExtensions[xWso2endpoints]; found {
		if val1, ok := val.([]interface{}); ok {
			for _, val2 := range val1 { // loop thorough multiple endpoints
				if eps, ok := val2.(map[string]interface{}); ok {
					for epName := range eps { // epName is endpoint's name
						endpointCluster, err := getEndpoints(eps, epName)
						if err == nil && endpointCluster != nil {
							endpointClusters = append(endpointClusters, endpointCluster)
						} else if err != nil {
							return endpointClusters, errors.New("error encountered when extracting x-wso2-endpoints")
						}
					}
				}
			}
			return endpointClusters, nil
		}
		return endpointClusters, errors.New("error while parsing x-wso2-endpoints extension")
	}
	return endpointClusters, nil
}

// SetEndpointsConfig set configs for Endpoints sent by api.yaml
func (endpointCluster *EndpointCluster) SetEndpointsConfig(endpointInfos []EndpointInfo) error {
	if endpointInfos == nil || len(endpointInfos) == 0 {
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

// getXWso2AuthHeader extracts the value of xWso2AuthHeader extension.
// if the property is not available, an empty string is returned.
func getXWso2AuthHeader(vendorExtensions map[string]interface{}) string {
	xWso2AuthHeader := ""
	if y, found := vendorExtensions[xAuthHeader]; found {
		if val, ok := y.(string); ok {
			xWso2AuthHeader = val
		}
	}
	return xWso2AuthHeader
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

	err = swagger.validateBasePath()
	if err != nil {
		logger.LoggerOasparser.Errorf("Error while parsing the API %s:%s - %v", swagger.title, swagger.version, err)
		return err
	}
	return nil
}

func (swagger *MgwSwagger) validateBasePath() error {
	if xWso2BasePath == "" {
		return errors.New("empty Basepath is provided. Either use x-wso2-basePath extension or assign basePath (if OpenAPI v2 definition is used)" +
			" / servers entry (if OpenAPI v3 definition is used) with non empty context")
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
func getEndpoints(vendorExtensions map[string]interface{}, endpointName string) (*EndpointCluster, error) {
	endpointCluster := EndpointCluster{
		EndpointName: endpointName,
	}
	// TODO: (VirajSalaka) x-wso2-production-endpoint 's type does not represent http/https, instead it indicates loadbalance and failover
	if endpointClusterYaml, found := vendorExtensions[endpointName]; found {
		if endpointClusterMap, ok := endpointClusterYaml.(map[string]interface{}); ok {
			// Set URLs
			if urlsProperty, found := endpointClusterMap[urls]; found {
				if urlsArray, ok := urlsProperty.([]interface{}); ok {
					endpoints, err := processEndpointUrls(urlsArray)
					if err != nil {
						return nil, err
					}
					endpointCluster.Endpoints = endpoints
					endpointCluster.EndpointType = LoadBalance
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
			if epType, found := endpointClusterMap[typeConst]; found {
				if endpointType, ok := epType.(string); ok {
					endpointCluster.EndpointType = endpointType
				}
			}
			// Set Endpoint Config
			if advanceEndpointConfig, found := endpointClusterMap[AdvanceEndpointConfig]; found {
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
			return &endpointCluster, nil
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
			endpoint, err := getHostandBasepathandPort(defHost)
			if err == nil {
				endpoint.ServiceDiscoveryString = queryString
				endpoints = append(endpoints, *endpoint)
			} else {
				return nil, err
			}
		} else {
			endpoint, err := getHostandBasepathandPort(v.(string))
			if err == nil {
				endpoints = append(endpoints, *endpoint)
			} else {
				return nil, err
			}
		}
	}
	return endpoints, nil
}

// getXWso2Basepath extracts the value of xWso2BasePath extension.
// if the property is not available, an empty string is returned.
func getXWso2Basepath(vendorExtensions map[string]interface{}) string {
	xWso2basepath := ""
	if y, found := vendorExtensions[xWso2BasePath]; found {
		if val, ok := y.(string); ok {
			xWso2basepath = val
		}
	}
	return xWso2basepath
}

func (swagger *MgwSwagger) setXWso2Basepath() {
	extBasepath := getXWso2Basepath(swagger.vendorExtensions)
	if extBasepath != "" {
		swagger.xWso2Basepath = extBasepath
	}
}

func (swagger *MgwSwagger) setXWso2Cors() {
	if cors, corsFound := swagger.vendorExtensions[xWso2Cors]; corsFound {
		logger.LoggerOasparser.Debugf("%v configuration is available", xWso2Cors)
		if parsedCors, parsedCorsOk := cors.(map[string]interface{}); parsedCorsOk {
			//Default CorsConfiguration
			corsConfig := &CorsConfig{
				Enabled: true,
			}
			err := parser.Decode(parsedCors, &corsConfig)
			if err != nil {
				logger.LoggerOasparser.Errorf("Error while parsing %v: "+err.Error(), xWso2Cors)
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
		logger.LoggerOasparser.Errorf("Error while parsing %v .", xWso2Cors)
	} else {
		swagger.xWso2Cors = generateGlobalCors()
	}
}

func generateEndpointCluster(endpointName string, endpoints []Endpoint, endpointType string) *EndpointCluster {
	if len(endpoints) > 0 {
		endpointCluster := EndpointCluster{
			EndpointName: endpointName,
			Endpoints:    endpoints,
			EndpointType: endpointType,
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

// ResolveThrottlingTier extracts the value of x-wso2-throttling-tier and
// x-throttling-tier extension. if x-wso2-throttling-tier is available it
// will be prioritized.
// if both the properties are not available, an empty string is returned.
func ResolveThrottlingTier(vendorExtensions map[string]interface{}) string {
	xTier := ""
	if x, found := vendorExtensions[xWso2ThrottlingTier]; found {
		if val, ok := x.(string); ok {
			xTier = val
		}
	} else if y, found := vendorExtensions[xThrottlingTier]; found {
		if val, ok := y.(string); ok {
			xTier = val
		}
	}
	return xTier
}

// ResolveDisableSecurity extracts the value of x-auth-type extension.
// if the property is not available, false is returned.
// If the API definition is fed from API manager, then API definition contains
// x-auth-type as "None" for non secured APIs. Then the return value would be true.
// If the API definition is fed through apictl, the users can use either
// x-wso2-disable-security : true/false to enable and disable security.
func ResolveDisableSecurity(vendorExtensions map[string]interface{}) bool {
	disableSecurity := false
	y, vExtAuthType := vendorExtensions[xAuthType]
	z, vExtDisableSecurity := vendorExtensions[xWso2DisableSecurity]
	if vExtDisableSecurity {
		// If x-wso2-disable-security is present, then disableSecurity = val
		if val, ok := z.(bool); ok {
			disableSecurity = val
		}
	} else if vExtAuthType {
		// If APIs are published through APIM, all resource levels contains x-auth-type
		// vendor extension.
		if val, ok := y.(string); ok {
			// If the x-auth-type vendor ext is None, then the API/resource is considerd
			// to be non secure
			if val == None {
				disableSecurity = true
			}
		}
	}
	return disableSecurity
}

//GetInterceptor returns interceptors
func (swagger *MgwSwagger) GetInterceptor(vendorExtensions map[string]interface{}, extensionName string) (InterceptEndpoint, error) {
	var endpointCluster EndpointCluster
	conf, _ := config.ReadConfigs()
	clusterTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
	requestTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
	includesV := &interceptor.RequestInclusions{}

	if x, found := vendorExtensions[extensionName]; found {
		if val, ok := x.(map[string]interface{}); ok {
			//serviceURL mandatory
			if v, found := val[serviceURL]; found {
				serviceURLV := v.(string)
				endpoint, err := getHostandBasepathandPort(serviceURLV)
				if err != nil {
					logger.LoggerOasparser.Error("Error reading interceptors service url value", err)
					return InterceptEndpoint{}, errors.New("error reading interceptors service url value")
				}
				if endpoint.Basepath != "" {
					logger.LoggerOasparser.Warnf("Interceptor serviceURL basepath is given as %v but it will be ignored",
						endpoint.Basepath)
				}
				endpointCluster.Endpoints = []Endpoint{*endpoint}

			} else {
				logger.LoggerOasparser.Error("Error reading interceptors service url value")
				return InterceptEndpoint{}, errors.New("error reading interceptors service url value")
			}
			//clusterTimeout optional
			if v, found := val[clusterTimeout]; found {
				p, err := strconv.ParseInt(fmt.Sprint(v), 0, 0)
				if err == nil {
					clusterTimeoutV = time.Duration(p)
				} else {
					logger.LoggerOasparser.Errorf("Error reading interceptors %v value : %v", clusterTimeout, err.Error())
				}
			}
			//requestTimeout optional
			if v, found := val[requestTimeout]; found {
				p, err := strconv.ParseInt(fmt.Sprint(v), 0, 0)
				if err == nil {
					requestTimeoutV = time.Duration(p)
				} else {
					logger.LoggerOasparser.Errorf("Error reading interceptors %v value : %v", requestTimeout, err.Error())
				}
			}
			//includes optional
			if v, found := val[includes]; found {
				includes := v.([]interface{})
				if len(includes) > 0 {
					for _, include := range includes {
						switch include.(string) {
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
				}
			}

			return InterceptEndpoint{
				Enable:          true,
				EndpointCluster: endpointCluster,
				ClusterTimeout:  clusterTimeoutV,
				RequestTimeout:  requestTimeoutV,
				Includes:        includesV,
			}, nil
		}
		return InterceptEndpoint{}, errors.New("error parsing response interceptors values to mgwSwagger")
	}
	return InterceptEndpoint{}, nil
}
