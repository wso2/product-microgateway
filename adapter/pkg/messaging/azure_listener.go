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
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

func startBrokerConsumer(subscriptionMetaData Subscription, reconnectInterval time.Duration) {
	var topicName = subscriptionMetaData.topicName
	var subscriptionName = subscriptionMetaData.subscriptionName

	dataChannel := make(chan []byte)
	if strings.EqualFold(topicName, notification) {
		dataChannel = AzureNotificationChannel
	} else if strings.EqualFold(topicName, tokenRevocation) {
		dataChannel = AzureRevokedTokenChannel
	} else if strings.EqualFold(topicName, stepQuotaThreshold) {
		dataChannel = AzureStepQuotaThresholdChannel
	} else if strings.EqualFold(topicName, stepQuotaReset) {
		dataChannel = AzureStepQuotaResetChannel
	} else if strings.EqualFold(topicName, organizationPurge) {
		dataChannel = AzureOrganizationPurgeChannel
	}
	parentContext := context.Background()

	for {
		//topic subscription client creation
		topicSubscriptionClient, err := subscriptionMetaData.subscriptionManager.Topic.NewSubscription(subscriptionName)
		if err != nil {
			logger.LoggerMsg.Errorf("Error occurred while trying to create "+
				"topic subscription client for %s from azure service bus for topic name %s:%v.",
				subscriptionName, topicName, err)
		}
		logger.LoggerMsg.Info("Starting the consumer for subscriptionName " + subscriptionName +
			" from azure service bus for topic name " + topicName)
		func() {
			ctx, cancel := context.WithCancel(parentContext)
			defer cancel()
			err = topicSubscriptionClient.Receive(ctx, servicebus.HandlerFunc(func(ctx context.Context,
				message *servicebus.Message) error {
				logger.LoggerMsg.Debugf("Message %s from ASB waits to be processed.", message.ID)
				dataChannel <- message.Data
				logger.LoggerMsg.Debugf("Message %s from ASB is complete", message.ID)
				return message.Complete(ctx)
			}))
		}()
		if err != nil {
			logger.LoggerMsg.Errorf("Error occurred while listening to subscription %s from azure "+
				"service bus for topic name %s:%v. Hence retrying in %s", subscriptionName, topicName, err, reconnectInterval)
			time.Sleep(reconnectInterval)
		}
	}
}
