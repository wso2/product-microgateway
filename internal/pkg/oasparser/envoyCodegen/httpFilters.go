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
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	envoyconfigfilterhttpextauthzv2 "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	hcm "github.com/envoyproxy/go-control-plane/envoy/config/filter/network/http_connection_manager/v2"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes"
)

func getHttpFilters() []*hcm.HttpFilter {
	//extAauth := GetExtAauthzHttpFilter()
	router := getRouterHttpFilter()

	httpFilters := []*hcm.HttpFilter{
		//&extAauth,
		&router,
	}

	return httpFilters
}

func getRouterHttpFilter() hcm.HttpFilter {
	return hcm.HttpFilter{Name: wellknown.Router}
}

func getExtAauthzHttpFilter() hcm.HttpFilter {
	extAuthzConfig := &envoyconfigfilterhttpextauthzv2.ExtAuthz{
		WithRequestBody: &envoyconfigfilterhttpextauthzv2.BufferSettings{
			MaxRequestBytes:     1024,
			AllowPartialMessage: false,
		},
		Services: &envoyconfigfilterhttpextauthzv2.ExtAuthz_GrpcService{
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
		panic(err2)
	}
	extAuthzFilter := hcm.HttpFilter{
		Name: "envoy.filters.http.ext_authz",
		ConfigType: &hcm.HttpFilter_TypedConfig{
			TypedConfig: ext,
		},
	}

	return extAuthzFilter
}
