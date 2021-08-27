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
	"strings"
	"time"
)

var bindingKeys = []string{tokenRevocation, notification, organizationPurge}

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
	// AzureOrganizationPurgeChannel stores the Organization Purge events
	AzureOrganizationPurgeChannel chan []byte
)

func init() {
	AzureRevokedTokenChannel = make(chan []byte)
	AzureNotificationChannel = make(chan []byte)
	AzureOrganizationPurgeChannel = make(chan []byte)
}

// InitiateBrokerConnectionAndValidate to initiate connection and validate azure service bus constructs to
// further process
func InitiateBrokerConnectionAndValidate(eventListeningEndpoint string, componentName string, reconnectRetryCount int,
	reconnectInterval time.Duration, subscriptionIdleTimeDuration time.Duration) ([]Subscription, error) {
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Trying to connect to azure service bus with " +
		"connection string " + eventListeningEndpoint)
	subscriptionMetaDataList := make([]Subscription, 0)
	namespace, processError := servicebus.NewNamespace(servicebus.NamespaceWithConnectionString(eventListeningEndpoint))
	if processError == nil {
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Successfully received namespace ")
		var isTopicsCreated bool
		topicManager := namespace.NewTopicManager()
		var availableTopics []*servicebus.TopicEntity

		for j := 0; j < reconnectRetryCount || reconnectRetryCount == -1; j++ {
			processError = nil
			if availableTopics == nil {
				availableTopics, processError = getTopicList(topicManager)
				if processError != nil {
					logError(reconnectRetryCount, reconnectInterval, processError)
					time.Sleep(reconnectInterval)
					continue
				}
			}
			if !isTopicsCreated {
				processError = createTopicsIfNotExist(availableTopics, namespace)
			}
			if processError != nil {
				logError(reconnectRetryCount, reconnectInterval, processError)
				time.Sleep(reconnectInterval)
				continue
			}
			isTopicsCreated = true
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
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] %v. Retry attempted %d times.",
				processError, reconnectRetryCount)
			return subscriptionMetaDataList, processError
		}
	} else {
		//Any error which comes to this point is because the connection url is not up to the expected format
		//Hence not retrying
		logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get the namespace "+
			"in azure service bus using the connection url %s :%v", eventListeningEndpoint, processError)
	}
	return subscriptionMetaDataList, processError
}

// InitiateConsumers to pass event consumption
func InitiateConsumers(subscriptionMetaDataList []Subscription, reconnectInterval time.Duration) {
	for _, subscriptionMetaData := range subscriptionMetaDataList {
		go func(subscriptionMetaData Subscription) {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] starting the consumer for topic : " +
				subscriptionMetaData.topicName)
			startBrokerConsumer(subscriptionMetaData, reconnectInterval)
		}(subscriptionMetaData)
	}
}

func createTopicsIfNotExist(availableTopicList []*servicebus.TopicEntity, ns *servicebus.Namespace) error {
	parentContext := context.Background()
	for _, key := range bindingKeys {
		if !isTopicExist(key, availableTopicList) {
			//create the topic
			topicManager := ns.NewTopicManager()
			var topicCreationError error
			var errorValue error
			func() {
				ctx, cancel := context.WithCancel(parentContext)
				defer cancel()
				_, topicCreationError = topicManager.Put(ctx, key)
			}()
			if topicCreationError != nil {
				errorValue = errors.New("Error occurred while trying to create topic " + key + " in azure service bus : " +
					topicCreationError.Error())
				return errorValue
			}
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + key + " created")
		} else {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + key + " Exist ")
		}
	}
	return nil
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
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription manager created for the " +
			"topic " + key)
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
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription " + subscriptionName + " created")
		subscriptionMetaData.subscriptionName = subscriptionName
		subscriptionMetaData.topicName = key
		metaDataList = append(metaDataList, subscriptionMetaData)
	}
	return metaDataList, nil
}

func getTopicList(topicManager *servicebus.TopicManager) ([]*servicebus.TopicEntity, error) {
	var errorValue error
	var getTopicListError error
	var topicList []*servicebus.TopicEntity
	func() {
		ctx, cancel := context.WithCancel(context.Background())
		defer cancel()
		topicList, getTopicListError = topicManager.List(ctx)
	}()
	if getTopicListError != nil {
		errorValue = errors.New("Error occurred while trying to get topic list from azure service bus:%v." + getTopicListError.Error())
		return topicList, errorValue
	}
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic list received ")
	return topicList, nil
}

func isTopicExist(topicName string, availableTopicList []*servicebus.TopicEntity) bool {
	for _, topic := range availableTopicList {
		if strings.EqualFold(topic.Name, topicName) {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " Exist ")
			return true
		}
	}
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + topicName + " does not Exist ")
	return false
}

func logError(reconnectRetryCount int, reconnectInterval time.Duration, errVal error) {
	retryAttemptMessage := ""
	if reconnectRetryCount > 0 {
		retryAttemptMessage = "Retry attempt : " + strconv.Itoa(reconnectRetryCount)
	}
	logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] :%v."+retryAttemptMessage+" Retrying after %s seconds",
		errVal, reconnectInterval)
}
