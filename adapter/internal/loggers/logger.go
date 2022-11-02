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
	pkgAPI                  = "github.com/wso2/product-microgateway/adapter/internal/api"
	pkgAuth                 = "github.com/wso2/product-microgateway/adapter/internal/auth"
	pkgMgw                  = "github.com/wso2/product-microgateway/adapter/internal/adapter"
	pkgOasparser            = "github.com/wso2/product-microgateway/adapter/internal/oasparser"
	pkgInterceptor          = "github.com/wso2/product-microgateway/adapter/internal/interceptor"
	pkgXds                  = "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	pkgSync                 = "github.com/wso2/product-microgateway/adapter/internal/synchronizer"
	pkgInternalMsg          = "github.com/wso2/product-microgateway/adapter/internal/messaging"
	pkgSvcDiscovery         = "github.com/wso2/product-microgateway/adapter/internal/svcDiscovery"
	pkgSubscription         = "github.com/wso2/product-microgateway/adapter/internal/subscription"
	pkgRouterXdsCallbacks   = "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/routercallbacks"
	pkgEnforcerXdsCallbacks = "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/enforcercallbacks"
	pkgGA                   = "github.com/wso2/product-microgateway/adapter/internal/ga"
	pkgNotifier             = "github.com/wso2/product-microgateway/adapter/internal/notifier"
	pkgSourceWatcher        = "github.com/wso2/product-microgateway/adapter/internal/sourcewatcher"
	pkgOperator             = "github.com/wso2/product-microgateway/adapter/internal/operator"
)

// logger package references
var (
	LoggerAPI                  logging.Log
	LoggerAuth                 logging.Log
	LoggerMgw                  logging.Log
	LoggerOasparser            logging.Log
	LoggerInterceptor          logging.Log
	LoggerXds                  logging.Log
	LoggerSync                 logging.Log
	LoggerInternalMsg          logging.Log
	LoggerSvcDiscovery         logging.Log
	LoggerSubscription         logging.Log
	LoggerRouterXdsCallbacks   logging.Log
	LoggerEnforcerXdsCallbacks logging.Log
	LoggerGA                   logging.Log
	LoggerNotifier             logging.Log
	LoggerSourceWatcher        logging.Log
	LoggerOperator             logging.Log
)

func init() {
	UpdateLoggers()
}

// UpdateLoggers initializes the logger package references
func UpdateLoggers() {

	LoggerAPI = logging.InitPackageLogger(pkgAPI)
	LoggerAuth = logging.InitPackageLogger(pkgAuth)
	LoggerMgw = logging.InitPackageLogger(pkgMgw)
	LoggerOasparser = logging.InitPackageLogger(pkgOasparser)
	LoggerInterceptor = logging.InitPackageLogger(pkgInterceptor)
	LoggerXds = logging.InitPackageLogger(pkgXds)
	LoggerSync = logging.InitPackageLogger(pkgSync)
	LoggerInternalMsg = logging.InitPackageLogger(pkgInternalMsg)
	LoggerSvcDiscovery = logging.InitPackageLogger(pkgSvcDiscovery)
	LoggerSubscription = logging.InitPackageLogger(pkgSubscription)
	LoggerRouterXdsCallbacks = logging.InitPackageLogger(pkgRouterXdsCallbacks)
	LoggerEnforcerXdsCallbacks = logging.InitPackageLogger(pkgEnforcerXdsCallbacks)
	LoggerGA = logging.InitPackageLogger(pkgGA)
	LoggerNotifier = logging.InitPackageLogger(pkgNotifier)
	LoggerSourceWatcher = logging.InitPackageLogger(pkgSourceWatcher)
	LoggerOperator = logging.InitPackageLogger(pkgOperator)
	logrus.Info("Updated loggers")
}
