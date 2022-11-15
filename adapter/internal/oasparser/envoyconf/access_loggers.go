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

package envoyconf

import (
	"fmt"

	config_access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	file_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/file/v3"
	grpc_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/grpc/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

// getAccessLogConfigs provides file access log configurations for envoy
func getFileAccessLogConfigs() *config_access_logv3.AccessLog {
	var logFormat *file_accesslogv3.FileAccessLog_LogFormat
	logpath := defaultAccessLogPath //default access log path

	logConf := config.ReadLogConfigs()

	if !logConf.AccessLogs.Enable {
		logger.LoggerOasparser.Info("Accesslog Configurations are disabled.")
		return nil
	}

	logFormat = &file_accesslogv3.FileAccessLog_LogFormat{
		LogFormat: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_TextFormatSource{
				TextFormatSource: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: logConf.AccessLogs.Format,
					},
				},
			},
			Formatters: []*corev3.TypedExtensionConfig{
				{
					Name: "envoy.formatter.req_without_query",
					TypedConfig: &anypb.Any{
						TypeUrl: "type.googleapis.com/envoy.extensions.formatter.req_without_query.v3.ReqWithoutQuery",
					},
				},
			},
		},
	}
	logpath = logConf.AccessLogs.LogFile

	accessLogConf := &file_accesslogv3.FileAccessLog{
		Path:            logpath,
		AccessLogFormat: logFormat,
	}

	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error marsheling access log configs. %v", err.Error()),
			Severity:  logging.CRITICAL,
			ErrorCode: 2200,
		})
		return nil
	}

	accessLog := config_access_logv3.AccessLog{
		Name:   fileAccessLogName,
		Filter: nil,
		ConfigType: &config_access_logv3.AccessLog_TypedConfig{
			TypedConfig: accessLogTypedConf,
		},
	}
	return &accessLog
}

// getAccessLogConfigs provides grpc access log configurations for envoy
func getGRPCAccessLogConfigs(conf *config.Config) *config_access_logv3.AccessLog {
	grpcAccessLogsEnabled := conf.Analytics.Enabled || conf.Enforcer.Metrics.Enabled
	if !grpcAccessLogsEnabled {
		logger.LoggerOasparser.Debug("gRPC access logs are not enabled as analytics is disabled.")
		return nil
	}
	var requestHeaders []string
	var responseHeaders []string
	var responseTrailers []string
	if conf.Analytics.Adapter.CustomProperties.Enabled {
		if len(conf.Analytics.Adapter.CustomProperties.RequestHeaders) == 0 {
			loggers.LoggerAPI.Warn("Analytics data with custom properties enabled with empty request headers list. Accept, User-Agent headers are available by default")
		}
		requestHeaders = append(requestHeaders, conf.Analytics.Adapter.CustomProperties.RequestHeaders...)
		if len(conf.Analytics.Adapter.CustomProperties.ResponseHeaders) == 0 {
			loggers.LoggerAPI.Warn("Analytics data with custom properties enabled with empty response headers list. Content-length, Content-type, Date headers are available by default")
		}
		responseHeaders = append(responseHeaders, conf.Analytics.Adapter.CustomProperties.ResponseHeaders...)
		responseTrailers = append(responseTrailers, conf.Analytics.Adapter.CustomProperties.ResponseTrailers...)
	}
	accessLogConf := &grpc_accesslogv3.HttpGrpcAccessLogConfig{
		CommonConfig: &grpc_accesslogv3.CommonGrpcAccessLogConfig{
			TransportApiVersion: corev3.ApiVersion_V3,
			LogName:             grpcAccessLogLogName,
			BufferFlushInterval: ptypes.DurationProto(conf.Analytics.Adapter.BufferFlushInterval),
			BufferSizeBytes:     wrapperspb.UInt32(conf.Analytics.Adapter.BufferSizeBytes),
			GrpcService: &corev3.GrpcService{
				TargetSpecifier: &corev3.GrpcService_EnvoyGrpc_{
					EnvoyGrpc: &corev3.GrpcService_EnvoyGrpc{
						ClusterName: accessLoggerClusterName,
					},
				},
				Timeout: ptypes.DurationProto(conf.Analytics.Adapter.GRPCRequestTimeout),
			},
		},
		AdditionalResponseHeadersToLog:  responseHeaders,
		AdditionalRequestHeadersToLog:   requestHeaders,
		AdditionalResponseTrailersToLog: responseTrailers,
	}
	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error marshalling gRPC access log configs. %v", err.Error()),
			Severity:  logging.CRITICAL,
			ErrorCode: 2201,
		})
		return nil
	}

	accessLog := config_access_logv3.AccessLog{
		Name:   grpcAccessLogName,
		Filter: nil,
		ConfigType: &config_access_logv3.AccessLog_TypedConfig{
			TypedConfig: accessLogTypedConf,
		},
	}
	return &accessLog
}

// getAccessLogs provides access logs for envoy
func getAccessLogs() []*config_access_logv3.AccessLog {
	conf, _ := config.ReadConfigs()
	var accessLoggers []*config_access_logv3.AccessLog
	fileAccessLog := getFileAccessLogConfigs()
	grpcAccessLog := getGRPCAccessLogConfigs(conf)
	if fileAccessLog != nil {
		accessLoggers = append(accessLoggers, fileAccessLog)
	}
	if grpcAccessLog != nil {
		accessLoggers = append(accessLoggers, getGRPCAccessLogConfigs(conf))
	}
	return accessLoggers
}
