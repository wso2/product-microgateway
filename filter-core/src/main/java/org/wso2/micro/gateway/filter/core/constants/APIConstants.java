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
package org.wso2.micro.gateway.filter.core.constants;

/**
 * Holds the common set of constants for the filter chain package.
 */
public class APIConstants {

    public static final String UNLIMITED_TIER = "Unlimited";

    //open API extensions
    public static final String X_WSO2_BASE_PATH = "x-wso2-basepath";
    public static final String BASE_PATH_PARAM = "basePath";
    public static final String RESOURCE_PATH_PARAMETER = "path";


    public static final String GATEWAY_SIGNED_JWT_CACHE = "SignedJWTParseCache";
    public static final String GATEWAY_PUBLIC_CERTIFICATE_ALIAS = "gateway_certificate_alias";
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

    public static final String BEGIN_CERTIFICATE_STRING = "-----BEGIN CERTIFICATE-----\n";
    public static final String END_CERTIFICATE_STRING = "-----END CERTIFICATE-----";
    public static final String BEGIN_PUBLIC_KEY_STRING = "-----BEGIN PUBLIC KEY-----\n";
    public static final String END_PUBLIC_KEY_STRING = "-----END PUBLIC KEY-----";
    public static final String OAUTH2_DEFAULT_SCOPE = "default";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_TIMESTAMP = "timestamp";
    public static final String EVENT_PAYLOAD = "event";

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
     * Holds the common set of constants for output of the subscription validation
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
     * Holds the common set of constants for validating the JWT tokens
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
        public static final String TOKEN_TYPE = "typ";
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
        public static final String GRAPHQL_MAX_DEPTH = "graphQLMaxDepth";
        public static final String GRAPHQL_MAX_COMPLEXITY = "graphQLMaxComplexity";

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
     * Topic Names.
     */
    public static class TopicNames {

        //APIM default topic names
        public static final String TOPIC_THROTTLE_DATA = "throttleData";
        public static final String TOPIC_TOKEN_REVOCATION = "tokenRevocation";
        public static final String TOPIC_CACHE_INVALIDATION = "cacheInvalidation";
        public static final String TOPIC_KEY_MANAGER = "keyManager";
        public static final String TOPIC_NOTIFICATION = "notification";
    }
}
