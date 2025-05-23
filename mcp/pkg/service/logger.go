/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 */

package service

import (
	"context"
	"log/slog"
	"os"
	"sync"
)

type LogHandler struct {
	slog.Handler
}

type contextKey string

const (
	ToolNameKey contextKey = "toolName"
	ApiNameKey  contextKey = "apiName"
)

var syncOnceLogger sync.Once

var logger *slog.Logger

// Custom log handler to add toolName and apiName attributes to each log
func (l *LogHandler) Handle(ctx context.Context, r slog.Record) error {
	if toolName, ok := ctx.Value(ToolNameKey).(string); ok {
		r.AddAttrs(slog.String("toolName", toolName))
	}
	if apiName, ok := ctx.Value(ApiNameKey).(string); ok {
		r.AddAttrs(slog.String("apiName", apiName))
	}
	return l.Handler.Handle(ctx, r)
}

func GetLogger() *slog.Logger {
	syncOnceLogger.Do(func() {
		if logger == nil {
			attributes := []slog.Attr{}
			baseHandler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{AddSource: false}).WithAttrs(attributes)
			customHandler := &LogHandler{Handler: baseHandler}
			logger = slog.New(customHandler)
		}
	})
	return logger
}
