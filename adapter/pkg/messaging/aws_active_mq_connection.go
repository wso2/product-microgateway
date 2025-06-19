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
	"time"

	"github.com/Azure/go-amqp"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/config"
)

// InitAwsActiveMqConnection initializes a connection to the AWS ActiveMQ broker
func InitAwsActiveMqConnection(ctx context.Context, brokerConnectionParameters config.BrokerConnectionParameters, topic string) (*amqp.Conn, error) {
	connectionString := brokerConnectionParameters.EventListeningEndpoints[0]
	username := brokerConnectionParameters.ActiveMqUsername
	password := brokerConnectionParameters.ActiveMqPassword
	idleTimeoutDuration := brokerConnectionParameters.ActiveMqIdleTimeoutDurationInSeconds
	reconnectRetryCount := brokerConnectionParameters.ReconnectRetryCount
	retryInterval := brokerConnectionParameters.ReconnectInterval

	con, err := dialForActiveMqConnection(ctx, connectionString, username, password, idleTimeoutDuration)
	if err != nil {
		return retryActiveMqConnection(ctx, connectionString, username, password, idleTimeoutDuration, reconnectRetryCount, topic, int(retryInterval))
	}
	return con, nil
}

func dialForActiveMqConnection(ctx context.Context, connectionString string, userName string, password string, idleTimeoutDuration time.Duration) (*amqp.Conn, error) {
	return amqp.Dial(
		ctx,
		connectionString,
		&amqp.ConnOptions{
			SASLType: amqp.SASLTypePlain(
				userName,
				password),
			IdleTimeout: idleTimeoutDuration * time.Second,
		},
	)
}

func retryActiveMqConnection(ctx context.Context, connectionString string, userName string, password string, idleTimeoutDuration time.Duration, retryCount int, topic string, retryInterval int) (*amqp.Conn, error) {
	var con *amqp.Conn
	var err error
	logger.LoggerMgw.Infof("Retrying to connect to AWS ActiveMQ broker for topic: %s, retry count: %d", topic, retryCount)
	for i := 0; i < retryCount; i++ {
		con, err = dialForActiveMqConnection(ctx, connectionString, userName, password, idleTimeoutDuration)
		if err == nil {
			logger.LoggerMgw.Infof("Successfully connected to AWS ActiveMQ broker for topic: %s on retry %d", topic, i+1)
			return con, nil
		}
		logger.LoggerMgw.Errorf("Retry %d failed to connect to AWS ActiveMQ broker for topic: %s error:%s", i+1, topic, err.Error())
		time.Sleep(time.Duration(retryInterval * int(time.Millisecond)))
	}
	logger.LoggerMgw.Errorf("Failed to connect to AWS ActiveMQ broker for topic: %s even after retries. Error:%s", topic, err.Error())
	return nil, err
}

// InitAwsActiveMqReceiverAndValidate initializes a receiver for the specified topic on the AWS ActiveMQ broker
func InitAwsActiveMqReceiverAndValidate(ctx context.Context, topicName string, con *amqp.Conn, maximumallowedUnacknowledgedMessages int) (*amqp.Receiver, error) {
	session, sessionErr := con.NewSession(ctx, nil)
	if sessionErr != nil {
		logger.LoggerMgw.Errorf("Failed to create session for topic: %s error:%s", topicName, sessionErr.Error())
		return nil, sessionErr
	}
	receiver, receiverErr := session.NewReceiver(
		ctx,
		"topic://"+topicName,
		&amqp.ReceiverOptions{
			Credit: int32(maximumallowedUnacknowledgedMessages),
		},
	)
	if receiverErr != nil {
		logger.LoggerMgw.Errorf("Failed to create receiver for topic: %s error:%s", topicName, receiverErr.Error())
		session.Close(ctx)
		return nil, receiverErr
	}
	return receiver, nil
}
