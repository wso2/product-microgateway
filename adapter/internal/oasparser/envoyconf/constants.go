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
 */

package envoyconf

const (
	extAuthzClusterName     string = "ext-authz"
	accessLoggerClusterName string = "access-logger"
	grpcAccessLogLogName    string = "mgw_access_logs"
	tracingClusterName      string = "wso2_cc_trace"
	extAuthzHTTPCluster     string = "ext_authz_http_cluster"
	rateLimitClusterName    string = "rate-limit"
)

const (
	extAuthzFilterName         string = "envoy.filters.http.ext_authz"
	rateLimitFilterName        string = "envoy.filters.http.ratelimit"
	luaFilterName              string = "envoy.filters.http.lua"
	transportSocketName        string = "envoy.transport_sockets.tls"
	fileAccessLogName          string = "envoy.access_loggers.file"
	grpcAccessLogName          string = "envoy.http_grpc_access_log"
	httpConManagerStartPrefix  string = "ingress_http"
	extAuthzPerRouteName       string = "type.googleapis.com/envoy.extensions.filters.http.ext_authz.v3.ExtAuthzPerRoute"
	luaPerRouteName            string = "type.googleapis.com/envoy.extensions.filters.http.lua.v3.LuaPerRoute"
	localRateLimitPerRouteName string = "type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit"
	httpProtocolOptionsName    string = "type.googleapis.com/envoy.extensions.upstreams.http.v3.HttpProtocolOptions"
	mgwWebSocketFilterName     string = "envoy.filters.http.mgw_websocket"
	mgwWebSocketWASMFilterName string = "envoy.filters.http.mgw_WASM_websocket"
	mgwWASMVmID                string = "mgw_WASM_vm"
	mgwWASMVmRuntime           string = "envoy.wasm.runtime.v8"
	mgwWebSocketWASMFilterRoot string = "mgw_WASM_websocket_root"
	mgwWebSocketWASM           string = "/home/wso2/wasm/websocket/mgw-websocket.wasm"
	localRatelimitFilterName   string = "envoy.filters.http.local_ratelimit"
)

const (
	localRateLimitStatPrefix        string = "http_local_rate_limiter"
	jwksRateLimitStatPrefix         string = "jwks_rate_limit"
	jwksRateLimitEnabledRuntimeKey  string = "jwks_ratelimit_enabled"
	jwksRateLimitEnforcedRuntimeKey string = "jwks_ratelimit_enforced"
)

const (
	defaultRdsConfigName            string = "default"
	defaultHTTPListenerName         string = "HTTPListener"
	defaultHTTPSListenerName        string = "HTTPSListener"
	defaultAccessLogPath            string = "/tmp/envoy.access.log"
	defaultListenerSecretConfigName string = "DefaultListenerSecret"
)

// cluster prefixes
const (
	xWso2EPClustersConfigNamePrefix     string = "xwso2cluster"
	requestInterceptClustersNamePrefix  string = "reqInterceptor"
	responseInterceptClustersNamePrefix string = "resInterceptor"
)

// Context Extensions which are set in ExtAuthzPerRoute Config
// These values are shared between the adapter and enforcer, hence if it is required to change
// these values, modifications should be done in the both adapter and enforcer.
const (
	pathContextExtension            string = "path"
	vHostContextExtension           string = "vHost"
	basePathContextExtension        string = "basePath"
	methodContextExtension          string = "method"
	apiVersionContextExtension      string = "version"
	apiNameContextExtension         string = "name"
	prodClusterNameContextExtension string = "prodClusterName"
	sandClusterNameContextExtension string = "sandClusterName"
	retryPolicyRetriableStatusCodes string = "retriable-status-codes"
)

const (
	// clusterHeaderName denotes the constant used for header based routing decisions.
	clusterHeaderName string = "x-wso2-cluster-header"
	// upstreamServiceTimeHeader the header which is used to denote the upstream service time
	upstreamServiceTimeHeader string = "x-envoy-upstream-service-time"
	// xWso2requestInterceptor used to provide request interceptor details for api and resource level
	xWso2requestInterceptor string = "x-wso2-request-interceptor"
	// xWso2responseInterceptor used to provide response interceptor details for api and resource level
	xWso2responseInterceptor string = "x-wso2-response-interceptor"
)

// interceptor levels
const (
	APILevelInterceptor       string = "api"
	ResourceLevelInterceptor  string = "resource"
	OperationLevelInterceptor string = "operation"
)
const (
	httpsURLType     string = "https"
	wssURLType       string = "wss"
	httpMethodHeader string = ":method"
)

// Paths exposed from the router by default
const (
	healthPath  string = "/health"
	testKeyPath string = "/testkey"
	readyPath   string = "/ready"
	jwksPath    string = "/.wellknown/jwks"
)

const (
	// healthEndpointResponse - response from the health endpoint
	healthEndpointResponse = "{\"status\": \"healthy\"}"
	readyEndpointResponse  = "{\"status\": \"ready\"}"
)

const (
	defaultListenerHostAddress = "0.0.0.0"
)

// tracing configuration constants
const (
	tracerHost          = "host"
	tracerPort          = "port"
	tracerMaxPathLength = "maxPathLength"
	tracerEndpoint      = "endpoint"
	tracerNameZipkin    = "envoy.tracers.zipkin"
	// Azure tracer's name
	TracerTypeAzure = "azure"
)

// Constants relevant to the rate-limit service
const (
	RateLimiterDomain                    = "Default"
	RateLimitPolicyOperationLevel string = "OPERATION"
	RateLimitPolicyAPILevel       string = "API"
	RateLimitDisabled             string = "UNLIMITED"
)
