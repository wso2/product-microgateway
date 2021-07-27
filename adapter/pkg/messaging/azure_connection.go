/*
 *
 *  * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

// Package messaging holds the implementation for event listeners functions
package messaging

import (
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

var (
	// AzureRevokedTokenChannel stores the revoked token events
	AzureRevokedTokenChannel chan []byte
)

func init() {
	AzureRevokedTokenChannel = make(chan []byte)
}


// connectToAzureServiceBus function tries to connect to the azure service bus server as long as it takes to
// establish a connection
func connectToAzureServiceBus() {
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Trying to connect to azure service bus")
}



// InitiateBrokerConnection to pass event consumption
func InitiateBrokerConnection(eventListeningEndpoints []string) error {
	var err error
	connectToAzureServiceBus()

	err = nil

	return err
}
