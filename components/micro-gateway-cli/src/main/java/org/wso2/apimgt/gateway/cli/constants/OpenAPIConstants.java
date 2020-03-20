package org.wso2.apimgt.gateway.cli.constants;

import com.google.common.collect.ImmutableList;

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
    public static final String APIM_THROTTLING_TIER = "x-throttling-tier";
    public static final String DISABLE_SECURITY = "x-wso2-disable-security";
    public static final String AUTHORIZATION_HEADER = "x-wso2-auth-header";
    public static final String RESPONSE_CACHE = "x-wso2-response-cache";
    public static final String INTERCEPTOR_FUNC_SEPARATOR = ":";
    public static final String INTERCEPTOR_PATH_SEPARATOR = "/";
    public static final String INTERCEPTOR_JAVA_PREFIX = "java:";
    public static final String MODULE_IMPORT_STATEMENT_CONSTANT = "as";
    public static final String API_OWNER = "x-wso2-owner";
    public static final String TRANSPORT_SECURITY = "x-wso2-transports";
    public static final String MUTUAL_SSL = "x-wso2-mutual-ssl";
    public static final String APPLICATION_SECURITY = "x-wso2-application-security";
    public static final String APPLICATION_SECURITY_TYPES = "security-types";
    public static final String DEFAULT_API_KEY_HEADER_QUERY = "apikey";
    public static final String TRANSPORT_HTTP = "http";
    public static final String TRANSPORT_HTTPS = "https";
    public static final String MANDATORY = "mandatory";
    public static final String OPTIONAL = "optional";
    public static final String ENABLED = "enabled";
    public static final String DISABLED = "Disabled";
    public static final String CACHE_TIMEOUT = "cacheTimeoutInSeconds";
    public static final String CACHE_ENABLED = "Enabled";
    public static final String VERSION_PLACEHOLDER = "{version}";
    public static final String BASE_PATH_PLACEHOLDER = "{basePath}";

    public static final ImmutableList<String> MODULE_IDENTIFIER_LIST = ImmutableList.of("vienna", "canberra",
            "berlin", "athens", "georgetown", "budapest", "jakarta", "rome", "dublin", "tokyo", "bucharest", "moscow",
            "lisbon", "manila", "lima", "oslo", "amsterdam", "kathmandu", "bridgetown", "cairo", "argentina",
            "austria", "belgium", "benin", "cameroon", "sofia", "chile", "chad", "cuba", "cyprus", "denmark",
            "nicosia", "fiji", "greece", "hungary", "laos", "libya", "malawi", "mauritius", "panama", "rabat", "peru",
            "romania", "rwanda", "kigali", "castries", "kingstown", "victoria", "slovenia", "bern");

    /**
     * API security types supported by mgw
     */
    public enum APISecurity {
        basic,
        oauth2,
        jwt,
        apikey
    }
    //map x-wso2-api-security security types to mgw security types
    public static final Map<String, String> APPLICATION_LEVEL_SECURITY;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("basic_auth", APISecurity.basic.name());
        map.put("api_key", APISecurity.apikey.name());
        map.put("oauth2", APISecurity.oauth2.name());
        APPLICATION_LEVEL_SECURITY = Collections.unmodifiableMap(map);
    }
}
