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

// Package envoyconf generates the envoyconfiguration for listeners, virtual hosts,
// routes, clusters, and endpoints.
package envoyconf

import (
	"fmt"
	"time"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	ext_authv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/ext_authz/v3"
	luav3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/lua/v3"
	routerv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/router/v3"
	wasm_filter_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/wasm/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	wasmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/wasm/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/wrappers"

	//rls "github.com/envoyproxy/go-control-plane/envoy/config/ratelimit/v3"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"

	//mgw_websocket "github.com/wso2/micro-gw/internal/oasparser/envoyconf/api"
	"github.com/golang/protobuf/ptypes/any"
)

// getHTTPFilters generates httpFilter configuration
func getHTTPFilters() []*hcmv3.HttpFilter {
	extAauth := getExtAuthzHTTPFilter()
	router := getRouterHTTPFilter()
	lua := getLuaFilter()
	cors := &hcmv3.HttpFilter{
		Name:       wellknown.CORS,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{},
	}

	httpFilters := []*hcmv3.HttpFilter{
		cors,
		extAauth,
		lua,
		router,
	}
	conf, _ := config.ReadConfigs()
	if conf.Envoy.Filters.Compression.Enabled {
		compressionFilter, err := getCompressorFilter()
		if err != nil {
			logger.LoggerXds.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Error occurred while creating the compression filter: %v", err.Error()),
				Severity:  logging.MINOR,
				ErrorCode: 2234,
			})
			return httpFilters
		}
		httpFilters = httpFilters[:len(httpFilters)-1]
		httpFilters = append(httpFilters, compressionFilter)
		httpFilters = append(httpFilters, router)
	}
	return httpFilters
}

// getRouterHTTPFilter gets router http filter.
func getRouterHTTPFilter() *hcmv3.HttpFilter {

	routeFilterConf := routerv3.Router{
		DynamicStats:             nil,
		StartChildSpan:           false,
		UpstreamLog:              nil,
		SuppressEnvoyHeaders:     true,
		StrictCheckHeaders:       nil,
		RespectExpectedRqTimeout: false,
	}

	routeFilterTypedConf, err := ptypes.MarshalAny(&routeFilterConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshaling route filter configs. ", err)
	}
	filter := hcmv3.HttpFilter{
		Name:       wellknown.Router,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{TypedConfig: routeFilterTypedConf},
	}
	return &filter
}

// UpgradeFilters that are applied in websocket upgrade mode
func getUpgradeFilters() []*hcmv3.HttpFilter {
	cors := &hcmv3.HttpFilter{
		Name:       wellknown.CORS,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{},
	}
	extAauth := getExtAuthzHTTPFilter()
	mgwWebSocketWASM := getMgwWebSocketWASMFilter()
	router := getRouterHTTPFilter()
	upgradeFilters := []*hcmv3.HttpFilter{
		cors,
		extAauth,
		mgwWebSocketWASM,
		router,
	}
	return upgradeFilters
}

// getExtAuthzHTTPFilter gets ExtAuthz http filter.
func getExtAuthzHTTPFilter() *hcmv3.HttpFilter {
	conf, _ := config.ReadConfigs()
	extAuthzConfig := &ext_authv3.ExtAuthz{
		// This would clear the route cache only if there is a header added/removed or changed
		// within ext-authz filter. Without this configuration, the API cannot have production
		// and sandbox endpoints both at once as the cluster is set based on the header added
		// from the ext-authz filter.
		ClearRouteCache:        true,
		IncludePeerCertificate: true,
		TransportApiVersion:    corev3.ApiVersion_V3,
		Services: &ext_authv3.ExtAuthz_GrpcService{
			GrpcService: &corev3.GrpcService{
				TargetSpecifier: &corev3.GrpcService_EnvoyGrpc_{
					EnvoyGrpc: &corev3.GrpcService_EnvoyGrpc{
						ClusterName: extAuthzClusterName,
					},
				},
				Timeout: ptypes.DurationProto(conf.Envoy.EnforcerResponseTimeoutInSeconds * time.Second),
			},
		},
	}

	// configures envoy to handle request body and GraphQL APIs require below configs to pass request
	// payload to the enforcer.
	extAuthzConfig.WithRequestBody = &ext_authv3.BufferSettings{
		MaxRequestBytes:     conf.Envoy.PayloadPassingToEnforcer.MaxRequestBytes,
		AllowPartialMessage: conf.Envoy.PayloadPassingToEnforcer.AllowPartialMessage,
		PackAsBytes:         conf.Envoy.PayloadPassingToEnforcer.PackAsBytes,
	}

	ext, err2 := ptypes.MarshalAny(extAuthzConfig)
	if err2 != nil {
		logger.LoggerOasparser.Error(err2)
	}
	extAuthzFilter := hcmv3.HttpFilter{
		Name: extAuthzFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}
	return &extAuthzFilter
}

// getLuaFilter gets Lua http filter.
func getLuaFilter() *hcmv3.HttpFilter {
	//conf, _ := config.ReadConfigs()
	luaConfig := &luav3.Lua{
		InlineCode: "function envoy_on_request(request_handle)" +
			"\nend" +
			"\nfunction envoy_on_response(response_handle)" +
			"\nend",
	}
	ext, err2 := ptypes.MarshalAny(luaConfig)
	if err2 != nil {
		logger.LoggerOasparser.Error(err2)
	}
	luaFilter := hcmv3.HttpFilter{
		Name: luaFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}
	return &luaFilter
}

func getMgwWebSocketWASMFilter() *hcmv3.HttpFilter {
	config := &wrappers.StringValue{
		Value: `{
			"node_id": "mgw_node_1",
			"rate_limit_service": "ext-authz",
			"timeout": "20s",
			"failure_mode_deny": "true"
		}`,
	}
	a, err := proto.Marshal(config)
	if err != nil {
		logger.LoggerOasparser.Error(err)
	}
	mgwWebsocketWASMConfig := wasmv3.PluginConfig{
		Name:   mgwWebSocketWASMFilterName,
		RootId: mgwWebSocketWASMFilterRoot,
		Vm: &wasmv3.PluginConfig_VmConfig{
			VmConfig: &wasmv3.VmConfig{
				VmId:             mgwWASMVmID,
				Runtime:          mgwWASMVmRuntime,
				AllowPrecompiled: true,
				Code: &corev3.AsyncDataSource{
					Specifier: &corev3.AsyncDataSource_Local{
						Local: &corev3.DataSource{
							Specifier: &corev3.DataSource_Filename{
								Filename: mgwWebSocketWASM,
							},
						},
					},
				},
			},
		},
		Configuration: &any.Any{
			TypeUrl: "type.googleapis.com/google.protobuf.StringValue",
			Value:   a,
		},
	}

	mgwWebSocketWASMFilterConfig := &wasm_filter_v3.Wasm{
		Config: &mgwWebsocketWASMConfig,
	}

	ext, err2 := proto.Marshal(mgwWebSocketWASMFilterConfig)
	if err2 != nil {
		logger.LoggerOasparser.Error(err2)
	}
	mgwWebSocketFilter := hcmv3.HttpFilter{
		Name: mgwWebSocketWASMFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: &any.Any{
				TypeUrl: "type.googleapis.com/envoy.extensions.filters.http.wasm.v3.Wasm",
				Value:   ext,
			},
		},
	}
	return &mgwWebSocketFilter

}
