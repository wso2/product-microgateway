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

// Package messaging holds the implementation for event listeners functions
package messaging

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	eh "github.com/wso2/product-microgateway/adapter/internal/eventhub"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/synchronizer"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

// constant variables
const (
	apiEventType                = "API"
	applicationEventType        = "APPLICATION"
	subscriptionEventType       = "SUBSCRIPTION"
	scopeEvenType               = "SCOPE"
	removeAPIFromGateway        = "REMOVE_API_FROM_GATEWAY"
	deployAPIToGateway          = "DEPLOY_API_IN_GATEWAY"
	applicationRegistration     = "APPLICATION_REGISTRATION_CREATE"
	removeApplicationKeyMapping = "REMOVE_APPLICATION_KEYMAPPING"
	apiLifeCycleChange          = "LIFECYCLE_CHANGE"
	applicationCreate           = "APPLICATION_CREATE"
	applicationUpdate           = "APPLICATION_UPDATE"
	applicationDelete           = "APPLICATION_DELETE"
	subscriptionCreate          = "SUBSCRIPTIONS_CREATE"
	subscriptionUpdate          = "SUBSCRIPTIONS_UPDATE"
	subscriptionDelete          = "SUBSCRIPTIONS_DELETE"
	policyCreate                = "POLICY_CREATE"
	policyUpdate                = "POLICY_UPDATE"
	policyDelete                = "POLICY_DELETE"
	blockedStatus               = "BLOCKED"
)

// var variables
var (
	ScopeList = make([]types.Scope, 0)
	// timestamps needs to be maintained as it is not guranteed to receive them in order,
	// hence older events should be discarded
	apiListTimeStampMap               = make(map[string]int64, 0)
	subsriptionsListTimeStampMap      = make(map[string]int64, 0)
	applicationKeyMappingTimeStampMap = make(map[string]int64, 0)
	applicationListTimeStampMap       = make(map[string]int64, 0)
)

// handleNotification to process
func handleNotification() {
	conf, _ := config.ReadConfigs()
	for d := range msg.NotificationChannel {
		var notification msg.EventNotification
		notificationErr := parseNotificationJSONEvent([]byte(string(d.Body)), &notification)
		if notificationErr != nil {
			continue
		}
		logger.LoggerInternalMsg.Infof("Event %s is received", notification.Event.PayloadData.EventType)
		err := processNotificationEvent(conf, &notification)
		if err != nil {
			continue
		}
		d.Ack(false)
	}
	logger.LoggerInternalMsg.Infof("handle: deliveries channel closed")
}

func handleAzureNotification() {
	conf, _ := config.ReadConfigs()
	for d := range msg.AzureNotificationChannel {
		var notification msg.EventNotification
		error := parseNotificationJSONEvent(d, &notification)
		if error != nil {
			continue
		}
		logger.LoggerInternalMsg.Infof("Event %s is received", notification.Event.PayloadData.EventType)
		err := processNotificationEvent(conf, &notification)
		if err != nil {
			continue
		}
	}
}

func processNotificationEvent(conf *config.Config, notification *msg.EventNotification) error {
	var eventType string
	var decodedByte, err = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Event)
	if err != nil {
		if _, ok := err.(base64.CorruptInputError); ok {
			logger.LoggerInternalMsg.Error("\nbase64 input is corrupt, check the provided key")
		}
		logger.LoggerInternalMsg.Errorf("Error occurred while decoding the notification event %v. "+
			"Hence dropping the event", err)
		return err
	}
	logger.LoggerInternalMsg.Debugf("\n\n[%s]", decodedByte)
	eventType = notification.Event.PayloadData.EventType
	if strings.Contains(eventType, apiLifeCycleChange) {
		handleLifeCycleEvents(decodedByte)
	} else if strings.Contains(eventType, apiEventType) && !conf.GlobalAdapter.Enabled {
		handleAPIEvents(decodedByte, eventType)
	} else if strings.Contains(eventType, applicationEventType) {
		handleApplicationEvents(decodedByte, eventType)
	} else if strings.Contains(eventType, subscriptionEventType) {
		handleSubscriptionEvents(decodedByte, eventType)
	} else {
		handlePolicyEvents(decodedByte, eventType)
	}
	return nil
}

