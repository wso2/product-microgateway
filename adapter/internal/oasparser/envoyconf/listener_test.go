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
	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/internal/oasparser/model"
)

func TestCreateListenerWithRds(t *testing.T) {
	// TODO: (Vajira) Add more test scenarios
	listener := CreateListenerWithRds("default")
	assert.NotNil(t, listener, "Listner creation has been failed")

	if listener.Validate() != nil {
		t.Error("Listener validation failed")
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
	if rConfig.Validate() != nil {
		t.Errorf("rConfig Validation failed")
	}
}

//Create some routes to perform unit tests
func testCreateRoutesForUnitTests(t *testing.T) []*routev3.Route {
	//cors configuration
	corsConfigModel3 := &model.CorsConfig{
		Enabled:                   true,
		AccessControlAllowMethods: []string{"GET"},
		AccessControlAllowOrigins: []string{"http://test1.com", "http://test2.com"},
	}

	route1 := createRoute(generateRouteCreateParamsForUnitTests("test", "HTTP", "/test", "1.0.0", "/test",
		"/testPath", []string{"GET"}, "test-cluster", "", corsConfigModel3))
	route2 := createRoute(generateRouteCreateParamsForUnitTests("test", "HTTP", "/test", "1.0.0", "/test",
		"/testPath", []string{"POST"}, "test-cluster", "", corsConfigModel3))
	route3 := createRoute(generateRouteCreateParamsForUnitTests("test", "HTTP", "/test", "1.0.0", "/test",
		"/testPath", []string{"PUT"}, "test-cluster", "", corsConfigModel3))

	routes := []*routev3.Route{route1, route2, route3}

	// check cors after creating routes
	for _, r := range routes {
		assert.NotNil(t, r.GetRoute().Cors, "Cors Configuration should not be null.")
	}

	return routes
}
