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

	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	eventhubTypes "github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

// constants related to key manager events
const (
	keyManagerConfigEvent = "KEY_MANAGER_CONFIGURATION"
	actionAdd             = "add"
	actionUpdate          = "update"
	actionDelete          = "delete"
	superTenantDomain     = "carbon.super"
)

// handleKMEvent
func handleKMConfiguration() {
	// This is not used in choreo. Instead it uses the notification channel.
	for d := range msg.KeyManagerChannel {
		var notification msg.EventKeyManagerNotification
		// var keyManagerConfig resourceTypes.KeymanagerConfig
		var kmConfigMap map[string]interface{}
		unmarshalErr := json.Unmarshal([]byte(string(d.Body)), &notification)
		if unmarshalErr != nil {
			logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling key manager event data %v", unmarshalErr.Error())
			return
		}
		logger.LoggerInternalMsg.Infof("Event %s is received", notification.Event.PayloadData.EventType)

		_, isFound := xds.KeyManagerMap[xds.GenerateKeyManagerMapKey(notification.Event.PayloadData.Name, notification.Event.PayloadData.Organization)]

		var decodedByte, err = base64.StdEncoding.DecodeString(notification.Event.PayloadData.Value)

		if err != nil {
			if _, ok := err.(base64.CorruptInputError); ok {
				logger.LoggerInternalMsg.Error("\nbase64 input is corrupt, check the provided key")
			}
			logger.LoggerInternalMsg.Errorf("Error occurred while decoding the notification event %v", err)
			return
		}

		if strings.EqualFold(keyManagerConfigEvent, notification.Event.PayloadData.EventType) {
			if isFound && strings.EqualFold(actionDelete, notification.Event.PayloadData.Action) {
				logger.LoggerInternalMsg.Infof("Found KM %s:%s to be deleted ", notification.Event.PayloadData.Name, notification.Event.PayloadData.Organization)
				delete(xds.KeyManagerMap, xds.GenerateKeyManagerMapKey(notification.Event.PayloadData.Name,
					notification.Event.PayloadData.Organization))
				xds.GenerateAndUpdateKeyManagerList()
			} else if decodedByte != nil {
				logger.LoggerInternalMsg.Infof("decoded stream %s", string(decodedByte))
				kmConfigMapErr := json.Unmarshal([]byte(string(decodedByte)), &kmConfigMap)
				if kmConfigMapErr != nil {
					logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling key manager config map %v", kmConfigMapErr)
					continue
				}

				if strings.EqualFold(actionAdd, notification.Event.PayloadData.Action) ||
					strings.EqualFold(actionUpdate, notification.Event.PayloadData.Action) {
					keyManager := eventhubTypes.KeyManager{Name: notification.Event.PayloadData.Name,
						Type: notification.Event.PayloadData.Type, Enabled: notification.Event.PayloadData.Enabled,
						TenantDomain: notification.Event.PayloadData.TenantDomain, Configuration: kmConfigMap,
						Organization: notification.Event.PayloadData.Organization}
					logger.LoggerInternalMsg.Infof("KeyManager data is saved. %v", keyManager.Configuration)
					xds.KeyManagerMap[xds.GenerateKeyManagerMapKey(keyManager.Name, keyManager.Organization)] = xds.MarshalKeyManager(&keyManager)
					xds.GenerateAndUpdateKeyManagerList()
				}
			}
		}
		d.Ack(false)
	}
	logger.LoggerInternalMsg.Info("handle: deliveries channel closed")
}
