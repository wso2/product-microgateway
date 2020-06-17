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
	v2 "github.com/envoyproxy/go-control-plane/envoy/api/v2"
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	listenerv2 "github.com/envoyproxy/go-control-plane/envoy/api/v2/listener"
	v2route "github.com/envoyproxy/go-control-plane/envoy/api/v2/route"
	envoy_config_filter_accesslog_v2 "github.com/envoyproxy/go-control-plane/envoy/config/filter/accesslog/v2"
	hcm "github.com/envoyproxy/go-control-plane/envoy/config/filter/network/http_connection_manager/v2"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes"
	structpb "github.com/golang/protobuf/ptypes/struct"
	"github.com/wso2/micro-gw/configs"
	logger "github.com/wso2/micro-gw/internal/loggers"
)

func CreateListener(listenerName string, routeConfigName string, vHostP v2route.VirtualHost) v2.Listener {
	conf, errReadConfig := configs.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}

	listenerAddress := &core.Address_SocketAddress{
		SocketAddress: &core.SocketAddress{
			Protocol: core.SocketAddress_TCP,
			Address:  conf.Envoy.ListenerAddress,
			PortSpecifier: &core.SocketAddress_PortValue{
				PortValue: conf.Envoy.ListenerPort,
			},
		},
	}
	listenerFilters := createListenerFilters(routeConfigName, vHostP)

	listener := v2.Listener{
		Name: listenerName,
		Address: &core.Address{
			Address: listenerAddress,
		},
		FilterChains: []*listenerv2.FilterChain{{
			Filters: listenerFilters},
		},
	}
	return listener
}

func createListenerFilters(routeConfigName string, vHost v2route.VirtualHost) []*listenerv2.Filter {
	var filters []*listenerv2.Filter

	//set connection manager filter for production
	managerP := createConectionManagerFilter(vHost, routeConfigName)

	pbst, err := ptypes.MarshalAny(managerP)
	if err != nil {
		panic(err)
	}
	connectionManagerFilterP := listenerv2.Filter{
		Name: wellknown.HTTPConnectionManager,
		ConfigType: &listenerv2.Filter_TypedConfig{
			TypedConfig: pbst,
		},
	}

	//add filters
	filters = append(filters, &connectionManagerFilterP)
	return filters
}

func createConectionManagerFilter(vHost v2route.VirtualHost, routeConfigName string) *hcm.HttpConnectionManager {

	httpFilters := getHttpFilters()
	accessLogs := getAccessLogConfigs()

	manager := &hcm.HttpConnectionManager{
		CodecType:  hcm.HttpConnectionManager_AUTO,
		StatPrefix: "ingress_http",
		RouteSpecifier: &hcm.HttpConnectionManager_RouteConfig{
			RouteConfig: &v2.RouteConfiguration{
				Name:         routeConfigName,
				VirtualHosts: []*v2route.VirtualHost{&vHost},
			},
		},
		HttpFilters: httpFilters,
		AccessLog: []*envoy_config_filter_accesslog_v2.AccessLog{&accessLogs},
	}
	return manager
}

func CreateVirtualHost(vHost_Name string, routes []*v2route.Route) (v2route.VirtualHost, error) {

	vHost_Domains := []string{"*"}

	virtual_host := v2route.VirtualHost{
		Name:    vHost_Name,
		Domains: vHost_Domains,
		Routes:  routes,
	}
	return virtual_host, nil
}

func createAddress(remoteHost string, Port uint32) core.Address {
	address := core.Address{Address: &core.Address_SocketAddress{
		SocketAddress: &core.SocketAddress{
			Address:  remoteHost,
			Protocol: core.SocketAddress_TCP,
			PortSpecifier: &core.SocketAddress_PortValue{
				PortValue: uint32(Port),
			},
		},
	}}
	return address
}

func getAccessLogConfigs() envoy_config_filter_accesslog_v2.AccessLog {
	logFormat := ""

	logConfig := &structpb.Struct{
		Fields: map[string]*structpb.Value{
			"path": {
				Kind: &structpb.Value_StringValue{
					StringValue: "/tmp/envoy.access.log",
				},
			},
		},
	}

	logConf, errReadConfig := configs.ReadLogConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Error("Error loading configuration. ", errReadConfig)
	} else {
		logFormat = logConf.AccessLogs.Format
	}

	if logFormat != "" {
		logConfig = &structpb.Struct{
			Fields: map[string]*structpb.Value{
				"path": {
					Kind: &structpb.Value_StringValue{
						StringValue: "/tmp/envoy.access.log",
					},
				},
				"format": {
					Kind: &structpb.Value_StringValue{
						StringValue: logConf.AccessLogs.Format,
					},
				},
			},
		}
	}

	access_log := envoy_config_filter_accesslog_v2.AccessLog{
		Name:                 "envoy.file_access_log",
		ConfigType: &envoy_config_filter_accesslog_v2.AccessLog_Config{
			Config: logConfig,
		} ,
		XXX_NoUnkeyedLiteral: struct{}{},
		XXX_unrecognized:     nil,
		XXX_sizecache:        0,
	}

	return access_log
}