// handleAPIEvents to process api related data
func handleAPIEvents(data []byte, eventType string) {
	var (
		apiEvent         msg.APIEvent
		currentTimeStamp int64 = apiEvent.Event.TimeStamp
	)

	apiEventErr := json.Unmarshal([]byte(string(data)), &apiEvent)
	if apiEventErr != nil {
		logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling API event data %v", apiEventErr)
		return
	}

	if !belongsToTenant(apiEvent.TenantDomain) {
		apiName := apiEvent.APIName
		if apiEvent.APIName == "" {
			apiName = apiEvent.Name
		}
		apiVersion := apiEvent.Version
		if apiEvent.Version == "" {
			apiVersion = apiEvent.Version
		}
		logger.LoggerInternalMsg.Debugf("API event for the API %s:%s is dropped due to having non related tenantDomain : %s",
			apiName, apiVersion, apiEvent.TenantDomain)
		return
	}

	// Per each revision, synchronization should happen.
	if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
		go synchronizer.FetchAPIsFromControlPlane(apiEvent.UUID, apiEvent.GatewayLabels)
	}

	for _, env := range apiEvent.GatewayLabels {
		if isLaterEvent(apiListTimeStampMap, apiEvent.UUID+":"+env, currentTimeStamp) {
			return
		}
		if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
			conf, _ := config.ReadConfigs()
			configuredEnvs := conf.ControlPlane.EnvironmentLabels
			if len(configuredEnvs) == 0 {
				configuredEnvs = append(configuredEnvs, config.DefaultGatewayName)
			}
			for _, configuredEnv := range configuredEnvs {
				if configuredEnv == env {
					if _, ok := eh.APIListMap[env]; ok {
						apiListOfEnv := eh.APIListMap[env].List
						for i := range apiListOfEnv {
							// If API is already found, it is a new revision deployement.
							// Subscription relates details of an API does not change between new revisions
							if apiEvent.Context == apiListOfEnv[i].Context && apiEvent.Version == apiListOfEnv[i].Version {
								logger.LoggerInternalMsg.Debugf("APIList for apiIId: %s is not updated as it already exists", apiEvent.UUID)
								return
							}
						}
						queryParamMap := make(map[string]string, 3)
						queryParamMap[eh.GatewayLabelParam] = configuredEnv
						queryParamMap[eh.ContextParam] = apiEvent.Context
						queryParamMap[eh.VersionParam] = apiEvent.Version
						go eh.InvokeService(eh.ApisEndpoint, eh.APIListMap[env], queryParamMap,
							eh.APIListChannel, 0)
					}
				}
			}
		} else if strings.EqualFold(removeAPIFromGateway, apiEvent.Event.Type) {
			xds.DeleteAPIWithAPIMEvent(apiEvent.UUID, apiEvent.Name, apiEvent.Version, apiEvent.GatewayLabels, apiEvent.TenantDomain)
			logger.LoggerInternalMsg.Debugf("Undeployed API from router")
			if _, ok := eh.APIListMap[env]; ok {
				apiListOfEnv := eh.APIListMap[env].List
				for i := range apiListOfEnv {
					// TODO: (VirajSalaka) Use APIId once it is fixed from control plane
					if apiEvent.Context == apiListOfEnv[i].Context && apiEvent.Version == apiListOfEnv[i].Version {
						eh.APIListMap[env].List = DeleteAPIFromList(apiListOfEnv, i, apiEvent.UUID, env)
						xds.UpdateEnforcerAPIList(env, xds.MarshalAPIList(eh.APIListMap[env]))
						break
					}
				}
			}
		}
	}
}

func handleLifeCycleEvents(data []byte) {
	var apiEvent msg.APIEvent
	apiLCEventErr := json.Unmarshal([]byte(string(data)), &apiEvent)
	if apiLCEventErr != nil {
		logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling Lifecycle event data %v", apiLCEventErr)
		return
	}
	if !belongsToTenant(apiEvent.TenantDomain) {
		logger.LoggerInternalMsg.Debugf("API Lifecycle event for the API %s:%s is dropped due to having non related tenantDomain : %s",
			apiEvent.APIName, apiEvent.APIVersion, apiEvent.TenantDomain)
		return
	}
	conf, _ := config.ReadConfigs()
	configuredEnvs := conf.ControlPlane.EnvironmentLabels
	logger.LoggerInternalMsg.Debugf("%s : %s API life cycle state change event triggered", apiEvent.APIName, apiEvent.APIVersion)
	if len(configuredEnvs) == 0 {
		configuredEnvs = append(configuredEnvs, config.DefaultGatewayName)
	}
	for _, configuredEnv := range configuredEnvs {
		if _, ok := eh.APIListMap[configuredEnv]; ok {
			apiListOfEnv := eh.APIListMap[configuredEnv].List
			for i := range apiListOfEnv {
				if apiEvent.UUID == apiListOfEnv[i].UUID && (apiListOfEnv[i].APIStatus == blockedStatus ||
					apiEvent.APIStatus == blockedStatus) {
					//If previous or current state is 'Blocked' only we update the xds. All other states are neglected at the gateway
					logger.LoggerInternalMsg.Infof("Lifecycle state changed from %s to %s", apiListOfEnv[i].APIStatus, apiEvent.APIStatus)
					apiListOfEnv[i].APIStatus = apiEvent.APIStatus
					xds.UpdateEnforcerAPIList(configuredEnv, xds.MarshalAPIList(eh.APIListMap[configuredEnv]))
					break
				}
			}
		}
	}
}

