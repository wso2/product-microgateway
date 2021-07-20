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
 * Constants for Admin Rest API
 */
public class AdminConstants {

    public static final String API_TYPE = "API";
    public static final String API_INFO_TYPE = "API_INFO";
    public static final String SUBSCRIPTION_TYPE = "SUBSCRIPTION";
    public static final String APPLICATION_TYPE = "APPLICATION";
    public static final String APPLICATION_THROTTLING_POLICY_TYPE = "APPLICATION_THROTTLING_POLICY";
    public static final String SUBSCRIPTION_THROTTLING_POLICY_TYPE = "SUBSCRIPTION_THROTTLING_POLICY";
    public static final String REVOKED_TOKEN_TYPE = "REVOKED_TOKENS";

    /**
     * Admin Resource definitions
     */
    public static class AdminResources {
        public static final String API_INFO = "/api/info";
        public static final String APIS = "/apis";
        public static final String APPLICATIONS = "/applications";
        public static final String SUBSCRIPTIONS = "/subscriptions";
        public static final String SUBSCRIPTION_THROTTLING_POLICIES = "/throttling_policies/subscription";
        public static final String APPLICATION_THROTTLING_POLICIES = "/throttling_policies/application";
        public static final String REVOKED_TOKENS = "/revoked_tokens";
    }

    /**
     * Query parameter definitions
     */
    public static class Parameters {
        public static final String NAME = "name";
        public static final String CONTEXT = "context";
        public static final String VERSION = "version";
        public static final String API_UUID = "apiUUID";
        public static final String APP_UUID = "appUUID";
        public static final String CONSUMER_KEY = "consumerKey";
        public static final String ORGANIZATION_ID = "orgId";
        public static final String TOKEN = "token";
        public static final String STATE = "state";
    }

    /**
     * Error messages
     */
    public static class ErrorMessages {
        public static final String METHOD_NOT_IMPLEMENTED =
                "{\"error\": true, \"message\": \"Request Method not implemented.\"}";
        public static final String INTERNAL_SERVER_ERROR =
                "{\"error\": true, \"message\": \"Internal server error while processing the request\"}";
        public static final String RESOURCE_NOT_FOUND_ERROR =
                "{\"error\": true, \"message\": \"Requested Resource Not Found\"}";
        public static final String UNAUTHORIZED_ERROR = "{\"error\": true, " +
                        "\"message\": \"Username/ password invalid. User is not authorized to invoke the resource.\"}";
        public static final String NO_AUTH_HEADER_ERROR =
                "{\"error\": true, \"message\": \"No Authorization header provided\"}";
    }
}
