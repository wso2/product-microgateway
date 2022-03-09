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

	asb "github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus"
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

func startBrokerConsumer(connectionString string, sub Subscription, reconnectInterval time.Duration) {
	var topic = sub.topicName
	var subName = sub.subscriptionName

	dataChannel := make(chan []byte)
	if strings.EqualFold(topic, notification) {
		dataChannel = AzureNotificationChannel
	} else if strings.EqualFold(topic, tokenRevocation) {
		dataChannel = AzureRevokedTokenChannel
	} else if strings.EqualFold(topic, stepQuotaThreshold) {
		dataChannel = AzureStepQuotaThresholdChannel
	} else if strings.EqualFold(topic, stepQuotaReset) {
		dataChannel = AzureStepQuotaResetChannel
	} else if strings.EqualFold(topic, organizationPurge) {
		dataChannel = AzureOrganizationPurgeChannel
	}
	parentContext := context.Background()

	for {
		// initializing the receiver client
		subClient, err := asb.NewClientFromConnectionString(connectionString, nil)
		if err != nil {
			logger.LoggerMsg.Errorf("Failed to create ASB client for %s , topic:  %s. error: %v.",
				subName, topic, err)
			continue
		}
		receiver, err := subClient.NewReceiverForSubscription(topic, subName, nil)
		if err != nil {
			logger.LoggerMsg.Errorf("Failed to create ASB receiver for %s , topic:  %s. error: %v.",
				subName, topic, err)
			continue
		}

		logger.LoggerMsg.Infof("Starting the ASB consumer for subscription: %s, topic: %s", subName, topic)
		func() {
			defer logger.LoggerMsg.Errorf("ASB consumer has stopped for subscription: %q, topic: %q", subName, topic)
			ctx, cancel := context.WithCancel(parentContext)
			defer cancel()

			// keep receiving messages from asb
			for {
				logger.LoggerMsg.Debugf("Continue processing messages from ASB for subscription: %q, topic: %q", subName, topic)
				messages, err := receiver.ReceiveMessages(ctx, 10, nil)
				if err != nil {
					logger.LoggerMsg.Errorf("Failed to receive messages from ASB. %v", err)
					time.Sleep(reconnectInterval)
					continue
				}
				for _, message := range messages {
					body, err := message.Body()
					if err != nil {
						logger.LoggerMsg.Errorf("Failed to parse the ASB message. %v", err)
					}

					logger.LoggerMsg.Debugf("Message %s from ASB is waiting to be processed.", message.MessageID)
					dataChannel <- body
					logger.LoggerMsg.Debugf("Message %s from ASB is processed", message.MessageID)

					err = receiver.CompleteMessage(ctx, message)
					if err != nil {
						logger.LoggerMsg.Warnf("Failed to complete the ASB message. %v", err)
					}
				}
				logger.LoggerMsg.Debugf("Processed %d messages from ASB for subscription: %q, topic: %q", len(messages), subName, topic)
			}
		}()
	}
}
