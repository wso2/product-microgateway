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
)

func handleAwsActiveTopic(parentContext context.Context, receiver *amqp.Receiver, topicName string, messageHandler MessageHandlerFunc) {
	ctx, stop := signal.NotifyContext(parentContext, syscall.SIGINT, syscall.SIGTERM)
	conf, _ := config.ReadConfigs()
	defer stop()
	defer receiver.Close(ctx)
	for {
		select {
		case <-ctx.Done():
			logger.LoggerMsg.Info("Shutting down AWS ActiveMQ receiver for topic: " + topicName)
			return
		default:
			receiveContext, cancel := context.WithCancel(ctx)
			msg, err := receiver.Receive(receiveContext, nil)
			cancel()
			if err != nil {
				logger.LoggerMsg.Errorf("Failed to receive message from AWS ActiveMQ for topic '%s': %v", topicName, err)
				time.Sleep(conf.ControlPlane.BrokerConnectionParameters.ReconnectInterval)
				continue
			}

			// Process the message
			logger.LoggerMsg.Debugf("Received message from AWS ActiveMQ topic '%s': %s", topicName, msg.Value)
			var body string
			if err != nil {
				logger.LoggerMsg.Errorf("Failed to acknowledge message from AWS ActiveMQ topic '%s': %v", topicName, err)
			}
			body, ok := msg.Value.(string)
			if !ok {
				rejectMsgContext, cancel := context.WithCancel(ctx)
				receiver.RejectMessage(rejectMsgContext, msg, nil)
				logger.LoggerMsg.Errorf("Received message cannot be processed considering the topic '%s'", topicName)
				cancel()
				continue
			}
			logger.LoggerMsg.Debugf("Handling AWS ActiveMQ notification for topic '%s': %s", topicName, body)
			err = messageHandler(body)
			if err != nil {
				logger.LoggerMsg.Errorf("Failed to handle message for topic '%s': %v", topicName, err)
				continue
			}
			// Acknowledge the message (remove from queue)
			acceptMsgContext, cancel := context.WithTimeout(ctx, 10*time.Second)
			err = receiver.AcceptMessage(acceptMsgContext, msg)
			cancel()
			if err != nil {
				logger.LoggerMsg.Errorf("Failed to acknowledge message on topic '%s': %v", topicName, err)
			}
		}
	}
}
