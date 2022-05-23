/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.constants;

/**
 * Adapter Constants class holds constants shared between adapter and enforcer.
 * If a certain value is changed, the change should be added to the adapter implementation
 * as well.
 */
public class AdapterConstants {
    // The header which should be populated to set the upstream cluster
    public static final String CLUSTER_HEADER = "x-wso2-cluster-header";
    // The key which specifies the production cluster name inside the request context
    public static final String PROD_CLUSTER_HEADER_KEY = "prodClusterName";
     // The key which specifies the sandbox cluster name inside the request context
    public static final String SAND_CLUSTER_HEADER_KEY = "sandClusterName";
    // The common enforcer Label
    public static final String COMMON_ENFORCER_LABEL = "commonEnforcerLabel";
    // The node identifier Key
    public static final String NODE_IDENTIFIER_KEY = "instanceIdentifier";

    /**
     * Key in a Key-Value pair of a router http header to configure retry, etc.
     * These are Envoy specific constants are not used in Adapter, but correlates with route configs added at adapter.
     */
    public static class HttpRouterHeaders {
        public static final String RETRY_ON = "x-envoy-retry-on";
        public static final String MAX_RETRIES = "x-envoy-max-retries";
        public static final String RETRIABLE_STATUS_CODES = "x-envoy-retriable-status-codes";
        public static final String UPSTREAM_REQ_TIMEOUT_MS = "x-envoy-upstream-rq-timeout-ms";

        private HttpRouterHeaders() {}
    }

    /**
     * Values in a Key-Value pair of a router http header to configure retry, etc.
     * These are Envoy specific constants that correlates with the route config added at the adapter.
     */
    public static class HttpRouterHeaderValues {
        public static final String RETRIABLE_STATUS_CODES = "retriable-status-codes";

        private HttpRouterHeaderValues() {}
    }
}
