package envoyconf

import (
	config_access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	file_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/file/v3"
	grpc_accesslogv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/grpc/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"time"
)

// getAccessLogConfigs provides access log configurations for envoy
func getFileAccessLogConfigs() *config_access_logv3.AccessLog {
	var logFormat *file_accesslogv3.FileAccessLog_Format
	logpath := defaultAccessLogPath //default access log path

	logConf, errReadConfig := config.ReadLogConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Error("Error loading configuration. ", errReadConfig)
	} else {
		logFormat = &file_accesslogv3.FileAccessLog_Format{
			Format:logConf.AccessLogs.Format,
		}
		logpath = logConf.AccessLogs.LogFile
	}

	accessLogConf := &file_accesslogv3.FileAccessLog{
		Path:            logpath,
		AccessLogFormat: logFormat,
	}

	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling access log configs. ", err)
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

// getAccessLogConfigs provides access log configurations for envoy
func getGRPCAccessLogConfigs() *config_access_logv3.AccessLog {
	//accessLogConf := &grpc_accesslogv3.CommonGrpcAccessLogConfig{
	accessLogConf := &grpc_accesslogv3.HttpGrpcAccessLogConfig{
		CommonConfig : &grpc_accesslogv3.CommonGrpcAccessLogConfig{
			TransportApiVersion: corev3.ApiVersion_V3,
			LogName:             "mgw_access_logs",
			GrpcService: &corev3.GrpcService{
				TargetSpecifier: &corev3.GrpcService_EnvoyGrpc_{
					EnvoyGrpc: &corev3.GrpcService_EnvoyGrpc{
						ClusterName: accessLoggerClusterName,
					},
				},
				Timeout: ptypes.DurationProto(20 * time.Second),
			},
		},
	}
	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling gRPC access log configs. ", err)
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

// getAccessLogConfigs provides access log configurations for envoy
func getAccessLogs() []*config_access_logv3.AccessLog {
	// TODO (amalimatharaarachchi) read and enable according to analytics config
	analytics := false
	if (analytics) {
		return []*config_access_logv3.AccessLog{getFileAccessLogConfigs(), getGRPCAccessLogConfigs()}
	}
	return []*config_access_logv3.AccessLog{getFileAccessLogConfigs()}
}
