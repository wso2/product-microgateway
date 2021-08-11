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
	"time"

	servicebus "github.com/Azure/azure-service-bus-go"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

const (
	componentName                string = "adapter"
	subscriptionIdleTimeDuration        = time.Duration(72 * time.Hour)
)

// InitiateAndProcessEvents to pass event consumption
func InitiateAndProcessEvents(config *config.Config) {
	var err error
	var namespace *servicebus.Namespace
	var reconnectRetryCount = config.ControlPlane.ASBConnectionParameters.ReconnectRetryCount
	var reconnectInterval = config.ControlPlane.ASBConnectionParameters.ReconnectInterval
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Starting InitiateAndProcessEvents method")
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] EventListeningEndpoint is ",
		config.ControlPlane.ASBConnectionParameters.EventListeningEndpoint)
	if reconnectRetryCount == 0 {
		reconnectRetryCount = 10
	}
	if reconnectInterval == 0 {
		reconnectInterval = 30
	}
	namespace, availableTopicList, err := msg.InitiateBrokerConnectionAndGetAvailableTopics(
		config.ControlPlane.ASBConnectionParameters.EventListeningEndpoint, reconnectRetryCount, reconnectInterval*time.Second)
	health.SetControlPlaneBrokerStatus(err == nil)
	if err == nil {
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Initiated broker connection successfully ")
		msg.InitiateConsumers(namespace, availableTopicList, componentName, subscriptionIdleTimeDuration,
			reconnectRetryCount, reconnectInterval*time.Second)
		go handleAzureNotification()
		go handleAzureTokenRevocation()
	}

}
