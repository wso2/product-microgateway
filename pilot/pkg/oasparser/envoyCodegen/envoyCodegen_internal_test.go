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
	"strings"
	"testing"

	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/pkg/oasparser/models/apiDefinition"
)

//https://www.red-gate.com/simple-talk/dotnet/software-testing/go-unit-tests-tips-from-the-trenches/#Go_Unit_Tests

func TestGenerateRoutePaths(t *testing.T) {

	xWso2BasePath := "/xWso2BasePath"
	basePath := "/basePath"
	resourcePath := "/resource"

	completeRoutePath := generateRoutePaths(xWso2BasePath, basePath, resourcePath)
	//TODO: (VirajSalaka) check if it is possible to perform an equals operation instead of prefix
	if !strings.HasPrefix(completeRoutePath, "^/xWso2BasePath/resource") {
		t.Error("The generated path should contain xWso2BasePath as a prefix if xWso2Basepath is available.")
	}

	xWso2BasePath = "/xWso2BasePath/"
	if !strings.HasPrefix(completeRoutePath, "^/xWso2BasePath/resource") {
		t.Error("The generated path should not contain the trailing '\\' of xWso2BasePath property within the generated route path.")
	}

	xWso2BasePath = ""
	completeRoutePath = generateRoutePaths(xWso2BasePath, basePath, resourcePath)
	if !strings.HasPrefix(completeRoutePath, "^/basePath/resource") {
		t.Error("The generated path should contain basePath as a prefix if xWso2Basepath is unavailable.")
	}
}

func TestCreateRoute(t *testing.T) {
	xWso2BasePath := "/xWso2BasePath"
	basePath := "/basepath"
	endpoint := apiDefinition.Endpoint{
		Host:     "abc.com",
		Basepath: basePath,
		UrlType:  "http",
		Port:     80,
	}
	version := "1.0"
	resource := apiDefinition.CreateMinimalDummyResourceForTests("/resourcePath", "get", "resource_operation_id", []apiDefinition.Endpoint{},
		[]apiDefinition.Endpoint{})
	clusterName := "resource_operation_id"
	hostRewriteSpecifier := &routev3.RouteAction_HostRewriteLiteral{
		HostRewriteLiteral: "abc.com",
	}
	clusterSpecifier := &routev3.RouteAction_Cluster{
		Cluster: "resource_operation_id",
	}
	regexRewriteWithXWso2BasePath := &envoy_type_matcherv3.RegexMatchAndSubstitute{
		Pattern: &envoy_type_matcherv3.RegexMatcher{
			EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
				GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
					MaxProgramSize: nil,
				},
			},
			Regex: "/xWso2BasePath",
		},
		Substitution: "/basepath",
	}

	expctedRouteActionWithXWso2BasePath := &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: hostRewriteSpecifier,
			RegexRewrite:         regexRewriteWithXWso2BasePath,
			ClusterSpecifier:     clusterSpecifier,
		},
	}

	expctedRouteActionWithoutXWso2BasePath := &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: hostRewriteSpecifier,
			ClusterSpecifier:     clusterSpecifier,
		},
	}

	generatedRouteWithXWso2BasePath := createRoute(xWso2BasePath, version, endpoint, resource, clusterName)
	assert.NotNil(t, generatedRouteWithXWso2BasePath, "Route should not be null")

	assert.Equal(t, generatedRouteWithXWso2BasePath.Action, expctedRouteActionWithXWso2BasePath,
		"Route generation mismatch when xWso2BasePath option is provided")

	generatedRouteWithoutXWso2BasePath := createRoute("", version, endpoint, resource, clusterName)
	assert.NotNil(t, generatedRouteWithoutXWso2BasePath, "Route should not be null")

	assert.Equal(t, generatedRouteWithoutXWso2BasePath.Action, expctedRouteActionWithoutXWso2BasePath,
		"Route generation mismatch when xWso2BasePath option is provided")

}
