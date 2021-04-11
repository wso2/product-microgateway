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
	"fmt"
	"io/ioutil"
	"os"
	"reflect"
	"regexp"
	"strings"
	"sync"

	toml "github.com/pelletier/go-toml"
	logger "github.com/sirupsen/logrus"
)

var (
	onceConfigRead      sync.Once
	onceGetDefaultVhost sync.Once
	onceLogConfigRead   sync.Once
	onceGetMgwHome      sync.Once
	adapterConfig       *Config
	defaultVhost        map[string]string
	adapterLogConfig    *LogConfig
	mgwHome             string
	e                   error
)

// DefaultGatewayName represents the name of the default gateway
const DefaultGatewayName = "Production and Sandbox" // TODO: should be changed to "Default"
// DefaultGatewayVHost represents the default vhost of default gateway environment if it is not configured
const DefaultGatewayVHost = "localhost" // TODO (renuka): check this with pubuduG and raji: do we want this?
// for /testtoken and /health check, if user not configured default env, we have no vhost

const (
	// The environtmental variable which represents the path of the distribution in host machine.
	mgwHomeEnvVariable = "MGW_HOME"
	// RelativeConfigPath is the relative file path where the configuration file is.
	relativeConfigPath = "/conf/config.toml"
	// RelativeLogConfigPath is the relative file path where the log configuration file is.
	relativeLogConfigPath = "/conf/log_config.toml"
	// The prefix used when configs should be read from environment variables.
	envConfigPrefix = "$env"
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
		resolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Adapter)).Elem())
		resolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.ControlPlane)).Elem())
		resolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Security)).Elem())
	})
	return adapterConfig, e
}

// GetDefaultVhost returns the default vhost of given environment read from Adapter
// configurations. Store the configuration in a map, so do not want to loop through
// the config value Config.Adapter.VhostMapping
func GetDefaultVhost(environment string) (string, bool, error) {
	var err error
	onceGetDefaultVhost.Do(func() {
		defaultVhost = make(map[string]string)
		configs, errConf := ReadConfigs()
		if errConf != nil {
			err = errConf
			return
		}
		for _, gateway := range configs.Adapter.VhostMapping {
			defaultVhost[gateway.Environment] = gateway.Vhost
		}
	})
	vhost, ok := defaultVhost[environment]
	if !ok && environment == DefaultGatewayName {
		return DefaultGatewayVHost, true, nil
	}
	return vhost, ok, err
}

// resolveConfigEnvValues looks for the string type config values which should be read from environment variables
// and replace the respective config values from environment variable.
func resolveConfigEnvValues(v reflect.Value) {
	s := v
	for fieldNum := 0; fieldNum < s.NumField(); fieldNum++ {
		field := s.Field(fieldNum)
		if field.Kind() == reflect.String && strings.Contains(fmt.Sprint(field.Interface()), envConfigPrefix) {
			field.SetString(resolveEnvValue(fmt.Sprint(field.Interface())))
		}
		if reflect.TypeOf(field.Interface()).Kind() == reflect.Slice {
			for index := 0; index < field.Len(); index++ {
				if field.Index(index).Kind() == reflect.Struct {
					resolveConfigEnvValues(field.Index(index).Addr().Elem())
				} else if field.Index(index).Kind() == reflect.String && strings.Contains(field.Index(index).String(), envConfigPrefix) {
					field.Index(index).SetString(resolveEnvValue(field.Index(index).String()))
				}
			}
		}
		if field.Kind() == reflect.Struct {
			resolveConfigEnvValues(field.Addr().Elem())
		}
	}
}

func resolveEnvValue(value string) string {
	re := regexp.MustCompile(`(?s)\{(.*)}`) // regex to get everything in between curly brackets
	m := re.FindStringSubmatch(value)
	if len(m) > 1 {
		envValue, exists := os.LookupEnv(m[1])
		if exists {
			return strings.ReplaceAll(re.ReplaceAllString(value, envValue), envConfigPrefix, "")
		}
	}
	return value
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
