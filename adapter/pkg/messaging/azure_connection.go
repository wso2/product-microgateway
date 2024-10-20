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
	"fmt"
	"regexp"
	"strconv"
	"time"

	asb "github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus"
	"github.com/Azure/azure-sdk-for-go/sdk/messaging/azservicebus/admin"
	"github.com/google/uuid"
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

// Subscription stores the metadata of a specific subscription
// TopicName: the topic name of the subscription
// SubscriptionName: the name of the subscription
// ConnectionString: the connection string of the service bus
// ClientOptions: the client options for initiating the client
// ReconnectInterval: the interval to wait before reconnecting
type Subscription struct {
	TopicName         string
	SubscriptionName  string
	ConnectionString  string
	ClientOptions     *asb.ClientOptions
	ReconnectInterval time.Duration
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
func InitiateBrokerConnectionAndValidate(connectionString string, topic string, clientOptions *asb.ClientOptions, componentName string, reconnectRetryCount int,
	reconnectInterval time.Duration, subscriptionIdleTimeDuration string) (*Subscription, error) {
	subProps := &admin.SubscriptionProperties{
		AutoDeleteOnIdle: &subscriptionIdleTimeDuration,
	}
	_, err := asb.NewClientFromConnectionString(connectionString, clientOptions)

	if err == nil {
		logger.LoggerMsg.Debugf("ASB client initialized for connection url: %s", maskSharedAccessKey(connectionString))

		for j := 0; j < reconnectRetryCount || reconnectRetryCount == -1; j++ {
			sub, err := RetrieveSubscriptionMetadataForTopic(connectionString, topic,
				clientOptions, componentName, subProps, reconnectInterval)
			if err != nil {
				logError(reconnectRetryCount, reconnectInterval, err)
				time.Sleep(reconnectInterval)
				continue
			}
			return sub, err
		}
		return nil, fmt.Errorf("failed to create subscription for topic %s", topic)
	}
	logger.LoggerMsg.Errorf("Error occurred while trying to create ASB client using the connection url %s, err: %v",
		maskSharedAccessKey(connectionString), err)
	return nil, err
}

// InitiateConsumer to start the broker consumer in a separate go routine
func InitiateConsumer(sub *Subscription, consumerType string) {
	go startBrokerConsumer(sub, consumerType)
}

func RetrieveSubscriptionMetadataForTopic(connectionString string, topicName string, clientOptions *asb.ClientOptions,
	componentName string, opts *admin.SubscriptionProperties, reconnectInterval time.Duration) (*Subscription, error) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	adminClient, clientErr := admin.NewClientFromConnectionString(connectionString, nil)
	if clientErr != nil {
		logger.LoggerMsg.Errorf("Error occurred while trying to create ASB admin client using the connection url %s", connectionString)
		return nil, clientErr
	}

	// we are creating a unique subscription for each adapter starts. Unused subscriptions will be deleted after
	// idle for three days

	// in ASB, subscription names can contain letters, numbers, periods (.), hyphens (-), and
	// underscores (_), up to 50 characters. Subscription names are also case-insensitive.
	subscriptionName := fmt.Sprintf("%s_%s_sub", componentName, uuid.New().String())
	_, err := adminClient.CreateSubscription(ctx, topicName, subscriptionName, &admin.CreateSubscriptionOptions{
		Properties: opts,
	})

	if err != nil {
		return nil, errors.New("Error occurred while trying to create subscription " + subscriptionName + " in ASB for topic name " +
			topicName + "." + err.Error())
	}

	logger.LoggerMsg.Debugf("Subscription %s created.", subscriptionName)

	return &Subscription{
		TopicName:         topicName,
		SubscriptionName:  subscriptionName,
		ConnectionString:  connectionString,
		ClientOptions:     clientOptions,
		ReconnectInterval: reconnectInterval,
	}, nil
}

func logError(reconnectRetryCount int, reconnectInterval time.Duration, errVal error) {
	retryAttemptMessage := ""
	if reconnectRetryCount > 0 {
		retryAttemptMessage = "Retry attempt : " + strconv.Itoa(reconnectRetryCount)
	}
	logger.LoggerMsg.Errorf("%v. %s .Retrying after %s seconds", errVal, retryAttemptMessage, reconnectInterval)
}

func maskSharedAccessKey(endpoint string) string {
	re := regexp.MustCompile(`(SharedAccessKey=)([^;]+)`)
	maskedEndpoint := re.ReplaceAllString(endpoint, "${1}************")
	return maskedEndpoint
}
