/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package confTypes

import (
	"sync"
	"time"
)

/**
* Experimenting asynchronous communication between go routines using channels
* This uses singleton pattern where creating a single channel for communication
*
* To get a instance of the channel for a data publisher go routine
*  `publisher := NewSender()`
*
* Create a receiver channel in worker go routine
* receiver := NewReceiver()
*
* From publisher go routine, feed string value to the channel
* publisher<- "some value"
*
* In worker go routine, read the value sent by the publisher
* message := <-receiver
 */
var once sync.Once

var (
	C chan string // better to be interface{} type which could send any type of data.
)

func NewSender() chan string {
	once.Do(func() {
		C = make(chan string)
	})
	return C
}

func NewReceiver() chan string {
	once.Do(func() {
		C = make(chan string)
	})
	return C
}

// The mgw configuration struct.
type Config struct {
	// The server parameters.
	Server struct {
		IP             string
		Port           string
		PublicKeyPath  string
		PrivateKeyPath string
		Username       string
		Password       string
	}

	//envoy proxy configuration
	Envoy struct {
		ListenerAddress         string
		ListenerPort            uint32
		ClusterTimeoutInSeconds time.Duration
		ListenerCertPath        string
		ListenerKeyPath         string
		ListenerTlsEnabled      bool
	}
}
