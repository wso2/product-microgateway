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
	"github.com/wso2/micro-gw/internal/configs"
	lumberjack "gopkg.in/natefinch/lumberjack.v2"
)

type PlainFormatter struct {
	TimestampFormat string
	LevelDesc       []string
}

func init() {
	logConf, errReadConfig := configs.ReadLogConfigs()
	if errReadConfig != nil {
		logrus.Error("Error loading log configuration. ", errReadConfig)
	}

	err := initGlobalLogger(logConf.Logfile)
	if err != nil {
		logrus.Error("Failed to initialize logging")
	}
}

/**
 * Initialise the global logger.
 * If the package root level is defined in the log_config.toml file, it is set.
 * otherwise log level is assigned to default lo level.
 *
 * @param filename   Log file name
 * @return *error Error
 */
func initGlobalLogger(filename string) error {

	// Create the log file if doesn't exist. And append to it if it already exists.
	_, err := os.OpenFile(filename, os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0644)

	logrus.SetReportCaller(true)
	formatter := loggerFromat()
	logrus.SetFormatter(formatter)

	if err != nil {
		// Cannot open log file. Logging to stderr
		logrus.Warn("failed to open logfile", err)
		logrus.SetOutput(os.Stdout)

	} else {
		//log output set to stdout and file
		multiWriter := io.MultiWriter(os.Stdout, setLogRotation(filename))
		logrus.SetOutput(multiWriter)
	}

	logConf, errReadConfig := configs.ReadLogConfigs()
	if errReadConfig != nil {
		logrus.Error("Error loading configuration. ", errReadConfig)
		logrus.SetLevel(logLevelMapper(logConf.LogLevel))
	} else {
		logrus.SetLevel(DEFAULT_LOG_LEVEL)
	}

	return err
}

/**
 * Initiate the log formatter.
 * @return *PlainFormatter Reference for a Plain formatter instance
 */
func loggerFromat() *PlainFormatter {

	formatter := new(PlainFormatter)
	formatter.TimestampFormat = "2006-01-02 15:04:05"
	formatter.LevelDesc = []string{
		LEVEL_PANIC,
		LEVEL_FATAL,
		LEVEL_ERROR,
		LEVEL_WARN,
		LEVEL_INFO,
		LEVEL_DEBUG}

	return formatter
}

/**
 * Retrieve only the last part from a path.
 *
 * @param path   Path
 * @return string Last part of the path
 */
func formatFilePath(path string) string {
	arr := strings.Split(path, "/")
	return arr[len(arr)-1]
}

/**
 * Set a custom format for loggers.
 * This method overrides the logrus library Format method.
 *
 * @param entry   Log entry
 * @return io.Writer  File as a io.Writer instance along with log rotation
 */
func (f *PlainFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	timestamp := fmt.Sprintf(entry.Time.Format(f.TimestampFormat))

	return []byte(fmt.Sprintf("%s %s [%s:%d] - [%s] [-] %s\n",
		timestamp,
		f.LevelDesc[entry.Level],
		formatFilePath(entry.Caller.File),
		entry.Caller.Line,
		formatFilePath(entry.Caller.Function), entry.Message)), nil
}

/**
 * Initiate the log rotation feature using lumberjack library.
 * All the rotation params reads for the configs and if it occurs
 * a error, all the params are set to the default values.
 *
 * @param filename   Name of the log file
 * @return io.Writer  File as a io.Writer instance along with log rotation
 */
func setLogRotation(filename string) io.Writer {
	logConf, errReadConfig := configs.ReadLogConfigs()
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
