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
	"context"
	"strings"
	"time"

	servicebus "github.com/Azure/azure-service-bus-go"
	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

func startBrokerConsumer(topicName string, ns *servicebus.Namespace,
	availableTopicList []*servicebus.TopicEntity, componentName string, reconnectRetryCount int,
		reconnectInterval time.Duration, opts ...servicebus.SubscriptionManagementOption) {

	var topicExistForFurtherProcess bool
	var subscriptionCreatedForFurtherProcess bool
	parentContext := context.Background()
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Starting broker consumer for topic name : " +
		topicName)

	uniqueID := uuid.New()
	//In Azure service bus subscription names can contain letters, numbers, periods (.), hyphens (-), and
	// underscores (_), up to 50 characters. Subscription names are also case-insensitive.
	var subscriptionName = componentName + "_" + uniqueID.String() + "_sub"

	if !isTopicExist(topicName, availableTopicList) {
		//create the topic
		topicManager := ns.NewTopicManager()
		for j := 0; j < reconnectRetryCount; j++ {
			ctx, cancel := context.WithCancel(parentContext)
			defer cancel()
			_, err := topicManager.Put(ctx, topicName)
			if err != nil {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create "+
					"topic %s in azure service bus :%v. Retrying after %d seconds",
						topicName, err, reconnectInterval)
				time.Sleep(reconnectInterval)
			} else {
				logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " created")
				topicExistForFurtherProcess = true
				break
			}
		}
		if !topicExistForFurtherProcess {
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not create topic %s " +
				" in azure service bus after %d retry attempts ", topicName, reconnectRetryCount)
		}
	} else {
		topicExistForFurtherProcess = true
	}

	if topicExistForFurtherProcess {
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " exist. Can proceed")
		var subManager *servicebus.SubscriptionManager
		var subManagerError error
		var subManagerExistForFurtherProcess bool

		for j := 0; j < reconnectRetryCount; j++ {
			subManager, subManagerError = ns.NewSubscriptionManager(topicName)
			if subManagerError != nil {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get subscription"+
					" manager from azure service bus for topic name %s:%v. Retrying after %d seconds",
					topicName, subManagerError, reconnectInterval)
				time.Sleep(reconnectInterval)
			} else {
				subManagerExistForFurtherProcess = true
				break
			}
		}
		if !subManagerExistForFurtherProcess {
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not create get subscription manager " +
				"from azure service bus after %d retry attempts ", reconnectRetryCount)
		}

		if subManagerExistForFurtherProcess {
			//We are creating a unique subscription for each adapter starts. Unused subscriptions will be deleted after
			// idle for three days
			for j := 0; j < reconnectRetryCount; j++ {
				ctx, cancel := context.WithCancel(parentContext)
				defer cancel()
				_, err := subManager.Put(ctx, subscriptionName, opts...)
				if err != nil {
					logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create "+
						"subscription %s in azure service bus for topic name %s:%v. " +
							"Retrying after %d seconds", subscriptionName, topicName, err, reconnectInterval)
					time.Sleep(reconnectInterval)
				} else {
					logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription " + subscriptionName + " created")
					subscriptionCreatedForFurtherProcess = true
					break
				}
			}
			if !subscriptionCreatedForFurtherProcess {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not create get subscription " +
					subscriptionName + " in azure service bus for topic name %s after %d retry " +
						"attempts ", topicName, reconnectRetryCount)
			}
		}
		if subscriptionCreatedForFurtherProcess {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] subscription " + subscriptionName +
				" exist. Can proceed")
			dataChannel := make(chan []byte)
			if strings.EqualFold(topicName, notification) {
				dataChannel = AzureNotificationChannel
			} else if strings.EqualFold(topicName, tokenRevocation) {
				dataChannel = AzureRevokedTokenChannel
			}

			for j := 0; j < reconnectRetryCount; j++ {
				//topic subscription client creation
				topicSubscriptionClient, err := subManager.Topic.NewSubscription(subscriptionName)
				if err != nil {
					//This will not throw any connection errors hence no need to retry for this error
					logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create "+
						"topic subscription client for %s from azure service bus for topic name %s:%v.",
							subscriptionName, topicName, err)
				}
				logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Starting to receive messages for " +
					"subscriptionName  " + subscriptionName + " from azure service bus for topic name " + topicName)
				ctx, cancel := context.WithCancel(parentContext)
				defer cancel()
				err = topicSubscriptionClient.Receive(ctx, servicebus.HandlerFunc(func(ctx context.Context,
					message *servicebus.Message) error {
					j = 0
					dataChannel <- message.Data
					return message.Complete(ctx)
				}))

				if err != nil {
					logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while receiving "+
						"events from subscription %s from azure service bus for topic name %s:%v. " +
							"Retrying after %d seconds", subscriptionName, topicName, err, reconnectInterval)
					time.Sleep(reconnectInterval)
				}
			}
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not recieve events for subscription " +
				subscriptionName + " in azure service bus for topic name %s after %d retry attempts " +
					"within %d seconds", topicName, reconnectRetryCount, time.Duration(reconnectRetryCount) * reconnectInterval)
		}
	}
}

func isTopicExist(topicName string, availableTopicList []*servicebus.TopicEntity) bool {
	for _, topic := range availableTopicList {
		if strings.EqualFold(topic.Name, topicName) {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " Exist ")
			return true
		}
	}
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " does not Exist ")
	return false
}
