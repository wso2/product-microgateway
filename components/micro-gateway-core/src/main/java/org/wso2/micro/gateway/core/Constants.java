/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.micro.gateway.core;

/**
 * Constants for gateway package native functions.
 */
public class Constants {
    /**
     * Organization name.
     */
    public static final String ORG_NAME = "wso2";

    /**
     * Package name.
     */
    public static final String PACKAGE_NAME = "gateway";

    /**
     * gateway package version.
     */
    public static final String PACKAGE_VERSION = "3.2.9";

    public static final String GATEWAY_VERSION = "MGW_VERSION";

    public static final String FILE_NOT_FOUND_ERROR = "{wso2/gateway}FileNotFoundError";

    public static final String SCHEMA_REFERENCE = "$ref";
    public static final String PATHS = "$..paths..";
    public static final String BODY_CONTENT = ".requestBody.content.application/json.schema";
    public static final String JSON_PATH = "$.";
    public static final String ITEMS = "items";
    public static final String OPEN_API = ".openapi";
    public static final char JSONPATH_SEPARATE = '.';
    public static final String PARAM_SCHEMA = ".parameters..schema";
    public static final String REQUEST_BODY = "..requestBody";
    public static final String JSON_RESPONSES = ".responses.";
    public static final String DEFAULT = "default";
    public static final String CONTENT = ".content";
    public static final String JSON_CONTENT = ".application/json.schema.$ref";
    public static final String SCHEMA = ".schema";
    public static final String EMPTY_ARRAY = "[]";
    public static final String DEFINITIONS = "definitions";
    public static final String COMPONENT_SCHEMA = "components/schemas";
    public static final char HASH = '#';
    public static final String EMPTY = "";
    public static final String BACKWARD_SLASH = "\"";
    public static final char FORWARD_SLASH = '/';
    public static final String REQUESTBODY_SCHEMA = "components.requestBodies.";
    public static final String REQUESTBODIES = "requestBodies";
    public static final String JSONPATH_SCHEMAS = "$..components.schemas.";
    public static final String JSON_SCHEMA = ".content.application/json.schema";
    public static final String VALIDATED_STATUS = "validated";
    public static final String RUNTIME_HOME_PATH = "mgw-runtime.home";
    public static final String BEGIN_CERTIFICATE_STRING = "-----BEGIN CERTIFICATE-----\n";
    public static final String END_CERTIFICATE_STRING = "-----END CERTIFICATE-----";
    public static final String RESOURCE_LOCATION = "resources/wso2/";
    public static final String DOT = ".";
    public static final String UNDER_SCORE = "_";
    public static final String JSON_EXTENSION = ".json";

    /**
     * Constants for API security related error codes
     */
    public static class APISecurityConstants {
        public static final int API_AUTH_GENERAL_ERROR = 900900;
        public static final int API_AUTH_INVALID_CREDENTIALS = 900901;
        public static final int API_AUTH_MISSING_CREDENTIALS = 900902;
        public static final int API_AUTH_ACCESS_TOKEN_EXPIRED = 900903;
        public static final int API_AUTH_ACCESS_TOKEN_INACTIVE = 900904;
        public static final int API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE = 900905;
        public static final int API_AUTH_INCORRECT_API_RESOURCE = 900906;
        public static final int INVALID_SCOPE = 900910;
        public static final int API_BLOCKED = 900907;
        public static final int API_AUTH_FORBIDDEN = 900908;
        public static final int SUBSCRIPTION_INACTIVE = 900909;
    }

    /**
     * Constants for API throttling related error codes
     */
    public static class APIThrottleConstants {
        public static final int API_THROTTLE_OUT_ERROR_CODE = 900800;
        public static final int RESOURCE_THROTTLE_OUT_ERROR_CODE = 900802;
        public static final int APPLICATION_THROTTLE_OUT_ERROR_CODE = 900803;
        public static final int SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE = 900804;
        public static final int BLOCKED_ERROR_CODE = 900805;
        public static final int CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE = 900806;
    }

    /**
     * Constants for API throttling related error codes
     */
    public static class TargetConnectivityConstants {
        public static final int BACKEND_CONNECTION_ERROR = 101503;
        public static final int HTTP_CONNECTION_TIMEOUT = 101504;
        public static final int MALFORMED_URL = 101505;
    }

    /**
     * APIM 4.x Analytics related constants
     */
    public static class AnalyticsConstants {
        public static final String API_USER_NAME_KEY = "userName";
        public static final String API_CONTEXT_KEY = "apiContext";
        public static final String RESPONSE_SIZE = "responseSize";
        public static final String RESPONSE_CONTENT_TYPE = "responseContentType";
        public static final String PUBLISHER_IMPL_CONFIG_KEY = "publisherImpl";
        public static final String IS_CHOREO_DEPLOYMENT_CONFIG_KEY = "isChoreoDeployment";
        public static final String TYPE_CONFIG_KEY = "type";
        public static final String PUBLISHER_REPORTER_CLASS_CONFIG_KEY = "publisher.reporter.class";
        public static final String AUTH_URL_CONFIG_KEY = "authURL";
        public static final String AUTH_TOKEN_CONFIG_KEY = "authToken";
        public static final String RESPONSE_SCHEMA = "RESPONSE";
        public static final String ERROR_SCHEMA = "ERROR";
        public static final String CHOREO_RESPONSE_SCHEMA = "CHOREO_RESPONSE";
        public static final String CHOREO_FAULT_SCHEMA = "CHOREO_ERROR";

        public static final String ELK_TYPE = "elk";
        public static final String DEFAULT_ELK_PUBLISHER_REPORTER_CLASS
                = "org.wso2.am.analytics.publisher.reporter.elk.ELKMetricReporter";

    }
}
