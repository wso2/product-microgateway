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
 * Holds the constants specific to the analytics data publishing.
 */
public class AnalyticsConstants {
    public static final String AUTH_URL_CONFIG_KEY = "authURL";
    public static final String AUTH_TOKEN_CONFIG_KEY = "authToken";
    public static final String UPSTREAM_SUCCESS_RESPONSE_DETAIL = "via_upstream";
    public static final String EXT_AUTH_DENIED_RESPONSE_DETAIL = "ext_authz_denied";
    public static final String EXT_AUTH_ERROR_RESPONSE_DETAIL = "ext_authz_error";
    public static final String ROUTE_NOT_FOUND_RESPONSE_DETAIL = "route_not_found";
    public static final String GATEWAY_LABEL = "ENVOY";

    public static final String TOKEN_ENDPOINT_PATH = "/testkey";
    public static final String HEALTH_ENDPOINT_PATH = "/health";

    public static final String DEFAULT_FOR_UNASSIGNED = "UNKNOWN";
    public static final String DATA_PROVIDER_CLASS_PROPERTY = "publisher.custom.data.provider.class";

    public static final int API_THROTTLE_OUT_ERROR_CODE = 900800;
    public static final int HARD_LIMIT_EXCEEDED_ERROR_CODE = 900801;
    public static final int RESOURCE_THROTTLE_OUT_ERROR_CODE = 900802;
    public static final int APPLICATION_THROTTLE_OUT_ERROR_CODE = 900803;
    public static final int SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE = 900804;
    public static final int BLOCKED_ERROR_CODE = 900805;
    public static final int CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE = 900806;

    public static final int NHTTP_RECEIVER_INPUT_OUTPUT_ERROR_SENDING = 101000;
    public static final int NHTTP_RECEIVER_INPUT_OUTPUT_ERROR_RECEIVING = 101001;
    public static final int NHTTP_SENDER_INPUT_OUTPUT_ERROR_SENDING = 101500;
    public static final int NHTTP_CONNECTION_FAILED = 101503;
    public static final int NHTTP_CONNECTION_TIMEOUT = 101504;
    public static final int NHTTP_CONNECTION_CLOSED = 101505;
    public static final int NHTTP_PROTOCOL_VIOLATION = 101506;
    public static final int NHTTP_CONNECT_TIMEOUT = 101508;

    public static final String WEBSOCKET_HANDSHAKE_RESOURCE_PREFIX = "init-request:";
}
