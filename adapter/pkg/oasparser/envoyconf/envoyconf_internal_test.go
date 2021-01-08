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

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/ext_authz/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/config"
	mgwconfig "github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/pkg/oasparser/model"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

func TestGenerateRoutePaths(t *testing.T) {
	// Tested features
	// 1. Route regex when xWso2BasePath is provided
	// 2. Route regex when xWso2BasePath is empty
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
	// Tested features
	// 1. RouteAction (Substitution involved) when xWso2BasePath is provided
	// 2. RouteAction (No substitution) config when xWso2BasePath is empty
	// 3. If HostRewriteSpecifier is set to Auto rewrite
	// 4. Method header regex matcher
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
	hostRewriteSpecifier := &routev3.RouteAction_AutoHostRewrite{
		AutoHostRewrite: &wrapperspb.BoolValue{
			Value: true,
		},
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

	generatedRouteWithXWso2BasePath := createRoute(title, xWso2BasePath, version, endpoint.Basepath, resourceWithGet, clusterName, "")
	assert.NotNil(t, generatedRouteWithXWso2BasePath, "Route should not be null.")
	assert.Equal(t, expectedRouteActionWithXWso2BasePath, generatedRouteWithXWso2BasePath.Action,
		"Route generation mismatch when xWso2BasePath option is provided.")
	assert.NotNil(t, generatedRouteWithXWso2BasePath.GetMatch().Headers, "Headers property should not be null")
	assert.Equal(t, "^(GET)$", generatedRouteWithXWso2BasePath.GetMatch().Headers[0].GetSafeRegexMatch().Regex,
		"Assigned HTTP Method Regex is incorrect when single method is available.")

	generatedRouteWithoutXWso2BasePath := createRoute(title, "", version, endpoint.Basepath, resourceWithGetPost, clusterName, "")
	assert.NotNil(t, generatedRouteWithoutXWso2BasePath, "Route should not be null")
	assert.NotNil(t, generatedRouteWithoutXWso2BasePath.GetMatch().Headers, "Headers property should not be null")
	assert.Equal(t, "^(GET|POST)$", generatedRouteWithoutXWso2BasePath.GetMatch().Headers[0].GetSafeRegexMatch().Regex,
		"Assigned HTTP Method Regex is incorrect when multiple methods are available.")

	assert.Equal(t, expectedRouteActionWithoutXWso2BasePath, generatedRouteWithoutXWso2BasePath.Action,
		"Route generation mismatch when xWso2BasePath option is provided")

}

func TestCreateRouteClusterSpecifier(t *testing.T) {
	// Tested features
	// 1. If the cluster for route is defined correctly depending on prodution only, sandbox only or both.

	// In this test case, the extAuthz context variables are not tested
	prodClusterName := "prodCluster"
	sandClusterName := "sandCluster"

	xWso2BasePath := "/xWso2BasePath"
	endpointBasePath := "/basepath"
	title := "WSO2"
	version := "1.0.0"

	resourceWithGet := model.CreateMinimalDummyResourceForTests("/resourcePath", []string{"GET"},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})

	routeWithProdEp := createRoute(title, xWso2BasePath, version, endpointBasePath, resourceWithGet, prodClusterName, "")
	assert.NotNil(t, routeWithProdEp, "Route should not be null")
	assert.NotNil(t, routeWithProdEp.GetRoute().GetCluster(), "Route Cluster Name should not be null.")
	assert.Empty(t, routeWithProdEp.GetRoute().GetClusterHeader(), "Route Cluster Header should be empty.")
	assert.Equal(t, prodClusterName, routeWithProdEp.GetRoute().GetCluster(), "Route Cluster Name mismatch.")

	routeWithSandEp := createRoute(title, xWso2BasePath, version, endpointBasePath, resourceWithGet, "", sandClusterName)
	assert.NotNil(t, routeWithSandEp, "Route should not be null")
	assert.NotNil(t, routeWithSandEp.GetRoute().GetCluster(), "Route Cluster Name should not be null.")
	assert.Empty(t, routeWithSandEp.GetRoute().GetClusterHeader(), "Route Cluster Header should be empty.")
	assert.Equal(t, sandClusterName, routeWithSandEp.GetRoute().GetCluster(), "Route Cluster Name mismatch.")

	routeWithProdSandEp := createRoute(title, xWso2BasePath, version, endpointBasePath, resourceWithGet, prodClusterName,
		sandClusterName)
	assert.NotNil(t, routeWithProdSandEp, "Route should not be null")
	assert.NotNil(t, routeWithProdSandEp.GetRoute().GetClusterHeader(), "Route Cluster Header should not be null.")
	assert.Empty(t, routeWithProdSandEp.GetRoute().GetCluster(), "Route Cluster Name should be empty.")
	assert.Equal(t, clusterHeaderName, routeWithProdSandEp.GetRoute().GetClusterHeader(), "Route Cluster Name mismatch.")
}

