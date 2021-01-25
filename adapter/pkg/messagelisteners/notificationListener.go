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

// Package messagelisteners holds the implementation for event listeners functions
package messagelisteners

import (
	"encoding/base64"
	"encoding/json"
	"strconv"
	"strings"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
	resourcetypes "github.com/wso2/micro-gw/pkg/resource_types"
)

const (
	apiEventType          = "API"
	applicationEventType  = "APPLICATION"
	subscriptionEventType = "SUBSCRIPTIONS"
	scopeEvenType         = "SCOPE"
)

// var variables
var (
	SubList                   = make([]resourcetypes.Subscription, 0)
	AppKeyMappingList         = make([]resourcetypes.ApplicationKeyMapping, 0)
	APIList                   = make([]resourcetypes.API, 0)
	ScopeList                 = make([]resourcetypes.Scope, 0)
	AppPolicyList             = make([]resourcetypes.ApplicationPolicy, 0)
	SubPolicyList             = make([]resourcetypes.SubscriptionPolicy, 0)
	ApplicationKeyMappingList = make([]resourcetypes.SubscriptionPolicy, 0)
	AppList                   = make([]resourcetypes.Application, 0)
)

// handleNotification to process
func handleNotification(deliveries <-chan amqp.Delivery, done chan error) {
	for d := range deliveries {
		var notification EventNotification
		var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)
		// fmt.Printf("EventType: %s, event: %s", notification.Event.PayloadData.EventType, notification.Event.PayloadData.Event)

		var decodedByte, err = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Event)
		if err != nil {
			if _, ok := err.(base64.CorruptInputError); ok {
				panic("\nbase64 input is corrupt, check service Key")
			}
			panic(err)
		}
		logger.LoggerJMS.Infof("\n\n[%s]", decodedByte)
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
	logger.LoggerJMS.Infof("handle: deliveries channel closed")
	done <- nil
}

// handleAPIRelatedEvents to process
func handleAPIEvents(data []byte, eventType string) {
	var apiEvent APIEvent
	json.Unmarshal([]byte(string(data)), &apiEvent)
	logger.LoggerJMS.Infof("EventType: %s, api: %v", apiEvent.Event.Type, apiEvent.APIType)

	apiID, err := strconv.Atoi(apiEvent.APIID)

	if err != nil {
		api := resourcetypes.API{APIID: apiID, Provider: apiEvent.APIProvider, Name: apiEvent.APIName,
			Version: apiEvent.APIVersion, Context: "", APIType: apiEvent.APIType, IsDefaultVersion: true,
			TenantID: -1, TenantDomain: apiEvent.Event.TenantDomain, TimeStamp: apiEvent.Event.TimeStamp}
		APIList = append(APIList, api)
	}
}

// handleApplicationRelatedEvents to process
func handleApplicationEvents(data []byte, eventType string) {
	if strings.EqualFold("APPLICATION_REGISTRATION_CREATE", eventType) ||
		strings.EqualFold("REMOVE_APPLICATION_KEYMAPPING", eventType) {
		var applicationRegistrationEvent ApplicationRegistrationEvent
		json.Unmarshal([]byte(string(data)), &applicationRegistrationEvent)
		logger.LoggerJMS.Infof("EventType: %s for application %v, consumerKey %s", eventType,
			applicationRegistrationEvent.ApplicationID, applicationRegistrationEvent.ConsumerKey)

		applicationKeyMapping := resourcetypes.ApplicationKeyMapping{ApplicationID: applicationRegistrationEvent.ApplicationID,
			ConsumerKey: applicationRegistrationEvent.ConsumerKey, KeyType: applicationRegistrationEvent.KeyType,
			KeyManager: applicationRegistrationEvent.KeyManager, TenantID: -1,
			TenantDomain: applicationRegistrationEvent.TenantDomain, TimeStamp: applicationRegistrationEvent.TimeStamp}

		AppKeyMappingList = append(AppKeyMappingList, applicationKeyMapping)
		// EventType.APPLICATION_REGISTRATION_CREATE, EventType.REMOVE_APPLICATION_KEYMAPPING
	} else {
		var applicationEvent ApplicationEvent
		json.Unmarshal([]byte(string(data)), &applicationEvent)
		logger.LoggerJMS.Infof("EventType: %s for application: %s", applicationEvent.Type, applicationEvent.ApplicationName)

		application := resourcetypes.Application{UUID: applicationEvent.UUID, ID: applicationEvent.ApplicationID,
			Name: applicationEvent.ApplicationName, SubName: applicationEvent.Subscriber, Policy: applicationEvent.ApplicationPolicy,
			TokenType: applicationEvent.TokenType, GroupIds: nil, Attributes: nil,
			TenantID: -1, TenantDomain: applicationEvent.TenantDomain, TimeStamp: applicationEvent.TimeStamp}

		AppList = append(AppList, application)
		// EventType.APPLICATION_CREATE, EventType.APPLICATION_UPDATE, EventType.APPLICATION_DELETE
	}
}

