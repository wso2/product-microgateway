/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"testing"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	listenerv3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	cors_filter_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/cors/v3"
	hcmv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/network/http_connection_manager/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/types/known/anypb"
)

func TestCreateListenerWithRds(t *testing.T) {
	// TODO: (Vajira) Add more test scenarios
	listeners := CreateListenersWithRds()
	assert.NotEmpty(t, listeners, "Listeners creation has been failed")
	assert.Equal(t, 2, len(listeners), "Two listeners are not created.")

	securedListener := listeners[0]
	if securedListener.Validate() != nil {
		t.Error("Listener validation failed")
	}
	assert.Equal(t, "0.0.0.0", securedListener.GetAddress().GetSocketAddress().GetAddress(),
		"Address mismatch for secured Listener.")
	assert.Equal(t, uint32(9095), securedListener.GetAddress().GetSocketAddress().GetPortValue(),
		"Address mismatch for secured Listener.")
	assert.NotEmpty(t, securedListener.FilterChains, "Filter chain for listener should not be null.")
	assert.NotNil(t, securedListener.FilterChains[0].GetTransportSocket(),
		"Transport Socket should not be null for secured listener")

	nonSecuredListener := listeners[1]
	if nonSecuredListener.Validate() != nil {
		t.Error("Listener validation failed")
	}
	assert.Equal(t, "0.0.0.0", nonSecuredListener.GetAddress().GetSocketAddress().GetAddress(),
		"Address mismatch for non-secured Listener.")
	assert.Equal(t, uint32(9090), nonSecuredListener.GetAddress().GetSocketAddress().GetPortValue(),
		"Address mismatch for non-secured Listener.")
	assert.NotEmpty(t, nonSecuredListener.FilterChains, "Filter chain for listener should not be null.")
	assert.Nil(t, nonSecuredListener.FilterChains[0].GetTransportSocket(),
		"Transport Socket should be null for non-secured listener")

	for _, listener := range listeners {
		assert.Equal(t, isCasePreserveEnabled(t, listener), false)
	}

}

func TestCreateVirtualHost(t *testing.T) {
	// TODO: (Vajira) Add more test scenarios

	vhostToRouteArrayMap := map[string][]*routev3.Route{
		"*":           testCreateRoutesForUnitTests(t),
		"mg.wso2.com": testCreateRoutesForUnitTests(t),
	}
	vHosts := CreateVirtualHosts(vhostToRouteArrayMap)

	if len(vHosts) != 2 {
		t.Error("Virtual Host creation failed")
	}

	for _, vHost := range vHosts {
		_, found := vhostToRouteArrayMap[vHost.Name]
		if found {
			if vHost.Domains[0] != vHost.Name {
				t.Errorf("Virtual Host domain mismatched, expected %s but found %s",
					vHost.Name, vHost.Domains[0])
			}
		} else {
			t.Errorf("Invalid additional Virtual Host: %s", vHost.Name)
		}
	}
}

func TestCreateRoutesConfigForRds(t *testing.T) {
	// TODO: (Vajira) Add more test scenarios
	vhostToRouteArrayMap := map[string][]*routev3.Route{
		"*":           testCreateRoutesForUnitTests(t),
		"mg.wso2.com": testCreateRoutesForUnitTests(t),
	}
	vHosts := CreateVirtualHosts(vhostToRouteArrayMap)
	rConfig := CreateRoutesConfigForRds(vHosts)

	assert.NotNil(t, rConfig, "CreateRoutesConfigForRds is failed")
	err := rConfig.Validate()
	if err != nil {
		t.Errorf("rConfig Validation failed " + err.Error())
	}
}

func TestCasePreserveEnabledOnListener(t *testing.T) {
	config, _ := config.ReadConfigs()

	config.Envoy.HeadersPreserveCase = true
	defer func() {
		config.Envoy.HeadersPreserveCase = false
	}()

	listeners := createListeners(config)
	assert.NotEmpty(t, listeners, "Listeners creation has been failed")
	assert.Equal(t, 2, len(listeners), "Two listeners are not created.")

	for _, listener := range listeners {
		assert.Equal(t, isCasePreserveEnabled(t, listener), true)
	}
}

// Create some routes to perform unit tests
func testCreateRoutesForUnitTests(t *testing.T) []*routev3.Route {
	//cors configuration
	corsConfigModel3 := &model.CorsConfig{
		Enabled:                   true,
		AccessControlAllowMethods: []string{"GET"},
		AccessControlAllowOrigins: []string{"http://test1.com", "http://test2.com"},
	}

	route1 := createRoute(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		"/testPath", []string{"GET"}, "test-cluster", corsConfigModel3, false))
	route2 := createRoute(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		"/testPath", []string{"POST"}, "test-cluster", corsConfigModel3, false))
	route3 := createRoute(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		"/testPath", []string{"PUT"}, "test-cluster", corsConfigModel3, false))

	routes := []*routev3.Route{route1, route2, route3}

	// check cors after creating routes
	for _, r := range routes {
		corsConfig := &cors_filter_v3.CorsPolicy{}
		err := r.GetTypedPerFilterConfig()[wellknown.CORS].UnmarshalTo(corsConfig)
		assert.Nilf(t, err, "Error while parsing Cors Configuration %v", corsConfig)
		assert.NotEmpty(t, corsConfig.GetAllowMethods(), "Cors AllowMethods should not be empty.")
		assert.NotEmpty(t, corsConfig.GetAllowOriginStringMatch(), "Cors AllowOriginStringMatch should not be empty.")
	}

	return routes
}

func isCasePreserveEnabled(t *testing.T, listener *listenerv3.Listener) bool {
	for _, filterChain := range listener.FilterChains {
		for _, filter := range filterChain.Filters {
			// only check connection manager filter
			if filter.Name == httpConnectionManagerFilterName {
				if isCasePreserveFormatterEnabled(t, filter) {
					return true
				}
			}
		}
	}
	return false
}

func isCasePreserveFormatterEnabled(t *testing.T, filter *listenerv3.Filter) bool {
	httpManager := &hcmv3.HttpConnectionManager{}
	err := anypb.UnmarshalTo(filter.GetTypedConfig(), httpManager, proto.UnmarshalOptions{})
	assert.NoError(t, err, "Failed to unmarshal HTTP connection manager")

	if httpManager.HttpProtocolOptions == nil ||
		httpManager.HttpProtocolOptions.HeaderKeyFormat == nil ||
		httpManager.HttpProtocolOptions.HeaderKeyFormat.HeaderFormat == nil {
		return false
	}

	return isStatefulFormatterEnabled(httpManager.HttpProtocolOptions.HeaderKeyFormat.HeaderFormat)
}

func isStatefulFormatterEnabled(headerFormat interface{}) bool {
	statefulFormatter, ok := headerFormat.(*corev3.Http1ProtocolOptions_HeaderKeyFormat_StatefulFormatter)
	if !ok {
		return false
	}
	if statefulFormatter.StatefulFormatter != nil && statefulFormatter.StatefulFormatter.Name == perserveCaseFormatterName {
		return true
	}
	return false
}
