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
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	servicebus "github.com/Azure/azure-service-bus-go"
	"context"
	"time"
)

var (
	// AzureRevokedTokenChannel stores the revoked token events
	AzureRevokedTokenChannel chan []byte
	// AzureNotificationChannel stores the notification events
	AzureNotificationChannel chan []byte
)

func init() {
	AzureRevokedTokenChannel = make(chan []byte)
	AzureNotificationChannel = make(chan []byte)
}

// InitiateBrokerConnectionAndGetAvailableTopics to initiate connection and get topic list
func InitiateBrokerConnectionAndGetAvailableTopics(eventListeningEndpoint string) (*servicebus.Namespace,
	[]*servicebus.TopicEntity, error) {
	var err error
	var availableTopics []*servicebus.TopicEntity
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Trying to connect to azure service bus with " +
		"connection string " + eventListeningEndpoint)
	namespace, err := servicebus.NewNamespace(servicebus.NamespaceWithConnectionString(eventListeningEndpoint))

	if err == nil {
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Successfully received namespace ")
		topicManager := namespace.NewTopicManager()
		ctx, cancel := context.WithCancel(context.Background())
		defer cancel()
		availableTopics, err = topicManager.List(ctx)
		//TODO (dnwick) (add retry logic)
		if err != nil {
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to get topic " +
				"list from azure service bus :%v", err)
		}
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Successfully received topic list ")
	} else {
		logger.LoggerMgw.Error("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get the namespace " +
			"in azure service bus using the connection url " + eventListeningEndpoint + ":%v", err)
	}
	return namespace, availableTopics, err
}

// InitiateConsumers to pass event consumption
func InitiateConsumers(ns *servicebus.Namespace, availableTopicList []*servicebus.TopicEntity, componentName string,
	subscriptionIdleTimeDuration time.Duration) {
	bindingKeys := []string {tokenRevocation, notification}

	for _, key := range bindingKeys {
		go func(key string) {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] starting the consumer for key : " + key)
			startBrokerConsumer(key, ns, availableTopicList, componentName,
				servicebus.SubscriptionWithAutoDeleteOnIdle(&subscriptionIdleTimeDuration))
			select {}
		}(key)
	}
}
