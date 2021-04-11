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

	"github.com/streadway/amqp"
	"github.com/wso2/adapter/config"
	"github.com/wso2/adapter/internal/discovery/xds"
	eh "github.com/wso2/adapter/internal/eventhub"
	"github.com/wso2/adapter/internal/eventhub/types"
	"github.com/wso2/adapter/internal/synchronizer"
	logger "github.com/wso2/adapter/loggers"
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
	apiLifeCycleChange          = "API_LIFECYCLE_CHANGE"
	applicationCreate           = "APPLICATION_CREATE"
	applicationUpdate           = "APPLICATION_UPDATE"
	applicationDelete           = "APPLICATION_DELETE"
	subscriptionCreate          = "SUBSCRIPTIONS_CREATE"
	subscriptionUpdate          = "SUBSCRIPTIONS_UPDATE"
	subscriptionDelete          = "SUBSCRIPTIONS_DELETE"
	policyCreate                = "POLICY_CREATE"
	policyUpdate                = "POLICY_UPDATE"
	policyDelete                = "POLICY_DELETE"
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
func handleNotification(deliveries <-chan amqp.Delivery, done chan error) {
	for d := range deliveries {
		var notification EventNotification
		var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)
		var decodedByte, err = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Event)
		if err != nil {
			if _, ok := err.(base64.CorruptInputError); ok {
				panic("\nbase64 input is corrupt, check the provided key")
			}
			panic(err)
		}
		logger.LoggerMsg.Debugf("\n\n[%s]", decodedByte)
		eventType = notification.Event.PayloadData.EventType

		if strings.Contains(eventType, apiEventType) {
			handleAPIEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, applicationEventType) {
			handleApplicationEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, subscriptionEventType) {
			handleSubscriptionEvents(decodedByte, eventType)
		} else {
			handlePolicyEvents(decodedByte, eventType)
		}
		d.Ack(false)
	}
	logger.LoggerMsg.Infof("handle: deliveries channel closed")
	done <- nil
}

// handleAPIEvents to process api related data
func handleAPIEvents(data []byte, eventType string) {
	var (
		apiEvent         APIEvent
		currentTimeStamp int64 = apiEvent.Event.TimeStamp
	)

	json.Unmarshal([]byte(string(data)), &apiEvent)
	// Per each revision, synchronization should happen.
	if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
		go synchronizer.FetchAPIsFromControlPlane(apiEvent.UUID, apiEvent.GatewayLabels)
	}

	// TODO: (VirajSalaka) Handle API Blocked event
	for _, env := range apiEvent.GatewayLabels {
		// TODO: (VirajSalaka) This stores unnecessary keyvalue pairs as well.
		if isLaterEvent(apiListTimeStampMap, apiEvent.UUID+":"+env, currentTimeStamp) {
			return
		}
		if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
			conf, _ := config.ReadConfigs()
			configuredEnvs := conf.ControlPlane.EventHub.EnvironmentLabels
			if len(configuredEnvs) == 0 {
				configuredEnvs = append(configuredEnvs, eh.DefaultGatewayLabelValue)
			}
			for _, configuredEnv := range configuredEnvs {
				if configuredEnv == env {
					if _, ok := eh.APIListMap[env]; ok {
						apiListOfEnv := eh.APIListMap[env].List
						for i := range apiListOfEnv {
							// If API is already found, it is a new revision deployement.
							// Subscription relates details of an API does not change between new revisions
							if apiEvent.Context == apiListOfEnv[i].Context && apiEvent.Version == apiListOfEnv[i].Version {
								logger.LoggerMsg.Debugf("APIList for apiIId: %s is not updated as it already exists", apiEvent.UUID)
								return
							}
						}
						queryParamMap := make(map[string]string, 3)
						queryParamMap[eh.GatewayLabelParam] = configuredEnv
						queryParamMap[eh.ContextParam] = apiEvent.Context
						queryParamMap[eh.VersionParam] = apiEvent.Version
						// TODO: (VirajSalaka) Fix the REST API call once the APIM Event hub implementation is fixed.
						// TODO: (VirajSalaka) Optimize the number of requests sent to /apis endpoint as the same API is returned
						// repeatedly. (If Eventhub implementation is not fixed)
						go eh.InvokeService(eh.ApisEndpoint, eh.APIListMap[env], queryParamMap,
							eh.APIListChannel, 0)
					}
				}
			}
		} else if strings.EqualFold(removeAPIFromGateway, apiEvent.Event.Type) {
			xds.DeleteAPIWithAPIMEvent(apiEvent.UUID, apiEvent.Name, apiEvent.Version, apiEvent.GatewayLabels)
			logger.LoggerMsg.Debugf("Undeployed API from router")
			if _, ok := eh.APIListMap[env]; ok {
				apiListOfEnv := eh.APIListMap[env].List
				for i := range apiListOfEnv {
					// TODO: (VirajSalaka) Use APIId once it is fixed from control plane
					if apiEvent.Context == apiListOfEnv[i].Context && apiEvent.Version == apiListOfEnv[i].Version {
						eh.APIListMap[env].List = deleteAPIFromList(apiListOfEnv, i, apiEvent.UUID, env)
						xds.UpdateEnforcerAPIList(env, xds.MarshalAPIList(eh.APIListMap[env]))
						break
					}
				}
			}
		}
	}
}

