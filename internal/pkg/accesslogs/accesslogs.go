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
package accesslogs

import (
	"fmt"
	"sync"
	"time"

	alf "github.com/envoyproxy/go-control-plane/envoy/data/accesslog/v2"
	als "github.com/envoyproxy/go-control-plane/envoy/service/accesslog/v2"

	//alsgrpc "github.com/envoyproxy/go-control-plane/envoy/service/accesslog/v2"

	//alf "github.com/envoyproxy/go-control-plane/envoy/config/filter/accesslog/v2"
	//als "github.com/envoyproxy/go-control-plane/envoy/service/accesslog/v2"

	slog "github.com/sirupsen/logrus"
)

type logger struct{}

func (logger logger) Infof(format string, args ...interface{}) {
	slog.Infof(format, args...)
}
func (logger logger) Errorf(format string, args ...interface{}) {
	slog.Errorf(format, args...)
}

// AccessLogService buffers access logs from the remote Envoy nodes.
type AccessLogService struct {
	entries []string
	mu      sync.Mutex
}

func (svc *AccessLogService) log(entry string) {
	svc.mu.Lock()
	defer svc.mu.Unlock()
	svc.entries = append(svc.entries, entry)

	slog.Infof("AccessLog:  " + entry)

}

// Dump releases the collected log entries and clears the log entry list.
func (svc *AccessLogService) Dump(f func(string)) {
	svc.mu.Lock()
	defer svc.mu.Unlock()
	for _, entry := range svc.entries {
		f(entry)
	}
	svc.entries = nil
}

// StreamAccessLogs implements the access log service.
func (svc *AccessLogService) StreamAccessLogs(stream als.AccessLogService_StreamAccessLogsServer) error {
	var logName string
	for {
		msg, err := stream.Recv()
		if err != nil {
			continue
		}
		if msg.Identifier != nil {
			logName = msg.Identifier.LogName
		}
		switch entries := msg.LogEntries.(type) {
		case *als.StreamAccessLogsMessage_HttpLogs:
			for _, entry := range entries.HttpLogs.LogEntry {
				if entry != nil {
					common := entry.CommonProperties
					req := entry.Request
					resp := entry.Response
					if common == nil {
						common = &alf.AccessLogCommon{}
					}
					if req == nil {
						req = &alf.HTTPRequestProperties{}
					}
					if resp == nil {
						resp = &alf.HTTPResponseProperties{}
					}
					svc.log(fmt.Sprintf("[%s%s] %s %s %s %d %s %s",
						logName, time.Now().Format(time.RFC3339), req.Authority, req.Path, req.Scheme,
						resp.ResponseCode.GetValue(), req.RequestId, common.UpstreamCluster))
				}
			}
		case *als.StreamAccessLogsMessage_TcpLogs:
			for _, entry := range entries.TcpLogs.LogEntry {
				if entry != nil {
					common := entry.CommonProperties
					if common == nil {
						common = &alf.AccessLogCommon{}
					}
					svc.log(fmt.Sprintf("[%s%s] tcp %s %s",
						logName, time.Now().Format(time.RFC3339), common.UpstreamLocalAddress, common.UpstreamCluster))
				}
			}
		}
	}
}
