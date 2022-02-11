/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package config

import (
	"github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
)

// package name constants
const (
	pkgConfig = "github.com/wso2/product-microgateway/adapter/config"
)

// logger package references
var (
	loggerConfig logging.Log
)

func init() {
	UpdateLoggers()
}

// UpdateLoggers initializes the logger package references
func UpdateLoggers() {
	loggerConfig = logging.InitPackageLogger(pkgConfig)
	logrus.Info("Updated loggers")
}
