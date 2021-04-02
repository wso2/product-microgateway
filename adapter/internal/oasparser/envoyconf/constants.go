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
	extAuthzClusterName string = "ext-authz"
)

const (
	extAuthzFilterName        string = "envoy.filters.http.ext_authz"
	mgwWebSocketFilterName    string = "envoy.filters.http.mgw_websocket"
	transportSocketName       string = "envoy.transport_sockets.tls"
	accessLogName             string = "envoy.access_loggers.file"
	httpConManagerStartPrefix string = "ingress_http"
	extAuthzPerRouteName      string = "type.googleapis.com/envoy.extensions.filters.http.ext_authz.v3.ExtAuthzPerRoute"
)

const (
	defaultRdsConfigName            string = "default"
	defaultHTTPListenerName         string = "HTTPListener"
	defaultHTTPSListenerName        string = "HTTPSListener"
	defaultAccessLogPath            string = "/tmp/envoy.access.log"
	defaultListenerSecretConfigName string = "DefaultListenerSecret"
)

const (
	sandClustersConfigNamePrefix string = "clusterSand_"
	prodClustersConfigNamePrefix string = "clusterProd_"
)

// Context Extensions which are set in ExtAuthzPerRoute Config
// These values are shared between the adapter and enforcer, hence if it is required to change
// these values, modifications should be done in the both adapter and enforcer.
const (
	pathContextExtension            string = "path"
	basePathContextExtension        string = "basePath"
	methodContextExtension          string = "method"
	apiVersionContextExtension      string = "version"
	apiNameContextExtension         string = "name"
	prodClusterNameContextExtension string = "prodClusterName"
	sandClusterNameContextExtension string = "sandClusterName"
)

const (
	// clusterHeaderName denotes the constant used for header based routing decisions.
	clusterHeaderName string = "x-wso2-cluster-header"
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
)

const (
	// healthEndpointResponse - response from the health endpoint
	healthEndpointResponse = "{\"status\": \"healthy\"}"
)

const (
	defaultListenerHostAddress = "0.0.0.0"
)
