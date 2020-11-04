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
	"io/ioutil"

	access_logv3 "github.com/envoyproxy/go-control-plane/envoy/config/accesslog/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_config_filter_accesslog_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/access_loggers/file/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"

	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/micro-gw/configs"
	logger "github.com/wso2/micro-gw/loggers"
)

func CreateRoutesConfigForRds(vHost routev3.VirtualHost) routev3.RouteConfiguration {
	//TODO: (VirajSalaka) Do we need a custom config here
	rdsConfigName := "default"

	routeConfiguration := routev3.RouteConfiguration{
		Name:         rdsConfigName,
		VirtualHosts: []*routev3.VirtualHost{&vHost},
	}
	return routeConfiguration
}

func CreateListenerWithRds(listenerName string) listenerv3.Listener {
	//TODO: (VirajSalaka) avoid duplicate functions
	httpFilters := getHttpFilters()
	accessLogs := getAccessLogConfigs()
	conf, errReadConfig := configs.ReadConfigs()
	var filters []*listenerv3.Filter

	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}
	//Implemented such that RDS is used
	manager := &hcmv3.HttpConnectionManager{
		CodecType:  hcmv3.HttpConnectionManager_AUTO,
		StatPrefix: "ingress_http",
		RouteSpecifier: &hcmv3.HttpConnectionManager_Rds{
			Rds: &hcmv3.Rds{
				//TODO: (VirajSalaka) Decide if we need this to be configurable in the first stage
				RouteConfigName: "default",
				ConfigSource: &corev3.ConfigSource{
					ConfigSourceSpecifier: &corev3.ConfigSource_Ads{
						Ads: &corev3.AggregatedConfigSource{},
					},
					ResourceApiVersion: corev3.ApiVersion_V3,
				},
			},
		},
		HttpFilters: httpFilters,
		AccessLog:   []*access_logv3.AccessLog{&accessLogs},
	}

	pbst, err := ptypes.MarshalAny(manager)
	if err != nil {
		panic(err)
	}
	connectionManagerFilterP := listenerv3.Filter{
		Name: wellknown.HTTPConnectionManager,
		ConfigType: &listenerv3.Filter_TypedConfig{
			TypedConfig: pbst,
		},
	}

	//add filters
	filters = append(filters, &connectionManagerFilterP)

	listenerAddress := &corev3.Address_SocketAddress{
		SocketAddress: &corev3.SocketAddress{
			Protocol: corev3.SocketAddress_TCP,
			Address:  conf.Envoy.ListenerAddress,
			PortSpecifier: &corev3.SocketAddress_PortValue{
				PortValue: conf.Envoy.ListenerPort,
			},
		},
	}

	listener := listenerv3.Listener{
		Name: listenerName,
		Address: &corev3.Address{
			Address: listenerAddress,
		},
		FilterChains: []*listenerv3.FilterChain{{
			Filters: filters,
		},
		},
	}

	if conf.Envoy.ListenerTlsEnabled {
		tlsCert, err := generateTlsCert(conf.Envoy.ListenerKeyPath, conf.Envoy.ListenerCertPath)
		if err != nil {
			panic(err)
		}
		//TODO: (VirajSalaka) Make it configurable via SDS
		tlsFilter := &tlsv3.DownstreamTlsContext{
			CommonTlsContext: &tlsv3.CommonTlsContext{
				//TlsCertificateSdsSecretConfigs
				TlsCertificates: []*tlsv3.TlsCertificate{&tlsCert},
			},
		}

		marshalledTlsFilter, err := ptypes.MarshalAny(tlsFilter)
		if err != nil {
			panic(err)
		}

		transportSocket := &corev3.TransportSocket{
			Name: "envoy.transport_sockets.tls",
			ConfigType: &corev3.TransportSocket_TypedConfig{
				TypedConfig: marshalledTlsFilter,
			},
		}

		// At the moment, the listener as only one filter chain
		listener.FilterChains[0].TransportSocket = transportSocket
	}
	logger.LoggerOasparser.Errorf("Listener \n %\n", listener)
	return listener
}

/**
 * Create listener filters for envoy.
 *
 * @param routeConfigName   Name of the route config
 * @param vHost  Virtual host
 * @return []*listenerv3.Filter  Listener filters as a array
 */
