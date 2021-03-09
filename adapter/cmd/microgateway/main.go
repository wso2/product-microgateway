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

// Package main is the main packages
package main

import (
	"os"

	logger "github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/config"
	_ "github.com/wso2/micro-gw/internal/logging"
	"github.com/wso2/micro-gw/internal/mgw"
)

func main() {

	var file string
	if len(os.Args) > 1 {
		file = os.Args[1]
		logger.Debug(file)
	}
	startMicroGateway(os.Args)
}

func initServer() error {
	return nil
}

// startMicroGateway reads the configuration files and then start the adapter components.
// Commandline arguments needs to be provided as args
func startMicroGateway(args []string) {

	logger.Info("Starting Microgateway")
	err := initServer()
	if err != nil {
		logger.Fatal("Error starting the adapter", err)
	}
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.Fatal("Error loading configuration. ", errReadConfig)
	}
	mgw.Run(conf)
}