// deleteAPIFromList when remove API From Gateway event happens
func deleteAPIFromList(apiList []types.API, indexToBeDeleted int, apiUUID string, label string) []types.API {
	apiList[indexToBeDeleted] = apiList[len(apiList)-1]
	logger.LoggerMsg.Infof("API %s is deleted from APIList under Label %s", apiUUID, label)
	return apiList[:len(apiList)-1]
}

// handleApplicationEvents to process application related events
func handleApplicationEvents(data []byte, eventType string) {
	if strings.EqualFold(applicationRegistration, eventType) ||
		strings.EqualFold(removeApplicationKeyMapping, eventType) {
		var applicationRegistrationEvent ApplicationRegistrationEvent
		json.Unmarshal([]byte(string(data)), &applicationRegistrationEvent)

		applicationKeyMapping := types.ApplicationKeyMapping{ApplicationID: applicationRegistrationEvent.ApplicationID,
			ConsumerKey: applicationRegistrationEvent.ConsumerKey, KeyType: applicationRegistrationEvent.KeyType,
			KeyManager: applicationRegistrationEvent.KeyManager, TenantID: -1, TenantDomain: applicationRegistrationEvent.TenantDomain,
			TimeStamp: applicationRegistrationEvent.TimeStamp}

		if isLaterEvent(applicationKeyMappingTimeStampMap, fmt.Sprint(applicationRegistrationEvent.ApplicationID),
			applicationRegistrationEvent.TimeStamp) {
			return
		}

		eh.AppKeyMappingList.List = append(eh.AppKeyMappingList.List, applicationKeyMapping)
		xds.UpdateEnforcerApplicationKeyMappings(xds.MarshalKeyMappingList(eh.AppKeyMappingList))
	} else {
		var applicationEvent ApplicationEvent
		json.Unmarshal([]byte(string(data)), &applicationEvent)
		application := types.Application{UUID: applicationEvent.UUID, ID: applicationEvent.ApplicationID,
			Name: applicationEvent.ApplicationName, SubName: applicationEvent.Subscriber,
			Policy: applicationEvent.ApplicationPolicy, TokenType: applicationEvent.TokenType,
			GroupIds: applicationEvent.GroupID, Attributes: nil,
			TenantID: -1, TenantDomain: applicationEvent.TenantDomain, TimeStamp: applicationEvent.TimeStamp}

		if isLaterEvent(applicationListTimeStampMap, fmt.Sprint(applicationEvent.ApplicationID), applicationEvent.TimeStamp) {
			return
		}

		if applicationEvent.Event.Type == applicationCreate {
			eh.AppList.List = append(eh.AppList.List, application)
			logger.LoggerMsg.Infof("Application %s is added.", applicationEvent.ApplicationName)
		} else if applicationEvent.Event.Type == applicationUpdate {
			eh.AppList.List = removeApplication(eh.AppList.List, applicationEvent.ApplicationID)
			eh.AppList.List = append(eh.AppList.List, application)
			logger.LoggerMsg.Infof("Application %s is added.", applicationEvent.ApplicationName)
		} else if applicationEvent.Event.Type == applicationDelete {
			eh.AppList.List = removeApplication(eh.AppList.List, applicationEvent.ApplicationID)
		}
		xds.UpdateEnforcerApplications(xds.MarshalApplicationList(eh.AppList))
	}
}

