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
	"fmt"
	"reflect"
	"runtime"
	"strings"

	logrus "github.com/sirupsen/logrus"
	"github.com/wso2/product-microgateway/adapter/pkg/config"
)

type plainFormatter struct {
	TimestampFormat string
	LevelDesc       []string
}

// errorHook for adding a custom logic to default the error details only for error logs
type errorHook struct {
}

// logFormatter is to keep the relevant log formatter
var (
	logFormatter logrus.Formatter
)

// init will read the configs (LogFormalization) and decide which formatter to be used for logging, when initializing
func init() {
	logConf := config.ReadLogConfigs()
	switch logConf.LogFormat {
	case JSON:
		formatter := new(logrus.JSONFormatter)
		formatter.TimestampFormat = "2006-01-02 15:04:05"
		formatter.CallerPrettyfier = func(frame *runtime.Frame) (function string, file string) {
			fileArr := strings.Split(frame.File, "/")
			return formatFilePath(frame.Function), fileArr[len(fileArr)-1] + ":" + fmt.Sprintf("%d", frame.Line)
		}
		logFormatter = formatter

	case TEXT:
		formatter := new(plainFormatter)
		formatter.TimestampFormat = "2006-01-02 15:04:05"
		formatter.LevelDesc = []string{
			panicLevel,
			fatalLevel,
			errorLevel,
			warnLevel,
			infoLevel,
			debugLevel}
		logFormatter = formatter
	}
}

// Format sets a custom format for loggers.
func (f *plainFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	timestamp := fmt.Sprintf(entry.Time.Format(f.TimestampFormat))
	return []byte(fmt.Sprintf("%s %s [%s:%d] - [%s] [-] %s [%s]\n",
		timestamp,
		f.LevelDesc[entry.Level],
		formatFilePath(entry.Caller.File),
		entry.Caller.Line,
		formatFilePath(entry.Caller.Function),
		entry.Message,
		createKeyValuePairs(entry.Data))), nil
}

func createKeyValuePairs(m logrus.Fields) string {
	b := new(bytes.Buffer)
	for key, value := range m {
		fmt.Fprintf(b, "%s=%v ", key, value)
	}
	return strings.TrimSpace(b.String())
}

// formatFilePath retrieves only the last part from a path.
func formatFilePath(path string) string {
	arr := strings.Split(path, "/")
	return arr[len(arr)-1]
}

// Levels to fire only on ErrorLevel (.Error(), .Errorf(), etc.)
func (h *errorHook) Levels() []logrus.Level {
	return []logrus.Level{logrus.ErrorLevel}
}

// Fire specifies a custom logic to execute when the hook fires
func (h *errorHook) Fire(e *logrus.Entry) error {
	// e.Data is a map with all fields attached to entry
	if _, ok := e.Data[SEVERITY]; !ok {
		e.Data[SEVERITY] = DEFAULT
	}
	if _, ok := e.Data[ERRORCODE]; !ok {
		e.Data[ERRORCODE] = 0
	}
	pcLogPkg, _, _, _ := runtime.Caller(7)
	detailsLogPkgFunc := runtime.FuncForPC(pcLogPkg)
	if strings.Contains(detailsLogPkgFunc.Name(), runtime.FuncForPC(reflect.ValueOf((*Log).ErrorC).Pointer()).Name()) {
		pc, filename, line, _ := runtime.Caller(8)
		details := runtime.FuncForPC(pc)
		e.Caller = &runtime.Frame{
			PC:       pc,
			Func:     details,
			Function: details.Name(),
			File:     filename,
			Line:     line,
			Entry:    0,
		}
	}
	return nil
}
