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

import java.util.List;

/**
 * Holds the common set of constants for the enforcer package.
 */
public class APIConstants {
    public static final String DEFAULT = "default";
    //open API extensions
    public static final String X_WSO2_BASE_PATH = "x-wso2-basepath";

    public static final String GW_VHOST_PARAM = "vHost";
    public static final String GW_BASE_PATH_PARAM = "basePath";
    public static final String GW_RES_PATH_PARAM = "path";
    public static final String GW_RES_METHOD_PARAM = "method";
    public static final String GW_VERSION_PARAM = "version";
    public static final String GW_API_NAME_PARAM = "name";
    public static final String PROTOTYPED_LIFE_CYCLE_STATUS = "PROTOTYPED";
    public static final String PUBLISHED_LIFE_CYCLE_STATUS = "PUBLISHED";
    public static final String UNLIMITED_TIER = "Unlimited";
    public static final String UNAUTHENTICATED_TIER = "Unauthenticated";
    public static final String END_USER_ANONYMOUS = "anonymous";
    public static final String ANONYMOUS_PREFIX = "anon:";

    public static final String GATEWAY_SIGNED_JWT_CACHE = "SignedJWTParseCache";
    public static final String PUBLISHER_CERTIFICATE_ALIAS = "publisher_certificate_alias";
    public static final String APIKEY_CERTIFICATE_ALIAS = "apikey_certificate_alias";
    public static final String WSO2_PUBLIC_CERTIFICATE_ALIAS = "wso2carbon";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";
    public static final String BANDWIDTH_TYPE = "bandwidthVolume";
    public static final String INTERNAL_WEB_APP_EP = "/internal/data/v1";
    public static final String AUTHORIZATION_HEADER_DEFAULT = "Authorization";
    public static final String AUTHORIZATION_BASIC = "Basic ";
    public static final String AUTHORIZATION_BEARER = "Bearer ";
    public static final String HEADER_TENANT = "xWSO2Tenant";
    public static final String DEFAULT_VERSION_PREFIX = "_default_";
    public static final String DEFAULT_WEBSOCKET_VERSION = "defaultVersion";
    public static final String DELEM_COLON = ":";
    public static final String API_KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String API_KEY_TYPE_SANDBOX = "SANDBOX";
    public static final String TENANT_DOMAIN_SEPARATOR = "@";

    public static final String AUTHORIZATION_HEADER_BASIC = "Basic";
    public static final String API_SECURITY_OAUTH2 = "oauth2";
    public static final String API_SECURITY_MUTUAL_SSL = "mutualssl";
    public static final String API_SECURITY_BASIC_AUTH = "basic_auth";
    public static final String SWAGGER_API_KEY_AUTH_TYPE_NAME = "apiKey";
    public static final String SWAGGER_API_KEY_IN_HEADER = "header";
    public static final String SWAGGER_API_KEY_IN_QUERY = "query";
    public static final String API_SECURITY_MUTUAL_SSL_MANDATORY = "mutualssl_mandatory";
    public static final String API_SECURITY_OAUTH_BASIC_AUTH_API_KEY_MANDATORY = "oauth_basic_auth_api_key_mandatory";
    public static final String API_SECURITY_MUTUAL_SSL_NAME = "mtls";
    public static final String CLIENT_CERTIFICATE_HEADER_DEFAULT = "X-WSO2-CLIENT-CERTIFICATE";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    public static final String BEGIN_CERTIFICATE_STRING = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERTIFICATE_STRING = "-----END CERTIFICATE-----";
    public static final String BEGIN_PUBLIC_KEY_STRING = "-----BEGIN PUBLIC KEY-----\n";
    public static final String END_PUBLIC_KEY_STRING = "-----END PUBLIC KEY-----";
    public static final String OAUTH2_DEFAULT_SCOPE = "default";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_TIMESTAMP = "timestamp";
    public static final String EVENT_PAYLOAD = "event";
    public static final String EVENT_PAYLOAD_DATA = "payloadData";

    public static final String NOT_FOUND_MESSAGE = "Not Found";
    public static final String NOT_FOUND_DESCRIPTION = "The requested resource is not available.";
    public static final String NOT_IMPLEMENTED_MESSAGE = "Not Implemented";
    public static final String BAD_REQUEST_MESSAGE = "Bad Request";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error";

    //headers and values
    public static final String CONTENT_TYPE_HEADER = "content-type";
    public static final String SOAP_ACTION_HEADER_NAME = "soapaction";
    public static final String ACCEPT_HEADER = "accept";
    public static final String PREFER_HEADER = "prefer";
    public static final String PREFER_CODE = "code";
    public static final String PREFER_EXAMPLE = "example";
    public static final List<String> PREFER_KEYS = List.of(PREFER_CODE, PREFER_EXAMPLE);
    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT_XML = "text/xml";
    public static final String CONTENT_TYPE_SOAP_XML = "application/soap+xml";
    public static final String APPLICATION_GRAPHQL = "application/graphql";
    public static final String X_FORWARDED_FOR = "x-forwarded-for";
    public static final String PATH_HEADER = ":path";
    public static final String UPGRADE_HEADER = "upgrade";
    public static final String WEBSOCKET = "websocket";

