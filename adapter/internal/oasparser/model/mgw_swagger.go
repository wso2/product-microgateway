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
	"strconv"
	"strings"
	"time"

	parser "github.com/mitchellh/mapstructure"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/svcdiscovery"
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
	xWso2Endpoints      []EndpointCluster
	resources           []Resource
	xWso2Basepath       string
	xWso2Cors           *CorsConfig
	securityScheme      []string
	xWso2ThrottlingTier string
	xWso2AuthHeader     string
	disableSecurity     bool
	OrganizationID      string
}

// EndpointCluster represent an upstream cluster
type EndpointCluster struct {
	EndpointName string
	Endpoints    []Endpoint
	EndpointType string // enum allowing failover or loadbalance
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
}

// EndpointConfig holds the configs such as timeout, retry, etc. for the EndpointCluster
type EndpointConfig struct {
	RetryConfig *RetryConfig `json:"retryConfig"`
}

// RetryConfig holds the parameters for retries done by cc to the EndpointCluster
type RetryConfig struct {
	Count       int32    `json:"count"`
	StatusCodes []string `json:"statusCodes"`
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
	Enable         bool
	Host           string
	URLType        string
	Port           uint32
	Path           string
	ClusterName    string
	ClusterTimeout time.Duration
	RequestTimeout int
	// Includes this is an enum allowing only values in
	// {"request_headers", "request_body", "request_trailer", "response_headers", "response_body", "response_trailer",
	//"invocation_context" }
	Includes []string
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

// SetSecurityScheme sets the securityscheme of the API
func (swagger *MgwSwagger) SetSecurityScheme(securityScheme []string) {
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

// GetSetSecurityScheme returns the securityscheme of the API
func (swagger *MgwSwagger) GetSetSecurityScheme() []string {
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

// SetXWso2SandboxEndpointForMgwSwagger set the MgwSwagger object with the SandboxEndpoint when
// it is not populated by SetXWso2Extensions
func (swagger *MgwSwagger) SetXWso2SandboxEndpointForMgwSwagger(sandBoxURL string) error {
	var sandboxEndpoints []Endpoint
	sandboxEndpoint, err := getHostandBasepathandPort(sandBoxURL)
	if err == nil {
		sandboxEndpoints = append(sandboxEndpoints, *sandboxEndpoint)
		swagger.sandboxEndpoints = generateEndpointCluster(xWso2SandbxEndpoints, sandboxEndpoints)
		// Error nil for successful execution
		return nil
	}
	logger.LoggerOasparser.Error("Invalid sandbox endpoint for API with basepath: ", swagger.xWso2Basepath)
	return errors.New("invalid sandbox endpoint")
}

// SetXWso2ProductionEndpointMgwSwagger set the MgwSwagger object with the productionEndpoint when
// it is not populated by SetXWso2Extensions
func (swagger *MgwSwagger) SetXWso2ProductionEndpointMgwSwagger(productionURL string) error {
	var productionEndpoints []Endpoint
	productionEndpoint, err := getHostandBasepathandPort(productionURL)
	if err == nil {
		productionEndpoints = append(productionEndpoints, *productionEndpoint)
		swagger.productionEndpoints = generateEndpointCluster(xWso2ProdEndpoints, productionEndpoints)
		// Error nil for successful execution
		return nil
	}
	logger.LoggerOasparser.Error("Invalid production endpoint for API with basepath: ", swagger.xWso2Basepath)
	return errors.New("invalid production endpoint")
}

func (swagger *MgwSwagger) setXWso2ProductionEndpoint() error {
	xWso2APIEndpoints, err := getXWso2Endpoints(swagger.vendorExtensions, xWso2ProdEndpoints)
	if xWso2APIEndpoints != nil {
		swagger.productionEndpoints = xWso2APIEndpoints
	} else if err != nil {
		return errors.New("error encountered when extracting endpoints. " + err.Error())
	}

	//resources
	for i, resource := range swagger.resources {
		xwso2ResourceEndpoints, err := getXWso2Endpoints(resource.vendorExtensions, xWso2ProdEndpoints)
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
	xWso2APIEndpoints, err := getXWso2Endpoints(swagger.vendorExtensions, xWso2SandbxEndpoints)
	if xWso2APIEndpoints != nil {
		swagger.sandboxEndpoints = xWso2APIEndpoints
	} else if err != nil {
		return errors.New("error encountered when extracting endpoints")
	}

	// resources
	for i, resource := range swagger.resources {
		xwso2ResourceEndpoints, err := getXWso2Endpoints(resource.vendorExtensions, xWso2SandbxEndpoints)
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
func (swagger *MgwSwagger) GetXWso2Endpoints() ([]EndpointCluster, error) {
	var endpointClusters []EndpointCluster
	if val, found := swagger.vendorExtensions[xWso2endpoints]; found {
		if val1, ok := val.([]interface{}); ok {
			for _, val2 := range val1 { // loop thorough multiple endpoints
				if eps, ok := val2.(map[string]interface{}); ok {
					for epName := range eps { // epName is endpoint's name
						endpointCluster, err := getXWso2Endpoints(eps, epName)
						if err == nil && endpointCluster != nil {
							endpointClusters = append(endpointClusters, *endpointCluster)
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
		return errors.New("No Endpoints are provided for the API")
	}
	if swagger.productionEndpoints != nil && len(swagger.productionEndpoints.Endpoints) > 0 {
		err := swagger.productionEndpoints.Endpoints[0].validateEndpoint()
		if err != nil {
			logger.LoggerOasparser.Errorf("Error while parsing the production endpoints of the API %s:%s - %v",
				swagger.title, swagger.version, err)
			return err
		}
	}

	if swagger.sandboxEndpoints != nil && len(swagger.sandboxEndpoints.Endpoints) > 0 {
		err := swagger.sandboxEndpoints.Endpoints[0].validateEndpoint()
		if err != nil {
			logger.LoggerOasparser.Errorf("Error while parsing the sandbox endpoints of the API %s:%s - %v",
				swagger.title, swagger.version, err)
			return err
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
	if xWso2BasePath == "" {
		return errors.New("Empty Basepath is provided. Either use x-wso2-basePath extension or assign basePath (if OpenAPI v2 definition is used)" +
			" / servers entry (if OpenAPI v3 definition is used) with non empty context.")
	}
	return nil
}

func (endpoint *Endpoint) validateEndpoint() error {
	if len(endpoint.ServiceDiscoveryString) > 0 {
		return nil
	}
	if endpoint.Port == 0 || endpoint.Port > 65535 {
		return errors.New("Endpoint port value should be between 0 and 65535")
	}
	if len(endpoint.Host) == 0 {
		return errors.New("Empty Hostname is provided")
	}
	if strings.HasPrefix(endpoint.Host, "/") {
		return errors.New("Relative paths are not supported as endpoint URLs")
	}
	urlString := endpoint.URLType + "://" + endpoint.Host
	_, err := url.ParseRequestURI(urlString)
	return err
}

// getXWso2Endpoints extracts and generate the EndpointCluster Object from any yaml map that has the following structure
//   endpoint-name:
// 		urls:
// 			- <endpoint-URL-1>
// 			- <endpoint-URL-2>
//		type: <loadbalance or failover>
//		advanceEndpointConfig:
//			<the configs>
func getXWso2Endpoints(vendorExtensions map[string]interface{}, endpointName string) (*EndpointCluster, error) {
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
				errMsg := "urls property is not provided with the x-wso2-production-endpoints/" +
					"x-wso2-sandbox-endpoints extension."
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
				var endpointConfig EndpointConfig
				json.Unmarshal(advanceEndpointConfig.([]byte), &endpointConfig)
				endpointCluster.Config = &endpointConfig
			}
			return &endpointCluster, nil
		}
		logger.LoggerOasparser.Error("x-wso2-production/sandbox-endpoints OpenAPI extension does not adhere with the schema")
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

func generateEndpointCluster(endpointName string, endpoints []Endpoint) *EndpointCluster {
	if len(endpoints) > 0 {
		endpointCluster := EndpointCluster{
			EndpointName: endpointName,
			Endpoints:    endpoints,
			EndpointType: LoadBalance,
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
	urlV := "http"
	conf, _ := config.ReadConfigs()
	clusterTimeoutV := conf.Envoy.ClusterTimeoutInSeconds
	requestTimeoutV := 30
	pathV := "/"
	hostV := ""
	portV := uint32(80)
	var includesV []string

	var err error

	if x, found := vendorExtensions[extensionName]; found {
		if val, ok := x.(map[string]interface{}); ok {
			//host mandatory
			if v, found := val[host]; found {
				hostV = v.(string)
			} else {
				logger.LoggerOasparser.Error("Error reading interceptors host value")
				return InterceptEndpoint{}, errors.New("error reading interceptors host value")
			}
			// port mandatory
			if v, found := val[port]; found {
				p, err := strconv.ParseUint(fmt.Sprint(v), 10, 32)
				if err == nil {
					portV = uint32(p)
				} else {
					logger.LoggerOasparser.Error("Error reading interceptors port value", err)
					return InterceptEndpoint{}, errors.New("error reading interceptors port value")
				}
			}
			//urlType optional
			if v, found := val[urlType]; found {
				urlV = v.(string)
			}
			//clusterTimeout optional
			if v, found := val[clusterTimeout]; found {
				p, err := strconv.ParseInt(fmt.Sprint(v), 0, 0)
				if err == nil {
					clusterTimeoutV = time.Duration(p)
				} else {
					logger.LoggerOasparser.Error("Error reading interceptors port value", err)
					return InterceptEndpoint{}, errors.New("error reading interceptors port value")
				}
			}
			//requestTimeout optional
			if v, found := val[requestTimeout]; found {
				p, err := strconv.ParseInt(fmt.Sprint(v), 0, 0)
				if err == nil {
					requestTimeoutV = int(p)
				} else {
					logger.LoggerOasparser.Error("Error reading interceptors port value", err)
					return InterceptEndpoint{}, errors.New("error reading interceptors port value")
				}
			}
			// path optional
			if v, found := val[path]; found {
				pathV = v.(string)
			}
			//includes optional
			if v, found := val[includes]; found {
				includes := v.([]interface{})
				if len(includes) > 0 {
					for _, include := range includes {
						switch include.(string) {
						case "request_headers", "request_body", "request_trailer", "response_headers", "response_body",
							"response_trailer", "invocation_context":
							includesV = append(includesV, include.(string))
						}
					}
				}
			}

			return InterceptEndpoint{
				Enable:         true,
				Host:           hostV,
				URLType:        urlV,
				Port:           portV,
				ClusterTimeout: clusterTimeoutV,
				RequestTimeout: requestTimeoutV,
				Path:           pathV,
				Includes:       includesV,
			}, err
		}
		return InterceptEndpoint{}, errors.New("error parsing response interceptors values to mgwSwagger")
	}
	return InterceptEndpoint{}, nil
}
