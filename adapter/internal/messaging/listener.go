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
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Package messaging holds the implementation for event listeners functions
package messaging

import (
	"fmt"
	"strings"
	"time"

	"github.com/wso2/adapter/pkg/health"

	"github.com/wso2/adapter/config"
	logger "github.com/wso2/adapter/internal/loggers"
	msg "github.com/wso2/adapter/pkg/messaging"
)

var lifetime = 0 * time.Second

const (
	notification    string = "notification"
	keymanager      string = "keymanager"
	tokenRevocation string = "tokenRevocation"
	throttleData    string = "throttleData"
	exchange        string = "amq.topic"
	exchangeType    string = "topic"
)

// ProcessEvents to pass event consumption
func ProcessEvents(config *config.Config) {
	var err error
	passConfigToPkg(config)
	bindingKeys := []string{notification, keymanager, tokenRevocation, throttleData}
	msg.RabbitConn, err = msg.ConnectToRabbitMQ()
	health.SetControlPlaneJmsStatus(err == nil)

	if err == nil {
		for i, key := range bindingKeys {
			logger.LoggerInternalMsg.Infof("Establishing consumer index %v for key %s ", i, key)
			go func(key string) {
				c := msg.StartConsumer(key)
				err := handleEvent(c, key)
				if err != nil {
					logger.LoggerInternalMsg.Fatalf("%s", err)
				}
				if lifetime > 0 {
					logger.LoggerInternalMsg.Debugf("running %s events for %s", key, lifetime)
					time.Sleep(lifetime)
				} else {
					logger.LoggerInternalMsg.Infof("process of receiving %s events running forever", key)
					select {}
				}

				logger.LoggerInternalMsg.Infof("shutting down")
				if err := c.Shutdown(); err != nil {
					logger.LoggerInternalMsg.Fatalf("error during shutdown: %s", err)
				}
			}(key)
		}
	}
}

func handleEvent(c *msg.Consumer, key string) error {
	var err error

	logger.LoggerInternalMsg.Debugf("got Connection, getting Channel for %s events", key)
	c.Channel, err = c.Conn.Channel()
	if err != nil {
		return fmt.Errorf("Channel: %s", err)
	}

	logger.LoggerInternalMsg.Debugf("got Channel, declaring Exchange (%q)", exchange)
	if err = c.Channel.ExchangeDeclare(
		exchange,     // name of the exchange
		exchangeType, // type
		true,         // durable
		false,        // delete when complete
		false,        // internal
		false,        // noWait
		nil,          // arguments
	); err != nil {
		return fmt.Errorf("Exchange Declare: %s", err)
	}

	logger.LoggerInternalMsg.Infof("declared Exchange, declaring Queue %q", key+"queue")
	queue, err := c.Channel.QueueDeclare(
		"",    // name of the queue
		false, // durable
		true,  // delete when usused
		false, // exclusive
		false, // noWait
		nil,   // arguments
	)
	if err != nil {
		return fmt.Errorf("Queue Declare: %s", err)
	}

	logger.LoggerInternalMsg.Debugf("declared Queue (%q %d messages, %d consumers), binding to Exchange (key %q)",
		queue.Name, queue.Messages, queue.Consumers, key)

	if err = c.Channel.QueueBind(
		queue.Name, // name of the queue
		key,        // bindingKey
		exchange,   // sourceExchange
		false,      // noWait
		nil,        // arguments
	); err != nil {
		return fmt.Errorf("Queue Bind: %s", err)
	}
	logger.LoggerInternalMsg.Infof("Queue bound to Exchange, starting Consume (consumer tag %q) events", c.Tag)
	deliveries, err := c.Channel.Consume(
		queue.Name, // name
		c.Tag,      // consumerTag,
		false,      // noAck
		false,      // exclusive
		false,      // noLocal
		false,      // noWait
		nil,        // arguments
	)
	if strings.EqualFold(key, notification) {
		go handleNotification(deliveries, c.Done)
	} else if strings.EqualFold(key, keymanager) {
		go handleKMConfiguration(deliveries, c.Done)
	} else if strings.EqualFold(key, tokenRevocation) {
		go handleTokenRevocation(deliveries, c.Done)
	} else if strings.EqualFold(key, throttleData) {
		go handleThrottleData(deliveries, c.Done)
	}
	return nil
}

func passConfigToPkg(config *config.Config) {
	msg.EventListeningEndpoints = config.ControlPlane.JmsConnectionParameters.EventListeningEndpoints
}
