/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 */

package service

import (
	"fmt"
	"os"
	"sync"

	toml "github.com/pelletier/go-toml/v2"
)

type Config struct {
	Server Server `mapstructure:"server"`
	Http   Http   `mapstructure:"http"`
	Log    Log    `mapstructure:"log"`
}

type Server struct {
	Port           int    `mapstructure:"port"`
	Host           string `mapstructure:"host"`
	KeyPath        string `mapstructure:"keyPath"`
	CertPath       string `mapstructure:"certPath"`
	Secure         bool   `mapstructure:"secure"`
	ReadTimeout    int    `mapstructure:"readTimeout"`
	WriteTimeout   int    `mapstructure:"writeTimeout"`
	IdleTimeout    int    `mapstructure:"idleTimeout"`
	MaxHeaderBytes int    `mapstructure:"maxHeaderBytes"`
}

type Http struct {
	Insecure        bool `mapstructure:"insecure"`
	MaxIdleConns    int  `mapstructure:"maxIdleConns"`
	IdleConnTimeout int  `mapstructure:"idleConnTimeout"`
}

type Log struct {
	Debug bool `mapstructure:"debug"`
}

var (
	config     *Config
	onceConfig sync.Once
	errConfig  error
)

func InitConfig() (*Config, error) {
	onceConfig.Do(func() {
		data, err := os.ReadFile("config.toml")
		if err != nil {
			logger.Error("Failed to read config file", "error", err)
			errConfig = err
			return
		}
		err = toml.Unmarshal(data, &config)
		if err != nil {
			logger.Error("Failed to unmarshal config file", "error", err)
			errConfig = err
			return
		}
		err = validateConfig()
		if err != nil {
			logger.Error("Invalid config file", "error", err)
			errConfig = err
			return
		}
	})
	return config, errConfig
}

func GetConfig() *Config {
	if config == nil {
		return nil
	}
	return config
}

func validateConfig() error {
	if config.Server.Port == 0 {
		return fmt.Errorf("server port is not set")
	}
	if config.Server.Host == "" {
		return fmt.Errorf("server host is not set")
	}
	if config.Server.KeyPath == "" {
		return fmt.Errorf("server key is not set")
	}
	if config.Server.CertPath == "" {
		return fmt.Errorf("server cert is not set")
	}
	if config.Server.ReadTimeout == 0 {
		config.Server.ReadTimeout = 10
	}
	if config.Server.WriteTimeout == 0 {
		config.Server.WriteTimeout = 20
	}
	if config.Server.IdleTimeout == 0 {
		config.Server.IdleTimeout = 60
	}
	if config.Server.MaxHeaderBytes == 0 {
		config.Server.MaxHeaderBytes = 102400 // 100KB
	}
	return nil
}
