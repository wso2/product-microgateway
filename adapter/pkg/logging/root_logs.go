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

package logging

import (
	"github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/adapter/pkg/config"
	lumberjack "gopkg.in/natefinch/lumberjack.v2"
	"io"
	"os"
)

func init() {
	logConf := config.ReadLogConfigs()

	err := initGlobalLogger(logConf.Logfile)
	if err != nil {
		logrus.Error("Failed to initialize logging")
	}
}

// initGlobalLogger initialises the global logger.
// If the package root level is defined in the log_config.toml file, it is set.
// otherwise log level is assigned to default lo level.
func initGlobalLogger(filename string) error {

	// Create the log file if doesn't exist. And append to it if it already exists.
	_, err := os.OpenFile(filename, os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0644)

	logrus.SetReportCaller(true)
	formatter := logFormatter
	logrus.SetFormatter(formatter)
	logrus.AddHook(&errorHook{})

	if err != nil {
		// Cannot open log file. Logging to stderr
		logrus.Warn("failed to open logfile", err)
		logrus.SetOutput(os.Stdout)

	} else {
		// log output set to stdout and file
		multiWriter := io.MultiWriter(os.Stdout, setLogRotation(filename))
		logrus.SetOutput(multiWriter)
	}

	logConf := config.ReadLogConfigs()
	logrus.SetLevel(logLevelMapper(logConf.LogLevel))
	return err
}

// setLogRotation initiates the log rotation feature using lumberjack library.
// All the rotation params reads for the configs and if it occurs
// a error, all the params are set to the default values.
func setLogRotation(filename string) io.Writer {
	logConf := config.ReadLogConfigs()
	var rotationWriter io.Writer

	rotationWriter = &lumberjack.Logger{
		Filename:   filename,
		MaxSize:    logConf.Rotation.MaxSize, // megabytes
		MaxBackups: logConf.Rotation.MaxBackups,
		MaxAge:     logConf.Rotation.MaxAge,   //days
		Compress:   logConf.Rotation.Compress, // disabled by default
	}

	return rotationWriter
}
