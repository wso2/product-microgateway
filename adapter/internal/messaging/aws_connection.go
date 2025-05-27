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

	"github.com/Azure/go-amqp"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
)

var awsActiveMqtopicNames = []string{tokenRevocation, notification, stepQuotaThreshold, stepQuotaReset}

// InitiateAndProcessAWSActiveMQEvents initiates AWS ActiveMQ connection and starts consuming messages from the specified topics.
func InitiateAndProcessAWSActiveMQEvents(config1 *config.Config) {
	conf, _ := config.ReadConfigs()
	for _, topic := range awsActiveMqtopicNames {
		logger.LoggerMgw.Debugf("Initiating AWS ActiveMQ connection for topic: %s", topic)
		connectionString := conf.ControlPlane.BrokerConnectionParameters.EventListeningEndpoints[0]
		ctx := context.Background()

		con, err := initAwsActiveMqConnection(ctx, connectionString)
		if err != nil {
			logger.LoggerMgw.Errorf("Failed to connect to AWS ActiveMQ broker for topic: %s error:%s", topic, err.Error())
			return
		}
		logger.LoggerMgw.Debugf("AWS ActiveMQ connection established for topic: %s", topic)

		receiver, receiverErr := initAwsActiveMqConnectionAndValidate(connectionString, topic, con)
		if receiverErr != nil {
			defer con.Close()
			logger.LoggerMgw.Errorf("Failed to a create receiver for topic: %s error:%s", topic, receiverErr.Error())
		}
		logger.LoggerMgw.Infof("Listening for messages on topic '%s'...\n", topic)
		startActiveMQTopicConsumer(topic, receiver)
	}
	health.SetControlPlaneBrokerStatus(true)
}

func initAwsActiveMqConnectionAndValidate(connectionString string, topicName string, con *amqp.Conn) (*amqp.Receiver, error) {
	conf, _ := config.ReadConfigs()
	session, sessionErr := con.NewSession(context.Background(), nil)
	if sessionErr != nil {
		logger.LoggerMgw.Errorf("Failed to create session for topic: %s error:%s", topicName, sessionErr.Error())
		return nil, sessionErr
	}
	receiver, receiverErr := session.NewReceiver(
		context.Background(),
		"topic://"+topicName,
		&amqp.ReceiverOptions{
			Credit: int32(conf.ControlPlane.BrokerConnectionParameters.MaximumAllowedUnacknowledgedMessages),
		},
	)
	if receiverErr != nil {
		logger.LoggerMgw.Errorf("Failed to create receiver for topic: %s error:%s", topicName, receiverErr.Error())
		return nil, receiverErr
	}
	return receiver, nil
}

func initAwsActiveMqConnection(ctx context.Context, connectionString string) (*amqp.Conn, error) {
	conf, _ := config.ReadConfigs()
	con, err := amqp.Dial(
		ctx,
		connectionString,
		&amqp.ConnOptions{
			SASLType: amqp.SASLTypePlain(
				conf.ControlPlane.BrokerConnectionParameters.ActiveMqUsername,
				conf.ControlPlane.BrokerConnectionParameters.ActiveMqPassword),
		},
	)
	if err != nil {
		logger.LoggerMgw.Infof("Failed to connect to AWS ActiveMQ broker: %s" + err.Error())
		return nil, err
	}
	return con, nil
}
