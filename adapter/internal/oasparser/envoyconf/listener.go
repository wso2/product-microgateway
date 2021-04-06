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

package envoyconf

import (
	"errors"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes"
	"github.com/golang/protobuf/ptypes/wrappers"

	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
)

// CreateRoutesConfigForRds generates the default RouteConfiguration.
// Only the provided virtual hosts will be assigned inside the configuration.
// This is used to provide the configuration for RDS.
func CreateRoutesConfigForRds(vHosts []*routev3.VirtualHost) *routev3.RouteConfiguration {
	rdsConfigName := defaultRdsConfigName
	routeConfiguration := routev3.RouteConfiguration{
		Name:         rdsConfigName,
		VirtualHosts: vHosts,
	}
	return &routeConfiguration
}

// CreateListenersWithRds create two listeners or one listener with the Route Configuration
// stated as RDS. (routes are not assigned directly to the listener.) RouteConfiguration name
// is assigned using its default value. Route Configuration would be resolved via ADS.
//
// If SecuredListenerPort and ListenerPort both are mentioned, two listeners would be added.
// If neither of the two properies are assigned with non-zero values, adapter would panic.
//
// HTTPConnectionManager with HTTP Filters, Accesslog configuration, TransportSocket
// Configuration is included within the implementation.
//
// Listener Address, ListenerPort Value, SecuredListener Address, and  SecuredListenerPort Values are
// fetched from the configuration accordingly.
//
// The relevant private keys and certificates (for securedListener) are fetched from the filepath
// mentioned in the adapter configuration. These certificate, key values are added
// as inline records (base64 encoded).
func CreateListenersWithRds() []*listenerv3.Listener {
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}
	return createListeners(conf)
}

