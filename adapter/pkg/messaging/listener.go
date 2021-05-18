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
	"time"

	"github.com/streadway/amqp"
	"github.com/wso2/adapter/config"
	logger "github.com/wso2/adapter/pkg/loggers"
)

var (
	// MgwConfig represents the configuration
	MgwConfig *config.Config
	// Lifetime represents the lifetime of an event
	Lifetime = 0 * time.Second
	amqpURI  string
	// RabbitConn represents the ampq connection
	RabbitConn       *amqp.Connection
	rabbitCloseError chan *amqp.Error
	// AmqpURIArray represents an array of AmqpFailoverURL objects
	AmqpURIArray = make([]AmqpFailoverURL, 0)
)

const (
	// Exchange represents the  string constant: "amq.topic"
	Exchange string = "amq.topic"
	// ExchangeType represents the  string constant: "topic"
	ExchangeType string = "topic"
	consumerTag  string = "jms-consumer"
)

// StartConsumer for provided key consume data
func StartConsumer(key string) (*Consumer) {
	c := &Consumer{
		Conn:    nil,
		Channel: nil,
		Tag:     consumerTag + "-key",
		Done:    make(chan error),
	}

	c.Conn = RabbitConn
	go func() {
		c.reconnect(key)
	}()

	return c
}

// Shutdown when error happens
func (c *Consumer) Shutdown() error {
	// will close() the deliveries channel
	if err := c.Channel.Cancel(c.Tag, true); err != nil {
		return fmt.Errorf("Consumer cancel failed: %s", err)
	}

	if err := c.Conn.Close(); err != nil {
		return fmt.Errorf("AMQP connection close error: %s", err)
	}

	defer logger.LoggerMsg.Infof("AMQP shutdown OK")

	// wait for handle() to exit
	return <-c.Done
}

// Consumer struct represents the structure of a consumer object
type Consumer struct {
	Conn    *amqp.Connection
	Channel *amqp.Channel
	Tag     string
	Done    chan error
}
