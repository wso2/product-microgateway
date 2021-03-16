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

package org.wso2.micro.gateway.enforcer.constants;

/**
 * Holds the constants specific to the analytics data publishing.
 */
public class AnalyticsConstants {
    public static final String UPSTREAM_SUCCESS_RESPONSE_DETAIL = "via_upstream";
    public static final String EXT_AUTH_DENIED_RESPONSE_DETAIL = "ext_authz_denied";
    public static final String GATEWAY_LABEL = "ENVOY";

    public static final int API_THROTTLE_OUT_ERROR_CODE = 900800;
    public static final int HARD_LIMIT_EXCEEDED_ERROR_CODE = 900801;
    public static final int RESOURCE_THROTTLE_OUT_ERROR_CODE = 900802;
    public static final int APPLICATION_THROTTLE_OUT_ERROR_CODE = 900803;
    public static final int SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE = 900804;
    public static final int SUBSCRIPTION_BURST_THROTTLE_OUT_ERROR_CODE = 900807;
    public static final int BLOCKED_ERROR_CODE = 900805;
    public static final int CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE = 900806;
    public static final int CONNECTIONS_COUNT_THROTTLE_OUT_ERROR_CODE = 900808;
    public static final int EVENTS_COUNT_THROTTLE_OUT_ERROR_CODE = 900808;
    public static final int GRAPHQL_QUERY_TOO_DEEP = 900820;
    public static final int GRAPHQL_QUERY_TOO_COMPLEX = 900821;
}
