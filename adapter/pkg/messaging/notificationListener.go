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
	"strconv"
	"strings"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
	resourceTypes "github.com/wso2/micro-gw/pkg/resourcetypes"
)

// constant variables
const (
	apiEventType                = "API"
	applicationEventType        = "APPLICATION"
	subscriptionEventType       = "SUBSCRIPTIONS"
	scopeEvenType               = "SCOPE"
	removeAPIFromGateway        = "REMOVE_API_FROM_GATEWAY"
	deployAPIToGateway          = "DEPLOY_API_IN_GATEWAY"
	applicationRegistration     = "APPLICATION_REGISTRATION_CREATE"
	removeApplicationKeyMapping = "REMOVE_APPLICATION_KEYMAPPING"
	apiLifeCycleChange          = "API_LIFECYCLE_CHANGE"
)

// var variables
var (
	SubList                   = make([]resourceTypes.Subscription, 0)
	AppKeyMappingList         = make([]resourceTypes.ApplicationKeyMapping, 0)
	APIList                   = make([]resourceTypes.API, 0)
	ScopeList                 = make([]resourceTypes.Scope, 0)
	AppPolicyList             = make([]resourceTypes.ApplicationPolicy, 0)
	SubPolicyList             = make([]resourceTypes.SubscriptionPolicy, 0)
	ApplicationKeyMappingList = make([]resourceTypes.SubscriptionPolicy, 0)
	AppList                   = make([]resourceTypes.Application, 0)
	APIListTimeStamp          = make(map[string]int64, 0)
	ApplicationListTimeStamp  = make(map[string]int64, 0)
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
		logger.LoggerMsg.Infof("\n\n[%s]", decodedByte)
		eventType = notification.Event.PayloadData.EventType

		if strings.Contains(eventType, apiEventType) {
			handleAPIEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, applicationEventType) {
			handleApplicationEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, subscriptionEventType) {
			handleSubscriptionEvents(decodedByte, eventType)
		} else if strings.Contains(eventType, scopeEvenType) {
			handleScopeEvents(decodedByte, eventType)
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
		apiEvent     APIEvent
		oldTimeStamp int64
		indexOfAPI   int
		isFound      bool
		newTimeStamp int64 = apiEvent.Event.TimeStamp
	)

	json.Unmarshal([]byte(string(data)), &apiEvent)
	timeStampList := APIListTimeStamp
	for apiID, timeStamp := range timeStampList {
		fmt.Println(apiID, " timeStamp value is", timeStamp)
		if strings.EqualFold(apiEvent.APIID, apiID) {
			oldTimeStamp = timeStamp
		}
	}

	APIListTimeStamp[apiEvent.APIID] = newTimeStamp

	for i := range APIList {
		if strings.EqualFold(apiEvent.APIID, string(APIList[i].APIID)) {
			isFound = true
			indexOfAPI = i
			break
		}
	}

	logger.LoggerMsg.Infof("oldTimeStamp: %v , newTimeStamp: %v", oldTimeStamp, newTimeStamp)
	if isFound && oldTimeStamp < newTimeStamp && strings.EqualFold(removeAPIFromGateway, apiEvent.Event.Type) {
		deleteAPIFromList(indexOfAPI, apiEvent.APIID)
	} else if strings.EqualFold(deployAPIToGateway, apiEvent.Event.Type) {
		// pull API details
		ID, err := strconv.ParseInt(apiEvent.APIID, 01, 32)
		if err != nil {
			logger.LoggerMsg.Errorf("Cannot cast %s to an Integer", apiEvent.APIID)
		} else {
			api := resourceTypes.API{APIID: int32(ID), Provider: apiEvent.APIProvider, Name: apiEvent.APIName,
				Version: apiEvent.APIVersion, Context: apiEvent.APIContext, APIType: apiEvent.APIType,
				APIStatus: apiEvent.APIStatus, IsDefaultVersion: true, TenantID: apiEvent.TenantID,
				TenantDomain: apiEvent.Event.TenantDomain, TimeStamp: apiEvent.Event.TimeStamp}
			APIList = append(APIList, api)
			logger.LoggerMsg.Infof("API %s is added/updated to APIList", apiEvent.APIID)
		}
	}
	fmt.Println(APIList)
}

// deleteAPIFromList when remove API From Gateway event happens
func deleteAPIFromList(indexToBeDeleted int, apiID string) {
	copy(APIList[indexToBeDeleted:], APIList[indexToBeDeleted+1:])
	APIList[len(APIList)-1] = resourceTypes.API{}
	APIList = APIList[:len(APIList)-1]
	logger.LoggerMsg.Infof("API %s is deleted from APIList", apiID)
}

