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
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/eventhub"
	"github.com/wso2/product-microgateway/adapter/internal/synchronizer"
	adapter "github.com/wso2/product-microgateway/adapter/pkg/messaging"
)

// MessageHandlerFunc defines the function signature for message handlers
type MessageHandlerFunc func(body string) error

func notificationHandler(body string) error {
	conf, _ := config.ReadConfigs()
	var notification adapter.EventNotification
	if err := parseNotificationJSONEvent([]byte(body), &notification); err != nil {
		return err
	}
	notificationProcessError := processNotificationEvent(conf, &notification)
	if notificationProcessError != nil {
		return notificationProcessError
	}
	return nil
}

func tokenRevocationHandler(body string) error {
	var notification adapter.EventTokenRevocationNotification
	error := parseRevokedTokenJSONEvent([]byte(body), &notification)
	if error != nil {
		return error
	}
	processTokenRevocationEvent(&notification)
	return nil
}

func organizationPurgeHandler(body string) error {
	var event adapter.EventOrganizationPurge
	error := parseOrganizationPurgeJSONEvent([]byte(body), &event)
	if error != nil {
		return error
	}
	conf, _ := config.ReadConfigs()
	eventhub.LoadSubscriptionData(conf, nil)
	// clear existing Key Manager Data
	synchronizer.ClearKeyManagerData()
	// Pull Key Manager Data from APIM
	synchronizer.FetchKeyManagersOnStartUp(conf)
	return nil
}
