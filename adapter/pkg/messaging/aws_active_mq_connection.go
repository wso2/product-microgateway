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
)

// InitAwsActiveMqConnection initializes a connection to the AWS ActiveMQ broker
func InitAwsActiveMqConnection(ctx context.Context, connectionString string, userName string, password string, idleTimeoutDuration time.Duration) (*amqp.Conn, error) {
	con, err := amqp.Dial(
		ctx,
		connectionString,
		&amqp.ConnOptions{
			SASLType: amqp.SASLTypePlain(
				userName,
				password),
			IdleTimeout: idleTimeoutDuration,
		},
	)
	if err != nil {
		logger.LoggerMgw.Infof("Failed to connect to AWS ActiveMQ broker: %s" + err.Error())
		return nil, err
	}
	return con, nil
}

// InitAwsActiveMqReceiverAndValidate initializes a receiver for the specified topic on the AWS ActiveMQ broker
func InitAwsActiveMqReceiverAndValidate(connectionString string, topicName string, con *amqp.Conn, maximumallowedUnacknowledgedMessages int) (*amqp.Receiver, error) {
	session, sessionErr := con.NewSession(context.Background(), nil)
	if sessionErr != nil {
		logger.LoggerMgw.Errorf("Failed to create session for topic: %s error:%s", topicName, sessionErr.Error())
		return nil, sessionErr
	}
	receiver, receiverErr := session.NewReceiver(
		context.Background(),
		"topic://"+topicName,
		&amqp.ReceiverOptions{
			Credit: int32(maximumallowedUnacknowledgedMessages),
		},
	)
	if receiverErr != nil {
		logger.LoggerMgw.Errorf("Failed to create receiver for topic: %s error:%s", topicName, receiverErr.Error())
		return nil, receiverErr
	}
	return receiver, nil
}
