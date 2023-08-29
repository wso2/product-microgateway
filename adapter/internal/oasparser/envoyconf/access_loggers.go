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
	"regexp"
	"strings"

	config_access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	file_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/file/v3"
	grpc_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/grpc/v3"
	matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/structpb"
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

	formatters := []*corev3.TypedExtensionConfig{
		{
			Name: "envoy.formatter.req_without_query",
			TypedConfig: &anypb.Any{
				TypeUrl: "type.googleapis.com/envoy.extensions.formatter.req_without_query.v3.ReqWithoutQuery",
			},
		},
	}
	// Set the default log format
	logFormat = &file_accesslogv3.FileAccessLog_LogFormat{
		LogFormat: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_TextFormatSource{
				TextFormatSource: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: logConf.AccessLogs.ReservedLogFormat +
							strings.TrimLeft(logConf.AccessLogs.SecondaryLogFormat, "'") + "\n",
					},
				},
			},
			Formatters: formatters,
		},
	}

	// Configure the log format based on the log type
	switch logConf.AccessLogs.LogType {
	case AccessLogTypeJSON:
		logFields := map[string]*structpb.Value{}
		for k, v := range logConf.AccessLogs.JSONFormat {
			logFields[k] = &structpb.Value{Kind: &structpb.Value_StringValue{StringValue: v}}
		}
		logFormat = &file_accesslogv3.FileAccessLog_LogFormat{
			LogFormat: &corev3.SubstitutionFormatString{
				Format: &corev3.SubstitutionFormatString_JsonFormat{
					JsonFormat: &structpb.Struct{
						Fields: logFields,
					},
				},
				Formatters: formatters,
			},
		}
		logger.LoggerOasparser.Debug("Access log type is set to json.")
	case AccessLogTypeText:
		logger.LoggerOasparser.Debug("Access log type is set to text.")
	default:
		logger.LoggerOasparser.Errorf("Error setting access log type. Invalid Access log type %q. Continue with default log type %q",
			logConf.AccessLogs.LogType, AccessLogTypeText)
	}

	logpath = logConf.AccessLogs.LogFile

	accessLogConf := &file_accesslogv3.FileAccessLog{
		Path:            logpath,
		AccessLogFormat: logFormat,
	}

	accessLogTypedConf, err := anypb.New(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling access log configs. ", err)
		return nil
	}

	accessLog := config_access_logv3.AccessLog{
		Name:   fileAccessLogName,
		Filter: getAccessLogFilterConfig(),
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
	}
	accessLogTypedConf, err := anypb.New(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshalling gRPC access log configs. ", err)
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

// getAccessLogFilterConfig provides exclude path configurations for envoy access logs
func getAccessLogFilterConfig() *config_access_logv3.AccessLogFilter {
	conf := config.ReadLogConfigs()
	logger.LoggerOasparser.Debugf("Access log exclude path configuration enabled is set to %t. Regex: %s",
		conf.AccessLogs.ExcludePaths.Enabled, conf.AccessLogs.ExcludePaths.Regex)
	if !conf.AccessLogs.ExcludePaths.Enabled {
		return nil
	}

	_, err := regexp.Compile(conf.AccessLogs.ExcludePaths.Regex)
	if err != nil {
		logger.LoggerOasparser.Fatal("Error compiling access log exclude path regex. ", err)
	}

	return &config_access_logv3.AccessLogFilter{
		FilterSpecifier: &config_access_logv3.AccessLogFilter_HeaderFilter{
			HeaderFilter: &config_access_logv3.HeaderFilter{
				Header: &routev3.HeaderMatcher{
					Name: ":path",
					HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
						SafeRegexMatch: &matcherv3.RegexMatcher{
							Regex: conf.AccessLogs.ExcludePaths.Regex,
						},
					},
					InvertMatch: true,
				},
			},
		},
	}
}