// handleSubscriptionRelatedEvents to process subscription related events
func handleSubscriptionEvents(data []byte, eventType string) {
	var subscriptionEvent SubscriptionEvent
	json.Unmarshal([]byte(string(data)), &subscriptionEvent)
	sub := types.Subscription{SubscriptionID: subscriptionEvent.SubscriptionID, PolicyID: subscriptionEvent.PolicyID,
		APIID: subscriptionEvent.APIID, AppID: subscriptionEvent.ApplicationID, SubscriptionState: subscriptionEvent.SubscriptionState,
		TenantID: subscriptionEvent.TenantID, TenantDomain: subscriptionEvent.TenantDomain, TimeStamp: subscriptionEvent.TimeStamp}

	if isLaterEvent(subsriptionsListTimeStampMap, fmt.Sprint(subscriptionEvent.SubscriptionID), subscriptionEvent.TimeStamp) {
		return
	}
	if subscriptionEvent.Event.Type == subscriptionCreate {
		updateSubscription(subscriptionEvent.SubscriptionID, sub)
	} else if subscriptionEvent.Event.Type == subscriptionUpdate {
		eh.SubList.List = removeSubscription(eh.SubList.List, subscriptionEvent.SubscriptionID)
		updateSubscription(subscriptionEvent.SubscriptionID, sub)
	} else if subscriptionEvent.Event.Type == subscriptionDelete {
		eh.SubList.List = removeSubscription(eh.SubList.List, subscriptionEvent.SubscriptionID)
	}
	xds.UpdateEnforcerSubscriptions(xds.MarshalSubscriptionList(eh.SubList))
	// EventTypes: SUBSCRIPTIONS_CREATE, SUBSCRIPTIONS_UPDATE, SUBSCRIPTIONS_DELETE
}

// handlePolicyRelatedEvents to process policy related events
func handlePolicyEvents(data []byte, eventType string) {
	var policyEvent PolicyInfo
	json.Unmarshal([]byte(string(data)), &policyEvent)

	// TODO: Handle policy events
	if strings.EqualFold(eventType, policyCreate) {
		logger.LoggerMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, policyUpdate) {
		logger.LoggerMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, policyDelete) {
		logger.LoggerMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	}

	// TODO: (VirajSalaka) Decide if it is required to have API Level Policies
	// if strings.EqualFold(apiEventType, policyEvent.PolicyType) {
	// 	var apiPolicyEvent APIPolicyEvent
	// 	json.Unmarshal([]byte(string(data)), &apiPolicyEvent)
	// } else
	if strings.EqualFold(applicationEventType, policyEvent.PolicyType) {
		applicationPolicy := types.ApplicationPolicy{ID: policyEvent.PolicyID, TenantID: policyEvent.Event.TenantID,
			Name: policyEvent.PolicyName, QuotaType: policyEvent.QuotaType}

		if policyEvent.Event.Type == policyCreate {
			eh.AppPolicyList.List = append(eh.AppPolicyList.List, applicationPolicy)
		} else if policyEvent.Event.Type == policyUpdate {
			eh.AppPolicyList.List = removeAppPolicy(eh.AppPolicyList.List, policyEvent.PolicyID)
			eh.AppPolicyList.List = append(eh.AppPolicyList.List, applicationPolicy)
		} else if policyEvent.Event.Type == policyDelete {
			eh.AppPolicyList.List = removeAppPolicy(eh.AppPolicyList.List, policyEvent.PolicyID)
		}
		xds.UpdateEnforcerApplicationPolicies(xds.MarshalApplicationPolicyList(eh.AppPolicyList))

	} else if strings.EqualFold(subscriptionEventType, policyEvent.PolicyType) {
		var subscriptionPolicyEvent SubscriptionPolicyEvent
		json.Unmarshal([]byte(string(data)), &subscriptionPolicyEvent)

		subscriptionPolicy := types.SubscriptionPolicy{ID: subscriptionPolicyEvent.PolicyID, TenantID: -1,
			Name: subscriptionPolicyEvent.PolicyName, QuotaType: subscriptionPolicyEvent.QuotaType,
			GraphQLMaxComplexity: subscriptionPolicyEvent.GraphQLMaxComplexity,
			GraphQLMaxDepth:      subscriptionPolicyEvent.GraphQLMaxDepth, RateLimitCount: subscriptionPolicyEvent.RateLimitCount,
			RateLimitTimeUnit: subscriptionPolicyEvent.RateLimitTimeUnit, StopOnQuotaReach: subscriptionPolicyEvent.StopOnQuotaReach,
			TenantDomain: subscriptionPolicyEvent.TenantDomain, TimeStamp: subscriptionPolicyEvent.TimeStamp}

		if subscriptionPolicyEvent.Event.Type == policyCreate {
			eh.SubPolicyList.List = append(eh.SubPolicyList.List, subscriptionPolicy)
		} else if subscriptionPolicyEvent.Event.Type == policyUpdate {
			eh.SubPolicyList.List = removeSubPolicy(eh.SubPolicyList.List, subscriptionPolicyEvent.PolicyID)
			eh.SubPolicyList.List = append(eh.SubPolicyList.List, subscriptionPolicy)
		} else if subscriptionPolicyEvent.Event.Type == policyDelete {
			eh.SubPolicyList.List = removeSubPolicy(eh.SubPolicyList.List, subscriptionPolicyEvent.PolicyID)
		}
		xds.UpdateEnforcerSubscriptionPolicies(xds.MarshalSubscriptionPolicyList(eh.SubPolicyList))
	}
}

