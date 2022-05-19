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
	"strconv"
	"time"

	asb "github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus"
	"github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus/admin"
	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

// TODO: (erandi) when refactoring, refactor organization purge flow as well
var bindingKeys = []string{tokenRevocation, notification, stepQuotaThreshold, stepQuotaReset, organizationPurge}

// Subscription stores the meta data of a specific subscription
type Subscription struct {
	topicName        string
	subscriptionName string
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
func InitiateBrokerConnectionAndValidate(connectionString string, componentName string, reconnectRetryCount int,
	reconnectInterval time.Duration, subscriptionIdleTimeDuration string) ([]Subscription, error) {
	subscriptionMetaDataList := make([]Subscription, 0)
	subProps := &admin.SubscriptionProperties{
		AutoDeleteOnIdle: &subscriptionIdleTimeDuration,
	}
	_, err := asb.NewClientFromConnectionString(connectionString, nil)

	if err == nil {
		logger.LoggerMsg.Debugf("ASB client initialized for connection url: %s", connectionString)

		for j := 0; j < reconnectRetryCount || reconnectRetryCount == -1; j++ {
			err = nil
			subscriptionMetaDataList, err = retrieveSubscriptionMetadata(subscriptionMetaDataList,
				connectionString, componentName, subProps)
			if err != nil {
				logError(reconnectRetryCount, reconnectInterval, err)
				subscriptionMetaDataList = nil
				time.Sleep(reconnectInterval)
				continue
			}
			return subscriptionMetaDataList, err
		}
		if err != nil {
			logger.LoggerMsg.Errorf("%v. Retry attempted %d times.", err, reconnectRetryCount)
			return subscriptionMetaDataList, err
		}
	} else {
		// any error which comes to this point is because the connection url is not up to the expected format
		// hence not retrying
		logger.LoggerMsg.Errorf("Error occurred while trying to create ASB client using the connection url %s, err: %v",
			connectionString, err)
	}
	return subscriptionMetaDataList, err
}

// InitiateConsumers to pass event consumption
func InitiateConsumers(connectionString string, subscriptionMetaDataList []Subscription, reconnectInterval time.Duration) {
	for _, subscriptionMetaData := range subscriptionMetaDataList {
		go func(subscriptionMetaData Subscription) {
			startBrokerConsumer(connectionString, subscriptionMetaData, reconnectInterval)
		}(subscriptionMetaData)
	}
}

func retrieveSubscriptionMetadata(metaDataList []Subscription, connectionString string, componentName string,
	opts *admin.SubscriptionProperties) ([]Subscription, error) {
	parentContext := context.Background()
	adminClient, clientErr := admin.NewClientFromConnectionString(connectionString, nil)
	if clientErr != nil {
		logger.LoggerMsg.Errorf("Error occurred while trying to create ASB admin client using the connection url %s", connectionString)
		return nil, clientErr
	}

	for _, key := range bindingKeys {
		var errorValue error
		subscriptionMetaData := Subscription{
			topicName:        key,
			subscriptionName: "",
		}
		// we are creating a unique subscription for each adapter starts. Unused subscriptions will be deleted after
		// idle for three days
		uniqueID := uuid.New()

		// in ASB, subscription names can contain letters, numbers, periods (.), hyphens (-), and
		// underscores (_), up to 50 characters. Subscription names are also case-insensitive.
		var subscriptionName = componentName + "_" + uniqueID.String() + "_sub"
		var subscriptionCreationError error
		func() {
			ctx, cancel := context.WithCancel(parentContext)
			defer cancel()
			_, subscriptionCreationError = adminClient.CreateSubscription(ctx, key, subscriptionName, &admin.CreateSubscriptionOptions{
				Properties: opts,
			})
		}()
		if subscriptionCreationError != nil {
			errorValue = errors.New("Error occurred while trying to create subscription " + subscriptionName + " in ASB for topic name " +
				key + "." + subscriptionCreationError.Error())
			return metaDataList, errorValue
		}
		logger.LoggerMsg.Debugf("Subscription %s created.", subscriptionName)
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
	logger.LoggerMsg.Errorf("%v. %s .Retrying after %s seconds", errVal, retryAttemptMessage, reconnectInterval)
}
