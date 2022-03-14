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
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

// constant variables
const (
	apiEventType                = "API"
	applicationEventType        = "APPLICATION"
	subscriptionEventType       = "SUBSCRIPTION"
	scopeEvenType               = "SCOPE"
	policyEventType             = "POLICY"
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
	apiUpdate                   = "API_UPDATE"
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
	} else if strings.Contains(eventType, policyEventType) {
		handlePolicyEvents(decodedByte, eventType)
	}
	// other events will ignore including HEALTH_CHECK event
	return nil
}

// handleDefaultVersionUpdate will redeploy default versioned API.
// API runtime artifact doesn't get updated in CP side when default version is updated
// (isDefaultVersion prop in apiYaml is not updated). API deployment or should happen
// for it to get updated. However we need to redeploy the API when there is a default
// version change. For that we call `/apis` endpoint to get updated API metadata (this
// contains the updated `isDefaultVersion` field). Now we proceed with fetching runtime
// artifact from the CP. When creating CC deployment objects we refer to updated `APIList`
// map and update runtime artifact's `isDefaultVersion` field to correctly deploy default
// versioned API.
func handleDefaultVersionUpdate(event msg.APIEvent) {
	deployedEnvs := xds.GetDeployedEnvironments(event.UUID)
	for _, env := range deployedEnvs {
		query := make(map[string]string, 3)
		query[eh.GatewayLabelParam] = env
		query[eh.ContextParam] = event.APIContext
		query[eh.VersionParam] = event.APIVersion
		eh.UpdateAPIMetadataFromCP(query)
	}

	synchronizer.FetchAPIsFromControlPlane(event.UUID, deployedEnvs)
}

