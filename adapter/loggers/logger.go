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

// Package loggers contains the package references for log messages
// If a new package is introduced, the corresponding logger reference is need to be created as well.
package loggers

import (
	"github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/internal/logging"
)

/* loggers should be initiated only for the main packages
 ********** Don't initiate loggers for sub packages ****************

When you add a new logger instance add the related package name as a constant
*/

// package name constants
const (
	pkgAPI          = "github.com/wso2/micro-gw/internal/api"
	pkgAuth         = "github.com/wso2/micro-gw/pkg/auth"
	pkgMgw          = "github.com/wso2/micro-gw/internal/adapter"
	pkgOasparser    = "github.com/wso2/micro-gw/internal/oasparser"
	pkgXds          = "github.com/wso2/micro-gw/internal/discovery/xds"
	pkgSync         = "github.com/wso2/micro-gw/pkg/synchronizer"
	pkgMsg          = "github.com/wso2/micro-gw/messaging"
	pkgSvcDiscovery = "github.com/wso2/micro-gw/internal/svcDiscovery"
	pkgTLSUtils     = "github.com/wso2/micro-gw/internal/tlsutils"
	pkgSubscription = "github.com/wso2/micro-gw/internal/subscription"
	pkgXdsCallbacks = "github.com/wso2/micro-gw/internal/discovery/xds"
)

// logger package references
var (
	LoggerAPI          *logrus.Logger
	LoggerAuth         *logrus.Logger
	LoggerMgw          *logrus.Logger
	LoggerOasparser    *logrus.Logger
	LoggerXds          *logrus.Logger
	LoggerSync         *logrus.Logger
	LoggerMsg          *logrus.Logger
	LoggerSvcDiscovery *logrus.Logger
	LoggerTLSUtils     *logrus.Logger
	LoggerSubscription *logrus.Logger
	LoggerXdsCallbacks *logrus.Logger
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
	LoggerXds = logging.InitPackageLogger(pkgXds)
	LoggerSync = logging.InitPackageLogger(pkgSync)
	LoggerMsg = logging.InitPackageLogger(pkgMsg)
	LoggerSvcDiscovery = logging.InitPackageLogger(pkgSvcDiscovery)
	LoggerTLSUtils = logging.InitPackageLogger(pkgTLSUtils)
	LoggerSubscription = logging.InitPackageLogger(pkgSubscription)
	LoggerXdsCallbacks = logging.InitPackageLogger(pkgXdsCallbacks)
	logrus.Info("Updated loggers")
}
