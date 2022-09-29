/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.choreo.connect.enforcer.tracing;

/**
 * This class contains all the constants related to the tracing implementation with azure app insights
 */
public class TracingConstants {
    public static final String SERVICE_NAME_PREFIX = "choreo_connect_enforcer";
    public static final String AZURE_TRACE_EXPORTER = "azure";
    public static final String JAEGER_TRACE_EXPORTER = "jaeger";
    public static final String ZIPKIN_TRACE_EXPORTER = "zipkin";
    public static final String OTLP_TRACE_EXPORTER = "otlp";
    public static final String DO_THROTTLE_SPAN = "ThrottleFilter:doThrottle():Throttling function";
    public static final String ENFORCER_START_SPAN = "Enforcer:Starting request verification";
    public static final String PUBLISH_THROTTLE_EVENT_SPAN = "ThrottleFilter:handleRequest():Publish non " +
            "throttled event";
    public static final String ANALYTICS_SPAN = "AnalyticsFilter:handleSuccessRequest():Analytics Success Flow";
    public static final String ANALYTICS_FAILURE_SPAN = "AnalyticsFilter:handleSuccessRequest():Analytics Failure Flow";
    public static final String CORS_SPAN = "CorsFilter:handleRequest():Cors request handler";
    public static final String EXT_AUTH_SERVICE_SPAN = "ExtAuthService:check()";
    public static final String DECODE_TOKEN_HEADER_SPAN = "JWTAuthenticator:authenticate():Decode token header";
    public static final String JWT_AUTHENTICATOR_SPAN = "JWTAuthenticator:authenticate():Authenticate request using " +
            "JWT Authenticator";
    public static final String API_KEY_AUTHENTICATOR_SPAN = "InternalAPIKeyAuthenticator:authenticate():Authenticate" +
            " request using API Key Authenticator";
    public static final String API_KEY_VALIDATE_SUBSCRIPTION_SPAN = "InternalAPIKeyAuthenticator:" +
            "authenticate():Validate API subscription";
    public static final String VERIFY_TOKEN_IN_CACHE_SPAN = "InternalAPIKeyAuthenticator:authenticate():Verify " +
            "internal key/token in cache";
    public static final String VERIFY_TOKEN_SPAN = "InternalAPIKeyAuthenticator:authenticate():Verify internal " +
            "key/token when it is not found in cache";
    public static final String OAUTH_AUTHENTICATOR_SPAN = "OAUTHAuthenticator:authenticate():Authenticate request "
            + "using OAUTH Authenticator";
    public static final String SUBSCRIPTION_VALIDATION_SPAN = "JWTAuthenticator:authenticate():Validate " +
            "subscription using key manager";
    public static final String SCOPES_VALIDATION_SPAN = "JWTAuthenticator:authenticate():Validate scopes";
    public static final String UNSECURED_API_AUTHENTICATOR_SPAN = "UnsecuredAPIAuthenticator:authenticate():" +
            "Authenticate request using Unsecured Api Authenticator.";
    public static final String MTLS_API_AUTHENTICATOR_SPAN = "MTLSAPIAuthenticator:authenticate():" +
            "Authenticate request using MTLS Api Authenticator.";
    public static final String WS_HANDLER_SPAN = "WebSocketHandler:process():Handle request coming through" +
            " websocket frame service";
    public static final String WS_THROTTLE_SPAN = "WebSocketThrottleFilter:handleRequest():WebSocket throttling filter";
    public static final String WS_METADATA_SPAN = "WebSocketMetaDataFilter:handleRequest():WebSocket Metadata filter";

    // config property keys
    public static final String CONF_CONNECTION_STRING = "connectionString";
    public static final String CONF_MAX_TRACES_PER_SEC = "maximumTracesPerSecond";
    public static final String CONF_INSTRUMENTATION_NAME = "instrumentationName";
    public static final String CONF_EXPORTER_TIMEOUT = "timeout";
    public static final String CONF_ENDPOINT = "endpoint";
    public static final String CONF_HOST = "host";
    public static final String CONF_PORT = "port";
    public static final String CONF_AUTH_HEADER_NAME = "authHeaderName";
    public static final String CONF_AUTH_HEADER_VALUE = "authHeaderValue";
    public static final int DEFAULT_MAX_TRACES_PER_SEC = 2;
    public static final String DEFAULT_INSTRUMENTATION_NAME = "CHOREO-CONNECT";
    public static final long DEFAULT_TRACING_READ_TIMEOUT = 15;
}
