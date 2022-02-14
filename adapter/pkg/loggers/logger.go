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

// Package loggers contains the package references for log messages
// If a new package is introduced, the corresponding logger reference is need to be created as well.
package loggers

import (
	"github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
)

/* loggers should be initiated only for the main packages
 ********** Don't initiate loggers for sub packages ****************

When you add a new logger instance add the related package name as a constant
*/

// package name constants
const (
	pkgAuth		= "github.com/wso2/product-microgateway/adapter/pkg/auth"
	pkgMsg      = "github.com/wso2/product-microgateway/adapter/pkg/messaging"
	pkgHealth   = "github.com/wso2/product-microgateway/adapter/pkg/health"
	pkgTLSUtils = "github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
	pkgAdapter  = "github.com/wso2/product-microgateway/adapter/pkg/adapter"
	pkgSync     = "github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

// logger package references
var (
	LoggerAuth	   logging.Log
	LoggerMsg      logging.Log
	LoggerHealth   logging.Log
	LoggerTLSUtils logging.Log
	LoggerAdapter  logging.Log
	LoggerSync     logging.Log
)

func init() {
	UpdateLoggers()
}

// UpdateLoggers initializes the logger package references
func UpdateLoggers() {
	LoggerAuth = logging.InitPackageLogger(pkgAuth)
	LoggerMsg = logging.InitPackageLogger(pkgMsg)
	LoggerHealth = logging.InitPackageLogger(pkgHealth)
	LoggerTLSUtils = logging.InitPackageLogger(pkgTLSUtils)
	LoggerAdapter = logging.InitPackageLogger(pkgAdapter)
	LoggerSync = logging.InitPackageLogger(pkgSync)
	logrus.Info("Updated loggers")
}
