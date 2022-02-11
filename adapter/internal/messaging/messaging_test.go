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

package messaging

import (
	"github.com/stretchr/testify/assert"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
	"testing"
)

func TestNotificationChannelSubscriptionAndEventFormat(t *testing.T) {
	logger.LoggerMgw.Infof("Starting test TestNotificationChannelSubscriptionAndEventFormat")

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
	outputData := <-msg.AzureNotificationChannel
	error := parseNotificationJSONEvent(outputData, &notification)
	if error != nil {
		logger.LoggerMgw.Info("Error occurred", error)
	} else {
		parsedSuccessfully = true
	}
	assert.Equal(t, true, parsedSuccessfully)
}

func TestTokenRevocationChannelSubscriptionAndEventFormat(t *testing.T) {
	logger.LoggerMgw.Infof("Starting test TestTokenRevocationChannelSubscriptionAndEventFormat")

	sampleTestEvent := "{\"event\":{\"payloadData\":{\"eventId\":\"444d2f9b-57d8-4245-bef2-3f8d824741c3\"," +
		"\"revokedToken\":\"fc8ee897-b3d9-3bb6-a9ca-f4aeb036e5c0\",\"ttl\":\"5000\",\"expiryTime\":1628175421481," +
		"\"type\":\"Default\",\"tenantId\":-1234}}}"
	var parsedSuccessfully bool
	var notification msg.EventTokenRevocationNotification
	go func() {
		msg.AzureRevokedTokenChannel <- []byte(sampleTestEvent)
	}()
	outputData := <-msg.AzureRevokedTokenChannel
	error := parseRevokedTokenJSONEvent(outputData, &notification)
	if error != nil {
		logger.LoggerMgw.Info("Error occurred", error)
	} else {
		parsedSuccessfully = true
	}
	assert.Equal(t, true, parsedSuccessfully)
}
