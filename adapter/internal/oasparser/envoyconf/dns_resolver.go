/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 */

package envoyconf

import (
	"fmt"
	"time"

	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	dnsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/clusters/common/dns/v3"
	dnsclusterv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/clusters/dns/v3"
	caresv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/network/dns_resolver/cares/v3"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/durationpb"
)

func getDNSResolverConf() (*corev3.TypedExtensionConfig, error) {
	conf, _ := config.ReadConfigs()
	var dnsResolverConf proto.Message

	switch conf.Envoy.Upstream.DNS.DNSResolver.ResolverType {
	case "": // Use Envoy default settings
		return nil, nil
	case config.DNSResolverCAres:
		resolvers := []*corev3.Address{}
		for _, resolver := range conf.Envoy.Upstream.DNS.DNSResolver.CAres.Resolvers {
			protocol := corev3.SocketAddress_Protocol_value[resolver.Protocol]
			resolvers = append(resolvers, &corev3.Address{
				Address: &corev3.Address_SocketAddress{
					SocketAddress: &corev3.SocketAddress{
						Protocol: corev3.SocketAddress_Protocol(protocol),
						Address:  resolver.Address,
						PortSpecifier: &corev3.SocketAddress_PortValue{
							PortValue: resolver.Port,
						},
					},
				},
			})
		}

		dnsResolverConf = &caresv3.CaresDnsResolverConfig{
			Resolvers:                resolvers,
			UseResolversAsFallback:   conf.Envoy.Upstream.DNS.DNSResolver.CAres.UseResolversAsFallback,
			FilterUnroutableFamilies: conf.Envoy.Upstream.DNS.DNSResolver.CAres.FilterUnroutableFamilies,
			DnsResolverOptions: &corev3.DnsResolverOptions{
				UseTcpForDnsLookups:   conf.Envoy.Upstream.DNS.DNSResolver.CAres.UseTCPForDNSLookups,
				NoDefaultSearchDomain: conf.Envoy.Upstream.DNS.DNSResolver.CAres.NoDefaultSearchDomain,
			},
		}
	// case config.DNS_RESOLVER_APPLE: // If required we can support other resolvers here
	default:
		return nil, fmt.Errorf("unsupported DNS resolver type: %s", conf.Envoy.Upstream.DNS.DNSResolver.ResolverType)
	}

	dnsResolverConfPbAny, err := anypb.New(dnsResolverConf)
	if err != nil {
		return nil, err
	}

	dnsResolverConfig := &corev3.TypedExtensionConfig{
		Name:        "Upstream DNS resolver",
		TypedConfig: dnsResolverConfPbAny,
	}

	if enableRouterConfigValidation {
		err = dnsResolverConfig.Validate()
		if err != nil {
			logger.LoggerOasparser.Error("Error while validating DNS Resolver configs. ", err)
		}
	}
	return dnsResolverConfig, nil
}

func getDNSClusterConfig() (*clusterv3.Cluster_CustomClusterType, error) {
	conf, _ := config.ReadConfigs()
	var dnsClusterConf proto.Message

	dnsResolverConf, err := getDNSResolverConf()
	if err != nil {
		return nil, err
	}

	// Calculate optimal refresh rate and jitter to reduce socket churn
    refreshRate := time.Duration(conf.Envoy.Upstream.DNS.DNSRefreshRate) * time.Millisecond
    if refreshRate < 30*time.Second {
        // Minimum refresh rate to prevent excessive DNS queries
        refreshRate = 30 * time.Second
        logger.LoggerOasparser.Warnf("DNS refresh rate increased to %v to reduce CPU overhead", refreshRate)
    }

    // Reduce jitter to minimize unnecessary variance
    jitter := refreshRate / 10 // 10% jitter instead of fixed 1 second
    if jitter > 5*time.Second {
        jitter = 5 * time.Second
    }

    dnsClusterConf = &dnsclusterv3.DnsCluster{
        DnsRefreshRate:         durationpb.New(refreshRate),
    	RespectDnsTtl:          true, // Respect DNS TTL to reduce unnecessary queries
    	DnsJitter:              durationpb.New(jitter),
    	TypedDnsResolverConfig: dnsResolverConf,
    	DnsLookupFamily:        dnsv3.DnsLookupFamily_V4_ONLY,
    }

	dnsClusterConfPbAny, err := anypb.New(dnsClusterConf)
	if err != nil {
		return nil, err
	}

	dnsClusterConfig := &clusterv3.Cluster_CustomClusterType{
		Name:        "DNS Cluster",
		TypedConfig: dnsClusterConfPbAny,
	}

	if enableRouterConfigValidation {
		err = dnsClusterConfig.Validate()
		if err != nil {
			logger.LoggerOasparser.Error("Error while validating DNS Cluster configs. ", err)
		}
	}
	return dnsClusterConfig, nil
}
