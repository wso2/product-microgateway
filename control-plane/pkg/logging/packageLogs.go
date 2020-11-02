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
package logging

import (
	"io"
	"os"

	"github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/configs"
)

/**
 * Map the log level strings to logrus log levels.
 *
 * @param pkgLevel   Package level as a string
 * @return logrus.Level Logrus log level
 */
func logLevelMapper(pkgLevel string) logrus.Level {
	logLevel := DEFAULT_LOG_LEVEL
	switch pkgLevel {
	case LEVEL_WARN:
		logLevel = logrus.WarnLevel
	case LEVEL_DEBUG:
		logLevel = logrus.DebugLevel
	case LEVEL_ERROR:
		logLevel = logrus.ErrorLevel
	case LEVEL_INFO:
		logLevel = logrus.InfoLevel
	case LEVEL_FATAL:
		logLevel = logrus.FatalLevel
	case LEVEL_PANIC:
		logLevel = logrus.PanicLevel
	}

	return logLevel
}

/**
 * Initialise the package loggers.
 * If the package log level is defined in the log_config.toml file, it override the
 * root log level.
 *
 * @param pkgName   Package name
 * @return *logrus.Logger Reference for the logger instance
 */
func InitPackageLogger(pkgName string) *logrus.Logger {

	pkgLogLevel := DEFAULT_LOG_LEVEL //default log level
	isPackegeLevelDefined := false

	logger := logrus.New()
	logger.SetReportCaller(true)

	formatter := loggerFromat()
	logger.SetFormatter(formatter)

	logConf, errReadConfig := configs.ReadLogConfigs()
	if errReadConfig != nil {
		logger.Error("Error loading log configuration. ", errReadConfig)
	}

	// Create the log file if doesn't exist. And append to it if it already exists.
	_, err := os.OpenFile(logConf.Logfile, os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0644)

	if err != nil {
		// Cannot open log file. Logging to stderr
		logger.Error("failed to open logfile", err)
		logger.SetOutput(os.Stdout)
	} else {
		//log output set to stdout and file
		multiWriter := io.MultiWriter(os.Stdout, setLogRotation(logConf.Logfile))
		logger.SetOutput(multiWriter)
	}

	for _, pkg := range logConf.Pkg {
		if pkg.Name == pkgName {
			pkgLogLevel = logLevelMapper(pkg.LogLevel)
			isPackegeLevelDefined = true
			break
		}
	}

	if !isPackegeLevelDefined {
		pkgLogLevel = logLevelMapper(logConf.LogLevel)
	}

	logger.SetLevel(pkgLogLevel)
	return logger
}