// DeleteAPIFromList when remove API From Gateway event happens
func DeleteAPIFromList(apiList []types.API, indexToBeDeleted int, apiUUID string, label string) []types.API {
	apiList[indexToBeDeleted] = apiList[len(apiList)-1]
	logger.LoggerInternalMsg.Infof("API %s is deleted from APIList under Label %s", apiUUID, label)
	return apiList[:len(apiList)-1]
}

// handleApplicationEvents to process application related events
func handleApplicationEvents(data []byte, eventType string) {
	if strings.EqualFold(applicationRegistration, eventType) ||
		strings.EqualFold(removeApplicationKeyMapping, eventType) {
		var applicationRegistrationEvent msg.ApplicationRegistrationEvent
		appRegEventErr := json.Unmarshal([]byte(string(data)), &applicationRegistrationEvent)
		if appRegEventErr != nil {
			logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling Application Registration event data %v", appRegEventErr)
			return
		}

		if !belongsToTenant(applicationRegistrationEvent.TenantDomain) {
			logger.LoggerInternalMsg.Debugf("Application Registration event for the Consumer Key : %s is dropped due to having non related tenantDomain : %s",
				applicationRegistrationEvent.ConsumerKey, applicationRegistrationEvent.TenantDomain)
			return
		}

		applicationKeyMapping := types.ApplicationKeyMapping{ApplicationID: applicationRegistrationEvent.ApplicationID,
			ConsumerKey: applicationRegistrationEvent.ConsumerKey, KeyType: applicationRegistrationEvent.KeyType,
			KeyManager: applicationRegistrationEvent.KeyManager, TenantID: -1, TenantDomain: applicationRegistrationEvent.TenantDomain,
			TimeStamp: applicationRegistrationEvent.TimeStamp, ApplicationUUID: applicationRegistrationEvent.ApplicationUUID}

		applicationKeyMappingReference := applicationKeyMapping.ConsumerKey + ":" + applicationKeyMapping.KeyManager

		if isLaterEvent(applicationKeyMappingTimeStampMap, fmt.Sprint(applicationKeyMappingReference),
			applicationRegistrationEvent.TimeStamp) {
			return
		}

		if strings.EqualFold(removeApplicationKeyMapping, eventType) {
			delete(eh.ApplicationKeyMappingMap, applicationKeyMappingReference)
			logger.LoggerInternalMsg.Infof("Application Key Mapping for the applicationKeyMappingReference %s is removed.",
				applicationKeyMappingReference)
		} else {
			eh.ApplicationKeyMappingMap[applicationKeyMappingReference] = &applicationKeyMapping
			logger.LoggerInternalMsg.Infof("Application Key Mapping for the applicationKeyMappingReference %s is added.",
				applicationKeyMappingReference)
		}

		xds.UpdateEnforcerApplicationKeyMappings(xds.MarshalKeyMappingMap(eh.ApplicationKeyMappingMap))
	} else {
		var applicationEvent msg.ApplicationEvent
		appEventErr := json.Unmarshal([]byte(string(data)), &applicationEvent)
		if appEventErr != nil {
			logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling Application event data %v", appEventErr)
			return
		}

		if !belongsToTenant(applicationEvent.TenantDomain) {
			logger.LoggerInternalMsg.Debugf("Application event for the Application : %s (with uuid %s) is dropped due to having non related tenantDomain : %s",
				applicationEvent.ApplicationName, applicationEvent.UUID, applicationEvent.TenantDomain)
			return
		}

		application := types.Application{UUID: applicationEvent.UUID, ID: applicationEvent.ApplicationID,
			Name: applicationEvent.ApplicationName, SubName: applicationEvent.Subscriber,
			Policy: applicationEvent.ApplicationPolicy, TokenType: applicationEvent.TokenType,
			GroupIds: applicationEvent.GroupID, Attributes: nil,
			TenantID: -1, TenantDomain: applicationEvent.TenantDomain, TimeStamp: applicationEvent.TimeStamp}

		if isLaterEvent(applicationListTimeStampMap, fmt.Sprint(applicationEvent.ApplicationID), applicationEvent.TimeStamp) {
			return
		}

		if applicationEvent.Event.Type == applicationCreate {
			eh.ApplicationMap[application.UUID] = &application
			logger.LoggerInternalMsg.Infof("Application %s is added.", applicationEvent.UUID)
		} else if applicationEvent.Event.Type == applicationUpdate {
			eh.ApplicationMap[application.UUID] = &application
			logger.LoggerInternalMsg.Infof("Application %s is updated.", applicationEvent.UUID)
		} else if applicationEvent.Event.Type == applicationDelete {
			delete(eh.ApplicationMap, application.UUID)
			logger.LoggerInternalMsg.Infof("Application %s is deleted.", applicationEvent.UUID)
		}
		xds.UpdateEnforcerApplications(xds.MarshalApplicationMap(eh.ApplicationMap))
	}
}

