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

	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	cors_filter_v3 "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/cors/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
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
	if rConfig.Validate() != nil {
		t.Errorf("rConfig Validation failed")
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

	operationGet := model.NewOperation("GET", nil, nil)
	operationPost := model.NewOperation("POST", nil, nil)
	operationPut := model.NewOperation("PUT", nil, nil)
	resourceWithGet := model.CreateMinimalDummyResourceForTests("/resourcePath", []*model.Operation{operationGet},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})
	resourceWithPost := model.CreateMinimalDummyResourceForTests("/resourcePath", []*model.Operation{operationPost},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})
	resourceWithPut := model.CreateMinimalDummyResourceForTests("/resourcePath", []*model.Operation{operationPut},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})
	resourceWithMultipleOperations := model.CreateMinimalDummyResourceForTests("/resourcePath", []*model.Operation{operationGet, operationPut},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})

	route1, err := createRoutes(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		&resourceWithGet, "test-cluster", "", corsConfigModel3, false))
	assert.Nil(t, err, "Error while creating routes for resourceWithGet")
	route2, err := createRoutes(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		&resourceWithPost, "test-cluster", "", corsConfigModel3, false))
	assert.Nil(t, err, "Error while creating routes for resourceWithPost")
	route3, err := createRoutes(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		&resourceWithPut, "test-cluster", "", corsConfigModel3, false))
	assert.Nil(t, err, "Error while creating routes for resourceWithPut")
	route4, err := createRoutes(generateRouteCreateParamsForUnitTests("test", "HTTP", "localhost", "/test", "1.0.0", "/test",
		&resourceWithMultipleOperations, "test-cluster", "", corsConfigModel3, false))
	assert.Nil(t, err, "Error while creating routes for resourceWithMultipleOperations")

	routes := []*routev3.Route{route1[0], route2[0], route3[0], route4[0]}

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
