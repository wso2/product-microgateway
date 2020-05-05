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
	"github.com/BurntSushi/toml"
	"github.com/sirupsen/logrus"
	"github.com/wso2/envoy-control-plane/internal/pkg/mgw"
	"os"

	config "github.com/wso2/envoy-control-plane/internal/pkg/config"
	"io/ioutil"
	"log"
)

func initServer() error {
	return nil
}

func readConfigs() (*config.Config, error) {
	anc := new(config.Config)
	mgwHome, _ := os.Getwd()
	logrus.Info("MGW_HOME: ", mgwHome)
	_, err := os.Stat(mgwHome + "/resources/conf/config.toml")
	if err != nil {
		log.Panic("Configuration file not found.", err)
	}
	content, readErr := ioutil.ReadFile(mgwHome + "/resources/conf/config.toml")
	if readErr != nil {
		log.Panic("Error reading configurations. ", readErr)
	}
	_, e := toml.Decode(string(content), anc)
	return anc, e
}

func StartMicroGateway(args []string) {

	err := initServer()
	if err != nil {
		log.Panic("Error starting the control plane", err)
	}
	conf, errReadConfig := readConfigs()
	if errReadConfig != nil {
		log.Panic("Error loading configuration. ", errReadConfig)
	}
	mgw.Run(conf)
}
