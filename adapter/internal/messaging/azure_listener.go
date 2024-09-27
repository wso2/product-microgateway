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
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

const (
	componentName                = "adapter"
	subscriptionIdleTimeDuration = "P0Y0M3DT0H0M0S"
)

// InitiateAndProcessEvents to pass event consumption
func InitiateAndProcessEvents(config *config.Config) {
	var err error
	var reconnectRetryCount = config.ControlPlane.BrokerConnectionParameters.ReconnectRetryCount
	var reconnectInterval = config.ControlPlane.BrokerConnectionParameters.ReconnectInterval

	connectionString := config.ControlPlane.BrokerConnectionParameters.EventListeningEndpoints[0]
	var clientOpts *azservicebus.ClientOptions
	if config.ControlPlane.BrokerConnectionParameters.AmqpOverWebsocketsEnabled {
		logger.LoggerMgw.Info("AMQP over Websockets is enabled. Initiating brokers with AMQP over Websockets.")
		newWebSocketConnFn := func(ctx context.Context, args azservicebus.NewWebSocketConnArgs) (net.Conn, error) {
			opts := &websocket.DialOptions{Subprotocols: []string{"amqp"}}
			wssConn, _, err := websocket.Dial(ctx, args.Host, opts)
			if err != nil {
				return nil, err
			}
			return websocket.NetConn(ctx, wssConn, websocket.MessageBinary), nil
		}
		clientOpts = &azservicebus.ClientOptions{
			NewWebSocketConn: newWebSocketConnFn,
		}
	}

	subscriptionMetaDataList, err := msg.InitiateBrokerConnectionAndValidate(connectionString, clientOpts, componentName,
		reconnectRetryCount, reconnectInterval*time.Millisecond, subscriptionIdleTimeDuration)
	health.SetControlPlaneBrokerStatus(err == nil)
	if err == nil {
		logger.LoggerMgw.Info("Service bus meta data successfully initialized.")
		msg.InitiateConsumers(connectionString, clientOpts, subscriptionMetaDataList, reconnectInterval*time.Millisecond)
		go handleAzureNotification()
		go handleAzureTokenRevocation()
		go handleAzureOrganizationPurge()
	}

}
