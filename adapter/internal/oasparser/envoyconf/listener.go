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
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_config_trace_v3 "github.com/envoyproxy/go-control-plane/envoy/config/trace/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes"
	"github.com/golang/protobuf/ptypes/wrappers"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/durationpb"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

var retryBannedVhosts map[string]struct{}

func init() {
	retryBannedVhosts := make(map[string]struct{})
	if os.Getenv("ROUTER_CONNECTION_FAILURE_RETRY_BANNED_VHOSTS") != "" {
		retryBannedVhostsList := strings.Split(os.Getenv("ROUTER_CONNECTION_FAILURE_RETRY_BANNED_VHOSTS"), ",")
		for _, vhost := range retryBannedVhostsList {
			retryBannedVhosts[vhost] = struct{}{}
		}
	}
}

// CreateRoutesConfigForRds generates the default RouteConfiguration.
// Only the provided virtual hosts will be assigned inside the configuration.
// This is used to provide the configuration for RDS.
func CreateRoutesConfigForRds(vHosts []*routev3.VirtualHost) *routev3.RouteConfiguration {
	rdsConfigName := defaultRdsConfigName
	routeConfiguration := routev3.RouteConfiguration{
		Name:                   rdsConfigName,
		VirtualHosts:           vHosts,
		RequestHeadersToRemove: []string{clusterHeaderName, gatewayURLHeaderName, choreoTestSessionHeaderName},
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
		LocalReplyConfig: &hcmv3.LocalReplyConfig{
			Mappers: getErrorResponseMappers(),
		},
		RequestTimeout:        ptypes.DurationProto(conf.Envoy.Connection.Timeouts.RequestTimeoutInSeconds * time.Second),        // default disabled
		RequestHeadersTimeout: ptypes.DurationProto(conf.Envoy.Connection.Timeouts.RequestHeadersTimeoutInSeconds * time.Second), // default disabled
		StreamIdleTimeout:     ptypes.DurationProto(conf.Envoy.Connection.Timeouts.StreamIdleTimeoutInSeconds * time.Second),     // Default 5 mins
		CommonHttpProtocolOptions: &corev3.HttpProtocolOptions{
			IdleTimeout: ptypes.DurationProto(conf.Envoy.Connection.Timeouts.IdleTimeoutInSeconds * time.Second), // Default 1 hr
		},
	}

	if len(accessLogs) > 0 {
		manager.AccessLog = accessLogs
	}

	if conf.Tracing.Enabled && conf.Tracing.Type != TracerTypeAzure {
		if tracing, err := getTracing(conf); err == nil {
			manager.Tracing = tracing
			manager.GenerateRequestId = &wrappers.BoolValue{Value: conf.Tracing.Enabled}
		} else {
			logger.LoggerOasparser.Error("Failed to initialize tracing. Router tracing will be disabled. ", err)
			conf.Tracing.Enabled = false
		}
	}

	pbst, err := anypb.New(manager)
	if err != nil {
		logger.LoggerOasparser.Fatal(err)
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

		tlsCert := generateTLSCert(conf.Envoy.KeyStore.KeyPath, conf.Envoy.KeyStore.CertPath)
		//TODO: (VirajSalaka) Make it configurable via SDS
		tlsFilter := &tlsv3.DownstreamTlsContext{
			CommonTlsContext: &tlsv3.CommonTlsContext{
				//TlsCertificateSdsSecretConfigs
				TlsCertificates: []*tlsv3.TlsCertificate{tlsCert},
			},
		}

		marshalledTLSFilter, err := anypb.New(tlsFilter)
		if err != nil {
			logger.LoggerOasparser.Fatal("Error while Marshalling the downstream TLS Context for the configuration.")
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
		logger.LoggerOasparser.Fatal(err)
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
			Domains: []string{vhost, fmt.Sprint(vhost, ":*")},
			Routes:  routes,
		}

		_, retryBanned := retryBannedVhosts[vhost]

		if os.Getenv("ROUTER_CONNECTION_FAILURE_RETRY_ENABLED") != "" && !retryBanned {
			config, _ := config.ReadConfigs()
			// Retry configs are always added via headers. This is to update the
			// default retry back-off base interval, which cannot be updated via headers.
			retryConfig := config.Envoy.Upstream.Retry
			maxInterval := retryConfig.MaxInterval
			if retryConfig.MaxInterval < retryConfig.BaseInterval {
				maxInterval = retryConfig.BaseInterval
			}
			commonRetryPolicy := &routev3.RetryPolicy{
				RetryOn: retryConfig.RetryOn,
				NumRetries: &wrapperspb.UInt32Value{
					Value: retryConfig.MaxRetryCount,
					// If not set to 0, default value 1 will be
					// applied to both prod and sandbox even if they are not set.
				},
				RetryBackOff: &routev3.RetryPolicy_RetryBackOff{
					BaseInterval: durationpb.New(retryConfig.BaseInterval),
					MaxInterval:  durationpb.New(maxInterval),
				},
			}
			virtualHost.RetryPolicy = commonRetryPolicy
		}
		virtualHosts = append(virtualHosts, virtualHost)
	}
	return virtualHosts
}

// TODO: (VirajSalaka) Still the following method is not utilized as Sds is not implement. Keeping the Implementation for future reference
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

func getTracing(conf *config.Config) (*hcmv3.HttpConnectionManager_Tracing, error) {
	var endpoint string
	var maxPathLength uint32

	if endpoint = conf.Tracing.ConfigProperties[tracerEndpoint]; len(endpoint) <= 0 {
		return nil, errors.New("Invalid endpoint path provided for tracing endpoint")
	}
	if length, err := strconv.ParseUint(conf.Tracing.ConfigProperties[tracerMaxPathLength], 10, 32); err == nil {
		maxPathLength = uint32(length)
	} else {
		return nil, errors.New("Invalid max path length provided for tracing endpoint")
	}

	providerConf := &envoy_config_trace_v3.ZipkinConfig{
		CollectorCluster:         tracingClusterName,
		CollectorEndpoint:        endpoint,
		CollectorEndpointVersion: envoy_config_trace_v3.ZipkinConfig_HTTP_JSON,
	}

	typedConf, err := anypb.New(providerConf)
	if err != nil {
		return nil, err
	}

	tracing := &hcmv3.HttpConnectionManager_Tracing{
		Provider: &envoy_config_trace_v3.Tracing_Http{
			Name: tracerNameZipkin,
			ConfigType: &envoy_config_trace_v3.Tracing_Http_TypedConfig{
				TypedConfig: typedConf,
			},
		},
		MaxPathTagLength: &wrappers.UInt32Value{Value: maxPathLength},
	}

	return tracing, nil
}
