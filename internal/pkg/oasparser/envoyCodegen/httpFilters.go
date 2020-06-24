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
package envoyCodegen

import (
	core "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	hcm "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	ext_auth "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/ext_authz/v3"
	router "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/router/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"

	logger "github.com/wso2/micro-gw/internal/loggers"
	"github.com/golang/protobuf/ptypes"
)

/**
 * Append all the http filters.
 *
 * @return []*hcm.HttpFilter  Http filter set as a array
 */
func getHttpFilters() []*hcm.HttpFilter {
	//extAauth := GetExtAauthzHttpFilter()
	router := getRouterHttpFilter()

	httpFilters := []*hcm.HttpFilter{
		//&extAauth,
		&router,
	}

	return httpFilters
}

/**
 * Get router http filter.
 *
 * @return hcm.HttpFilter  Http filter instance
 */
func getRouterHttpFilter() hcm.HttpFilter {

	routeFilterConf := router.Router{
		DynamicStats:             nil,
		StartChildSpan:           false,
		UpstreamLog:              nil,
		SuppressEnvoyHeaders:     false,
		StrictCheckHeaders:       nil,
		RespectExpectedRqTimeout: false,
		XXX_NoUnkeyedLiteral:     struct{}{},
		XXX_unrecognized:         nil,
		XXX_sizecache:            0,
	}

	routeFilterTypedConf, err := ptypes.MarshalAny(&routeFilterConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marshaling route filter configs. ", err)
	}

	filter := hcm.HttpFilter{
		Name:                 wellknown.Router,
		ConfigType:           &hcm.HttpFilter_TypedConfig{TypedConfig: routeFilterTypedConf},
		XXX_NoUnkeyedLiteral: struct{}{},
		XXX_unrecognized:     nil,
		XXX_sizecache:        0,
	}

	return filter
}

/**
 * Get ExtAauthz http filter.
 *
 * @return hcm.HttpFilter  Http filter instance
 */
func getExtAauthzHttpFilter() hcm.HttpFilter {
	extAuthzConfig := &ext_auth.ExtAuthz{
		WithRequestBody: &ext_auth.BufferSettings{
			MaxRequestBytes:     1024,
			AllowPartialMessage: false,
		},
		Services: &ext_auth.ExtAuthz_GrpcService{
			GrpcService: &core.GrpcService{
				TargetSpecifier: &core.GrpcService_EnvoyGrpc_{
					EnvoyGrpc: &core.GrpcService_EnvoyGrpc{
						ClusterName: "ext-authz",
					},
				},
			},
		},
	}
	ext, err2 := ptypes.MarshalAny(extAuthzConfig)
	if err2 != nil {
		logger.LoggerOasparser.Error(err2)
	}
	extAuthzFilter := hcm.HttpFilter{
		Name: "envoy.filters.http.ext_authz",
		ConfigType: &hcm.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}

	return extAuthzFilter
}
