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

package logging

import (
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"runtime"
	"strings"
	"testing"

	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

type LogAttr struct {
	ErrorCode int    `json:"error_code"`
	File      string `json:"file"`
	Function  string `json:"func"`
	Level     string `json:"level"`
	Msg       string `json:"msg"`
	Severity  string `json:"severity"`
	Time      string `json:"time"`
}

func TestInitPackageLogger(t *testing.T) {
	// LoggerTest1 : to test JSON formatted logger
	LoggerTest1 := InitPackageLogger("sample.package1")
	assert.NotNil(t, LoggerTest1, "InitPackageLogger(sample.package1) failed.")
	jsonFormatter := new(logrus.JSONFormatter)
	jsonFormatter.TimestampFormat = "2006-01-02 15:04:05"
	jsonFormatter.CallerPrettyfier = func(frame *runtime.Frame) (function string, file string) {
		fileArr := strings.Split(frame.File, "/")
		return formatFilePath(frame.Function), fileArr[len(fileArr)-1] + ":" + fmt.Sprintf("%d", frame.Line)
	}
	LoggerTest1.SetFormatter(jsonFormatter)
	var buf bytes.Buffer
	LoggerTest1.SetOutput(&buf)
	defer func() {
		LoggerTest1.SetOutput(os.Stderr)
	}()
	// Test error logs printed using ErrorC
	LoggerTest1.ErrorC(ErrorDetails{Message: "Test error log 1", Severity: CRITICAL, ErrorCode: 455678})
	var log1 LogAttr
	buf2 := new(bytes.Buffer)
	if err := json.Compact(buf2, buf.Bytes()); err != nil {
		fmt.Println(err)
	}
	err1 := json.Unmarshal(buf2.Bytes(), &log1)
	assert.Nil(t, err1, "JSON formatted error log parsing error")
	assert.Equal(t, "Test error log 1", log1.Msg, "Error log message mismatch")
	assert.Equal(t, CRITICAL, log1.Severity, "Error log severity mismatch")
	assert.Equal(t, 455678, log1.ErrorCode, "Error log code mismatch")
	buf.Reset()
	buf2.Reset()

	// Test error logs printed using Errorf
	var log2 LogAttr
	LoggerTest1.Errorf("Sample error log without details")
	if err := json.Compact(buf2, buf.Bytes()); err != nil {
		fmt.Println(err)
	}
	err2 := json.Unmarshal(buf2.Bytes(), &log2)
	assert.Nil(t, err2, "JSON formatted error log parsing error")
	assert.Equal(t, "Sample error log without details", log2.Msg, "Error log message mismatch")
	assert.Equal(t, DEFAULT, log2.Severity, "Error log severity mismatch")
	assert.Equal(t, 0, log2.ErrorCode, "Error log code mismatch")
	buf.Reset()
	buf2.Reset()

	// LoggerTest2 : to test plain formatted logger
	LoggerTest2 := InitPackageLogger("sample.package2")
	assert.NotNil(t, LoggerTest2, "InitPackageLogger(sample.package2) failed.")
	PlainFormatter := new(plainFormatter)
	PlainFormatter.TimestampFormat = "2006-01-02 15:04:05"
	PlainFormatter.LevelDesc = []string{
		panicLevel,
		fatalLevel,
		errorLevel,
		warnLevel,
		infoLevel,
		debugLevel}
	LoggerTest2.SetFormatter(PlainFormatter)
	LoggerTest2.SetOutput(&buf)
	defer func() {
		LoggerTest2.SetOutput(os.Stderr)
	}()
	LoggerTest2.ErrorC(ErrorDetails{Message: "Test error log2", Severity: MAJOR, ErrorCode: 345678})
	assert.Contains(t, buf.String(), "severity="+MAJOR, "Invalid error log in plain format"+
		"(included severity, but not found)")
}

func TestInitGlobalLogger(t *testing.T) {
	globalLoggerTest1 := initGlobalLogger("log_adapter.log")
	assert.Nil(t, globalLoggerTest1, "Global logger initialization failed")
	jsonFormatter := new(logrus.JSONFormatter)
	jsonFormatter.TimestampFormat = "2006-01-02 15:04:05"
	jsonFormatter.CallerPrettyfier = func(frame *runtime.Frame) (function string, file string) {
		fileArr := strings.Split(frame.File, "/")
		return formatFilePath(frame.Function), fileArr[len(fileArr)-1] + ":" + fmt.Sprintf("%d", frame.Line)
	}
	logrus.SetFormatter(jsonFormatter)
	var buf bytes.Buffer
	logrus.SetOutput(&buf)
	defer func() {
		logrus.SetOutput(os.Stderr)
	}()
	// Test error logs printed using logrus.Error with fields
	logrus.WithFields(logrus.Fields{"severity": BLOCKER, "error_code": 234567}).Errorf("Test error log JSON format")
	var log1 LogAttr
	buf2 := new(bytes.Buffer)
	if err := json.Compact(buf2, buf.Bytes()); err != nil {
		fmt.Println(err)
	}
	err1 := json.Unmarshal(buf2.Bytes(), &log1)
	assert.Nil(t, err1, "JSON formatted error log parsing error")
	assert.Equal(t, "Test error log JSON format", log1.Msg, "Error log message mismatch")
	assert.Equal(t, BLOCKER, log1.Severity, "Error log severity mismatch")
	assert.Equal(t, 234567, log1.ErrorCode, "Error log code mismatch")
	buf.Reset()
	buf2.Reset()

	// to remove the log_adapter.log file created by initGlobalLogger
	e := os.Remove("log_adapter.log")
	if e != nil {
		t.Error(e)
	}
}
