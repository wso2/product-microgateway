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
package microgateway

import (
	logger "github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/configs"
	"github.com/wso2/micro-gw/internal/pkg/mgw"
)

func initServer() error {
	return nil
}

func StartMicroGateway(args []string) {

	logger.Info("Starting Microgateway")
	err := initServer()
	if err != nil {
		logger.Fatal("Error starting the control plane", err)
	}
	conf, errReadConfig := configs.ReadConfigs()
	if errReadConfig != nil {
		logger.Fatal("Error loading configuration. ", errReadConfig)
	}
	mgw.Run(conf)
}