func createListeners(conf *config.Config) []*listenerv3.Listener {
	httpFilters := getHTTPFilters()
	upgradeFilters := getUpgradeFilters()
	accessLogs := getAccessLogs()
	var filters []*listenerv3.Filter
	var listeners []*listenerv3.Listener

	manager := &hcmv3.HttpConnectionManager{
		CodecType:  hcmv3.HttpConnectionManager_AUTO,
		StatPrefix: httpConManagerStartPrefix,
		// WebSocket upgrades enabled from the HCM
		UpgradeConfigs: []*hcmv3.HttpConnectionManager_UpgradeConfig{{
			UpgradeType: "websocket",
			Enabled:     &wrappers.BoolValue{Value: true},
			Filters:     upgradeFilters,
		}},
		RouteSpecifier: &hcmv3.HttpConnectionManager_Rds{
			Rds: &hcmv3.Rds{
				RouteConfigName: defaultRdsConfigName,
				ConfigSource: &corev3.ConfigSource{
					ConfigSourceSpecifier: &corev3.ConfigSource_Ads{
						Ads: &corev3.AggregatedConfigSource{},
					},
					ResourceApiVersion: corev3.ApiVersion_V3,
				},
			},
		},
		HttpFilters: httpFilters,
	}

	if len(accessLogs) > 0 {
		manager.AccessLog = accessLogs
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

	// add filters
	filters = append(filters, &connectionManagerFilterP)

	if conf.Envoy.SecuredListenerPort > 0 {
		listenerHostAddress := defaultListenerHostAddress
		if len(conf.Envoy.SecuredListenerHost) > 0 {
			listenerHostAddress = conf.Envoy.SecuredListenerHost
		}
		securedListenerAddress := &corev3.Address_SocketAddress{
			SocketAddress: &corev3.SocketAddress{
				Protocol: corev3.SocketAddress_TCP,
				Address:  listenerHostAddress,
				PortSpecifier: &corev3.SocketAddress_PortValue{
					PortValue: conf.Envoy.SecuredListenerPort,
				},
			},
		}

		securedListener := listenerv3.Listener{
			Name: defaultHTTPSListenerName,
			Address: &corev3.Address{
				Address: securedListenerAddress,
			},
			FilterChains: []*listenerv3.FilterChain{{
				Filters: filters,
			},
			},
		}

		tlsCert := generateTLSCert(conf.Envoy.KeyStore.PrivateKeyLocation, conf.Envoy.KeyStore.PublicKeyLocation)
		//TODO: (VirajSalaka) Make it configurable via SDS
		tlsFilter := &tlsv3.DownstreamTlsContext{
			CommonTlsContext: &tlsv3.CommonTlsContext{
				//TlsCertificateSdsSecretConfigs
				TlsCertificates: []*tlsv3.TlsCertificate{tlsCert},
			},
		}

		marshalledTLSFilter, err := ptypes.MarshalAny(tlsFilter)
		if err != nil {
			logger.LoggerOasparser.Fatal("Error while Marshalling the downstream TLS Context for the configuration.")
			panic(err)
		}

		transportSocket := &corev3.TransportSocket{
			Name: transportSocketName,
			ConfigType: &corev3.TransportSocket_TypedConfig{
				TypedConfig: marshalledTLSFilter,
			},
		}

		// At the moment, the listener as only one filter chain
		securedListener.FilterChains[0].TransportSocket = transportSocket
		listeners = append(listeners, &securedListener)
		logger.LoggerOasparser.Infof("Secured Listener is added. %s : %d", listenerHostAddress, conf.Envoy.SecuredListenerPort)
	} else {
		logger.LoggerOasparser.Info("No SecuredListenerPort is included.")
	}

	if conf.Envoy.ListenerPort > 0 {
		listenerHostAddress := defaultListenerHostAddress
		if len(conf.Envoy.ListenerHost) > 0 {
			listenerHostAddress = conf.Envoy.ListenerHost
		}
		listenerAddress := &corev3.Address_SocketAddress{
			SocketAddress: &corev3.SocketAddress{
				Protocol: corev3.SocketAddress_TCP,
				Address:  listenerHostAddress,
				PortSpecifier: &corev3.SocketAddress_PortValue{
					PortValue: conf.Envoy.ListenerPort,
				},
			},
		}

		listener := listenerv3.Listener{
			Name: defaultHTTPListenerName,
			Address: &corev3.Address{
				Address: listenerAddress,
			},
			FilterChains: []*listenerv3.FilterChain{{
				Filters: filters,
			},
			},
		}
		listeners = append(listeners, &listener)
		logger.LoggerOasparser.Infof("Non-secured Listener is added. %s : %d", listenerHostAddress, conf.Envoy.ListenerPort)
	} else {
		logger.LoggerOasparser.Info("No Non-securedListenerPort is included.")
	}

	if len(listeners) == 0 {
		err := errors.New("No Listeners are configured as no port value is mentioned under securedListenerPort or ListenerPort")
		panic(err)
	}
	return listeners
}

// CreateVirtualHosts creates VirtualHost configurations for envoy which serves
// request from the vHost domain. The routes array will be included as the routes
// for the created virtual host.
func CreateVirtualHosts(vhostToRouteArrayMap map[string][]*routev3.Route) []*routev3.VirtualHost {
	virtualHosts := make([]*routev3.VirtualHost, 0, len(vhostToRouteArrayMap))
	for vhost, routes := range vhostToRouteArrayMap {
		virtualHost := &routev3.VirtualHost{
			Name:    vhost,
			Domains: []string{vhost},
			Routes:  routes,
		}
		virtualHosts = append(virtualHosts, virtualHost)
	}
	return virtualHosts
}

//TODO: (VirajSalaka) Still the following method is not utilized as Sds is not implement. Keeping the Implementation for future reference
func generateDefaultSdsSecretFromConfigfile(privateKeyPath string, pulicKeyPath string) (*tlsv3.Secret, error) {
	var secret tlsv3.Secret
	tlsCert := generateTLSCert(privateKeyPath, pulicKeyPath)
	secret = tlsv3.Secret{
		Name: defaultListenerSecretConfigName,
		Type: &tlsv3.Secret_TlsCertificate{
			TlsCertificate: tlsCert,
		},
	}
	return &secret, nil
}

// generateTLSCert generates the TLS Certiificate with given private key filepath and the corresponding public Key filepath.
// The files should be mounted to the router container unless the default cert is used.
func generateTLSCert(privateKeyPath string, publicKeyPath string) *tlsv3.TlsCertificate {
	var tlsCert tlsv3.TlsCertificate
	tlsCert = tlsv3.TlsCertificate{
		PrivateKey: &corev3.DataSource{
			Specifier: &corev3.DataSource_Filename{
				Filename: privateKeyPath,
			},
		},
		CertificateChain: &corev3.DataSource{
			Specifier: &corev3.DataSource_Filename{
				Filename: publicKeyPath,
			},
		},
	}
	return &tlsCert
}
