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
	onceConfigRead    sync.Once
	onceLogConfigRead sync.Once
	onceGetMgwHome    sync.Once
	adapterConfig     *Config
	adapterLogConfig  *LogConfig
	mgwHome           string
	e                 error
)

const (
	// The environtmental variable which represents the path of the distribution in host machine.
	mgwHomeEnvVariable = "MGW_HOME"
	// RelativeConfigPath is the relative file path where the configuration file is.
	relativeConfigPath = "/conf/config.toml"
	// RelativeLogConfigPath is the relative file path where the log configuration file is.
	relativeLogConfigPath = "/conf/log_config.toml"
)

// ReadConfigs implements adapter configuration read operation. The read operation will happen only once, hence
// the consistancy is ensured.
//
// If the "MGW_HOME" variable is set, the configuration file location would be picked relative to the
// variable's value ("/conf/config.toml"). otherwise, the "MGW_HOME" variable would be set to the directory
// from where the executable is called from.
//
// Returns the configuration object mapped from the configuration file during the startup.
func ReadConfigs() (*Config, error) {
	onceConfigRead.Do(func() {
		adapterConfig = new(Config)
		_, err := os.Stat(GetMgwHome() + relativeConfigPath)
		if err != nil {
			logger.Fatal("Configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + relativeConfigPath)
		if readErr != nil {
			logger.Fatal("Error reading configurations. ", readErr)
			return
		}
		parseErr := toml.Unmarshal(content, adapterConfig)
		if parseErr != nil {
			logger.Fatal("Error parsing the configuration ", parseErr)
			return
		}
	})
	return adapterConfig, e
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
		_, err := os.Stat(GetMgwHome() + relativeLogConfigPath)
		if err != nil {
			logger.Fatal("Log configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + relativeLogConfigPath)
		if readErr != nil {
			logger.Fatal("Error reading log configurations. ", readErr)
			return
		}
		parseErr := toml.Unmarshal(content, adapterLogConfig)
		if parseErr != nil {
			logger.Fatal("Error parsing the log configuration ", parseErr)
		}

	})
	return adapterLogConfig, e
}

// ReadConsulConfig reads configuration
// if a config file(consul_config.toml) is not found ignore.
//func ReadConsulConfig() (*Consul, error) {
//		onceConsulConfig.Do(func() {
//		consulGlobalConfig = new(Consul)
//		_, err := os.Stat(GetMgwHome() + consulConfigPath)
//		if err != nil {
//			//A configuration file not found for consul service discovery
//			return
//		}
//		content, readErr := ioutil.ReadFile(mgwHome + consulConfigPath)
//		if readErr != nil {
//			logger.Fatal("Error reading configurations for consul config: ", readErr)
//		}
//		_, errConsul = toml.Decode(string(content), consulGlobalConfig)
//	})
//	logger.Debugln("Consul config Loaded: ", consulGlobalConfig)
//	return consulGlobalConfig, errConsul
//}

// ClearLogConfigInstance removes the existing configuration.
// Then the log configuration can be re-initialized.
func ClearLogConfigInstance() {
	onceLogConfigRead = sync.Once{}
}

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
