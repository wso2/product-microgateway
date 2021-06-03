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
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Package messaging holds the implementation for event listeners functions
package messaging

import (
	"time"

	"github.com/wso2/product-microgateway/adapter/pkg/health"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

var lifetime = 0 * time.Second

const (
	notification    string = "notification"
	keymanager      string = "keymanager"
	tokenRevocation string = "tokenRevocation"
	throttleData    string = "throttleData"
	exchange        string = "amq.topic"
	exchangeType    string = "topic"
)

// ProcessEvents to pass event consumption
func ProcessEvents(config *config.Config) {
	var err error
	passConfigToPkg(config)
	bindingKeys := []string{notification, keymanager, tokenRevocation, throttleData}
	msg.RabbitConn, err = msg.ConnectToRabbitMQ()
	health.SetControlPlaneJmsStatus(err == nil)

	go handleNotification()
	go handleKMConfiguration()
	go handleThrottleData()
	go handleTokenRevocation()

	if err == nil {
		for i, key := range bindingKeys {
			logger.LoggerInternalMsg.Infof("Establishing consumer index %v for key %s ", i, key)
			go func(key string) {
				msg.StartConsumer(key)
			}(key)
		}
	}
}

func passConfigToPkg(config *config.Config) {
	msg.EventListeningEndpoints = config.ControlPlane.JmsConnectionParameters.EventListeningEndpoints
}