// handleSubscriptionRelatedEvents to process
func handleSubscriptionEvents(data []byte, eventType string) {
	var subscriptionEvent SubscriptionEvent
	// SUBSCRIPTIONS_CREATE, SUBSCRIPTIONS_UPDATE, SUBSCRIPTIONS_DELETE
	json.Unmarshal([]byte(string(data)), &subscriptionEvent)
	logger.LoggerJMS.Infof("EventType: %s for subscription: %v. application %v with api %v", subscriptionEvent.Type,
		subscriptionEvent.SubscriptionID, subscriptionEvent.ApplicationID, subscriptionEvent.APIID)

	subscription := resourcetypes.Subscription{SubscriptionID: subscriptionEvent.SubscriptionID, PolicyID: subscriptionEvent.PolicyID,
		APIID: subscriptionEvent.APIID, AppID: subscriptionEvent.ApplicationID, SubscriptionState: subscriptionEvent.SubscriptionState,
		TenantID: -1, TenantDomain: subscriptionEvent.TenantID, TimeStamp: subscriptionEvent.TimeStamp}

	SubList = append(SubList, subscription)
}

// handleScopeRelatedEvents to process
func handleScopeEvents(data []byte, eventType string) {
	var scopeEvent ScopeEvent
	json.Unmarshal([]byte(string(data)), &scopeEvent)

	logger.LoggerJMS.Infof("EventType: %s for scope: %s", scopeEvent.Type, scopeEvent.DisplayName)

	scope := resourcetypes.Scope{Name: scopeEvent.Name, DisplayName: scopeEvent.DisplayName, ApplicationName: scopeEvent.ApplicationName}
	ScopeList = append(ScopeList, scope)
	// EventType.SCOPE_CREATE, EventType.SCOPE_UPDATE, EventType.SCOPE_DELETE
}

// handlePolicyRelatedEvents to process
func handlePolicyEvents(data []byte, eventType string) {
	var policyEvent PolicyInfo
	json.Unmarshal([]byte(string(data)), &policyEvent)
	if strings.EqualFold(eventType, "POLICY_CREATE") {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, "POLICY_UPDATE") {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	} else if strings.EqualFold(eventType, "POLICY_DELETE") {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
	}

	type PolicyInfo struct {
		PolicyID   int32  `json:"policyId"`
		PolicyName string `json:"policyName"`
		QuotaType  string `json:"quotaType"`
		PolicyType string `json:"policyType"`
		Event
	}

	// ApplicationPolicy for struct ApplicationPolicy
	type ApplicationPolicy struct {
		ID        int    `json:"id"`
		TenantID  int    `json:"tenantId"`
		Name      string `json:"name"`
		QuotaType string `json:"quotaType"`
	}

	if strings.EqualFold(apiEventType, policyEvent.PolicyType) {
		var apiPolicyEvent APIPolicyEvent
		json.Unmarshal([]byte(string(data)), &apiPolicyEvent)
	} else if strings.EqualFold(applicationEventType, policyEvent.PolicyType) {
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s", policyEvent.PolicyName, policyEvent.PolicyType)
		applicationPolicy := resourcetypes.ApplicationPolicy{ID: policyEvent.PolicyID, TenantID: -1, Name: policyEvent.PolicyName,
			QuotaType: policyEvent.QuotaType}
		AppPolicyList = append(AppPolicyList, applicationPolicy)

	} else if strings.EqualFold(subscriptionEventType, policyEvent.PolicyType) {
		var subscriptionPolicyEvent SubscriptionPolicyEvent
		json.Unmarshal([]byte(string(data)), &subscriptionPolicyEvent)
		logger.LoggerJMS.Infof("Policy: %s for policy type: %s , rateLimitCount : %v, QuotaType: %s ",
			subscriptionPolicyEvent.PolicyName, subscriptionPolicyEvent.PolicyType, subscriptionPolicyEvent.RateLimitCount,
			subscriptionPolicyEvent.QuotaType)

		subscriptionPolicy := resourcetypes.SubscriptionPolicy{ID: subscriptionPolicyEvent.PolicyID, TenantID: -1,
			Name: subscriptionPolicyEvent.PolicyName, QuotaType: subscriptionPolicyEvent.QuotaType,
			GraphQLMaxComplexity: subscriptionPolicyEvent.GraphQLMaxComplexity,
			GraphQLMaxDepth:      subscriptionPolicyEvent.GraphQLMaxDepth, RateLimitCount: subscriptionPolicyEvent.RateLimitCount,
			RateLimitTimeUnit: subscriptionPolicyEvent.RateLimitTimeUnit, StopOnQuotaReach: subscriptionPolicyEvent.StopOnQuotaReach,
			TenantDomain: subscriptionPolicyEvent.TenantDomain, TimeStamp: subscriptionPolicyEvent.TimeStamp}

		SubPolicyList = append(SubPolicyList, subscriptionPolicy)
	}
	// EventType.POLICY_CREATE, EventType.POLICY_UPDATE, EventType.POLICY_DELETE, API, APPLICATION, SUBSCRIPTION
}
