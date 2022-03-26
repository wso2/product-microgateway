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
	"encoding/json"

	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	stringutils "github.com/wso2/product-microgateway/adapter/internal/utils"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/keymgt"
	msg "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

func handleTokenRevocation() {
	for d := range msg.RevokedTokenChannel {
		var notification msg.EventTokenRevocationNotification
		unmarshalErr := parseRevokedTokenJSONEvent([]byte(string(d.Body)), &notification)
		if unmarshalErr != nil {
			continue
		}
		logger.LoggerInternalMsg.Infof("Event %s is received", notification.Event.PayloadData.Type)
		logger.LoggerInternalMsg.Debugf("RevokedToken: %s, Token Type: %s", stringutils.MaskToken(notification.Event.PayloadData.RevokedToken),
			notification.Event.PayloadData.Type)
		processTokenRevocationEvent(&notification)
		d.Ack(false)
	}
	logger.LoggerInternalMsg.Infof("handle: deliveries channel closed")
}

func handleAzureTokenRevocation() {
	for d := range msg.AzureRevokedTokenChannel {
		var notification msg.EventTokenRevocationNotification
		error := parseRevokedTokenJSONEvent(d, &notification)
		if error != nil {
			continue
		}
		logger.LoggerInternalMsg.Infof("Event %s is received", notification.Event.PayloadData.Type)
		logger.LoggerInternalMsg.Debugf("RevokedToken: %s, Token Type: %s", stringutils.MaskToken(notification.Event.PayloadData.RevokedToken),
			notification.Event.PayloadData.Type)
		processTokenRevocationEvent(&notification)
	}
}

func processTokenRevocationEvent(notification *msg.EventTokenRevocationNotification) {
	var revokedTokens []types.Resource
	token := &keymgt.RevokedToken{}
	token.Jti = notification.Event.PayloadData.RevokedToken
	token.Expirytime = notification.Event.PayloadData.ExpiryTime
	revokedTokens = append(revokedTokens, token)
	xds.UpdateEnforcerRevokedTokens(revokedTokens)
}

func parseRevokedTokenJSONEvent(data []byte, notification *msg.EventTokenRevocationNotification) error {
	unmarshalErr := json.Unmarshal(data, &notification)
	if unmarshalErr != nil {
		logger.LoggerInternalMsg.Errorf("Error occurred while unmarshalling revoked token event data %v. "+
			"Hence dropping the event.", unmarshalErr)
	}
	return unmarshalErr
}
