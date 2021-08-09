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
	"strings"
)


func startBrokerConsumer(topicName string, ns *servicebus.Namespace,
	availableTopicList []*servicebus.TopicEntity, componentName string) {

	var topicExistForFurtherProcess bool
	var subscriptionExistForFurtherProcess bool
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Starting broker consumer for topic name : " +
		topicName)

	//In Azure service bus subscription names can contain letters, numbers, periods (.), hyphens (-), and
	// underscores (_), up to 50 characters. Subscription names are also case-insensitive.
	var subscriptionName = topicName + "_" + componentName + "_sub"

	if (!isTopicExist(topicName, availableTopicList)) {
		//create the topic
		topicManager := ns.NewTopicManager()
		ctx := context.Background()
		ctx, cancel := context.WithCancel(ctx)
		defer cancel()

		_, err := topicManager.Put(ctx, topicName)
		if err != nil {
			logger.LoggerMgw.Error("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create " +
				"topic " + topicName + " from azure service bus :%v", err)
		} else {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " +
				topicName + "created")
			topicExistForFurtherProcess = true
		}
	} else {
		topicExistForFurtherProcess = true
	}

	if topicExistForFurtherProcess {
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " exist. Can proceed")
		subManager, err := ns.NewSubscriptionManager(topicName)

		if err != nil {
			logger.LoggerMgw.Error("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get subscription" +
				" manager from azure service bus for topic name " + topicName + ":%v", err)
		}

		ctx := context.Background()
		ctx, cancel := context.WithCancel(ctx)
		defer cancel()

		availableSubscriptionList, err := subManager.List(ctx)

		if err != nil {
			logger.LoggerMgw.Error("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get available " +
				"subscriptions from azure service bus for " + "topic name " + topicName + ":%v", err)
		}

		if isSubscriptionExist(subscriptionName, availableSubscriptionList) {
			logger.LoggerMgw.Debugf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription " + subscriptionName +
				"exist in the system")
			subscriptionExistForFurtherProcess = true
		} else {
			ctx := context.Background()
			ctx, cancel := context.WithCancel(ctx)
			defer cancel()
			_, err = subManager.Put(ctx, subscriptionName)

			if err != nil {
				logger.LoggerMgw.Error("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create " +
					"subscription " + subscriptionName + " from azure service bus for topic name " +
						topicName + ":%v", err)
			} else {
				logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription " +
					subscriptionName + "created")
				subscriptionExistForFurtherProcess = true
			}
		}

		if subscriptionExistForFurtherProcess {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] subscription " + subscriptionName +
				" exist. Can proceed")
			//topic subscription client creation
			topicSubscriptionClient, err := subManager.Topic.NewSubscription(subscriptionName)

			if err != nil {
				logger.LoggerMgw.Error("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create " +
					"topic subscription client for  " + subscriptionName + " from azure service bus for topic name " +
						topicName + ":%v", err)
			} else {
				dataChannel := make(chan []byte)
				ackChannel := make(chan bool)
				if strings.EqualFold(topicName, notification) {
					dataChannel = AzureNotificationChannel
					ackChannel = AzureNotificationAck
				} else if strings.EqualFold(topicName, tokenRevocation) {
					dataChannel = AzureRevokedTokenChannel
					ackChannel = AzureRevokedTokenAck
				}

				logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Starting to receive messages for " +
					"subscriptionName  " + subscriptionName + " from azure service bus for topic name " + topicName)

				err = topicSubscriptionClient.Receive(ctx, servicebus.HandlerFunc(func(ctx context.Context,
					message *servicebus.Message) error {
					dataChannel <- message.Data
					<-ackChannel
					logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Received ACK for " +
						"subscriptionName  " + subscriptionName + " and going to do ACK service bus now")
					return message.Complete(ctx)
				}))
			}
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

func isSubscriptionExist(subscriptionName string, subscriptionList []*servicebus.SubscriptionEntity) bool {
	for _, subscription := range subscriptionList {
		if strings.EqualFold(subscription.Name, subscriptionName) {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] subscription " + subscriptionName + " Exist ")
			return true
		}
	}
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] subscription " + subscriptionName +
		" does not Exist ")
	return false
}

