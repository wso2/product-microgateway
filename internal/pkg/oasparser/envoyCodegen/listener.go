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
	listener "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	route "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	core "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	envoy_config_filter_accesslog_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/file/v3"
	access_log "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	hcm "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"

	logger "github.com/wso2/micro-gw/internal/loggers"
	"github.com/wso2/micro-gw/configs"
	"github.com/golang/protobuf/ptypes"
)

/**
 * Create a listener for envoy.
 *
 * @param listenerName   Name of the listener
 * @param routeConfigName   Name of the route config
 * @param vHostP  Virtual host
 * @return v2.Listener  V2 listener instance
 */
func CreateListener(listenerName string, routeConfigName string, vHostP route.VirtualHost) listener.Listener {
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

	listener := listener.Listener{
		Name: listenerName,
		Address: &core.Address{
			Address: listenerAddress,
		},
		FilterChains: []*listener.FilterChain{{
			Filters: listenerFilters},
		},
	}
	return listener
}

/**
 * Create listener filters for envoy.
 *
 * @param routeConfigName   Name of the route config
 * @param vHost  Virtual host
 * @return []*listenerv2.Filter  Listener filters as a array
 */
func createListenerFilters(routeConfigName string, vHost route.VirtualHost) []*listener.Filter {
	var filters []*listener.Filter

	//set connection manager filter for production
	managerP := createConectionManagerFilter(vHost, routeConfigName)

	pbst, err := ptypes.MarshalAny(managerP)
	if err != nil {
		panic(err)
	}
	connectionManagerFilterP := listener.Filter{
		Name: wellknown.HTTPConnectionManager,
		ConfigType: &listener.Filter_TypedConfig{
			TypedConfig: pbst,
		},
	}

	//add filters
	filters = append(filters, &connectionManagerFilterP)
	return filters
}

/**
 * Create connection manager filter.
 *
 * @param vHostP  Virtual host
 * @param routeConfigName   Name of the route config
 * @return *hcm.HttpConnectionManager  Reference for a connection manager instance
 */
func createConectionManagerFilter(vHost route.VirtualHost, routeConfigName string) *hcm.HttpConnectionManager {

	httpFilters := getHttpFilters()
	accessLogs := getAccessLogConfigs()

	manager := &hcm.HttpConnectionManager{
		CodecType:  hcm.HttpConnectionManager_AUTO,
		StatPrefix: "ingress_http",
		RouteSpecifier: &hcm.HttpConnectionManager_RouteConfig{
			RouteConfig: &route.RouteConfiguration{
				Name:         routeConfigName,
				VirtualHosts: []*route.VirtualHost{&vHost},
			},
		},
		HttpFilters: httpFilters,
		AccessLog: []*access_log.AccessLog{&accessLogs},
	}
	return manager
}

/**
 * Create a virtual host for envoy listener.
 *
 * @param vHost_Name  Name for virtual host
 * @param routes   Routes of the virtual host
 * @return v2route.VirtualHost  Virtual host instance
 * @return error  Error
 */
func CreateVirtualHost(vHost_Name string, routes []*route.Route) (route.VirtualHost, error) {

	vHost_Domains := []string{"*"}

	virtual_host := route.VirtualHost{
		Name:    vHost_Name,
		Domains: vHost_Domains,
		Routes:  routes,
	}
	return virtual_host, nil
}

/**
 * Create a socket address.
 *
 * @param remoteHost  Host address or host ip
 * @param port  Port
 * @return core.Address  Endpoint as a core address
 */
func createAddress(remoteHost string, port uint32) core.Address {
	address := core.Address{Address: &core.Address_SocketAddress{
		SocketAddress: &core.SocketAddress{
			Address:  remoteHost,
			Protocol: core.SocketAddress_TCP,
			PortSpecifier: &core.SocketAddress_PortValue{
				PortValue: uint32(port),
			},
		},
	}}
	return address
}

/**
 * Get access log configs for envoy.
 *
 * @return envoy_config_filter_accesslog_v2.AccessLog  Access log config
 */
func getAccessLogConfigs() access_log.AccessLog {
	var logFormat *envoy_config_filter_accesslog_v3.FileAccessLog_Format
	logpath := "/tmp/envoy.access.log"   //default access log path

	logConf, errReadConfig := configs.ReadLogConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Error("Error loading configuration. ", errReadConfig)
	} else {
		logFormat = &envoy_config_filter_accesslog_v3.FileAccessLog_Format{
			Format:  logConf.AccessLogs.Format,
		}
		logpath = logConf.AccessLogs.LogFile
	}

	accessLogConf := &envoy_config_filter_accesslog_v3.FileAccessLog{
		Path:   logpath,
		AccessLogFormat: logFormat,
	}

	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling access log configs. ", err)
	}

	access_logs := access_log.AccessLog{
		Name:                 "envoy.access_loggers.file",
		Filter:               nil,
		ConfigType:           &access_log.AccessLog_TypedConfig{
			TypedConfig: accessLogTypedConf,
		},
		XXX_NoUnkeyedLiteral: struct{}{},
		XXX_unrecognized:     nil,
		XXX_sizecache:        0,
	}

	return access_logs
}
