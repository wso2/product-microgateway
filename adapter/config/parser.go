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
	"reflect"
	"strings"
	"sync"

	toml "github.com/pelletier/go-toml"
	logger "github.com/sirupsen/logrus"
	pkgconf "github.com/wso2/product-microgateway/adapter/pkg/config"
)

var (
	onceConfigRead      sync.Once
	onceGetDefaultVhost sync.Once
	adapterConfig       *Config
	defaultVhost        map[string]string
	e                   error
)

// DefaultGatewayName represents the name of the default gateway
const DefaultGatewayName = "Default"

// DefaultGatewayVHost represents the default vhost of default gateway environment if it is not configured
const DefaultGatewayVHost = "localhost" // TODO (renuka): check this with pubuduG and raji: do we want this?
// for /testtoken and /health check, if user not configured default env, we have no vhost

const (
	// The environtmental variable which represents the path of the distribution in host machine.
	mgwHomeEnvVariable = "MGW_HOME"
	// RelativeConfigPath is the relative file path where the configuration file is.
	relativeConfigPath = "/conf/config.toml"
)

// Constants related to utility functions
const (
	tenantDomainSeparator = "@"
	superTenantDomain     = "carbon.super"
)

// ReadConfigs implements adapter configuration read operation. The read operation will happen only once, hence
// the consistancy is ensured.
//
// If the "MGW_HOME" variable is set, the configuration file location would be picked relative to the
// variable's value ("/conf/config.toml"). otherwise, the "MGW_HOME" variable would be set to the directory
// from where the executable is called from.
//
// Returns the configuration object that is initialized with default values. Changes to the default
// configuration object is achieved through the configuration file.
func ReadConfigs() (*Config, error) {
	onceConfigRead.Do(func() {
		adapterConfig = defaultConfig
		_, err := os.Stat(pkgconf.GetMgwHome() + relativeConfigPath)
		if err != nil {
			logger.Fatal("Configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(pkgconf.GetMgwHome() + relativeConfigPath)
		if readErr != nil {
			logger.Fatal("Error reading configurations. ", readErr)
			return
		}
		parseErr := toml.Unmarshal(content, adapterConfig)
		if parseErr != nil {
			logger.Fatal("Error parsing the configuration ", parseErr)
			return
		}
		pkgconf.ResolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Adapter)).Elem(), "Adapter", true)
		pkgconf.ResolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.ControlPlane)).Elem(), "ControlPlane", true)
		pkgconf.ResolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Envoy)).Elem(), "Router", true)
		pkgconf.ResolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.GlobalAdapter)).Elem(), "GlobalAdapter", true)
		pkgconf.ResolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Enforcer)).Elem(), "Enforcer", false)
		pkgconf.ResolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Analytics)).Elem(), "Analytics", false)
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

// ReadLogConfigs implements adapter/proxy log-configuration read operation.The read operation will happen only once, hence
// the consistancy is ensured.
//
// If the "MGW_HOME" variable is set, the log configuration file location would be picked relative to the
// variable's value ("/conf/log_config.toml"). otherwise, the "MGW_HOME" variable would be set to the directory
// from where the executable is called from.
//
// Returns the log configuration object mapped from the configuration file during the startup.
func ReadLogConfigs() *pkgconf.LogConfig {
	return pkgconf.ReadLogConfigs()
}

// ClearLogConfigInstance removes the existing configuration.
// Then the log configuration can be re-initialized.
func ClearLogConfigInstance() {
	pkgconf.ClearLogConfigInstance()
}

// GetLogConfigPath returns the file location of the log-config path
func GetLogConfigPath() (string, error) {
	return pkgconf.GetLogConfigPath()
}

// GetMgwHome reads the MGW_HOME environmental variable and returns the value.
// This represent the directory where the distribution is located.
// If the env variable is not present, the directory from which the executable is triggered will be assigned.
func GetMgwHome() string {
	return pkgconf.GetMgwHome()
}

// GetControlPlaneConnectedTenantDomain returns the tenant domain of the user used to authenticate with event hub.
func GetControlPlaneConnectedTenantDomain() string {
	// Read configurations to get the control plane authenticated user
	conf, _ := ReadConfigs()

	// Populate data from the config
	cpTenantAdminUser := conf.ControlPlane.Username
	tenantDomain := strings.Split(cpTenantAdminUser, tenantDomainSeparator)
	if len(tenantDomain) > 1 {
		return tenantDomain[len(tenantDomain)-1]
	}
	return superTenantDomain
}
