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
	"github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus"
	"net"
	"nhooyr.io/websocket"
	"os"
	"strconv"
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

const (
	componentName                = "adapter"
	subscriptionIdleTimeDuration = "P0Y0M3DT0H0M0S"
	notification                 = "notification"
	tokenRevocation              = "tokenRevocation"
	stepQuotaThreshold           = "thresholdEvent"
	stepQuotaReset               = "billingCycleResetEvent"
	organizationPurge            = "organizationPurge"
)

var topicNames = []string{tokenRevocation, notification, stepQuotaThreshold, stepQuotaReset}

const orgPurgeEnabled = "ORG_PURGE_ENABLED"

func init() {
	// Temporarily disable reacting organization Purge
	orgPurgeEnabled, envParseErr := strconv.ParseBool(os.Getenv(orgPurgeEnabled))

	if envParseErr == nil {
		if orgPurgeEnabled {
			topicNames = append(topicNames, organizationPurge)
		}
	}
}

// InitiateAndProcessEvents to pass event consumption
func InitiateAndProcessEvents(config *config.Config) {
	if len(config.ControlPlane.ASBDataplaneTopics) > 0 {
		for _, topic := range config.ControlPlane.ASBDataplaneTopics {
			subscription, err := msg.InitiateBrokerConnectionAndValidate(
				topic.ConnectionString,
				topic.TopicName,
				getAmqpClientOptions(topic.AmqpOverWebsocketsEnabled),
				componentName,
				topic.ReconnectRetryCount,
				topic.ReconnectInterval*time.Millisecond,
				subscriptionIdleTimeDuration)
			if err != nil {
				logger.LoggerMgw.Errorf("Error while initiating broker connection for topic %s: %v", topic.TopicName, err)
				health.SetControlPlaneBrokerStatus(false)
				return
			}
			msg.InitiateConsumer(subscription, topic.Type)
			startChannelConsumer(topic.Type)
			logger.LoggerMgw.Infof("Broker connection initiated and lsitening on topic %s...", topic.TopicName)
		}
		health.SetControlPlaneBrokerStatus(true)
	} else {
		for _, topic := range topicNames {
			connectionString := config.ControlPlane.BrokerConnectionParameters.EventListeningEndpoints[0]
			reconnectRetryCount := config.ControlPlane.BrokerConnectionParameters.ReconnectRetryCount
			reconnectInterval := config.ControlPlane.BrokerConnectionParameters.ReconnectInterval

			subscription, err := msg.InitiateBrokerConnectionAndValidate(
				connectionString,
				topic,
				getAmqpClientOptions(config.ControlPlane.BrokerConnectionParameters.AmqpOverWebsocketsEnabled),
				componentName,
				reconnectRetryCount,
				reconnectInterval*time.Millisecond,
				subscriptionIdleTimeDuration)
			if err != nil {
				logger.LoggerMgw.Errorf("Error while initiating broker connection for topic %s: %v", topic, err)
				health.SetControlPlaneBrokerStatus(false)
				return
			}
			msg.InitiateConsumer(subscription, topic)
			startChannelConsumer(topic)
			logger.LoggerMgw.Infof("Broker connection initiated and lsitening on topic %s...", topic)
		}
		health.SetControlPlaneBrokerStatus(true)
	}
}

func startChannelConsumer(consumerType string) {
	switch consumerType {
	case notification:
		go handleAzureNotification()
	case tokenRevocation:
		go handleAzureTokenRevocation()
	case organizationPurge:
		go handleAzureOrganizationPurge()
	}
}

func getAmqpClientOptions(isAmqpOverWebsocketsEnabled bool) *azservicebus.ClientOptions {
	if isAmqpOverWebsocketsEnabled {
		logger.LoggerMgw.Info("AMQP over Websockets is enabled. Initiating brokers with AMQP over Websockets.")
		newWebSocketConnFn := func(ctx context.Context, args azservicebus.NewWebSocketConnArgs) (net.Conn, error) {
			opts := &websocket.DialOptions{Subprotocols: []string{"amqp"}}
			wssConn, _, err := websocket.Dial(ctx, args.Host, opts)
			if err != nil {
				return nil, err
			}
			return websocket.NetConn(ctx, wssConn, websocket.MessageBinary), nil
		}
		return &azservicebus.ClientOptions{
			NewWebSocketConn: newWebSocketConnFn,
		}
	}
	return nil
}