    public static final String LOG_TRACE_ID = "traceId";

    // SOAP protocol versions
    public static final String SOAP11_PROTOCOL = "SOAP 1.1 Protocol";
    public static final String SOAP12_PROTOCOL = "SOAP 1.2 Protocol";

    /**
     * Holds the constants related to denied response types.
     */
    public static class ErrorResponseTypes {
        public static final String SOAP11 = "SOAP11";
        public static final String SOAP12 = "SOAP12";
        public static final String JSON = "JSON";
    }

    /**
     * Holds the common set of constants related to the output status codes of the security validations.
     */
    public static class KeyValidationStatus {

        public static final int API_AUTH_GENERAL_ERROR = 900900;
        public static final int API_AUTH_INVALID_CREDENTIALS = 900901;
        public static final int API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE = 900905;
        public static final int API_BLOCKED = 900907;
        public static final int API_AUTH_RESOURCE_FORBIDDEN = 900908;
        public static final int SUBSCRIPTION_INACTIVE = 900909;
        public static final int INVALID_SCOPE = 900910;
        public static final int KEY_MANAGER_NOT_AVAILABLE = 900912;

        private KeyValidationStatus() {

        }
    }

    /**
     * Holds the common set of constants for output of the subscription validation.
     */
    public static class SubscriptionStatus {

        public static final String BLOCKED = "BLOCKED";
        public static final String PROD_ONLY_BLOCKED = "PROD_ONLY_BLOCKED";
        public static final String UNBLOCKED = "UNBLOCKED";
        public static final String ON_HOLD = "ON_HOLD";
        public static final String TIER_UPDATE_PENDING = "TIER_UPDATE_PENDING";
        public static final String REJECTED = "REJECTED";

        private SubscriptionStatus() {

        }
    }

    /**
     * Holds the common set of constants related to life cycle states.
     */
    public static class LifecycleStatus {
        public static final String BLOCKED = "BLOCKED";
    }

    /**
     * Holds the common set of constants for validating the JWT tokens.
     */
    public static class JwtTokenConstants {

        public static final String APPLICATION = "application";
        public static final String APPLICATION_ID = "id";
        public static final String APPLICATION_UUID = "uuid";
        public static final String APPLICATION_NAME = "name";
        public static final String APPLICATION_TIER = "tier";
        public static final String APPLICATION_OWNER = "owner";
        public static final String KEY_TYPE = "keytype";
        public static final String CONSUMER_KEY = "consumerKey";
        public static final String AUTHORIZED_PARTY = "azp";
        public static final String KEY_ID = "kid";
        public static final String JWT_ID = "jti";
        public static final String SUBSCRIPTION_TIER = "subscriptionTier";
        public static final String SUBSCRIBER_TENANT_DOMAIN = "subscriberTenantDomain";
        public static final String TIER_INFO = "tierInfo";
        public static final String STOP_ON_QUOTA_REACH = "stopOnQuotaReach";
        public static final String SPIKE_ARREST_LIMIT = "spikeArrestLimit";
        public static final String SPIKE_ARREST_UNIT = "spikeArrestUnit";
        public static final String SCOPE = "scope";
        public static final String SCOPE_DELIMITER = " ";
        public static final String ISSUED_TIME = "iat";
        public static final String EXPIRY_TIME = "exp";
        public static final String JWT_KID = "kid";
        public static final String SIGNATURE_ALGORITHM = "alg";
        public static final String TOKEN_TYPE = "token_type";
        public static final String BACKEND_TOKEN = "backendJwt";
        public static final String SUBSCRIBED_APIS = "subscribedAPIs";
        public static final String API_CONTEXT = "context";
        public static final String API_VERSION = "version";
        public static final String API_PUBLISHER = "publisher";
        public static final String API_NAME = "name";
        public static final String QUOTA_TYPE = "tierQuotaType";
        public static final String QUOTA_TYPE_BANDWIDTH = "bandwidthVolume";
        public static final String PERMITTED_IP = "permittedIP";
        public static final String PERMITTED_REFERER = "permittedReferer";
        public static final String INTERNAL_KEY_TOKEN_TYPE = "InternalKey";
        public static final String PARAM_SEPARATOR = "&";
        public static final String PARAM_VALUE_SEPARATOR = "=";
        public static final String INTERNAL_KEY_APP_NAME = "internal-key-app";

    }

    /**
     * Holds the common set of constants for validating the JWT tokens
     */
    public static class KeyManager {

