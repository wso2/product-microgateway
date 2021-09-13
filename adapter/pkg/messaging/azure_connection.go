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
	"errors"
	servicebus "github.com/Azure/azure-service-bus-go"
	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"strconv"
	"time"
)

// TODO: (erandi) when refactoring, refactor organization purge flow as well
var bindingKeys = []string{tokenRevocation, notification, stepQuotaThreshold, stepQuotaReset, organizationPurge}

// Subscription stores the meta data of a specific subscription
type Subscription struct {
	topicName           string
	subscriptionName    string
	subscriptionManager *servicebus.SubscriptionManager
}

var (
	// AzureRevokedTokenChannel stores the revoked token events
	AzureRevokedTokenChannel chan []byte
	// AzureNotificationChannel stores the notification events
	AzureNotificationChannel chan []byte
	// AzureStepQuotaThresholdChannel stores the step quota threshold events
	AzureStepQuotaThresholdChannel chan []byte
	// AzureStepQuotaResetChannel stores the step quota reset events
	AzureStepQuotaResetChannel chan []byte
	// AzureOrganizationPurgeChannel stores the Organization Purge events
	AzureOrganizationPurgeChannel chan []byte
)

func init() {
	AzureRevokedTokenChannel = make(chan []byte)
	AzureNotificationChannel = make(chan []byte)
	AzureStepQuotaThresholdChannel = make(chan []byte)
	AzureStepQuotaResetChannel = make(chan []byte)
	AzureOrganizationPurgeChannel = make(chan []byte)
}

// InitiateBrokerConnectionAndValidate to initiate connection and validate azure service bus constructs to
// further process
func InitiateBrokerConnectionAndValidate(eventListeningEndpoint string, componentName string, reconnectRetryCount int,
	reconnectInterval time.Duration, subscriptionIdleTimeDuration time.Duration) ([]Subscription, error) {
	subscriptionMetaDataList := make([]Subscription, 0)
	namespace, processError := servicebus.NewNamespace(servicebus.NamespaceWithConnectionString(eventListeningEndpoint))
	if processError == nil {
		logger.LoggerMgw.Debug("Service bus namespace successfully received for connection url : " +
			eventListeningEndpoint)
		for j := 0; j < reconnectRetryCount || reconnectRetryCount == -1; j++ {
			processError = nil
			subscriptionMetaDataList, processError = retrieveSubscriptionMetadata(subscriptionMetaDataList,
				namespace, componentName,
				servicebus.SubscriptionWithAutoDeleteOnIdle(&subscriptionIdleTimeDuration))
			if processError != nil {
				logError(reconnectRetryCount, reconnectInterval, processError)
				subscriptionMetaDataList = nil
				time.Sleep(reconnectInterval)
				continue
			}
			return subscriptionMetaDataList, processError
		}
		if processError != nil {
			logger.LoggerMgw.Errorf("%v. Retry attempted %d times.", processError, reconnectRetryCount)
			return subscriptionMetaDataList, processError
		}
	} else {
		//Any error which comes to this point is because the connection url is not up to the expected format
		//Hence not retrying
		logger.LoggerMgw.Errorf("Error occurred while trying get the namespace "+
			"in azure service bus using the connection url %s :%v", eventListeningEndpoint, processError)
	}
	return subscriptionMetaDataList, processError
}

// InitiateConsumers to pass event consumption
func InitiateConsumers(subscriptionMetaDataList []Subscription, reconnectInterval time.Duration) {
	for _, subscriptionMetaData := range subscriptionMetaDataList {
		go func(subscriptionMetaData Subscription) {
			startBrokerConsumer(subscriptionMetaData, reconnectInterval)
		}(subscriptionMetaData)
	}
}

func retrieveSubscriptionMetadata(metaDataList []Subscription, ns *servicebus.Namespace, componentName string,
	opts ...servicebus.SubscriptionManagementOption) ([]Subscription, error) {
	parentContext := context.Background()
	for _, key := range bindingKeys {
		var subManager *servicebus.SubscriptionManager
		var subManagerError error
		var errorValue error
		subscriptionMetaData := Subscription{
			topicName:           key,
			subscriptionName:    "",
			subscriptionManager: nil,
		}
		subManager, subManagerError = ns.NewSubscriptionManager(key)
		if subManagerError != nil {
			errorValue = errors.New("Error occurred while trying to get subscription manager from azure service bus for topic name : " +
				key + ":" + subManagerError.Error())
			return metaDataList, errorValue
		}
		logger.LoggerMgw.Debug("Subscription manager created for the topic " + key)
		subscriptionMetaData.subscriptionManager = subManager
		//We are creating a unique subscription for each adapter starts. Unused subscriptions will be deleted after
		// idle for three days
		uniqueID := uuid.New()

		//In Azure service bus subscription names can contain letters, numbers, periods (.), hyphens (-), and
		// underscores (_), up to 50 characters. Subscription names are also case-insensitive.
		var subscriptionName = componentName + "_" + uniqueID.String() + "_sub"
		var subscriptionCreationError error
		func() {
			ctx, cancel := context.WithCancel(parentContext)
			defer cancel()
			_, subscriptionCreationError = subManager.Put(ctx, subscriptionName, opts...)
		}()
		if subscriptionCreationError != nil {
			errorValue = errors.New("Error occurred while trying to create subscription " + subscriptionName + "in azure service bus for topic name " +
				key + ":" + subscriptionCreationError.Error())
			return metaDataList, errorValue
		}
		logger.LoggerMgw.Debug("Subscription " + subscriptionName + " created.")
		subscriptionMetaData.subscriptionName = subscriptionName
		subscriptionMetaData.topicName = key
		metaDataList = append(metaDataList, subscriptionMetaData)
	}
	return metaDataList, nil
}

func logError(reconnectRetryCount int, reconnectInterval time.Duration, errVal error) {
	retryAttemptMessage := ""
	if reconnectRetryCount > 0 {
		retryAttemptMessage = "Retry attempt : " + strconv.Itoa(reconnectRetryCount)
	}
	logger.LoggerMgw.Errorf("%v."+retryAttemptMessage+" .Retrying after %s seconds",
		errVal, reconnectInterval)
}