// handleSubscriptionRelatedEvents to process subscription related events
func handleSubscriptionEvents(data []byte, eventType string) {
	var subscriptionEvent msg.SubscriptionEvent
	subEventErr := json.Unmarshal([]byte(string(data)), &subscriptionEvent)
	if subEventErr != nil {
		logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling Subscription event data %v", subEventErr)
		return
	}
	if !belongsToTenant(subscriptionEvent.TenantDomain) {
		logger.LoggerInternalMsg.Debugf("Subscription event for the Application : %s and API %s is dropped due to having non related tenantDomain : %s",
			subscriptionEvent.ApplicationUUID, subscriptionEvent.APIUUID, subscriptionEvent.TenantDomain)
		return
	}

	sub := types.Subscription{SubscriptionID: subscriptionEvent.SubscriptionID, SubscriptionUUID: subscriptionEvent.SubscriptionUUID,
		PolicyID: subscriptionEvent.PolicyID, APIUUID: subscriptionEvent.APIUUID, ApplicationUUID: subscriptionEvent.ApplicationUUID,
		APIID: subscriptionEvent.APIID, AppID: subscriptionEvent.ApplicationID, SubscriptionState: subscriptionEvent.SubscriptionState,
		TenantID: subscriptionEvent.TenantID, TenantDomain: subscriptionEvent.TenantDomain, TimeStamp: subscriptionEvent.TimeStamp}

	if isLaterEvent(subsriptionsListTimeStampMap, fmt.Sprint(subscriptionEvent.SubscriptionID), subscriptionEvent.TimeStamp) {
		return
	}
	if subscriptionEvent.Event.Type == subscriptionCreate {
		eh.SubscriptionMap[sub.SubscriptionID] = &sub
		logger.LoggerInternalMsg.Infof("Subscription for %s:%s is added.", subscriptionEvent.APIUUID, subscriptionEvent.ApplicationUUID)
	} else if subscriptionEvent.Event.Type == subscriptionUpdate {
		eh.SubscriptionMap[sub.SubscriptionID] = &sub
		logger.LoggerInternalMsg.Infof("Subscription for %s:%s is updated.", subscriptionEvent.APIUUID, subscriptionEvent.ApplicationUUID)
	} else if subscriptionEvent.Event.Type == subscriptionDelete {
		delete(eh.SubscriptionMap, sub.SubscriptionID)
		logger.LoggerInternalMsg.Infof("Subscription for %s:%s is deleted.", subscriptionEvent.APIUUID, subscriptionEvent.ApplicationUUID)
	}
	xds.UpdateEnforcerSubscriptions(xds.MarshalSubscriptionMap(eh.SubscriptionMap))
	// EventTypes: SUBSCRIPTIONS_CREATE, SUBSCRIPTIONS_UPDATE, SUBSCRIPTIONS_DELETE
}

