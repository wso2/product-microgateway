/*
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

package eventhub

import (
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
	"reflect"
	"strconv"

	"github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/auth"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
)

const (
	authorizationBasic         string = "Basic "
	authorizationHeaderDefault string = "Authorization"
	internalWebAppEP           string = "internal/data/v1/"
	// ContextParam is required to call /apis endpoint
	ContextParam string = "context"
	// VersionParam is trequired to call /apis endpoint
	VersionParam string = "version"
	// GatewayLabelParam is trequired to call /apis endpoint
	GatewayLabelParam string = "gatewayLabel"
	// ApisEndpoint is the resource path of /apis endpoint
	ApisEndpoint string = "apis"
)

var (
	// SubList contains the Subscription list
	SubList *types.SubscriptionList
	// AppList contains the Application list
	AppList *types.ApplicationList
	// AppKeyMappingList contains the Application key mapping list
	AppKeyMappingList *types.ApplicationKeyMappingList
	// APIListMap contains the Api list against each label
	APIListMap map[string]*types.APIList
	// AppPolicyList contains the Application policy list
	AppPolicyList *types.ApplicationPolicyList
	// SubPolicyList contains the Subscription policy list
	SubPolicyList *types.SubscriptionPolicyList
	resources     = []resource{
		{
			endpoint:     "subscriptions",
			responseType: SubList,
		},
		{
			endpoint:     "applications",
			responseType: AppList,
		},
		{
			endpoint:     "application-key-mappings",
			responseType: AppKeyMappingList,
		},
		{
			endpoint:     "application-policies",
			responseType: AppPolicyList,
		},
		{
			endpoint:     "subscription-policies",
			responseType: SubPolicyList,
		},
	}
	// APIListChannel is used to add apis
	APIListChannel chan response
	accessToken    string
	conf           *config.Config
)

type response struct {
	Error        error
	Payload      []byte
	Endpoint     string
	GatewayLabel string
	Type         interface{}
}

type resource struct {
	endpoint     string
	responseType interface{}
}

func init() {
	APIListChannel = make(chan response)
	APIListMap = make(map[string]*types.APIList)
}

// LoadSubscriptionData loads subscription data from control-plane
func LoadSubscriptionData(configFile *config.Config) {
	conf = configFile
	accessToken = auth.GetBasicAuth(configFile.ControlPlane.Username, configFile.ControlPlane.Password)

	var responseChannel = make(chan response)
	for _, url := range resources {
		go InvokeService(url.endpoint, url.responseType, nil, responseChannel, 0)
	}

	// Take the configured labels from the adapter
	configuredEnvs := conf.ControlPlane.EnvironmentLabels

	// If no environments are configured, default gateway label value is assigned.
	if len(configuredEnvs) == 0 {
		configuredEnvs = append(configuredEnvs, config.DefaultGatewayName)
	}
	for _, configuredEnv := range configuredEnvs {
		queryParamMap := make(map[string]string, 1)
		queryParamMap[GatewayLabelParam] = configuredEnv
		go InvokeService(ApisEndpoint, APIListMap[configuredEnv], queryParamMap, APIListChannel, 0)
	}

	go retrieveAPIListFromChannel(APIListChannel)
	var response response
	for i := 1; i <= len(resources); i++ {
		response = <-responseChannel

		responseType := reflect.TypeOf(response.Type).Elem()
		newResponse := reflect.New(responseType).Interface()

		if response.Error == nil && response.Payload != nil {
			err := json.Unmarshal(response.Payload, &newResponse)

			if err != nil {
				logger.LoggerSubscription.Errorf("Error occurred while unmarshalling the response received for: "+response.Endpoint, err)
			} else {
				switch t := newResponse.(type) {
				case *types.SubscriptionList:
					logger.LoggerSubscription.Debug("Received Subscription information.")
					SubList = newResponse.(*types.SubscriptionList)
					xds.UpdateEnforcerSubscriptions(xds.MarshalSubscriptionList(SubList))
				case *types.ApplicationList:
					logger.LoggerSubscription.Debug("Received Application information.")
					AppList = newResponse.(*types.ApplicationList)
					xds.UpdateEnforcerApplications(xds.MarshalApplicationList(AppList))
				case *types.ApplicationPolicyList:
					logger.LoggerSubscription.Debug("Received Application Policy information.")
					AppPolicyList = newResponse.(*types.ApplicationPolicyList)
					xds.UpdateEnforcerApplicationPolicies(xds.MarshalApplicationPolicyList(AppPolicyList))
				case *types.SubscriptionPolicyList:
					logger.LoggerSubscription.Debug("Received Subscription Policy information.")
					SubPolicyList = newResponse.(*types.SubscriptionPolicyList)
					xds.UpdateEnforcerSubscriptionPolicies(xds.MarshalSubscriptionPolicyList(SubPolicyList))
				case *types.ApplicationKeyMappingList:
					logger.LoggerSubscription.Debug("Received Application Key Mapping information.")
					AppKeyMappingList = newResponse.(*types.ApplicationKeyMappingList)
					xds.UpdateEnforcerApplicationKeyMappings(xds.MarshalKeyMappingList(AppKeyMappingList))
				default:
					logger.LoggerSubscription.Debugf("Unknown type %T", t)
				}
			}
		}
	}
}

// InvokeService invokes the internal data resource
func InvokeService(endpoint string, responseType interface{}, queryParamMap map[string]string, c chan response,
	retryAttempt int) {

	serviceURL := conf.ControlPlane.ServiceURL + internalWebAppEP + endpoint
	// Create the request
	req, err := http.NewRequest("GET", serviceURL, nil)
	// gatewayLabel will only be required for apis endpoint
	gatewayLabel, ok := queryParamMap[GatewayLabelParam]
	if !ok {
		gatewayLabel = ""
	}
	if queryParamMap != nil && len(queryParamMap) > 0 {
		q := req.URL.Query()
		// Making necessary query parameters for the request
		for queryParamKey, queryParamValue := range queryParamMap {
			q.Add(queryParamKey, queryParamValue)
		}
		req.URL.RawQuery = q.Encode()
	}
	if err != nil {
		c <- response{err, nil, endpoint, gatewayLabel, responseType}
		logger.LoggerSubscription.Errorf("Error occurred while creating an HTTP request for serviceURL: "+serviceURL, err)
		return
	}

	// Check if TLS is enabled
	skipSSL := conf.ControlPlane.SkipSSLVerification

	// Setting authorization header
	req.Header.Set(authorizationHeaderDefault, authorizationBasic+accessToken)

	// Make the request
	logger.LoggerSubscription.Debug("Sending the request to the control plane over the REST API: " + serviceURL)
	resp, err := tlsutils.InvokeControlPlane(req, skipSSL)

	if err != nil {
		c <- response{err, nil, endpoint, gatewayLabel, responseType}
		logger.LoggerSubscription.Errorf("Error occurred while calling the REST API: "+serviceURL, err)
		return
	}

	responseBytes, err := ioutil.ReadAll(resp.Body)
	if resp.StatusCode == http.StatusOK {
		if err != nil {
			c <- response{err, nil, endpoint, gatewayLabel, responseType}
			logger.LoggerSubscription.Errorf("Error occurred while reading the response received for: "+serviceURL, err)
			return
		}
		logger.LoggerSubscription.Debug("Request to the control plane over the REST API: " + serviceURL + " is successful.")
		c <- response{nil, responseBytes, endpoint, gatewayLabel, responseType}
	} else {
		c <- response{errors.New(string(responseBytes)), nil, endpoint, gatewayLabel, responseType}
		logger.LoggerSubscription.Errorf("Failed to fetch data! "+serviceURL+" responded with "+strconv.Itoa(resp.StatusCode),
			err)
	}
}

func retrieveAPIListFromChannel(c chan response) {
	for response := range c {
		responseType := reflect.TypeOf(response.Type).Elem()
		newResponse := reflect.New(responseType).Interface()
		if response.Error == nil && response.Payload != nil {
			err := json.Unmarshal(response.Payload, &newResponse)
			if err != nil {
				logger.LoggerSubscription.Errorf("Error occurred while unmarshalling the APIList response received for: "+
					response.Endpoint, err)
			} else {
				switch t := newResponse.(type) {
				case *types.APIList:
					apiListResponse := newResponse.(*types.APIList)
					if logger.LoggerSubscription.Level == logrus.DebugLevel {
						for _, api := range apiListResponse.List {
							logger.LoggerSubscription.Infof("Received API List information for API : %s", api.UUID)
						}
					}
					if _, ok := APIListMap[response.GatewayLabel]; !ok {
						// During the startup
						APIListMap[response.GatewayLabel] = newResponse.(*types.APIList)
					} else {
						// API Details retrieved after startup contains single API per response.
						if len(apiListResponse.List) == 1 {
							APIListMap[response.GatewayLabel].List = append(APIListMap[response.GatewayLabel].List,
								apiListResponse.List[0])
						}
					}
					xds.UpdateEnforcerAPIList(response.GatewayLabel, xds.MarshalAPIList(APIListMap[response.GatewayLabel]))
				default:
					logger.LoggerSubscription.Warnf("APIList Type DTO is not recieved. Unknown type %T", t)
				}
			}
		}
	}
}