        public static final String SERVICE_URL = "ServiceURL";
        public static final String INIT_DELAY = "InitDelay";
        public static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
        public static final String CLIENT_REGISTRATION_ENDPOINT = "client_registration_endpoint";
        public static final String KEY_MANAGER_OPERATIONS_DCR_ENDPOINT = "/keymanager-operations/dcr/register";
        public static final String KEY_MANAGER_OPERATIONS_USERINFO_ENDPOINT = "/keymanager-operations/user-info";
        public static final String TOKEN_ENDPOINT = "token_endpoint";
        public static final String REVOKE_ENDPOINT = "revoke_endpoint";
        public static final String WELL_KNOWN_ENDPOINT = "well_known_endpoint";
        public static final String SCOPE_MANAGEMENT_ENDPOINT = "scope_endpoint";
        public static final String AVAILABLE_GRANT_TYPE = "grant_types";
        public static final String ENABLE_TOKEN_GENERATION = "enable_token_generation";
        public static final String ENABLE_TOKEN_HASH = "enable_token_hash";
        public static final String ENABLE_TOKEN_ENCRYPTION = "enable_token_encryption";
        public static final String ENABLE_OAUTH_APP_CREATION = "enable_oauth_app_creation";
        public static final String DEFAULT_KEY_MANAGER = "Resident Key Manager";
        public static final String APIM_PUBLISHER_ISSUER = "APIM Publisher";
        public static final String APIM_APIKEY_ISSUER = "APIM APIkey";

        // APIM_APIKEY_ISSUER_URL is intentionally different from the Resident Key Manager
        // to avoid conflicts with the access token issuer configs.
        public static final String APIM_APIKEY_ISSUER_URL = "https://host:9443/apikey";
        
        public static final String DEFAULT_KEY_MANAGER_TYPE = "default";
        public static final String DEFAULT_KEY_MANAGER_DESCRIPTION = "This is Resident Key Manager";

        public static final String ISSUER = "issuer";
        public static final String JWKS_ENDPOINT = "jwks_endpoint";
        public static final String USERINFO_ENDPOINT = "userinfo_endpoint";
        public static final String AUTHORIZE_ENDPOINT = "authorize_endpoint";
        public static final String EVENT_HUB_CONFIGURATIONS = "EventHubConfigurations";
        public static final String KEY_MANAGER = "KeyManager";
        public static final String APPLICATION_CONFIGURATIONS = "ApplicationConfigurations";
        public static final String EVENT_RECEIVER_CONFIGURATION = "EventReceiverConfiguration";

        public static final String ENABLE = "Enable";
        public static final String USERNAME = "Username";
        public static final String PASSWORD = "Password";
        public static final String SELF_VALIDATE_JWT = "self_validate_jwt";
        public static final String CLAIM_MAPPING = "claim_mappings";
        public static final String VALIDATION_TYPE = "validation_type";
        public static final String VALIDATION_JWT = "jwt";
        public static final String VALIDATION_REFERENCE = "reference";
        public static final String VALIDATION_CUSTOM = "custom";
        public static final String TOKEN_FORMAT_STRING = "token_format_string";
        public static final String ENABLE_TOKEN_VALIDATION = "validation_enable";
        public static final String VALIDATION_ENTRY_JWT_BODY = "body";
        public static final String API_LEVEL_ALL_KEY_MANAGERS = "all";
        public static final String REGISTERED_TENANT_DOMAIN = "tenantDomain";
        public static final String ENABLE_MAP_OAUTH_CONSUMER_APPS = "enable_map_oauth_consumer_apps";
        public static final String KEY_MANAGER_TYPE = "type";
        public static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F" +
                "]{3}-[0-9a-fA-F]{12}";
        public static final String CONSUMER_KEY_CLAIM = "consumer_key_claim";
        public static final String SCOPES_CLAIM = "scopes_claim";
        public static final String CERTIFICATE_TYPE = "certificate_type";
        public static final String CERTIFICATE_VALUE = "certificate_value";
        public static final String CERTIFICATE_TYPE_JWKS_ENDPOINT = "JWKS";
        public static final String CERTIFICATE_TYPE_PEM_FILE = "PEM";
        public static final String EVENT_PUBLISHER_CONFIGURATIONS = "EventPublisherConfiguration";
        public static final String KEY_MANAGER_TYPE_HEADER = "X-WSO2-KEY-MANAGER";
        public static final String ACCESS_TOKEN = "accessToken";
        public static final String AUTH_CODE = "authCode";
        public static final String CLAIM_DIALECT = "dialect";
        public static final String DEFAULT_KEY_MANAGER_OPENID_CONNECT_DISCOVERY_ENDPOINT =
                "/oauth2/token/.well-known/openid-configuration";
        public static final String DEFAULT_JWKS_ENDPOINT = "/oauth2/jwks";
        public static final String PRODUCTION_TOKEN_ENDPOINT = "production_token_endpoint";
        public static final String SANDBOX_TOKEN_ENDPOINT = "sandbox_token_endpoint";
        public static final String PRODUCTION_REVOKE_ENDPOINT = "production_revoke_endpoint";
        public static final String SANDBOX_REVOKE_ENDPOINT = "sandbox_revoke_endpoint";
        public static final String APPLICATION_ACCESS_TOKEN_EXPIRY_TIME = "application_access_token_expiry_time";
        public static final String USER_ACCESS_TOKEN_EXPIRY_TIME = "user_access_token_expiry_time";
        public static final String REFRESH_TOKEN_EXPIRY_TIME = "refresh_token_expiry_time";
        public static final String ID_TOKEN_EXPIRY_TIME = "id_token_expiry_time";
        public static final String NOT_APPLICABLE_VALUE = "N/A";

