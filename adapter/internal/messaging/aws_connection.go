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

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

var awsActiveMqtopicNames = []string{tokenRevocation, notification, stepQuotaThreshold, stepQuotaReset}

// InitiateAndProcessAWSActiveMQEvents initiates AWS ActiveMQ connection and starts consuming messages from the specified topics.
func InitiateAndProcessAWSActiveMQEvents(conf *config.Config) {
	for _, topic := range awsActiveMqtopicNames {
		logger.LoggerMgw.Debugf("Initiating AWS ActiveMQ connection for topic: %s", topic)
		connectionString := conf.ControlPlane.BrokerConnectionParameters.EventListeningEndpoints[0]
		username := conf.ControlPlane.BrokerConnectionParameters.ActiveMqUsername
		password := conf.ControlPlane.BrokerConnectionParameters.ActiveMqPassword
		idleTimeoutDuration := conf.ControlPlane.BrokerConnectionParameters.ActiveMqIdleTimeoutDurationInSeconds
		ctx := context.Background()

		con, err := msg.InitAwsActiveMqConnection(ctx, connectionString, username, password, idleTimeoutDuration)
		if err != nil {
			logger.LoggerMgw.Errorf("Failed to connect to AWS ActiveMQ broker for topic: %s error:%s", topic, err.Error())
			return
		}
		logger.LoggerMgw.Debugf("AWS ActiveMQ connection established for topic: %s", topic)
		maximumallowedUnacknowledgedMessages := conf.ControlPlane.BrokerConnectionParameters.MaximumAllowedUnacknowledgedMessages

		receiver, receiverErr := msg.InitAwsActiveMqReceiverAndValidate(ctx, topic, con, maximumallowedUnacknowledgedMessages)
		if receiverErr != nil {
			defer con.Close()
			logger.LoggerMgw.Errorf("Failed to a create receiver for topic: %s error:%s", topic, receiverErr.Error())
		}
		logger.LoggerMgw.Infof("Listening for messages on topic '%s'...\n", topic)
		startActiveMQTopicConsumer(topic, receiver)
	}
	health.SetControlPlaneBrokerStatus(true)
}