func removeApplication(applications []types.Application, id int32) []types.Application {
	// TODO: (VirajSalaka) Improve the search logic with binary search mechanism
	deleteIndex := -1
	appName := ""
	for index, app := range applications {
		if app.ID == id {
			deleteIndex = index
			appName = app.Name
			break
		}
	}
	if deleteIndex == -1 {
		logger.LoggerMsg.Debugf("Application under id: %d is not available", id)
		return nil
	}
	applications[deleteIndex] = applications[len(applications)-1]
	logger.LoggerMsg.Infof("Application %s is deleted.", appName)
	return applications[:len(applications)-1]
}

func removeSubscription(subscriptions []types.Subscription, id int32) []types.Subscription {
	deleteIndex := -1
	// multiple events are sent in subscription scenario
	for index, sub := range subscriptions {
		if sub.SubscriptionID == id {
			deleteIndex = index
		}
	}
	if deleteIndex == -1 {
		logger.LoggerMsg.Debugf("Subscription under id: %d is not available", id)
		return nil
	}
	subscriptions[deleteIndex] = subscriptions[len(subscriptions)-1]
	logger.LoggerMsg.Debugf("Subscription under id: %d is deleted.", id)
	return subscriptions[:len(subscriptions)-1]
}

func updateSubscription(id int32, sub types.Subscription) {
	//Iterated in reverse to optimize handling subscription creation scenario.
	updateIndex := -1
	for index := len(eh.SubList.List) - 1; index >= 0; index-- {
		if eh.SubList.List[index].SubscriptionID == id {
			updateIndex = index
			break
		}
	}
	if updateIndex == -1 {
		eh.SubList.List = append(eh.SubList.List, sub)
		return
	}
	eh.SubList.List[updateIndex] = sub
}

func removeAppPolicy(appPolicies []types.ApplicationPolicy, id int32) []types.ApplicationPolicy {
	deleteIndex := -1
	for index, policy := range appPolicies {
		if policy.ID == id {
			deleteIndex = index
			break
		}
	}
	if deleteIndex == -1 {
		logger.LoggerMsg.Debugf("Application Policy under id: %d is not available", id)
		return nil
	}
	appPolicies[deleteIndex] = appPolicies[len(appPolicies)-1]
	return appPolicies[:len(appPolicies)-1]
}

func removeSubPolicy(subPolicies []types.SubscriptionPolicy, id int32) []types.SubscriptionPolicy {
	deleteIndex := -1
	for index, policy := range subPolicies {
		if policy.ID == id {
			deleteIndex = index
			break
		}
	}
	if deleteIndex == -1 {
		logger.LoggerMsg.Debugf("Subscription Policy under id: %d is not available", id)
		return nil
	}
	subPolicies[deleteIndex] = subPolicies[len(subPolicies)-1]
	return subPolicies[:len(subPolicies)-1]
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
