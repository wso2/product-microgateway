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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

func TestRemoveApplication(t *testing.T) {
	application1 := types.Application{UUID: "123e4567-e89b-42d3-a456-556642440000", ID: 1,
		Name: "app1"}
	application2 := types.Application{UUID: "123e4567-e89b-42d3-a456-556642440001", ID: 2,
		Name: "app2"}
	application3 := types.Application{UUID: "123e4567-e89b-42d3-a456-556642440003", ID: 3,
		Name: "app3"}
	var appArray []types.Application
	appArray = append(appArray, application1)
	appArray = append(appArray, application2)
	appArray = append(appArray, application3)

	appArray = removeApplication(appArray, 1)
	assert.Len(t, appArray, 2)
	appArray = removeApplication(appArray, 1)
	assert.Len(t, appArray, 2)
	assert.Equal(t, application3.ID, appArray[0].ID)
	assert.Equal(t, application2.ID, appArray[1].ID)
}

func TestRemoveSubscription(t *testing.T) {
	sub1 := types.Subscription{SubscriptionID: 1, SubscriptionUUID: "123e4567-e89b-42d3-a456-556642440001"}
	sub2 := types.Subscription{SubscriptionID: 2, SubscriptionUUID: "123e4567-e89b-42d3-a456-556642440002"}
	sub3 := types.Subscription{SubscriptionID: 2, SubscriptionUUID: "123e4567-e89b-42d3-a456-556642440003"}
	var subArray []types.Subscription
	subArray = append(subArray, sub1)
	subArray = append(subArray, sub2)
	subArray = append(subArray, sub3)

	subArray = removeSubscription(subArray, 1)
	assert.Len(t, subArray, 2)
	subArray = removeSubscription(subArray, 1)
	assert.Len(t, subArray, 2)
	assert.Equal(t, sub3.SubscriptionID, subArray[0].SubscriptionID)
	assert.Equal(t, sub2.SubscriptionID, subArray[1].SubscriptionID)
}

func TestRemoveSubPolicy(t *testing.T) {
	policy1 := types.SubscriptionPolicy{ID: 1}
	policy2 := types.SubscriptionPolicy{ID: 2}
	policy3 := types.SubscriptionPolicy{ID: 3}

	var policyArr []types.SubscriptionPolicy
	policyArr = append(policyArr, policy1)
	policyArr = append(policyArr, policy2)
	policyArr = append(policyArr, policy3)

	policyArr = removeSubPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	policyArr = removeSubPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	assert.Equal(t, policy3.ID, policyArr[0].ID)
	assert.Equal(t, policy2.ID, policyArr[1].ID)
}

func TestRemoveAppPolicy(t *testing.T) {
	policy1 := types.ApplicationPolicy{ID: 1}
	policy2 := types.ApplicationPolicy{ID: 2}
	policy3 := types.ApplicationPolicy{ID: 3}

	var policyArr []types.ApplicationPolicy
	policyArr = append(policyArr, policy1)
	policyArr = append(policyArr, policy2)
	policyArr = append(policyArr, policy3)

	policyArr = removeAppPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	policyArr = removeAppPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	assert.Equal(t, policy3.ID, policyArr[0].ID)
	assert.Equal(t, policy2.ID, policyArr[1].ID)
}

func TestTokenRevocationChannelSubscriptionAndEventFormat(t *testing.T) {
	logger.LoggerInternalMsg.Infof("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] starting test " +
		"TestTokenRevocationChannelSubscriptionAndEventFormat")

	sampleTestEvent := "{\"event\":{\"payloadData\":{\"eventId\":\"444d2f9b-57d8-4245-bef2-3f8d824741c3\"," +
		"\"revokedToken\":\"fc8ee897-b3d9-3bb6-a9ca-f4aeb036e5c0\",\"ttl\":\"5000\",\"expiryTime\":1628175421481," +
			"\"type\":\"Default\",\"tenantId\":-1234}}}"
	var parsedSuccessfully bool
	var notification msg.EventTokenRevocationNotification
	go func() {
		msg.AzureRevokedTokenChannel <- []byte(sampleTestEvent)
	}()
	outputData := <- msg.AzureRevokedTokenChannel
	logger.LoggerInternalMsg.Infof("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Event %s is received from channel", outputData)
	error := parseRevokedTokenJSONEvent(outputData, &notification)
	if error != nil {
		logger.LoggerInternalMsg.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred", error)
	} else {
		parsedSuccessfully = true
		logger.LoggerInternalMsg.Infof("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Event %s is received",
			notification.Event.PayloadData.Type)
		logger.LoggerInternalMsg.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Revoked token value is ",
			notification.Event.PayloadData.RevokedToken)
	}
	assert.Equal(t, true, parsedSuccessfully)
}

func TestNotificationChannelSubscriptionAndEventFormat(t *testing.T) {
	logger.LoggerInternalMsg.Infof("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] starting test " +
		"TestNotificationChannelSubscriptionAndEventFormat")

	sampleTestEvent := "{\"event\":{\"payloadData\":{\"eventType\":\"API_CREATE\",\"timestamp\":1628490908147," +
		"\"event\":\"eyJhcGlOYW1lIjoiTXlBUEkiLCJhcGlJZCI6MiwidXVpZCI6Ijc4MDhhZjg0LTZiOWEtNGM4Ni05NTNhL" +
			"TRmNDBmMTU3NjcxZiIsImFwaVZlcnNpb24iOiJ2MSIsImFwaUNvbnRleHQiOiIvbXlhcGkvdjEiLCJhcGlQcm92aWRlc" +
				"iI6ImFkbWluIiwiYXBpVHlwZSI6IkhUVFAiLCJhcGlTdGF0dXMiOiJDUkVBVEVEIiwiZXZlbnRJZCI6IjE0NjY3Mz" +
					"A0LTIzZGQtNGI5Zi04YzM5LWExMTAzZDA2ZDA1OCIsInRpbWVTdGFtcCI6MTYyODQ5MDkwODE0NywidHlwZSI" +
						"6IkFQSV9DUkVBVEUiLCJ0ZW5hbnRJZCI6LTEyMzQsInRlbmFudERvbWFpbiI6ImNhcmJvbi5zdXBlciJ9\"}}}"

	var parsedSuccessfully bool
	var notification msg.EventNotification
	go func() {
		msg.AzureNotificationChannel <- []byte(sampleTestEvent)
	}()
	outputData := <- msg.AzureNotificationChannel
	logger.LoggerInternalMsg.Infof("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Event %s is received from channel", outputData)
	error := parseNotificationJSONEvent(outputData, &notification)
	if error != nil {
		logger.LoggerInternalMsg.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred", error)
	} else {
		parsedSuccessfully = true
		logger.LoggerInternalMsg.Infof("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Event %s is received",
			notification.Event.PayloadData.EventType)
	}
	assert.Equal(t, true, parsedSuccessfully)
}


