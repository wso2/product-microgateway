package org.wso2.apimgt.gateway.cli.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants for wso2 OpenAPI extensions.
 */
public class OpenAPIConstants {
    public static final String BASEPATH = "x-wso2-basePath";
    public static final String REQUEST_INTERCEPTOR = "x-wso2-request-interceptor";
    public static final String RESPONSE_INTERCEPTOR = "x-wso2-response-interceptor";
    public static final String ENDPOINTS = "x-wso2-endpoints";
    public static final String ENDPOINTS_REFERENCE = "#/x-wso2-endpoints/";
    public static final String PRODUCTION_ENDPOINTS = "x-wso2-production-endpoints";
    public static final String SANDBOX_ENDPOINTS = "x-wso2-sandbox-endpoints";
    public static final String CORS = "x-wso2-cors";
    public static final String THROTTLING_TIER = "x-wso2-throttling-tier";
    public static final String DISABLE_SECURITY = "x-wso2-disable-security";
    public static final String AUTHORIZATION_HEADER = "x-wso2-auth-header";
    public static final String API_OWNER = "x-wso2-owner";
    public static final String TRANSPORT_SECURITY = "x-wso2-transports";
    public static final String APPLICATION_SECURITY = "x-wso2-application-security";
    public static final String APPLICATION_SECURITY_TYPES = "security-types";

    /**
     * API security types supported by mgw
     */
    public enum APISecurity {
        basic,
        oauth2,
        jwt,
        apikey
    }
    public static final String DEFAULT_API_KEY_HEADER_QUERY = "apikey";
    //map x-wso2-api-security security types to mgw security types
    public static final Map<String, String> APPLICATION_LEVEL_SECURITY;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("basic_auth", APISecurity.basic.name());
        map.put("api_key", APISecurity.apikey.name());
        map.put("oauth2", APISecurity.oauth2.name());
        APPLICATION_LEVEL_SECURITY = Collections.unmodifiableMap(map);
    }
    public static final String TRANSPORT_HTTP = "http";
    public static final String TRANSPORT_HTTPS = "https";
}
