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
	"strings"
	"time"

	servicebus "github.com/Azure/azure-service-bus-go"
	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"fmt"
)

var bindingKeys = []string{tokenRevocation, notification}

// SubscriptionType stores the meta data of a specific subscription
type SubscriptionType struct {
	topicName           string
	subscriptionName    string
	subscriptionManager *servicebus.SubscriptionManager
}

var (
	// AzureRevokedTokenChannel stores the revoked token events
	AzureRevokedTokenChannel chan []byte
	// AzureNotificationChannel stores the notification events
	AzureNotificationChannel chan []byte
)

func init() {
	AzureRevokedTokenChannel = make(chan []byte)
	AzureNotificationChannel = make(chan []byte)
}

// InitiateBrokerConnectionAndValidate to initiate connection and validate azure service bus constructs to
// further process
func InitiateBrokerConnectionAndValidate(eventListeningEndpoint string, componentName string, reconnectRetryCount int,
	reconnectInterval time.Duration, subscriptionIdleTimeDuration time.Duration) ([]SubscriptionType, error) {
	logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Trying to connect to azure service bus with " +
		"connection string " + eventListeningEndpoint)
	subscriptionMetaDataList := make([]SubscriptionType, 0)
	namespace, err := servicebus.NewNamespace(servicebus.NamespaceWithConnectionString(eventListeningEndpoint))
	if err == nil {
		var getTopicListError error
		var subscriptionMetaDataCreationError error
		logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Successfully received namespace ")
		topicManager := namespace.NewTopicManager()
		var availableTopics []*servicebus.TopicEntity
		for j := 0; j < reconnectRetryCount; j++ {
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()
			availableTopics, getTopicListError = topicManager.List(ctx)
			if getTopicListError != nil {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to get topic "+
					"list from azure service bus :%v. Retrying after %d seconds", err, reconnectInterval)
				time.Sleep(reconnectInterval)
			}
		}
		if getTopicListError != nil {
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not get topic list from "+
				" azure service bus after %d retry attempts ", reconnectRetryCount)
			return subscriptionMetaDataList, getTopicListError
		}
		topicCreationError := validateAndCreateTopicForSubscription(availableTopics, namespace,
			reconnectRetryCount, reconnectInterval)
		if topicCreationError != nil {
			return subscriptionMetaDataList, topicCreationError
		}
		subscriptionMetaDataList, subscriptionMetaDataCreationError = validateAndGetSubscriptionMetaDataList(subscriptionMetaDataList, namespace, reconnectRetryCount,
			reconnectInterval, componentName, servicebus.SubscriptionWithAutoDeleteOnIdle(&subscriptionIdleTimeDuration))
		if subscriptionMetaDataCreationError != nil {
			return subscriptionMetaDataList, subscriptionMetaDataCreationError
		}
	} else {
		//Any error which comes to this point is because the connection url is not up to the expected format
		//Hence not retrying
		logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get the namespace "+
			"in azure service bus using the connection url %s :%v", eventListeningEndpoint, err)
	}
	return subscriptionMetaDataList, err
}

// InitiateConsumers to pass event consumption
func InitiateConsumers(subscriptionMetaDataList []SubscriptionType) {
	for _, subscriptionMetaData := range subscriptionMetaDataList {
		go func(subscriptionMetaData SubscriptionType) {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] starting the consumer for topic : " +
				subscriptionMetaData.topicName)
			startBrokerConsumer(subscriptionMetaData)
			select {}
		}(subscriptionMetaData)
	}
}

func validateAndCreateTopicForSubscription(availableTopicList []*servicebus.TopicEntity, ns *servicebus.Namespace,
	reconnectRetryCount int, reconnectInterval time.Duration) error {
	parentContext := context.Background()
	for _, key := range bindingKeys {
		if !isTopicExist(key, availableTopicList) {
			//create the topic
			topicManager := ns.NewTopicManager()
			var topicCreationError error
			for j := 0; j < reconnectRetryCount; j++ {
				ctx, cancel := context.WithCancel(parentContext)
				defer cancel()
				_, topicCreationError := topicManager.Put(ctx, key)
				if topicCreationError != nil {
					logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create "+
						"topic %s in azure service bus :%v. Retrying after %d seconds",
						key, topicCreationError, reconnectInterval)
					time.Sleep(reconnectInterval)
				} else {
					logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + key + " created")
					break
				}
			}
			if topicCreationError != nil {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not create topic %s "+
					" in azure service bus after %d retry attempts ", key, reconnectRetryCount)
				return topicCreationError
			}
		} else {
			logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Topic " + key + " Exist ")
		}
	}
	return nil
}

func validateAndGetSubscriptionMetaDataList(metaDataList []SubscriptionType, ns *servicebus.Namespace, reconnectRetryCount int,
	reconnectInterval time.Duration, componentName string, opts ...servicebus.SubscriptionManagementOption) ([]SubscriptionType, error) {
	parentContext := context.Background()
	for _, key := range bindingKeys {
		var subManager *servicebus.SubscriptionManager
		var subManagerError error
		subscriptionMetaData := SubscriptionType{
			topicName:           key,
			subscriptionName:    "",
			subscriptionManager: nil,
		}
		for j := 0; j < reconnectRetryCount; j++ {
			subManager, subManagerError = ns.NewSubscriptionManager(key)
			if subManagerError != nil {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying get "+
					"subscription manager from azure service bus for topic name %s:%v. Retrying after %d seconds",
					key, subManagerError, reconnectInterval)
				time.Sleep(reconnectInterval)
			} else {
				logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription manager created for the " +
					"topic " + key)
				break
			}
		}
		if subManagerError != nil {
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not create subscription manager for "+
				"topic %s in azure service bus after %d retry attempts ", key, reconnectRetryCount)
			return metaDataList, subManagerError
		}
		subscriptionMetaData.subscriptionManager = subManager
		//We are creating a unique subscription for each adapter starts. Unused subscriptions will be deleted after
		// idle for three days
		uniqueID := uuid.New()

		//In Azure service bus subscription names can contain letters, numbers, periods (.), hyphens (-), and
		// underscores (_), up to 50 characters. Subscription names are also case-insensitive.
		var subscriptionName = componentName + "_" + uniqueID.String() + "_sub"
		var subscriptionCreationError error
		for j := 0; j < reconnectRetryCount; j++ {
			ctx, cancel := context.WithCancel(parentContext)
			defer cancel()
			_, subscriptionCreationError := subManager.Put(ctx, subscriptionName, opts...)
			if subscriptionCreationError != nil {
				logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Error occurred while trying to create "+
					"subscription %s in azure service bus for topic name %s:%v. "+
					"Retrying after %d seconds", subscriptionName, key, subscriptionCreationError, reconnectInterval)
				time.Sleep(reconnectInterval)
			} else {
				logger.LoggerMgw.Info("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Subscription " + subscriptionName + " created")
				break
			}
		}
		if subscriptionCreationError != nil {
			logger.LoggerMgw.Errorf("[TEST][FEATURE_FLAG_REPLACE_EVENT_HUB] Could not create get subscription "+
				subscriptionName+" in azure service bus for topic name %s after %d retry "+
				"attempts ", key, reconnectRetryCount)
			return metaDataList, subscriptionCreationError
		}
		subscriptionMetaData.subscriptionName = subscriptionName
		subscriptionMetaData.topicName = key
		metaDataList = append(metaDataList, subscriptionMetaData)
	}
	return metaDataList, nil
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
