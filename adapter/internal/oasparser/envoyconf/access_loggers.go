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

// getAccessLogConfigs provides default formatters
func getDefaultFormatters() []*corev3.TypedExtensionConfig {
	return []*corev3.TypedExtensionConfig{
		{
			Name: "envoy.formatter.req_without_query",
			TypedConfig: &anypb.Any{
				TypeUrl: "type.googleapis.com/envoy.extensions.formatter.req_without_query.v3.ReqWithoutQuery",
			},
		},
	}
}

// getDefaultTextLogFormat provides default text log format
func getDefaultTextLogFormat(loggingFormat string, omitEmptyValues bool) *file_accesslogv3.FileAccessLog_LogFormat {
	formatters := getDefaultFormatters()

	return &file_accesslogv3.FileAccessLog_LogFormat{
		LogFormat: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_TextFormatSource{
				TextFormatSource: &corev3.DataSource{
					Specifier: &corev3.DataSource_InlineString{
						InlineString: loggingFormat,
					},
				},
			},
			Formatters:      formatters,
			OmitEmptyValues: omitEmptyValues,
		},
	}
}

// getAccessLogConfigs provides file access log configurations for envoy
func getInsightsAccessLogConfigs() *config_access_logv3.AccessLog {
	var logFormat *file_accesslogv3.FileAccessLog_LogFormat
	logpath := defaultAccessLogPath //default access log path

	logConf := config.ReadLogConfigs()

	if !logConf.InsightsLogs.Enable {
		return nil
	}

	// Set the default log format
	loggingFormat := logConf.InsightsLogs.LoggingFormat + "\n"
	logFormat = getDefaultTextLogFormat(loggingFormat, logConf.InsightsLogs.OmitEmptyValues)

	logpath = logConf.InsightsLogs.LogFile
	accessLogConf := &file_accesslogv3.FileAccessLog{
		Path:            logpath,
		AccessLogFormat: logFormat,
	}

	accessLogTypedConf, err := anypb.New(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshaling access log configs. ", err)
		return nil
	}

	orgIDFilter := &config_access_logv3.AccessLogFilter{
		FilterSpecifier: &config_access_logv3.AccessLogFilter_MetadataFilter{
			MetadataFilter: &config_access_logv3.MetadataFilter{
				Matcher: &matcherv3.MetadataMatcher{
					Filter: extAuthzFilterName,
					Path: []*matcherv3.MetadataMatcher_PathSegment{
						{
							Segment: &matcherv3.MetadataMatcher_PathSegment_Key{
								Key: xWso2ApiOrganizationId,
							},
						},
					},
					Value: &matcherv3.ValueMatcher{
						MatchPattern: &matcherv3.ValueMatcher_StringMatch{
							StringMatch: &matcherv3.StringMatcher{
								MatchPattern: &matcherv3.StringMatcher_SafeRegex{
									SafeRegex: &matcherv3.RegexMatcher{
										EngineType: &matcherv3.RegexMatcher_GoogleRe2{
											GoogleRe2: &matcherv3.RegexMatcher_GoogleRE2{},
										},
										Regex: "^.{2,}$",
									},
								},
							},
						},
					},
					Invert: false,
				},
				MatchIfKeyNotFound: &wrapperspb.BoolValue{Value: false},
			},
		},
	}

	accessLog := config_access_logv3.AccessLog{
		Name:   fileAccessLogName,
		Filter: orgIDFilter,
		ConfigType: &config_access_logv3.AccessLog_TypedConfig{
			TypedConfig: accessLogTypedConf,
		},
	}

	return &accessLog
}

