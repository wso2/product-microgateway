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
	config_access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	file_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/file/v3"
	grpc_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/grpc/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

// getAccessLogConfigs provides file access log configurations for envoy
func getFileAccessLogConfigs() *config_access_logv3.AccessLog {
	var logFormat *file_accesslogv3.FileAccessLog_LogFormat
	logpath := defaultAccessLogPath //default access log path

	logConf, errReadConfig := config.ReadLogConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Error("Error loading configuration. ", errReadConfig)
		return nil
	}

	if !logConf.AccessLogs.Enable {
		logger.LoggerOasparser.Info("Accesslog Configurations are disabled.")
		return nil
	}

	logFormat = &file_accesslogv3.FileAccessLog_LogFormat{
		LogFormat: &corev3.SubstitutionFormatString{
			Format: &corev3.SubstitutionFormatString_TextFormat{
				TextFormat: logConf.AccessLogs.Format,
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
		logger.LoggerOasparser.Error("Error marsheling access log configs. ", err)
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
	analyticsEnable := conf.Analytics.Enabled
	if !analyticsEnable {
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
	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling gRPC access log configs. ", err)
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
