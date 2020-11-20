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
	"io/ioutil"
	"regexp"
	"strings"
	"testing"

	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/stretchr/testify/assert"
	mgwconfig "github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/pkg/oasparser/model"
)

func TestGenerateRoutePaths(t *testing.T) {

	xWso2BasePath := "/xWso2BasePath"
	basePath := "/basePath"
	resourcePath := "/resource"

	completeRoutePath := generateRoutePaths(xWso2BasePath, basePath, resourcePath)
	// TODO: (VirajSalaka) check if it is possible to perform an equals operation instead of prefix
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
	title := "WSO2"
	endpoint := model.Endpoint{
		Host:     "abc.com",
		Basepath: basePath,
		URLType:  "http",
		Port:     80,
	}
	version := "1.0"
	resourceWithGet := model.CreateMinimalDummyResourceForTests("/resourcePath", []string{"GET"},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})
	resourceWithGetPost := model.CreateMinimalDummyResourceForTests("/resourcePath", []string{"GET", "POST"},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})
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

	expectedRouteActionWithXWso2BasePath := &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: hostRewriteSpecifier,
			RegexRewrite:         regexRewriteWithXWso2BasePath,
			ClusterSpecifier:     clusterSpecifier,
		},
	}

	expectedRouteActionWithoutXWso2BasePath := &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: hostRewriteSpecifier,
			ClusterSpecifier:     clusterSpecifier,
		},
	}

	generatedRouteWithXWso2BasePath := createRoute(title, xWso2BasePath, version, endpoint, resourceWithGet, clusterName)
	assert.NotNil(t, generatedRouteWithXWso2BasePath, "Route should not be null")
	assert.Equal(t, expectedRouteActionWithXWso2BasePath, generatedRouteWithXWso2BasePath.Action,
		"Route generation mismatch when xWso2BasePath option is provided")
	assert.NotNil(t, generatedRouteWithXWso2BasePath.GetMatch().Headers, "Headers property should not be null")
	assert.Equal(t, "^(GET)$", generatedRouteWithXWso2BasePath.GetMatch().Headers[0].GetSafeRegexMatch().Regex,
		"Assigned HTTP Method Regex is incorrect when single method is available.")

	generatedRouteWithoutXWso2BasePath := createRoute(title, "", version, endpoint, resourceWithGetPost, clusterName)
	assert.NotNil(t, generatedRouteWithoutXWso2BasePath, "Route should not be null")
	assert.NotNil(t, generatedRouteWithoutXWso2BasePath.GetMatch().Headers, "Headers property should not be null")
	assert.Equal(t, "^(GET|POST)$", generatedRouteWithoutXWso2BasePath.GetMatch().Headers[0].GetSafeRegexMatch().Regex,
		"Assigned HTTP Method Regex is incorrect when multiple methods are available.")

	assert.Equal(t, expectedRouteActionWithoutXWso2BasePath, generatedRouteWithoutXWso2BasePath.Action,
		"Route generation mismatch when xWso2BasePath option is provided")

}

func TestCreateListener(t *testing.T) {
	var listenerPort uint32
	listenerPort = 10001
	listenerAddress := "test.com"

	config := new(mgwconfig.Config)
	config.Envoy.ListenerPort = listenerPort
	config.Envoy.ListenerHost = listenerAddress
	config.Envoy.ListenerTLSEnabled = true
	config.Envoy.ListenerCertPath = mgwconfig.GetMgwHome() + "/adapter/security/localhost.pem"
	config.Envoy.ListenerKeyPath = mgwconfig.GetMgwHome() + "/adapter/security/localhost.key"

	tlsEnabledListener := createListener(config, "test-id")

	assert.NotNil(t, tlsEnabledListener, "The TLS Enabled Listener configuration should not be null")

	assert.NotNil(t, tlsEnabledListener.Address.GetSocketAddress().Address, "The socket address of the listener should not be null")
	assert.Equal(t, tlsEnabledListener.Address.GetSocketAddress().Address, listenerAddress,
		"The assigned socket address of the listener is incorrect")

	assert.NotNil(t, tlsEnabledListener.Address.GetSocketAddress().GetPortValue(), "The socket's port value of the listener should not be null")
	assert.Equal(t, tlsEnabledListener.Address.GetSocketAddress().GetPortValue(), listenerPort,
		"The assigned socket port of the listener is incorrect")

	assert.NotNil(t, tlsEnabledListener.FilterChains[0].TransportSocket, "Transport Socket configuration should not be null")

	config.Envoy.ListenerTLSEnabled = false
	tlsDisabledListener := createListener(config, "test-id")

	assert.NotNil(t, tlsDisabledListener, "The TLS Enabled Listener configuration should not be null")
	assert.Nil(t, tlsDisabledListener.FilterChains[0].TransportSocket, "Transport Socket configuration should be null")
}

func TestGenerateTLSCert(t *testing.T) {
	publicKeyPath := mgwconfig.GetMgwHome() + "/adapter/security/localhost.pem"
	privateKeyPath := mgwconfig.GetMgwHome() + "/adapter/security/localhost.key"

	tlsCert, _ := generateTLSCert(privateKeyPath, publicKeyPath)

	assert.NotNil(t, tlsCert, "TLS Certificate should not be null")

	privateKeyByteArray, _ := ioutil.ReadFile(privateKeyPath)
	publicKeyByteArray, _ := ioutil.ReadFile(publicKeyPath)

	assert.NotNil(t, tlsCert.GetPrivateKey(), "Private Key should not be null in the TLS certificate")
	assert.NotNil(t, tlsCert.GetCertificateChain(), "Certificate chain should not be null in the TLS certificate")

	assert.Equal(t, tlsCert.GetPrivateKey().GetInlineBytes(), privateKeyByteArray, "Private Key Value mismatch in the TLS Certificate")
	assert.Equal(t, tlsCert.GetCertificateChain().GetInlineBytes(), publicKeyByteArray, "Certificate Chain Value mismatch in the TLS Certificate")
}

func TestGenerateRegex(t *testing.T) {

	type generateRegexTestItem struct {
		inputpath     string
		userInputPath string
		message       string
		isMatched     bool
	}
	dataItems := []generateRegexTestItem{
		{
			inputpath:     "/v2/pet/{petId}",
			userInputPath: "/v2/pet/5",
			message:       "when path parameter is provided end of the path",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}/info",
			userInputPath: "/v2/pet/5/info",
			message:       "when path parameter is provided in the middle of the path",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}",
			userInputPath: "/v2/pet/5",
			message:       "when path parameter is provided end of the path",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}/tst/{petId}",
			userInputPath: "/v2/pet/5/tst/3",
			message:       "when multiple path parameter are provided",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}",
			userInputPath: "/v2/pet/5/test",
			message:       "when path parameter is provided end of the path and provide incorrect path",
			isMatched:     false,
		},
		{
			inputpath:     "/v2/pet/5",
			userInputPath: "/v2/pett/5",
			message:       "when provide a incorrect path",
			isMatched:     false,
		},
		{
			inputpath:     "/v2/pet/findById",
			userInputPath: "/v2/pet/findById?status=availabe",
			message:       "when query parameter is provided",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/findById",
			userInputPath: "/v2/pet/findByIdstatus=availabe",
			message:       "when query parameter is provided without ?",
			isMatched:     false,
		},
	}

	for _, item := range dataItems {
		resultPattern := generateRegex(item.inputpath)
		resultIsMatching, err := regexp.MatchString(resultPattern, item.userInputPath)

		assert.Equal(t, item.isMatched, resultIsMatching, item.message)
		assert.Nil(t, err)
	}
}
