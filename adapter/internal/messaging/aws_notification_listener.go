/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
	"context"
	"os/signal"
	"syscall"
	"time"

	"github.com/Azure/go-amqp"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
	adapter "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

func handleAwsActiveMqNotification(receiver *amqp.Receiver, topicName string) {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	conf, _ := config.ReadConfigs()
	defer stop()
	defer receiver.Close(context.Background())
	for {
		select {
		case <-ctx.Done():
			logger.LoggerMsg.Info("Shutting down AWS ActiveMQ receiver for topic: " + topicName)
			return
		default:
			msg, err := receiver.Receive(ctx, nil)
			if err != nil {
				logger.LoggerMsg.Errorf("Failed to receive message from AWS ActiveMQ for topic '%s': %v", topicName, err)
				time.Sleep(conf.ControlPlane.BrokerConnectionParameters.ReconnectInterval)
				continue
			}

			// Process the message
			logger.LoggerMsg.Infof("Received message from AWS ActiveMQ topic '%s': %s", topicName, msg.Value)
			var body string
			// Acknowledge the message (remove from queue)
			err = receiver.AcceptMessage(ctx, msg)
			if err != nil {
				logger.LoggerMsg.Errorf("Failed to acknowledge message from AWS ActiveMQ topic '%s': %v", topicName, err)
			}
			body, ok := msg.Value.(string)
			if !ok {
				logger.LoggerMsg.Errorf("Received message value is not a string for topic '%s'", topicName)
				continue
			}
			logger.LoggerMsg.Debugf("Handling AWS ActiveMQ notification for topic '%s': %s", topicName, body)
			var notification adapter.EventNotification
			error := parseNotificationJSONEvent([]byte(body), &notification)
			if error != nil {
				logger.LoggerMsg.Errorf("Failed to parse notification message for topic '%s': %v", topicName, error)
				continue
			}
			notificationProcessError := processNotificationEvent(conf, &notification)
			if notificationProcessError != nil {
				logger.LoggerMsg.Errorf("Failed to process notification event for topic '%s': %v", topicName, notificationProcessError)
				continue
			}
		}
	}
}