func TestCreateRouteExtAuthzContext(t *testing.T) {
	// Tested features
	// 1. The context variables inside extAuthzPerRoute configuration including
	// (prod/sand clustername, method regex, basePath, resourcePath, title, version)
	prodClusterName := "prodCluster"
	sandClusterName := "sandCluster"

	xWso2BasePath := "/xWso2BasePath"
	endpointBasePath := "/basepath"
	title := "WSO2"
	version := "1.0.0"

	resourceWithGet := model.CreateMinimalDummyResourceForTests("/resourcePath", []string{"GET"},
		"resource_operation_id", []model.Endpoint{}, []model.Endpoint{})

	routeWithProdEp := createRoute(title, xWso2BasePath, version, endpointBasePath, resourceWithGet, prodClusterName, sandClusterName)
	assert.NotNil(t, routeWithProdEp, "Route should not be null")
	assert.NotNil(t, routeWithProdEp.GetTypedPerFilterConfig(), "TypedPerFilter config should not be null")
	assert.NotNil(t, routeWithProdEp.GetTypedPerFilterConfig()[wellknown.HTTPExternalAuthorization],
		"ExtAuthzPerRouteConfig should not be empty")

	extAuthPerRouteConfig := &extAuthService.ExtAuthzPerRoute{}
	err := ptypes.UnmarshalAny(routeWithProdEp.TypedPerFilterConfig[wellknown.HTTPExternalAuthorization],
		extAuthPerRouteConfig)
	assert.Nilf(t, err, "Error while parsing ExtAuthzPerRouteConfig %v", extAuthPerRouteConfig)
	assert.NotEmpty(t, extAuthPerRouteConfig.GetCheckSettings(), "Check Settings per ext authz route should not be empty")
	assert.NotEmpty(t, extAuthPerRouteConfig.GetCheckSettings().ContextExtensions,
		"ContextExtensions per ext authz route should not be empty")

	contextExtensionMap := extAuthPerRouteConfig.GetCheckSettings().ContextExtensions
	assert.Equal(t, title, contextExtensionMap[apiNameContextExtension], "Title mismatch in route ext authz context.")
	assert.Equal(t, xWso2BasePath, contextExtensionMap[basePathContextExtension], "Basepath mismatch in route ext authz context.")
	assert.Equal(t, version, contextExtensionMap[apiVersionContextExtension], "Version mismatch in route ext authz context.")
	assert.Equal(t, "GET", contextExtensionMap[methodContextExtension], "Method mismatch in route ext authz context.")
	assert.Equal(t, prodClusterName, contextExtensionMap[prodClusterNameContextExtension],
		"Production Cluster mismatch in route ext authz context.")
	assert.Equal(t, sandClusterName, contextExtensionMap[sandClusterNameContextExtension],
		"Sandbox Cluster mismatch in route ext authz context.")
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

	tlsCert := generateTLSCert(privateKeyPath, publicKeyPath)

	assert.NotNil(t, tlsCert, "TLS Certificate should not be null")

	assert.NotNil(t, tlsCert.GetPrivateKey(), "Private Key should not be null in the TLS certificate")
	assert.NotNil(t, tlsCert.GetCertificateChain(), "Certificate chain should not be null in the TLS certificate")

	assert.Equal(t, tlsCert.GetPrivateKey().GetFilename(), privateKeyPath, "Private Key Value mismatch in the TLS Certificate")
	assert.Equal(t, tlsCert.GetCertificateChain().GetFilename(), publicKeyPath, "Certificate Chain Value mismatch in the TLS Certificate")
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

func TestCreateUpstreamTLSContext(t *testing.T) {
	certFilePath := config.GetMgwHome() + "/../adapter/test-resources/envoycodegen/certs/testcrt.crt"
	certByteArr, err := ioutil.ReadFile(certFilePath)
	assert.Nil(t, err, "Error while reading the certificate : "+certFilePath)
	defaultMgwKeyPath := "/home/wso2/security/mg.key"
	defaultMgwCertPath := "/home/wso2/security/mg.pem"
	defaultCipherArray := "ECDHE-ECDSA-AES128-GCM-SHA256, ECDHE-RSA-AES128-GCM-SHA256, ECDHE-ECDSA-AES128-SHA," +
		" ECDHE-RSA-AES128-SHA, AES128-GCM-SHA256, AES128-SHA, ECDHE-ECDSA-AES256-GCM-SHA384, ECDHE-RSA-AES256-GCM-SHA384," +
		" ECDHE-ECDSA-AES256-SHA, ECDHE-RSA-AES256-SHA, AES256-GCM-SHA384, AES256-SHA"
	defaultCACertPath := "/etc/ssl/certs/ca-certificates.crt"
	hostNameAddress := &corev3.Address{Address: &corev3.Address_SocketAddress{
		SocketAddress: &corev3.SocketAddress{
			Address:  "abc.com",
			Protocol: corev3.SocketAddress_TCP,
			PortSpecifier: &corev3.SocketAddress_PortValue{
				PortValue: uint32(2384),
			},
		},
	}}

	tlsCert := generateTLSCert(defaultMgwKeyPath, defaultMgwCertPath)
	upstreamTLSContextWithCerts := createUpstreamTLSContext(certByteArr, hostNameAddress)
	upstreamTLSContextWithoutCerts := createUpstreamTLSContext(nil, hostNameAddress)

	assert.NotEmpty(t, upstreamTLSContextWithCerts, "Upstream TLS Context should not be null when certs provided")
	assert.NotEmpty(t, upstreamTLSContextWithCerts.CommonTlsContext, "CommonTLSContext should not be "+
		"null when certs provided")
	assert.NotEmpty(t, upstreamTLSContextWithCerts.CommonTlsContext.TlsParams, "TlsParams should not be "+
		"null when certs provided")
	// tested against default TLS Parameters
	assert.Equal(t, tlsv3.TlsParameters_TLSv1_2, upstreamTLSContextWithCerts.CommonTlsContext.TlsParams.TlsMaximumProtocolVersion,
		"TLS maximum parameter mismatch")
	assert.Equal(t, tlsv3.TlsParameters_TLSv1_1, upstreamTLSContextWithCerts.CommonTlsContext.TlsParams.TlsMinimumProtocolVersion,
		"TLS minimum parameter mismatch")

	assert.Equal(t, defaultCipherArray, strings.Join(upstreamTLSContextWithCerts.CommonTlsContext.TlsParams.CipherSuites, ", "), "cipher suites mismatch")
	// the microgateway's certificate will be provided all the time. (For mutualSSL when required)
	assert.NotEmpty(t, upstreamTLSContextWithCerts.CommonTlsContext.TlsCertificates, "TLScerts should not be null")
	assert.Equal(t, tlsCert, upstreamTLSContextWithCerts.CommonTlsContext.TlsCertificates[0], "TLScert mismatch")
	assert.Equal(t, certByteArr, upstreamTLSContextWithCerts.CommonTlsContext.GetValidationContext().GetTrustedCa().GetInlineBytes(),
		"validation context certificate mismatch")
	assert.Equal(t, defaultCACertPath, upstreamTLSContextWithoutCerts.CommonTlsContext.GetValidationContext().GetTrustedCa().GetFilename(),
		"validation context certificate filepath mismatch")
	assert.NotEmpty(t, upstreamTLSContextWithCerts.CommonTlsContext.GetValidationContext().GetMatchSubjectAltNames(),
		"Subject Alternative Names Should not be empty.")
	assert.Equal(t, "abc.com", upstreamTLSContextWithCerts.CommonTlsContext.GetValidationContext().GetMatchSubjectAltNames()[0].GetExact(),
		"Upstream SAN mismatch.")
}
