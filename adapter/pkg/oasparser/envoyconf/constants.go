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
	extAuthzFilterName string = "envoy.filters.http.ext_authz"
	transportSocketName string = "envoy.transport_sockets.tls"
	accessLogName       string = "envoy.access_loggers.file"
	httpConManagerStartPrefix string = "ingress_http"
)

const (
	defaultRdsConfigName            string = "default"
	defaultAccessLogPath            string = "/tmp/envoy.access.log"
	defaultListenerSecretConfigName string = "DefaultListenerSecret"
	defaultCACertPath               string = "/etc/ssl/certs/ca-certificates.crt"
)

const (
	sandClustersConfigNamePrefix string = "clusterSand_"
	prodClustersConfigNamePrefix string = "clusterProd_"
)

const (
	httpsURLType     string = "https"
	httpMethodHeader string = ":method"
)