// getAccessLogConfigs provides file access log configurations for envoy
func getFileAccessLogConfigs() *config_access_logv3.AccessLog {
	var logFormat *file_accesslogv3.FileAccessLog_LogFormat
	logpath := defaultAccessLogPath //default access log path

	logConf := config.ReadLogConfigs()

	if !logConf.AccessLogs.Enable {
		logger.LoggerOasparser.Info("Accesslog Configurations are disabled.")
		return nil
	}

	// Set the default log format
	loggingFormat := logConf.AccessLogs.ReservedLogFormat +
		strings.TrimLeft(logConf.AccessLogs.SecondaryLogFormat, "'") + "\n"
	logFormat = getDefaultTextLogFormat(loggingFormat, false)

	// Configure the log format based on the log type
	switch logConf.AccessLogs.LogType {
	case AccessLogTypeJSON:
		logFields := map[string]*structpb.Value{}
		for k, v := range logConf.AccessLogs.JSONFormat {
			logFields[k] = &structpb.Value{Kind: &structpb.Value_StringValue{StringValue: v}}
		}
		formattedSecondaryLogFormat := strings.ReplaceAll(logConf.AccessLogs.SecondaryLogFormat, "'", "")
		secondaryLogFields := strings.Split(formattedSecondaryLogFormat, " ")
		// iterate through secondary log fields and update the logfields map
		for _, field := range secondaryLogFields {
			if field == "" {
				continue
			}
			logFields[strings.ReplaceAll(field, "%", "")] =
				&structpb.Value{Kind: &structpb.Value_StringValue{StringValue: field}}
		}
		logFormat = &file_accesslogv3.FileAccessLog_LogFormat{
			LogFormat: &corev3.SubstitutionFormatString{
				Format: &corev3.SubstitutionFormatString_JsonFormat{
					JsonFormat: &structpb.Struct{
						Fields: logFields,
					},
				},
				Formatters: getDefaultFormatters(),
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

	if enableRouterConfigValidation {
		err = accessLog.Validate()
		if err != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating file access log configs. ", err)
			} else {
				logger.LoggerOasparser.Error("Error while validating file access log configs. ", err)
			}
		}
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

	if enableRouterConfigValidation {
		err = accessLog.Validate()
		if err != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating grpc access log configs. ", err)
			} else {
				logger.LoggerOasparser.Error("Error while validating grpc access log configs. ", err)
			}
		}
	}
	return &accessLog
}

// getAccessLogs provides access logs for envoy
func getAccessLogs() []*config_access_logv3.AccessLog {
	conf, _ := config.ReadConfigs()
	var accessLoggers []*config_access_logv3.AccessLog
	fileAccessLog := getFileAccessLogConfigs()
	grpcAccessLog := getGRPCAccessLogConfigs(conf)
	insightsAccessLog := getInsightsAccessLogConfigs()
	if fileAccessLog != nil {
		accessLoggers = append(accessLoggers, fileAccessLog)
	}
	if grpcAccessLog != nil {
		accessLoggers = append(accessLoggers, getGRPCAccessLogConfigs(conf))
	}
	if insightsAccessLog != nil {
		accessLoggers = append(accessLoggers, insightsAccessLog)
	}
	return accessLoggers
}

// getAccessLogFilterConfig provides exclude path configurations for envoy access logs
func getAccessLogFilterConfig() *config_access_logv3.AccessLogFilter {
	logConf := config.ReadLogConfigs()
	conf, _ := config.ReadConfigs()

	if !logConf.AccessLogs.Excludes.SystemHost.Enabled {
		return nil
	}
	logger.LoggerOasparser.Debugf("Access log excludes for system host is enabled with path regex: %q",
		logConf.AccessLogs.Excludes.SystemHost.PathRegex)

	systemHostFilter := &config_access_logv3.AccessLogFilter{
		FilterSpecifier: &config_access_logv3.AccessLogFilter_HeaderFilter{
			HeaderFilter: &config_access_logv3.HeaderFilter{
				Header: &routev3.HeaderMatcher{
					Name: ":authority",
					HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
						SafeRegexMatch: &matcherv3.RegexMatcher{
							Regex: fmt.Sprintf("^%s(:\\d+)?$", conf.Envoy.SystemHost),
						},
					},
					InvertMatch: true,
				},
			},
		},
	}

	if logConf.AccessLogs.Excludes.SystemHost.PathRegex != "" {
		_, err := regexp.Compile(logConf.AccessLogs.Excludes.SystemHost.PathRegex)
		if err != nil {
			logger.LoggerOasparser.Fatal("Error compiling access log exclude path regex. ", err)
		}

		// if path regex is specified, use the OrFilter to combine the system host filter and the path regex filter
		return &config_access_logv3.AccessLogFilter{
			FilterSpecifier: &config_access_logv3.AccessLogFilter_OrFilter{
				OrFilter: &config_access_logv3.OrFilter{
					Filters: []*config_access_logv3.AccessLogFilter{
						systemHostFilter,
						{
							FilterSpecifier: &config_access_logv3.AccessLogFilter_HeaderFilter{
								HeaderFilter: &config_access_logv3.HeaderFilter{
									Header: &routev3.HeaderMatcher{
										Name: ":path",
										HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
											SafeRegexMatch: &matcherv3.RegexMatcher{
												Regex: logConf.AccessLogs.Excludes.SystemHost.PathRegex,
											},
										},
										InvertMatch: true,
									},
								},
							},
						},
					},
				},
			},
		}
	}

	// if path regex is not specified, use the system host filter alone
	return systemHostFilter
}