// handleApplicationEvents to process application related events
func handleApplicationEvents(data []byte, eventType string) {
	if strings.EqualFold(applicationRegistration, eventType) ||
		strings.EqualFold(removeApplicationKeyMapping, eventType) {
		var applicationRegistrationEvent ApplicationRegistrationEvent
		json.Unmarshal([]byte(string(data)), &applicationRegistrationEvent)

		applicationKeyMapping := resourceTypes.ApplicationKeyMapping{ApplicationID: applicationRegistrationEvent.ApplicationID,
			ConsumerKey: applicationRegistrationEvent.ConsumerKey, KeyType: applicationRegistrationEvent.KeyType,
			KeyManager: applicationRegistrationEvent.KeyManager, TenantID: -1, TenantDomain: applicationRegistrationEvent.TenantDomain,
			TimeStamp: applicationRegistrationEvent.TimeStamp}

		AppKeyMappingList = append(AppKeyMappingList, applicationKeyMapping)
	} else {
		var applicationEvent ApplicationEvent
		json.Unmarshal([]byte(string(data)), &applicationEvent)
		application := resourceTypes.Application{UUID: applicationEvent.UUID, ID: applicationEvent.ApplicationID,
			Name: applicationEvent.ApplicationName, SubName: applicationEvent.Subscriber, Policy: applicationEvent.ApplicationPolicy, TokenType: applicationEvent.TokenType, GroupIds: applicationEvent.GroupID, Attributes: nil,
			TenantID: -1, TenantDomain: applicationEvent.TenantDomain, TimeStamp: applicationEvent.TimeStamp}

		AppList = append(AppList, application)
		// EventTypes: APPLICATION_CREATE, APPLICATION_UPDATE, APPLICATION_DELETE
	}
}

// handleSubscriptionRelatedEvents to process subscription related events
func handleSubscriptionEvents(data []byte, eventType string) {
	var subscriptionEvent SubscriptionEvent
	json.Unmarshal([]byte(string(data)), &subscriptionEvent)
	subscription := resourceTypes.Subscription{SubscriptionID: subscriptionEvent.SubscriptionID, PolicyID: subscriptionEvent.PolicyID,
		APIID: subscriptionEvent.APIID, AppID: subscriptionEvent.ApplicationID, SubscriptionState: subscriptionEvent.SubscriptionState,
		TenantID: subscriptionEvent.TenantID, TenantDomain: subscriptionEvent.TenantDomain, TimeStamp: subscriptionEvent.TimeStamp}

	SubList = append(SubList, subscription)
	// EventTypes: SUBSCRIPTIONS_CREATE, SUBSCRIPTIONS_UPDATE, SUBSCRIPTIONS_DELETE
}

// handleScopeRelatedEvents to process scope related events
func handleScopeEvents(data []byte, eventType string) {
	var scopeEvent ScopeEvent
	json.Unmarshal([]byte(string(data)), &scopeEvent)
	scope := resourceTypes.Scope{Name: scopeEvent.Name, DisplayName: scopeEvent.DisplayName, ApplicationName: scopeEvent.ApplicationName}
	ScopeList = append(ScopeList, scope)
	// EventTypes: SCOPE_CREATE, SCOPE_UPDATE,SCOPE_DELETE
}

// handlePolicyRelatedEvents to process policy related events
func handlePolicyEvents(data []byte, eventType string) {
	var policyEvent PolicyInfo
	json.Unmarshal([]byte(string(data)), &policyEvent)

	// TODO: Handle policy events
	if strings.EqualFold(eventType, "POLICY_CREATE") {
		logger.LoggerMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, "POLICY_UPDATE") {
		logger.LoggerMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, "POLICY_DELETE") {
		logger.LoggerMsg.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	}

	if strings.EqualFold(apiEventType, policyEvent.PolicyType) {
		var apiPolicyEvent APIPolicyEvent
		json.Unmarshal([]byte(string(data)), &apiPolicyEvent)
	} else if strings.EqualFold(applicationEventType, policyEvent.PolicyType) {
		applicationPolicy := resourceTypes.ApplicationPolicy{ID: policyEvent.PolicyID, TenantID: -1, Name: policyEvent.PolicyName,
			QuotaType: policyEvent.QuotaType}
		AppPolicyList = append(AppPolicyList, applicationPolicy)

	} else if strings.EqualFold(subscriptionEventType, policyEvent.PolicyType) {
		var subscriptionPolicyEvent SubscriptionPolicyEvent
		json.Unmarshal([]byte(string(data)), &subscriptionPolicyEvent)

		subscriptionPolicy := resourceTypes.SubscriptionPolicy{ID: subscriptionPolicyEvent.PolicyID, TenantID: -1,
			Name: subscriptionPolicyEvent.PolicyName, QuotaType: subscriptionPolicyEvent.QuotaType,
			GraphQLMaxComplexity: subscriptionPolicyEvent.GraphQLMaxComplexity,
			GraphQLMaxDepth:      subscriptionPolicyEvent.GraphQLMaxDepth, RateLimitCount: subscriptionPolicyEvent.RateLimitCount,
			RateLimitTimeUnit: subscriptionPolicyEvent.RateLimitTimeUnit, StopOnQuotaReach: subscriptionPolicyEvent.StopOnQuotaReach,
			TenantDomain: subscriptionPolicyEvent.TenantDomain, TimeStamp: subscriptionPolicyEvent.TimeStamp}

		SubPolicyList = append(SubPolicyList, subscriptionPolicy)
	}
}
