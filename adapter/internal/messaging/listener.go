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
	"github.com/wso2/adapter/internal/health"
	"strings"
	"time"

	"github.com/streadway/amqp"
	"github.com/wso2/adapter/config"
	logger "github.com/wso2/adapter/loggers"
)

var (
	mgwConfig        *config.Config
	lifetime         = 0 * time.Second
	amqpURI          string
	rabbitConn       *amqp.Connection
	rabbitCloseError chan *amqp.Error
	amqpURIArray     = make([]amqpFailoverURL, 0)
)

const (
	exchange        string = "amq.topic"
	exchangeType    string = "topic"
	consumerTag     string = "jms-consumer"
	notification    string = "notification"
	keymanager      string = "keymanager"
	tokenRevocation string = "tokenRevocation"
	throttleData    string = "throttleData"
)

// ProcessEvents to pass event consumption
func ProcessEvents(config *config.Config) {
	var err error
	mgwConfig = config
	amqpURIArray = retrieveAMQPURLList()
	bindingKeys := []string{notification, keymanager, tokenRevocation, throttleData}

	logger.LoggerMsg.Infof("dialing %q", amqpURIArray[0].url+"/")
	rabbitConn, err = connectToRabbitMQ(amqpURIArray[0].url + "/")
	isHealthy := err == nil
	logger.LoggerMsg.Debugf("Updating startup health status of EventHubAMQPConsumerService. Is healthy: %v", isHealthy)
	health.EventHubAMQPConsumerService.SetStatus(isHealthy)

	if err == nil {
		for i, key := range bindingKeys {
			logger.LoggerMsg.Infof("Establishing consumer index %v for key %s ", i, key)
			go func(key string) {
				c, err := StartConsumer(key)
				if err != nil {
					logger.LoggerMsg.Fatalf("%s", err)
				}
				if lifetime > 0 {
					logger.LoggerMsg.Debugf("running %s events for %s", key, lifetime)
					time.Sleep(lifetime)
				} else {
					logger.LoggerMsg.Infof("process of receiving %s events running forever", key)
					select {}
				}

				logger.LoggerMsg.Infof("shutting down")
				if err := c.Shutdown(); err != nil {
					logger.LoggerMsg.Fatalf("error during shutdown: %s", err)
				}
			}(key)
		}
	}
}

// StartConsumer for provided key consume data
func StartConsumer(key string) (*Consumer, error) {
	var err error
	c := &Consumer{
		conn:    nil,
		channel: nil,
		tag:     consumerTag + "-key",
		done:    make(chan error),
	}

	c.conn = rabbitConn
	go func() {
		c.reconnect(key)
	}()

	logger.LoggerMsg.Debugf("got Connection, getting Channel for %s events", key)
	c.channel, err = c.conn.Channel()
	if err != nil {
		return nil, fmt.Errorf("Channel: %s", err)
	}

	logger.LoggerMsg.Debugf("got Channel, declaring Exchange (%q)", exchange)
	if err = c.channel.ExchangeDeclare(
		exchange,     // name of the exchange
		exchangeType, // type
		true,         // durable
		false,        // delete when complete
		false,        // internal
		false,        // noWait
		nil,          // arguments
	); err != nil {
		return nil, fmt.Errorf("Exchange Declare: %s", err)
	}

	logger.LoggerMsg.Infof("declared Exchange, declaring Queue %q", key+"queue")
	queue, err := c.channel.QueueDeclare(
		"",    // name of the queue
		false, // durable
		true,  // delete when usused
		false, // exclusive
		false, // noWait
		nil,   // arguments
	)
	if err != nil {
		return nil, fmt.Errorf("Queue Declare: %s", err)
	}

	logger.LoggerMsg.Debugf("declared Queue (%q %d messages, %d consumers), binding to Exchange (key %q)",
		queue.Name, queue.Messages, queue.Consumers, key)

	if err = c.channel.QueueBind(
		queue.Name, // name of the queue
		key,        // bindingKey
		exchange,   // sourceExchange
		false,      // noWait
		nil,        // arguments
	); err != nil {
		return nil, fmt.Errorf("Queue Bind: %s", err)
	}
	logger.LoggerMsg.Infof("Queue bound to Exchange, starting Consume (consumer tag %q) events", c.tag)
	deliveries, err := c.channel.Consume(
		queue.Name, // name
		c.tag,      // consumerTag,
		false,      // noAck
		false,      // exclusive
		false,      // noLocal
		false,      // noWait
		nil,        // arguments
	)
	if err != nil {
		return nil, fmt.Errorf("Queue Consume: %s", err)
	}

	if strings.EqualFold(key, notification) {
		go handleNotification(deliveries, c.done)
	} else if strings.EqualFold(key, keymanager) {
		go handleKMConfiguration(deliveries, c.done)
	} else if strings.EqualFold(key, tokenRevocation) {
		go handleTokenRevocation(deliveries, c.done)
	} else if strings.EqualFold(key, throttleData) {
		go handleThrottleData(deliveries, c.done)
	}

	return c, nil
}

// Shutdown when error happens
func (c *Consumer) Shutdown() error {
	// will close() the deliveries channel
	if err := c.channel.Cancel(c.tag, true); err != nil {
		return fmt.Errorf("Consumer cancel failed: %s", err)
	}

	if err := c.conn.Close(); err != nil {
		return fmt.Errorf("AMQP connection close error: %s", err)
	}

	defer logger.LoggerMsg.Infof("AMQP shutdown OK")

	// wait for handle() to exit
	return <-c.done
}

// Consumer struct
type Consumer struct {
	conn    *amqp.Connection
	channel *amqp.Channel
	tag     string
	done    chan error
}