        /**
         * Holds the common set of constants for validating the JWT tokens
         */
        public static class KeyManagerEvent {

            public static final String EVENT_TYPE = "event_type";
            public static final String KEY_MANAGER_CONFIGURATION = "key_manager_configuration";
            public static final String ACTION = "action";
            public static final String NAME = "name";
            public static final String ENABLED = "enabled";
            public static final String VALUE = "value";
            public static final String TENANT_DOMAIN = "tenantDomain";
            public static final String ACTION_ADD = "add";
            public static final String ACTION_UPDATE = "update";
            public static final String ACTION_DELETE = "delete";
            public static final String TYPE = "type";
            public static final String KEY_MANAGER_STREAM_ID = "org.wso2.apimgt.keymgt.stream:1.0.0";
        }
    }

    /**
     * Holds the common set of constants which holds service paths to load subscription related data.
     */
    public static class SubscriptionValidationResources {

        public static final String APIS = "/apis";
        public static final String APPLICATIONS = "/applications";
        public static final String SUBSCRIPTIONS = "/subscriptions";
        public static final String SUBSCRIBERS = "/subscribers";
        public static final String APPLICATION_KEY_MAPPINGS = "/application-key-mappings";
        public static final String APPLICATION_POLICIES = "/application-policies";
        public static final String API_POLICIES = "/api-policies";
        public static final String SUBSCRIPTION_POLICIES = "/subscription-policies";

        private SubscriptionValidationResources() {

        }
    }

    /**
     * Supported event types.
     */
    public enum EventType {
        API_CREATE,
        API_UPDATE,
        API_DELETE,
        API_LIFECYCLE_CHANGE,
        APPLICATION_CREATE,
        APPLICATION_UPDATE,
        APPLICATION_DELETE,
        APPLICATION_REGISTRATION_CREATE,
        POLICY_CREATE,
        POLICY_UPDATE,
        POLICY_DELETE,
        SUBSCRIPTIONS_CREATE,
        SUBSCRIPTIONS_UPDATE,
        SUBSCRIPTIONS_DELETE,
        SCOPE_CREATE,
        SCOPE_UPDATE,
        SCOPE_DELETE
    }

    /**
     * Supported policy types.
     */
    public enum PolicyType {
        API,
        APPLICATION,
        SUBSCRIPTION
    }

    /**
     * Holds the constants related to attributes to be sent in the response in case of an error
     * scenario raised within the enforcer.
     */
    public static class MessageFormat {
        public static final String STATUS_CODE = "status_code";
        public static final String ERROR_CODE = "code";
        public static final String ERROR_MESSAGE = "error_message";
        public static final String ERROR_DESCRIPTION = "error_description";
    }

    /**
     * Holds the values related http status codes.
     */
    public enum StatusCodes {
        OK("200", 200),
        UNAUTHENTICATED("401", 401),
        UNAUTHORIZED("403", 403),
        NOTFOUND("404", 404),
        THROTTLED("429", 429),
        SERVICE_UNAVAILABLE("503", 503),
        INTERNAL_SERVER_ERROR("500", 500),
        BAD_REQUEST_ERROR("400", 400),
        NOT_IMPLEMENTED_ERROR("501", 501);

        private String value;
        private int code;
        private  StatusCodes(String value, int code) {
            this.value = value;
            this.code = code;
        }

        public String getValue() {
            return this.value;
        }

        public int getCode() {
            return this.code;
        }
    }

    /**
     *  Holds the values for API types
     */
    public static class ApiType {
        public static final String WEB_SOCKET = "WS";
        public static final String GRAPHQL = "GRAPHQL";
        public static final String REST = "HTTP";
    }


    /**
     * Holds values for optionality.
     */
    public static class Optionality {
        public static final String MANDATORY = "mandatory";
        public static final String OPTIONAL = "optional";
    }

}
