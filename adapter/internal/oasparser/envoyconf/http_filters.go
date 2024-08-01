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
	"strings"
	"time"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	envoy_config_ratelimit_v3 "github.com/envoyproxy/go-control-plane/envoy/config/ratelimit/v3"
	cors_filter_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/cors/v3"
	ext_authv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/ext_authz/v3"
	local_ratelimit_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/local_ratelimit/v3"
	luav3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/lua/v3"
	rate_limit "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/ratelimit/v3"
	routerv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/router/v3"
	wasm_filter_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/wasm/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	wasmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/wasm/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/wrappers"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/durationpb"

	//rls "github.com/envoyproxy/go-control-plane/envoy/config/ratelimit/v3"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"

	//mgw_websocket "github.com/wso2/micro-gw/internal/oasparser/envoyconf/api"
	"github.com/golang/protobuf/ptypes/any"
)

// getHTTPFilters generates httpFilter configuration
func getHTTPFilters() []*hcmv3.HttpFilter {
	extAauth := getExtAuthzHTTPFilter()
	router := getRouterHTTPFilter()
	lua := getLuaFilter()
	cors := getCorsHTTPFilter()
	localRateLimit := getHTTPLocalRateLimitFilter()

	httpFilters := []*hcmv3.HttpFilter{
		cors,
		localRateLimit,
		extAauth,
		lua,
		router,
	}
	conf, _ := config.ReadConfigs()
	if conf.Envoy.RateLimit.Enabled {
		rateLimit := getRateLimitFilter()
		httpFilters = httpFilters[:len(httpFilters)-2]
		httpFilters = append(httpFilters, rateLimit)
		httpFilters = append(httpFilters, lua)
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

	routeFilterTypedConf, err := anypb.New(&routeFilterConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshaling route filter configs. ", err)
	}
	filter := hcmv3.HttpFilter{
		Name:       wellknown.Router,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{TypedConfig: routeFilterTypedConf},
	}
	if enableRouterConfigValidation {
		err = filter.Validate()
		if err != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating Router HTTP filter configs. ", err)
			} else {
				logger.LoggerOasparser.Error("Error while validating Router HTTP filter configs. ", err)
			}
		}
	}
	return &filter
}

func getCorsHTTPFilter() *hcmv3.HttpFilter {

	corsFilterConf := cors_filter_v3.CorsPolicy{}
	corsFilterTypedConf, err := anypb.New(&corsFilterConf)

	if err != nil {
		logger.LoggerOasparser.Error("Error marshaling cors filter configs. ", err)
	}
	filter := hcmv3.HttpFilter{
		Name:       wellknown.CORS,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{TypedConfig: corsFilterTypedConf},
	}
	if enableRouterConfigValidation {
		err = filter.Validate()
		if err != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating cors filter configs. ", err)
			} else {
				logger.LoggerOasparser.Error("Error while validating cors filter configs. ", err)
			}
		}
	}
	return &filter
}

// UpgradeFilters that are applied in websocket upgrade mode
func getUpgradeFilters() []*hcmv3.HttpFilter {
	cors := getCorsHTTPFilter()
	extAauth := getExtAuthzHTTPFilter()
	router := getRouterHTTPFilter()
	upgradeFilters := []*hcmv3.HttpFilter{
		cors,
		extAauth,
		router,
	}
	return upgradeFilters
}

// getRateLimitFilter configures the ratelimit filter
func getRateLimitFilter() *hcmv3.HttpFilter {
	conf, _ := config.ReadConfigs()

	// X-RateLimit Headers
	var enableXRatelimitHeaders rate_limit.RateLimit_XRateLimitHeadersRFCVersion
	if conf.Envoy.RateLimit.XRateLimitHeaders.Enabled {
		switch strings.ToUpper(conf.Envoy.RateLimit.XRateLimitHeaders.RFCVersion) {
		case rate_limit.RateLimit_DRAFT_VERSION_03.String():
			enableXRatelimitHeaders = rate_limit.RateLimit_DRAFT_VERSION_03
		default:
			defaultType := rate_limit.RateLimit_DRAFT_VERSION_03
			logger.LoggerOasparser.Errorf("Invalid XRatelimitHeaders type, continue with default type %s", defaultType)
			enableXRatelimitHeaders = defaultType
		}
	} else {
		enableXRatelimitHeaders = rate_limit.RateLimit_OFF
	}

	rateLimit := &rate_limit.RateLimit{
		Domain:                         RateLimiterDomain,
		FailureModeDeny:                conf.Envoy.RateLimit.FailureModeDeny,
		EnableXRatelimitHeaders:        enableXRatelimitHeaders,
		DisableXEnvoyRatelimitedHeader: true,
		RateLimitService: &envoy_config_ratelimit_v3.RateLimitServiceConfig{
			TransportApiVersion: corev3.ApiVersion_V3,
			GrpcService: &corev3.GrpcService{
				TargetSpecifier: &corev3.GrpcService_EnvoyGrpc_{
					EnvoyGrpc: &corev3.GrpcService_EnvoyGrpc{
						ClusterName: rateLimitClusterName,
					},
				},
				Timeout: &durationpb.Duration{
					Nanos:   (int32(conf.Envoy.RateLimit.RequestTimeoutInMillis) % 1000) * 1000000,
					Seconds: conf.Envoy.RateLimit.RequestTimeoutInMillis / 1000,
				},
			},
		},
	}
	ext, err2 := anypb.New(rateLimit)
	if err2 != nil {
		logger.LoggerOasparser.Errorf("Error occurred while parsing ratelimit filter config. Error: %s", err2.Error())
	}
	rlFilter := hcmv3.HttpFilter{
		Name: rateLimitFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}
	if enableRouterConfigValidation {
		err2 = rlFilter.Validate()
		if err2 != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating the rate limit filter.", err2)
			} else {
				logger.LoggerOasparser.Error("Error while validating the rate limit filter.", err2)
			}
		}
	}
	return &rlFilter
}

