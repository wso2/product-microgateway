# --------------------------------------------------------------------
# Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------

FROM envoyproxy/envoy:v1.16.0

COPY maven/envoy.yaml /etc/envoy/envoy.yaml
COPY maven/security /home/wso2/security

ENV ROUTER_ADMIN_HOST=0.0.0.0
ENV ROUTER_ADMIN_PORT=9000

ENV ROUTER_CLUSTER=default_cluster
ENV ROUTER_LABEL="Production and Sandbox"
ENV ROUTER_PRIVATE_KEY_PATH=/home/wso2/security/keystore/mg.key
ENV ROUTER_PUBLIC_CERT_PATH=/home/wso2/security/keystore/mg.pem

ENV ADAPTER_HOST=adapter
ENV ADAPTER_PORT=18000
ENV ADAPTER_CA_CERT_PATH=/home/wso2/security/truststore/mg.pem

ENV ENFORCER_HOST=enforcer
ENV ENFORCER_PORT=8081
ENV ENFORCER_CA_CERT_PATH=/home/wso2/security/truststore/mg.pem

ENV TRAILING_ARGS=

CMD /usr/local/bin/envoy -c /etc/envoy/envoy.yaml --config-yaml "{admin: {access_log_path: /dev/null, address: {socket_address: {address: '${ROUTER_ADMIN_HOST}', port_value: '${ROUTER_ADMIN_PORT}'}}}, dynamic_resources: {ads_config: {api_type: GRPC, transport_api_version: V3, grpc_services: [{envoy_grpc: {cluster_name: xds_cluster}}]}, cds_config: {ads: {}, resource_api_version: V3}, lds_config: {ads: {}, resource_api_version: V3}}, node: {cluster: '${ROUTER_CLUSTER}', id: '${ROUTER_LABEL}'}, static_resources: {clusters: [{name: xds_cluster, type: STRICT_DNS, connect_timeout: 1s, hosts: [{socket_address: {address: '${ADAPTER_HOST}', port_value: '${ADAPTER_PORT}'}}], http2_protocol_options: {}, transport_socket: {name: envoy.transport_sockets.tls, typed_config: {'@type': type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext, common_tls_context: {tls_params: {tls_minimum_protocol_version: TLSv1_2, tls_maximum_protocol_version: TLSv1_2}, tls_certificates: {private_key: {filename: '${ROUTER_PRIVATE_KEY_PATH}'}, certificate_chain: {filename: '${ROUTER_PUBLIC_CERT_PATH}'}}, validation_context: {trusted_ca: {filename: '${ADAPTER_CA_CERT_PATH}'}}}}}}, {name: ext-authz, type: STRICT_DNS, connect_timeout: 20s, http2_protocol_options: {}, transport_socket: {name: envoy.transport_sockets.tls, typed_config: {'@type': type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext, common_tls_context: {tls_params: {tls_minimum_protocol_version: TLSv1_2, tls_maximum_protocol_version: TLSv1_2}, tls_certificates: {private_key: {filename: '${ROUTER_PRIVATE_KEY_PATH}'}, certificate_chain: {filename: '${ROUTER_PUBLIC_CERT_PATH}'}}, validation_context: {trusted_ca: {filename: '${ENFORCER_CA_CERT_PATH}'}}}}}, load_assignment: {cluster_name: ext-authz, endpoints: [{lb_endpoints: [{endpoint: {address: {socket_address: {address: '${ENFORCER_HOST}', port_value: '${ENFORCER_PORT}'}}}}]}]}}]}}" $TRAILING_ARGS