// handlePolicyRelatedEvents to process policy related events
func handlePolicyEvents(data []byte, eventType string) {
	var policyEvent msg.PolicyInfo
	policyEventErr := json.Unmarshal([]byte(string(data)), &policyEvent)
	if policyEventErr != nil {
		logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling Throttling Policy event data %v", policyEventErr)
		return
	}
	// TODO: Handle policy events
	if strings.EqualFold(eventType, policyCreate) {
		logger.LoggerInternalMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, policyUpdate) {
		logger.LoggerInternalMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, policyDelete) {
		logger.LoggerInternalMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	}

	if strings.EqualFold(applicationEventType, policyEvent.PolicyType) {
		applicationPolicy := types.ApplicationPolicy{ID: policyEvent.PolicyID, TenantID: policyEvent.Event.TenantID,
			Name: policyEvent.PolicyName, QuotaType: policyEvent.QuotaType}

		if policyEvent.Event.Type == policyCreate {
			eh.ApplicationPolicyMap[applicationPolicy.ID] = &applicationPolicy
			logger.LoggerInternalMsg.Infof("Application Policy: %s is added.", applicationPolicy.Name)
		} else if policyEvent.Event.Type == policyUpdate {
			eh.ApplicationPolicyMap[applicationPolicy.ID] = &applicationPolicy
			logger.LoggerInternalMsg.Infof("Application Policy: %s is updated.", applicationPolicy.Name)
		} else if policyEvent.Event.Type == policyDelete {
			delete(eh.ApplicationPolicyMap, policyEvent.PolicyID)
			logger.LoggerInternalMsg.Infof("Application Policy: %s is deleted.", applicationPolicy.Name)
		}
		xds.UpdateEnforcerApplicationPolicies(xds.MarshalApplicationPolicyMap(eh.ApplicationPolicyMap))

	} else if strings.EqualFold(subscriptionEventType, policyEvent.PolicyType) {
		var subscriptionPolicyEvent msg.SubscriptionPolicyEvent
		subPolicyErr := json.Unmarshal([]byte(string(data)), &subscriptionPolicyEvent)
		if subPolicyErr != nil {
			logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling Subscription Policy event data %v", subPolicyErr)
			return
		}

		subscriptionPolicy := types.SubscriptionPolicy{ID: subscriptionPolicyEvent.PolicyID, TenantID: -1,
			Name: subscriptionPolicyEvent.PolicyName, QuotaType: subscriptionPolicyEvent.QuotaType,
			GraphQLMaxComplexity: subscriptionPolicyEvent.GraphQLMaxComplexity,
			GraphQLMaxDepth:      subscriptionPolicyEvent.GraphQLMaxDepth, RateLimitCount: subscriptionPolicyEvent.RateLimitCount,
			RateLimitTimeUnit: subscriptionPolicyEvent.RateLimitTimeUnit, StopOnQuotaReach: subscriptionPolicyEvent.StopOnQuotaReach,
			TenantDomain: subscriptionPolicyEvent.TenantDomain, TimeStamp: subscriptionPolicyEvent.TimeStamp}

		if subscriptionPolicyEvent.Event.Type == policyCreate {
			eh.SubscriptionPolicyMap[subscriptionPolicy.ID] = &subscriptionPolicy
			logger.LoggerInternalMsg.Infof("Subscription Policy: %s is added.", subscriptionPolicy.Name)
		} else if subscriptionPolicyEvent.Event.Type == policyUpdate {
			eh.SubscriptionPolicyMap[subscriptionPolicy.ID] = &subscriptionPolicy
			logger.LoggerInternalMsg.Infof("Subscription Policy: %s is updated.", subscriptionPolicy.Name)
		} else if subscriptionPolicyEvent.Event.Type == policyDelete {
			delete(eh.SubscriptionPolicyMap, subscriptionPolicy.ID)
			logger.LoggerInternalMsg.Infof("Subscription Policy: %s is deleted.", subscriptionPolicy.Name)
		}
		xds.UpdateEnforcerSubscriptionPolicies(xds.MarshalSubscriptionPolicyMap(eh.SubscriptionPolicyMap))
	}
}

func isLaterEvent(timeStampMap map[string]int64, mapKey string, currentTimeStamp int64) bool {
	if timeStamp, ok := timeStampMap[mapKey]; ok {
		if timeStamp > currentTimeStamp {
			return true
		}
	}
	timeStampMap[mapKey] = currentTimeStamp
	return false
}

func belongsToTenant(tenantDomain string) bool {
	// TODO : enable this once the events are fixed in apim
	// return config.GetControlPlaneConnectedTenantDomain() == tenantDomain
	return true
}

func parseNotificationJSONEvent(data []byte, notification *msg.EventNotification) error {
	unmarshalErr := json.Unmarshal(data, &notification)
	if unmarshalErr != nil {
		logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling "+
			"notification event data %v. Hence dropping the event", unmarshalErr)
	}
	return unmarshalErr
}