func createListenerFilters(routeConfigName string, vHost routev3.VirtualHost) []*listenerv3.Filter {
	var filters []*listenerv3.Filter

	//set connection manager filter for production
	managerP := createConectionManagerFilter(vHost, routeConfigName)

	pbst, err := ptypes.MarshalAny(managerP)
	if err != nil {
		panic(err)
	}
	connectionManagerFilterP := listenerv3.Filter{
		Name: wellknown.HTTPConnectionManager,
		ConfigType: &listenerv3.Filter_TypedConfig{
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
func createConectionManagerFilter(vHost routev3.VirtualHost, routeConfigName string) *hcmv3.HttpConnectionManager {

	httpFilters := getHttpFilters()
	accessLogs := getAccessLogConfigs()

	manager := &hcmv3.HttpConnectionManager{
		CodecType:  hcmv3.HttpConnectionManager_AUTO,
		StatPrefix: "ingress_http",
		RouteSpecifier: &hcmv3.HttpConnectionManager_RouteConfig{
			RouteConfig: &routev3.RouteConfiguration{
				Name:         routeConfigName,
				VirtualHosts: []*routev3.VirtualHost{&vHost},
			},
		},
		HttpFilters: httpFilters,
		AccessLog:   []*access_logv3.AccessLog{&accessLogs},
	}
	return manager
}

/**
 * Create a virtual host for envoy listener.
 *
 * @param vHost_Name  Name for virtual host
 * @param routes   Routes of the virtual host
 * @return v3route.VirtualHost  Virtual host instance
 * @return error  Error
 */
func CreateVirtualHost(vHost_Name string, routes []*routev3.Route) (routev3.VirtualHost, error) {

	vHost_Domains := []string{"*"}

	virtual_host := routev3.VirtualHost{
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
func createAddress(remoteHost string, port uint32) corev3.Address {
	address := corev3.Address{Address: &corev3.Address_SocketAddress{
		SocketAddress: &corev3.SocketAddress{
			Address:  remoteHost,
			Protocol: corev3.SocketAddress_TCP,
			PortSpecifier: &corev3.SocketAddress_PortValue{
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
func getAccessLogConfigs() access_logv3.AccessLog {
	var logFormat *envoy_config_filter_accesslog_v3.FileAccessLog_Format
	logpath := "/tmp/envoy.access.log" //default access log path

	logConf, errReadConfig := configs.ReadLogConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Error("Error loading configuration. ", errReadConfig)
	} else {
		logFormat = &envoy_config_filter_accesslog_v3.FileAccessLog_Format{
			Format: logConf.AccessLogs.Format,
		}
		logpath = logConf.AccessLogs.LogFile
	}

	accessLogConf := &envoy_config_filter_accesslog_v3.FileAccessLog{
		Path:            logpath,
		AccessLogFormat: logFormat,
	}

	accessLogTypedConf, err := ptypes.MarshalAny(accessLogConf)
	if err != nil {
		logger.LoggerOasparser.Error("Error marsheling access log configs. ", err)
	}

	access_logs := access_logv3.AccessLog{
		Name:   "envoy.access_loggers.file",
		Filter: nil,
		ConfigType: &access_logv3.AccessLog_TypedConfig{
			TypedConfig: accessLogTypedConf,
		},
		XXX_NoUnkeyedLiteral: struct{}{},
		XXX_unrecognized:     nil,
		XXX_sizecache:        0,
	}

	return access_logs
}

//TODO: (VirajSalaka) Still the following method is not utilized as Sds is not implement. Keeping the Implementation for future reference
func generateDefaultSdsSecretFromConfigfile(privateKeyPath string, pulicKeyPath string) (tlsv3.Secret, error) {
	var secret tlsv3.Secret
	tlsCert, err := generateTlsCert(privateKeyPath, pulicKeyPath)
	if err != nil {
		return secret, err
	}
	secret = tlsv3.Secret{
		Name: "DefaultListenerSecret",
		Type: &tlsv3.Secret_TlsCertificate{
			TlsCertificate: &tlsCert,
		},
	}
	return secret, nil
}

func generateTlsCert(privateKeyPath string, publicKeyPath string) (tlsv3.TlsCertificate, error) {
	var tlsCert tlsv3.TlsCertificate
	privateKeyByteArray, err := readFileAsByteArray(privateKeyPath)
	if err != nil {
		return tlsCert, err
	}
	publicKeyByteArray, err := readFileAsByteArray(publicKeyPath)
	if err != nil {
		return tlsCert, err
	}
	tlsCert = tlsv3.TlsCertificate{
		PrivateKey: &corev3.DataSource{
			Specifier: &corev3.DataSource_InlineBytes{
				InlineBytes: privateKeyByteArray,
			},
		},
		CertificateChain: &corev3.DataSource{
			Specifier: &corev3.DataSource_InlineBytes{
				InlineBytes: publicKeyByteArray,
			},
		},
	}
	return tlsCert, nil
}

func readFileAsByteArray(filepath string) ([]byte, error) {
	content, readErr := ioutil.ReadFile(filepath)
	if readErr != nil {
		logger.LoggerOasparser.Errorf("Error reading File : %v ", filepath, readErr)
	}
	return content, readErr
}
