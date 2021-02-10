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
	"fmt"
	"strings"

	"github.com/streadway/amqp"
	logger "github.com/wso2/micro-gw/loggers"
	resourceTypes "github.com/wso2/micro-gw/pkg/resourcetypes"
)

const (
	keyManagerConfigEvent = "key_manager_configuration"
	actionAdd             = "add"
	actionUpdate          = "update"
	actionDelete          = "delete"
)

// KeyManagerList to store data
var KeyManagerList = make([]resourceTypes.Keymanager, 0)

// handleKMEvent
func handleKMConfiguration(deliveries <-chan amqp.Delivery, done chan error) {
	var (
		indexOfKeymanager int
		isFound           bool
	)
	for d := range deliveries {
		var notification EventKeyManagerNotification
		// var keyManagerConfig resourceTypes.KeymanagerConfig
		var keyManagerConfig map[string]interface{}

		// var eventType string
		json.Unmarshal([]byte(string(d.Body)), &notification)

		for i := range KeyManagerList {
			if strings.EqualFold(notification.Event.PayloadData.Name, KeyManagerList[i].Name) {
				isFound = true
				indexOfKeymanager = i
				break
			}
		}

		var decodedByte, err = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Value)
		if err != nil {
			if _, ok := err.(base64.CorruptInputError); ok {
				panic("\nbase64 input is corrupt, check the provided key")
			}
			panic(err)
		}
		if strings.EqualFold(keyManagerConfigEvent, notification.Event.PayloadData.EventType) {
			if isFound || strings.EqualFold(actionDelete, notification.Event.PayloadData.Action) {
				logger.LoggerMsg.Infof("Found KM %s to be deleted index %d", notification.Event.PayloadData.Name,
					indexOfKeymanager)
				// TODO: deleteKeyManagerFromList(indexOfKeymanager)
			}
			if decodedByte != nil {
				logger.LoggerMsg.Infof("decoded stream %s", string(decodedByte))
				json.Unmarshal([]byte(string(decodedByte)), &keyManagerConfig)

				if strings.EqualFold(actionAdd, notification.Event.PayloadData.Action) ||
					strings.EqualFold(actionUpdate, notification.Event.PayloadData.Action) {
					keyManager := resourceTypes.Keymanager{Name: notification.Event.PayloadData.Name,
						Type: notification.Event.PayloadData.Type, Enabled: notification.Event.PayloadData.Enabled,
						TenantDomain: notification.Event.PayloadData.TenantDomain, Configuration: keyManagerConfig}
					logger.LoggerMsg.Infof("data %v", keyManager.Configuration)

					for key, value := range keyManager.Configuration {
						fmt.Printf("configs: - [%s] = %s\n", key, value)
					}
					KeyManagerList = append(KeyManagerList, keyManager)
					// TODO: passEventDataToEnforcer(KeyManagerList)
				}
			}
		}
		d.Ack(false)
	}
	logger.LoggerMsg.Info("handle: deliveries channel closed")
	done <- nil
}
