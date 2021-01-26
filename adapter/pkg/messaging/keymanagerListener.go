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
	"encoding/base64"
	"encoding/json"
	"strings"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
)

const keyManagerConfig = "key_manager_configuration"

// handleKMEvent
func handleKMConfiguration(deliveries <-chan amqp.Delivery, done chan error) {

	for d := range deliveries {
		var notification EventKeyManagerNotification
		var keyManagerEvent KeyManagerEvent
		// var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)

		var decodedByte, err = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Value)
		if err != nil {
			if _, ok := err.(base64.CorruptInputError); ok {
				panic("\nbase64 input is corrupt, check the provided key")
			}
			panic(err)
		}
		if strings.EqualFold(keyManagerConfig, notification.Event.PayloadData.EventType) {
			if decodedByte != nil {
				json.Unmarshal([]byte(string(decodedByte)), &keyManagerEvent)
				logger.LoggerJMS.Infof("EventType: %s, Action: %s ",
					notification.Event.PayloadData.EventType, notification.Event.PayloadData.Action)
			}
		}
		d.Ack(false)
	}
	logger.LoggerJMS.Info("handle: deliveries channel closed")
	done <- nil
}
