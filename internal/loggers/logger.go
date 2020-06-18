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
package loggers

import (
	"github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/internal/pkg/logging"
)

/* loggers should be initiated only for the main packages
 ********** Don't initiate loggers for sub packages ****************

When you add a new logger instance add the related package name as a constant
 */


const (
	pkgApi = "github.com/wso2/micro-gw/internal/pkg/api"
	pkgAuth = "github.com/wso2/micro-gw/internal/pkg/auth"
	pkgMgw = "github.com/wso2/micro-gw/internal/pkg/mgw"
	pkgOasparser = "github.com/wso2/micro-gw/internal/pkg/oasparser"
)

var (
	LoggerApi          *logrus.Logger
	LoggerAuth         *logrus.Logger
	LoggerMgw          *logrus.Logger
	LoggerOasparser    *logrus.Logger
)

func init() {
	UpdateLoggers()
}

/**
 * Update the logger instances.
 *
 */
func UpdateLoggers() {
	logrus.Info("Updating loggers....")

	LoggerApi = logging.InitPackageLogger(pkgApi)
	LoggerAuth = logging.InitPackageLogger(pkgAuth)
	LoggerMgw = logging.InitPackageLogger(pkgMgw)
	LoggerOasparser = logging.InitPackageLogger(pkgOasparser)
}
