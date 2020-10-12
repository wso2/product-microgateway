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
package configs

import (
	"io/ioutil"
	"os"
	"sync"

	"github.com/BurntSushi/toml"
	logger "github.com/sirupsen/logrus"
	config "github.com/wso2/micro-gw/internal/configs/confTypes"
)

var (
	once_c     sync.Once
	once_lc    sync.Once
	configs    *config.Config
	logConfigs *config.LogConfig
	e          error
)

/**
 * Read the control plane main configs.
 *
 * @return *config.Config Reference for config instance
 * @return *error Error
 */
func ReadConfigs() (*config.Config, error) {
	once_c.Do(func() {
		configs = new(config.Config)
		mgwHome, _ := os.Getwd()
		// logger.Info("MGW_HOME: ", mgwHome)
		_, err := os.Stat(mgwHome + "/resources/conf/config.toml")
		if err != nil {
			logger.Fatal("Configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + "/resources/conf/config.toml")
		if readErr != nil {
			logger.Fatal("Error reading configurations. ", readErr)
		}
		_, e = toml.Decode(string(content), configs)
	})

	return configs, e
}

/**
 * Read the control plane log configs.
 *
 * @return *config.LogConfig Reference for log config instance
 * @return *error Error
 */
func ReadLogConfigs() (*config.LogConfig, error) {
	once_lc.Do(func() {
		logConfigs = new(config.LogConfig)
		mgwHome, _ := os.Getwd()
		_, err := os.Stat(mgwHome + "/resources/conf/log_config.toml")
		if err != nil {
			logger.Fatal("Log configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + "/resources/conf/log_config.toml")
		if readErr != nil {
			logger.Fatal("Error reading log configurations. ", readErr)
		}
		_, e = toml.Decode(string(content), logConfigs)

	})

	return logConfigs, e
}

/**
 * Clear the singleton log config instance for the hot update.
 *
 */
func ClearLogConfigInstance() {
	once_lc = sync.Once{}
}
