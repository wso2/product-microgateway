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
	"fmt"
	"io"
	"os"
	"strings"

	"github.com/sirupsen/logrus"
	"github.com/wso2/adapter/config"
	lumberjack "gopkg.in/natefinch/lumberjack.v2"
)

type plainFormatter struct {
	TimestampFormat string
	LevelDesc       []string
}

func init() {
	logConf, errReadConfig := config.ReadLogConfigs()
	if errReadConfig != nil {
		logrus.Error("Error loading log configuration. ", errReadConfig)
	}

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
	formatter := loggerFormat()
	logrus.SetFormatter(formatter)

	if err != nil {
		// Cannot open log file. Logging to stderr
		logrus.Warn("failed to open logfile", err)
		logrus.SetOutput(os.Stdout)

	} else {
		// log output set to stdout and file
		multiWriter := io.MultiWriter(os.Stdout, setLogRotation(filename))
		logrus.SetOutput(multiWriter)
	}

	logConf, errReadConfig := config.ReadLogConfigs()
	if errReadConfig != nil {
		logrus.Error("Error loading configuration. ", errReadConfig)
		logrus.SetLevel(logLevelMapper(logConf.LogLevel))
	} else {
		logrus.SetLevel(defaultLogLevel)
	}

	return err
}

// loggerFormat initiates the log formatter.
func loggerFormat() *plainFormatter {

	formatter := new(plainFormatter)
	formatter.TimestampFormat = "2006-01-02 15:04:05"
	formatter.LevelDesc = []string{
		panicLevel,
		fatalLevel,
		errorLevel,
		warnLevel,
		infoLevel,
		debugLevel}

	return formatter
}

// formatFilePath retrieves only the last part from a path.
func formatFilePath(path string) string {
	arr := strings.Split(path, "/")
	return arr[len(arr)-1]
}

// Format sets a custom format for loggers.
func (f *plainFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	timestamp := fmt.Sprintf(entry.Time.Format(f.TimestampFormat))

	return []byte(fmt.Sprintf("%s %s [%s:%d] - [%s] [-] %s\n",
		timestamp,
		f.LevelDesc[entry.Level],
		formatFilePath(entry.Caller.File),
		entry.Caller.Line,
		formatFilePath(entry.Caller.Function), entry.Message)), nil
}

// setLogRotation initiates the log rotation feature using lumberjack library.
// All the rotation params reads for the configs and if it occurs
// a error, all the params are set to the default values.
func setLogRotation(filename string) io.Writer {
	logConf, errReadConfig := config.ReadLogConfigs()
	var rotationWriter io.Writer

	if errReadConfig != nil {
		logrus.Error("Error loading log configuration. ", errReadConfig)
		//set default values
		rotationWriter = &lumberjack.Logger{
			Filename:   filename,
			MaxSize:    10, // megabytes
			MaxBackups: 3,
			MaxAge:     2,    //days
			Compress:   true, // disabled by default
		}
	}

	rotationWriter = &lumberjack.Logger{
		Filename:   filename,
		MaxSize:    logConf.Rotation.MaxSize, // megabytes
		MaxBackups: logConf.Rotation.MaxBackups,
		MaxAge:     logConf.Rotation.MaxAge,   //days
		Compress:   logConf.Rotation.Compress, // disabled by default
	}

	return rotationWriter
}
