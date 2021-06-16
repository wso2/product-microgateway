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

// Package config contains the implementation and data structures related to configurations and
// configuration (log and adapter config) parsing. If a new configuration is introduced to the adapter
// configuration file, the corresponding change needs to be added to the relevant data stucture as well.
package config

import (
	"io/ioutil"
	"os"
	"strings"
	"sync"

	toml "github.com/pelletier/go-toml"
	logger "github.com/sirupsen/logrus"
)

var (
	onceGetMgwHome       sync.Once
	onceGetLogConfigPath sync.Once
	onceLogConfigRead    sync.Once
	adapterLogConfig     *LogConfig
	mgwHome              string
	logConfigPath        string
	e                    error
)

const (
	// The environtmental variable which represents the path of the distribution in host machine.
	mgwHomeEnvVariable = "MGW_HOME"
	// The environtmental variable which represents the path of the log_config.toml in host machine.
	logConfigPathEnvVariable = "LOG_CONFIG_PATH"
	// RelativeLogConfigPath is the relative file path where the log configuration file is.
	relativeLogConfigPath = "/conf/log_config.toml"
)

// GetMgwHome reads the MGW_HOME environmental variable and returns the value.
// This represent the directory where the distribution is located.
// If the env variable is not present, the directory from which the executable is triggered will be assigned.
func GetMgwHome() string {
	onceGetMgwHome.Do(func() {
		mgwHome = os.Getenv(mgwHomeEnvVariable)
		if len(strings.TrimSpace(mgwHome)) == 0 {
			mgwHome, _ = os.Getwd()
		}
	})
	return mgwHome
}

// getLogConfigPath reads the LOG_CONFIG_PATH environmental variable and returns the value.
// If the env variable is not available, returned value would be the combination of MGW_HOME
// env variable value + relative log config path
func getLogConfigPath() string {
	logConfigPath = os.Getenv(logConfigPathEnvVariable)
	if len(strings.TrimSpace(logConfigPath)) == 0 {
		//for backward compatibility
		logConfigPath = GetMgwHome() + relativeLogConfigPath
	}
	_, err := os.Stat(logConfigPath)
	if err != nil {
		logger.Fatal("Log configuration file not found.", err)
		panic(err)
	}
	return logConfigPath
}

// ReadLogConfigs implements adapter/proxy log-configuration read operation.The read operation will happen only once, hence
// the consistancy is ensured.
//
// If the "MGW_HOME" variable is set, the log configuration file location would be picked relative to the
// variable's value ("/conf/log_config.toml"). otherwise, the "MGW_HOME" variable would be set to the directory
// from where the executable is called from.
//
// Returns the log configuration object mapped from the configuration file during the startup.
func ReadLogConfigs() (*LogConfig, error) {
	onceLogConfigRead.Do(func() {
		adapterLogConfig = new(LogConfig)
		content, readErr := ioutil.ReadFile(getLogConfigPath())
		if readErr != nil {
			logger.Fatal("Error reading log configurations. ", readErr)
			panic(readErr)
		}
		parseErr := toml.Unmarshal(content, adapterLogConfig)
		if parseErr != nil {
			logger.Fatal("Error parsing the log configuration ", parseErr)
			panic(parseErr)
		}

	})
	return adapterLogConfig, e
}

// ClearLogConfigInstance removes the existing configuration.
// Then the log configuration can be re-initialized.
func ClearLogConfigInstance() {
	onceLogConfigRead = sync.Once{}
}