// handleAPIEvents to process api related data
func handleAPIEvents(data []byte, eventType string) {
	var (
		apiEvent              msg.APIEvent
		currentTimeStamp      int64 = apiEvent.Event.TimeStamp
		isDefaultVersionEvent bool
	)

	apiEventErr := json.Unmarshal([]byte(string(data)), &apiEvent)
	if apiEventErr != nil {
		logger.LoggerInternalMsg.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error occurred while unmarshalling API event data %v", apiEventErr),
			Severity:  logging.MAJOR,
			ErrorCode: 2004,
		})
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

	isDefaultVersionEvent = isDefaultVersionUpdate(apiEvent)

	if isDefaultVersionEvent {
		handleDefaultVersionUpdate(apiEvent)
		return
	}

	// Per each revision, synchronization should happen.
	if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
		go synchronizer.FetchAPIsFromControlPlane(apiEvent.UUID, apiEvent.GatewayLabels)
	}

	for _, env := range apiEvent.GatewayLabels {
		if isLaterEvent(apiListTimeStampMap, apiEvent.UUID+":"+env, currentTimeStamp) {
			break
		}
		// removeFromGateway event with multiple labels could only appear when the API is subjected
		// to delete. Hence we could simply delete after checking against just one iteration.
		if strings.EqualFold(removeAPIFromGateway, apiEvent.Event.Type) {
			xds.DeleteAPIWithAPIMEvent(apiEvent.UUID, apiEvent.TenantDomain, apiEvent.GatewayLabels, "")
			for _, env := range apiEvent.GatewayLabels {
				xdsAPIList := xds.DeleteAPIAndReturnList(apiEvent.UUID, apiEvent.TenantDomain, env)
				if xdsAPIList != nil {
					xds.UpdateEnforcerAPIList(env, xdsAPIList)
				}
			}
			break
		}
		if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
			conf, _ := config.ReadConfigs()
			configuredEnvs := conf.ControlPlane.EnvironmentLabels
			if len(configuredEnvs) == 0 {
				configuredEnvs = append(configuredEnvs, config.DefaultGatewayName)
			}
			for _, configuredEnv := range configuredEnvs {
				if configuredEnv == env {
					if xds.CheckIfAPIMetadataIsAlreadyAvailable(apiEvent.UUID, env) {
						logger.LoggerInternalMsg.Debugf("API Metadata for api Id: %s is not updated as it already exists", apiEvent.UUID)
						continue
					}
					logger.LoggerInternalMsg.Debugf("Fetching Metadata for api Id: %s ", apiEvent.UUID)
					queryParamMap := make(map[string]string, 3)
					queryParamMap[eh.GatewayLabelParam] = configuredEnv
					queryParamMap[eh.ContextParam] = apiEvent.Context
					queryParamMap[eh.VersionParam] = apiEvent.Version
					var apiList *types.APIList
					go eh.InvokeService(eh.ApisEndpoint, apiList, queryParamMap, eh.APIListChannel, 0)
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
		xdsAPIList := xds.MarshalAPIForLifeCycleChangeEventAndReturnList(apiEvent.UUID, apiEvent.APIStatus, configuredEnv)
		if xdsAPIList != nil {
			xds.UpdateEnforcerAPIList(configuredEnv, xdsAPIList)
		}
	}
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

		applicationKeyMappingReference := xds.GetApplicationKeyMappingReference(&applicationKeyMapping)

		if isLaterEvent(applicationKeyMappingTimeStampMap, fmt.Sprint(applicationKeyMappingReference),
			applicationRegistrationEvent.TimeStamp) {
			return
		}

		var appKeyMappingList *subscription.ApplicationKeyMappingList
		if strings.EqualFold(removeApplicationKeyMapping, eventType) {
			appKeyMappingList = xds.MarshalApplicationKeyMappingEventAndReturnList(&applicationKeyMapping, xds.DeleteEvent)
		} else {
			appKeyMappingList = xds.MarshalApplicationKeyMappingEventAndReturnList(&applicationKeyMapping, xds.CreateEvent)
		}
		xds.UpdateEnforcerApplicationKeyMappings(appKeyMappingList)
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

		app := types.Application{UUID: applicationEvent.UUID, ID: applicationEvent.ApplicationID,
			Name: applicationEvent.ApplicationName, SubName: applicationEvent.Subscriber,
			Policy: applicationEvent.ApplicationPolicy, TokenType: applicationEvent.TokenType,
			GroupIds: applicationEvent.GroupID, Attributes: nil,
			TenantID: -1, TenantDomain: applicationEvent.TenantDomain, TimeStamp: applicationEvent.TimeStamp}

		if isLaterEvent(applicationListTimeStampMap, fmt.Sprint(applicationEvent.ApplicationID), applicationEvent.TimeStamp) {
			return
		}

		var appList *subscription.ApplicationList
		if applicationEvent.Event.Type == applicationCreate {
			appList = xds.MarshalApplicationEventAndReturnList(&app, xds.CreateEvent)
		} else if applicationEvent.Event.Type == applicationUpdate {
			appList = xds.MarshalApplicationEventAndReturnList(&app, xds.UpdateEvent)
		} else if applicationEvent.Event.Type == applicationDelete {
			appList = xds.MarshalApplicationEventAndReturnList(&app, xds.DeleteEvent)
		} else {
			logger.LoggerInternalMsg.Warnf("Application Event Type is not recognized for the Event under "+
				"Application UUID %s", app.UUID)
			return
		}
		xds.UpdateEnforcerApplications(appList)
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
	var subList *subscription.SubscriptionList
	if subscriptionEvent.Event.Type == subscriptionCreate {
		subList = xds.MarshalSubscriptionEventAndReturnList(&sub, xds.CreateEvent)
	} else if subscriptionEvent.Event.Type == subscriptionUpdate {
		subList = xds.MarshalSubscriptionEventAndReturnList(&sub, xds.UpdateEvent)
	} else if subscriptionEvent.Event.Type == subscriptionDelete {
		subList = xds.MarshalSubscriptionEventAndReturnList(&sub, xds.DeleteEvent)
	} else {
		logger.LoggerInternalMsg.Warnf("Subscription Event Type is not recognized for the Event under "+
			"Application UUID %s and API UUID %s", sub.ApplicationUUID, sub.APIUUID)
		return
	}
	// EventTypes: SUBSCRIPTIONS_CREATE, SUBSCRIPTIONS_UPDATE, SUBSCRIPTIONS_DELETE
	xds.UpdateEnforcerSubscriptions(subList)
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
		var applicationPolicyList *subscription.ApplicationPolicyList
		if policyEvent.Event.Type == policyCreate {
			applicationPolicyList = xds.MarshalApplicationPolicyEventAndReturnList(&applicationPolicy, xds.CreateEvent)
		} else if policyEvent.Event.Type == policyUpdate {
			applicationPolicyList = xds.MarshalApplicationPolicyEventAndReturnList(&applicationPolicy, xds.UpdateEvent)
		} else if policyEvent.Event.Type == policyDelete {
			applicationPolicyList = xds.MarshalApplicationPolicyEventAndReturnList(&applicationPolicy, xds.DeleteEvent)
		} else {
			logger.LoggerInternalMsg.Warnf("ApplicationPolicy Event Type is not recognized for the Event under "+
				" policy name %s", policyEvent.PolicyName)
			return
		}
		xds.UpdateEnforcerApplicationPolicies(applicationPolicyList)

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

		var subscriptionPolicyList *subscription.SubscriptionPolicyList
		if subscriptionPolicyEvent.Event.Type == policyCreate {
			subscriptionPolicyList = xds.MarshalSubscriptionPolicyEventAndReturnList(&subscriptionPolicy, xds.CreateEvent)
		} else if subscriptionPolicyEvent.Event.Type == policyUpdate {
			subscriptionPolicyList = xds.MarshalSubscriptionPolicyEventAndReturnList(&subscriptionPolicy, xds.UpdateEvent)
		} else if subscriptionPolicyEvent.Event.Type == policyDelete {
			subscriptionPolicyList = xds.MarshalSubscriptionPolicyEventAndReturnList(&subscriptionPolicy, xds.DeleteEvent)
		} else {
			logger.LoggerInternalMsg.Warnf("SubscriptionPolicy Event Type is not recognized for the Event under "+
				" policy name %s", policyEvent.PolicyName)
			return
		}
		xds.UpdateEnforcerSubscriptionPolicies(subscriptionPolicyList)
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

func isDefaultVersionUpdate(event msg.APIEvent) bool {
	return strings.EqualFold(apiUpdate, event.Event.Type) && strings.EqualFold("DEFAULT_VERSION", event.Action)
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