// getExtAuthzHTTPFilter gets ExtAauthz http filter.
func getExtAuthzHTTPFilter() *hcmv3.HttpFilter {
	conf, _ := config.ReadConfigs()
	extAuthzConfig := &ext_authv3.ExtAuthz{
		// This would clear the route cache only if there is a header added/removed or changed
		// within ext-authz filter. Without this configuration, the API cannot have production
		// and sandbox endpoints both at once as the cluster is set based on the header added
		// from the ext-authz filter.
		ClearRouteCache:     true,
		TransportApiVersion: corev3.ApiVersion_V3,
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
	ext, err2 := anypb.New(extAuthzConfig)
	if err2 != nil {
		logger.LoggerOasparser.Error(err2)
	}
	extAuthzFilter := hcmv3.HttpFilter{
		Name: extAuthzFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}
	if enableRouterConfigValidation {
		err2 = extAuthzFilter.Validate()
		if err2 != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating the ext authz filter.", err2)
			} else {
				logger.LoggerOasparser.Error("Error while validating the ext authz filter.", err2)
			}
		}
	}
	return &extAuthzFilter
}

// getLuaFilter gets Lua http filter.
func getLuaFilter() *hcmv3.HttpFilter {
	//conf, _ := config.ReadConfigs()
	luaConfig := &luav3.Lua{
		DefaultSourceCode: &corev3.DataSource{
			Specifier: &corev3.DataSource_InlineString{
				InlineString: "function envoy_on_request(request_handle)" +
					"\nend" +
					"\nfunction envoy_on_response(response_handle)" +
					"\nend",
			},
		},
	}
	ext, err2 := anypb.New(luaConfig)
	if err2 != nil {
		logger.LoggerOasparser.Error(err2)
	}
	luaFilter := hcmv3.HttpFilter{
		Name: luaFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}
	if enableRouterConfigValidation {
		err2 = luaFilter.Validate()
		if err2 != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating the lua filter.", err2)
			} else {
				logger.LoggerOasparser.Error("Error while validating the lua filter.", err2)
			}
		}
	}
	return &luaFilter
}

// getHTTPLocalRateLimitFilter returns the local rate limit filter which is used for JWKS endpoint specifically.
func getHTTPLocalRateLimitFilter() *hcmv3.HttpFilter {
	localRateLimitConfig := &local_ratelimit_v3.LocalRateLimit{
		StatPrefix: localRateLimitStatPrefix,
	}
	marshalledRateLimitConfig, err := anypb.New(localRateLimitConfig)
	if err != nil {
		logger.LoggerOasparser.Error("Error while generating the local rate limit filter.", err)
	}
	localRateLimitFilter := &hcmv3.HttpFilter{
		Name: localRatelimitFilterName,
		ConfigType: &hcmv3.HttpFilter_TypedConfig{
			TypedConfig: marshalledRateLimitConfig,
		},
	}

	if enableRouterConfigValidation {
		err = localRateLimitFilter.Validate()
		if err != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating the local rate limit filter.", err)
			} else {
				logger.LoggerOasparser.Error("Error while validating the local rate limit filter.", err)
			}
		}
	}
	return localRateLimitFilter
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
	if enableRouterConfigValidation {
		err = mgwWebSocketFilter.Validate()
		if err != nil {
			if panicOnValidationFailure {
				logger.LoggerOasparser.Fatal("Error while validating web socket filter.", err)
			} else {
				logger.LoggerOasparser.Error("Error while validating web socket filter.", err)
			}
		}
	}
	return &mgwWebSocketFilter

}
