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
	"strings"
	"time"

	"github.com/wso2/adapter/pkg/health"

	"github.com/wso2/adapter/config"
	logger "github.com/wso2/adapter/internal/loggers"
	msg "github.com/wso2/adapter/pkg/messaging"
)

const (
	notification    string = "notification"
	keymanager      string = "keymanager"
	tokenRevocation string = "tokenRevocation"
	throttleData    string = "throttleData"
)

// ProcessEvents to pass event consumption
func ProcessEvents(config *config.Config) {
	var err error
	msg.MgwConfig = config
	msg.AmqpURIArray = msg.RetrieveAMQPURLList()
	bindingKeys := []string{notification, keymanager, tokenRevocation, throttleData}

	logger.LoggerInternalMsg.Infof("dialing %q", msg.MaskURL(msg.AmqpURIArray[0].URL)+"/")
	msg.RabbitConn, err = msg.ConnectToRabbitMQ(msg.AmqpURIArray[0].URL + "/")
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
				if msg.Lifetime > 0 {
					logger.LoggerInternalMsg.Debugf("running %s events for %s", key, msg.Lifetime)
					time.Sleep(msg.Lifetime)
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

func handleEvent(c *msg.Consumer, key string) (error){
	var err error

	logger.LoggerInternalMsg.Debugf("got Connection, getting Channel for %s events", key)
	c.Channel, err = c.Conn.Channel()
	if err != nil {
		//return nil, fmt.Errorf("Channel: %s", err)
		logger.LoggerInternalMsg.Errorf("Channel: %s", err)
	}

	logger.LoggerInternalMsg.Debugf("got Channel, declaring Exchange (%q)", msg.Exchange)
	if err = c.Channel.ExchangeDeclare(
		msg.Exchange,     // name of the exchange
		msg.ExchangeType, // type
		true,             // durable
		false,            // delete when complete
		false,            // internal
		false,            // noWait
		nil,              // arguments
	); err != nil {
		logger.LoggerInternalMsg.Errorf("Exchange Declare: %s", err)
		//return nil, fmt.Errorf("Exchange Declare: %s", err)
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
		logger.LoggerInternalMsg.Errorf("Queue Declare: %s", err)
		//return nil, fmt.Errorf("Queue Declare: %s", err)
	}

	logger.LoggerInternalMsg.Debugf("declared Queue (%q %d messages, %d consumers), binding to Exchange (key %q)",
		queue.Name, queue.Messages, queue.Consumers, key)

	if err = c.Channel.QueueBind(
		queue.Name,   // name of the queue
		key,          // bindingKey
		msg.Exchange, // sourceExchange
		false,        // noWait
		nil,          // arguments
	); err != nil {
		logger.LoggerInternalMsg.Errorf("Queue Bind: %s", err)
		//return nil, fmt.Errorf("Queue Bind: %s", err)
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
	return err
}
