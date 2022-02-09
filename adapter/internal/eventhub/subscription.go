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
	"time"

	"github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	pkgAuth "github.com/wso2/product-microgateway/adapter/pkg/auth"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
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
	// APIUUIDParam is required to call /apis endpoint
	APIUUIDParam string = "apiId"
	// ApisEndpoint is the resource path of /apis endpoint
	ApisEndpoint string = "apis"
)

var (
	// This set of variables are used just for Type resolution with reflect.
	// Hence no value needs to be assigned.
	subList           *types.SubscriptionList
	appList           *types.ApplicationList
	appKeyMappingList *types.ApplicationKeyMappingList
	appPolicyList     *types.ApplicationPolicyList
	subPolicyList     *types.SubscriptionPolicyList
	apiList           *types.APIList

	resources = []resource{
		{
			endpoint:     "subscriptions",
			responseType: subList,
		},
		{
			endpoint:     "applications",
			responseType: appList,
		},
		{
			endpoint:     "application-key-mappings",
			responseType: appKeyMappingList,
		},
		{
			endpoint:     "application-policies",
			responseType: appPolicyList,
		},
		{
			endpoint:     "subscription-policies",
			responseType: subPolicyList,
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
	ErrorCode    int
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
}

// LoadSubscriptionData loads subscription data from control-plane
func LoadSubscriptionData(configFile *config.Config, initialAPIUUIDListMap map[string]int) {
	conf = configFile
	accessToken = pkgAuth.GetBasicAuth(configFile.ControlPlane.Username, configFile.ControlPlane.Password)

	var responseChannel = make(chan response)
	for _, url := range resources {
		go InvokeService(url.endpoint, url.responseType, nil, responseChannel, 0)
		for {
			data := <-responseChannel
			logger.LoggerSync.Debug("Receiving subscription data for an environment")
			if data.Payload != nil {
				logger.LoggerSync.Info("Payload data with subscription information recieved")
				retrieveSubscriptionDataFromChannel(data)
				break
			} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
				logger.LoggerSync.Errorf("Error occurred when retrieving Subscription information from the control plane: %v", data.Error)
				health.SetControlPlaneRestAPIStatus(false)
			} else {
				// Keep the iteration going on until a response is recieved.
				logger.LoggerSync.Errorf("Error occurred while fetching data from control plane: %v", data.Error)
				go func(d response) {
					// Retry fetching from control plane after a configured time interval
					if conf.ControlPlane.RetryInterval == 0 {
						// Assign default retry interval
						conf.ControlPlane.RetryInterval = 5
					}
					logger.LoggerSync.Debugf("Time Duration for retrying: %v", conf.ControlPlane.RetryInterval*time.Second)
					time.Sleep(conf.ControlPlane.RetryInterval * time.Second)
					logger.LoggerSync.Infof("Retrying to fetch APIs from control plane. Time Duration for the next retry: %v", conf.ControlPlane.RetryInterval*time.Second)
					go InvokeService(url.endpoint, url.responseType, nil, responseChannel, 0)
				}(data)
			}
		}
	}

	// TODO: (VirajSalaka) Calling /apis endpoint is temporarily removed.

	// Take the configured labels from the adapter
	// configuredEnvs := conf.ControlPlane.EnvironmentLabels

	// If no environments are configured, default gateway label value is assigned.
	// if len(configuredEnvs) == 0 {
	// 	configuredEnvs = append(configuredEnvs, config.DefaultGatewayName)
	// }
	// for _, configuredEnv := range configuredEnvs {
	// 	queryParamMap := make(map[string]string, 1)
	// 	queryParamMap[GatewayLabelParam] = configuredEnv
	// 	go InvokeService(ApisEndpoint, apiList, queryParamMap, APIListChannel, 0)
	// 	for {
	// 		data := <-APIListChannel
	// 		logger.LoggerSync.Debug("Receiving API information for an environment")
	// 		if data.Payload != nil {
	// 			logger.LoggerSync.Info("Payload data with API information recieved")
	// 			retrieveAPIList(data, initialAPIUUIDListMap)
	// 			break
	// 		} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
	// 			logger.LoggerSync.Errorf("Error occurred when retrieving Subscription information from the control plane: %v", data.Error)
	// 			health.SetControlPlaneRestAPIStatus(false)
	// 		} else {
	// 			// Keep the iteration going on until a response is recieved.
	// 			logger.LoggerSync.Errorf("Error occurred while fetching data from control plane: %v", data.Error)
	// 			go func(d response) {
	// 				// Retry fetching from control plane after a configured time interval
	// 				if conf.ControlPlane.RetryInterval == 0 {
	// 					// Assign default retry interval
	// 					conf.ControlPlane.RetryInterval = 5
	// 				}
	// 				logger.LoggerSync.Debugf("Time Duration for retrying: %v", conf.ControlPlane.RetryInterval*time.Second)
	// 				time.Sleep(conf.ControlPlane.RetryInterval * time.Second)
	// 				logger.LoggerSync.Infof("Retrying to fetch APIs from control plane. Time Duration for the next retry: %v", conf.ControlPlane.RetryInterval*time.Second)
	// 				go InvokeService(ApisEndpoint, apiList, queryParamMap, APIListChannel, 0)
	// 			}(data)
	// 		}
	// 	}
	// }
	// TODO: (VirajSalaka) APIList (/apis response) processing is temporarily blocked.
	// // InitialAPIUUIDList is already processed (if available). Then onwards, that list is not required.
	// go retrieveAPIListFromChannel(APIListChannel, nil)
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
		c <- response{err, nil, 0, endpoint, gatewayLabel, responseType}
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
		if resp != nil {
			c <- response{err, nil, resp.StatusCode, endpoint, gatewayLabel, responseType}
		} else {
			c <- response{err, nil, 0, endpoint, gatewayLabel, responseType}
		}
		logger.LoggerSubscription.Errorf("Error occurred while calling the REST API: "+serviceURL, err)
		return
	}

	responseBytes, err := ioutil.ReadAll(resp.Body)
	if resp.StatusCode == http.StatusOK {
		if err != nil {
			c <- response{err, nil, resp.StatusCode, endpoint, gatewayLabel, responseType}
			logger.LoggerSubscription.Errorf("Error occurred while reading the response received for: "+serviceURL, err)
			return
		}
		logger.LoggerSubscription.Debug("Request to the control plane over the REST API: " + serviceURL + " is successful.")
		c <- response{nil, responseBytes, resp.StatusCode, endpoint, gatewayLabel, responseType}
	} else {
		c <- response{errors.New(string(responseBytes)), nil, resp.StatusCode, endpoint, gatewayLabel, responseType}
		logger.LoggerSubscription.Errorf("Failed to fetch data! "+serviceURL+" responded with "+strconv.Itoa(resp.StatusCode),
			err)
	}
}

func retrieveAPIListFromChannel(c chan response, initialAPIUUIDListMap map[string]int) {
	for response := range c {
		retrieveAPIList(response, initialAPIUUIDListMap)
	}
}

func retrieveAPIList(response response, initialAPIUUIDListMap map[string]int) {

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
						logger.LoggerSubscription.Debugf("Received API List information for API : %s", api.UUID)
					}
				}

				xds.UpdateEnforcerAPIList(response.GatewayLabel,
					xds.MarshalAPIMetataAndReturnList(apiListResponse, initialAPIUUIDListMap, response.GatewayLabel))
			default:
				logger.LoggerSubscription.Warnf("APIList Type DTO is not recieved. Unknown type %T", t)
			}
		}
	}
}

func retrieveSubscriptionDataFromChannel(response response) {
	responseType := reflect.TypeOf(response.Type).Elem()
	newResponse := reflect.New(responseType).Interface()
	err := json.Unmarshal(response.Payload, &newResponse)

	if err != nil {
		logger.LoggerSubscription.Errorf("Error occurred while unmarshalling the response received for: "+response.Endpoint, err)
	} else {
		switch t := newResponse.(type) {
		case *types.SubscriptionList:
			logger.LoggerSubscription.Debug("Received Subscription information.")
			subList = newResponse.(*types.SubscriptionList)
			xds.UpdateEnforcerSubscriptions(xds.MarshalMultipleSubscriptions(subList))
		case *types.ApplicationList:
			logger.LoggerSubscription.Debug("Received Application information.")
			appList = newResponse.(*types.ApplicationList)
			xds.UpdateEnforcerApplications(xds.MarshalMultipleApplications(appList))
		case *types.ApplicationPolicyList:
			logger.LoggerSubscription.Debug("Received Application Policy information.")
			appPolicyList = newResponse.(*types.ApplicationPolicyList)
			xds.UpdateEnforcerApplicationPolicies(xds.MarshalMultipleApplicationPolicies(appPolicyList))
		case *types.SubscriptionPolicyList:
			logger.LoggerSubscription.Debug("Received Subscription Policy information.")
			subPolicyList = newResponse.(*types.SubscriptionPolicyList)
			xds.UpdateEnforcerSubscriptionPolicies(xds.MarshalMultipleSubscriptionPolicies(subPolicyList))
		case *types.ApplicationKeyMappingList:
			logger.LoggerSubscription.Debug("Received Application Key Mapping information.")
			appKeyMappingList = newResponse.(*types.ApplicationKeyMappingList)
			xds.UpdateEnforcerApplicationKeyMappings(xds.MarshalMultipleApplicationKeyMappings(appKeyMappingList))
		default:
			logger.LoggerSubscription.Debugf("Unknown type %T", t)
		}
	}
}
