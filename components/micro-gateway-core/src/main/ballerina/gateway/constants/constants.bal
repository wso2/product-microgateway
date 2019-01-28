// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

@Description { value: "Authn scheme basic" }
@final public string AUTHN_SCHEME_BASIC = "basic";
@Description { value: "Authn scheme JWT" }
@final public string AUTH_SCHEME_JWT = "jwt";
@Description { value: "Authn scheme OAuth2" }
@final public string AUTH_SCHEME_OAUTH2 = "oauth2";
@Description { value: "Auth provider config name" }
@final public string AUTH_PROVIDER_CONFIG = "config";
@Description { value: "Authentication header name" }
@final public string AUTH_HEADER = "Authorization";
@Description { value: "Temp Authentication header name" }
@final public string TEMP_AUTH_HEADER = "WSO2-Authorization";
@Description { value: "Basic authentication scheme" }
@final public string AUTH_SCHEME_BASIC = "Basic";
@Description { value: "Bearer authentication scheme" }
@final public string AUTH_SCHEME_BEARER = "Bearer";
@Description { value: "Auth annotation package" }
@final public string ANN_PACKAGE = "ballerina/http";
@Description { value: "Resource level annotation name" }
@final public string RESOURCE_ANN_NAME = "ResourceConfig";
@Description { value: "Service level annotation name" }
@final public string SERVICE_ANN_NAME = "ServiceConfig";
@Description { value: "API annotation name in service level" }
@final public string API_ANN_NAME = "API";
@Description { value: "skip filters annotation name in service level" }
@final public string SKIP_FILTERS_ANN_NAME = "SkipFilters";
@Description { value: "gateway annotation package" }
@final public string GATEWAY_ANN_PACKAGE = "wso2/gateway";

@Description { value: "Basic prefix for authorization header with ending spce" }
@final public string BASIC_PREFIX_WITH_SPACE = "Basic ";
@Description { value: "authorization header " }
@final public string AUTHORIZATION_HEADER = "Authorization";
@Description { value: "Cookie header " }
@final public string COOKIE_HEADER = "Cookie";
@Description { value: "Content type header " }
@final public string CONTENT_TYPE_HEADER = "Content-Type";
@Description { value: "Form url encoded" }
@final public string X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
@Description { value: "Application JSON type" }
@final public string APPLICATION_JSON = "application/json";
@Description { value: "Cookie is not matched " }
@final public string INVALID_COOKIE = "Cookie is Invalid";

@Description { value: "X-Forward-For Header " }
@final public string X_FORWARD_FOR_HEADER = "X-FORWARDED-FOR";
@Description { value: "KeyValidation Response" }
@final public string KEY_VALIDATION_RESPONSE = "KEY_VALIDATION_RESPONSE";
@Description { value: "Authentication context attribute" }
@final public string AUTHENTICATION_CONTEXT = "AUTHENTICATION_CONTEXT";
@Description { value: "carbon.super Tenant Domain" }
@final public string SUPER_TENANT_DOMAIN_NAME = "carbon.super";
@Description { value: "Super Tenant Domain Tenant Id" }
@final public int SUPER_TENANT_ID = -1234;
@Description { value: "throttleKey" }
@final public string THROTTLE_KEY = "throttleKey";
@Description { value: "Resource Tier annotation package" }
@final public string RESOURCE_TIER_ANN_PACKAGE = "ballerina.gateway";
@Description { value: "Resource level annotation name" }
@final public string RESOURCE_TIER_ANN_NAME = "ResourceTier";
@Description { value: "Unlimited Tier" }
@final public string UNLIMITED_TIER = "Unlimited";
@Description { value: "Un authenticated tier level" }
@final public string UNAUTHENTICATED_TIER = "Unauthenticated";
@Description { value: "Anonymous user name" }
@final public string END_USER_ANONYMOUS = "anonymous";
@Description { value: "Anonymous user tenant domain" }
@final public string ANONYMOUS_USER_TENANT_DOMAIN = "anonymous";
@Description { value: "Anonymous app name" }
@final public string ANONYMOUS_APP_NAME = "anonymous";
@Description { value: "Anonymous app ID" }
@final public string ANONYMOUS_APP_ID = "anonymous";
@Description { value: "Anonymous app owner" }
@final public string ANONYMOUS_APP_OWNER = "anonymous";
@Description { value: "Anonymous consumer key" }
@final public string ANONYMOUS_CONSUMER_KEY = "anonymous";
@Description { value: "message id" }
@final public string MESSAGE_ID = "MESSAGE_ID";
@Description { value: "Is requested throttld out" }
@final public string IS_THROTTLE_OUT = "IS_THROTTLE_OUT";
@Description { value: "Is requested allowed after throttld out" }
@final public string ALLOWED_ON_QUOTA_REACHED = "ALLOWED_ON_QUOTA_REACHED";
@Description { value: "Is api secured" }
@final public string IS_SECURED = "IS_SECURED";
@Description { value: "Requested throttld out reason" }
@final public string THROTTLE_OUT_REASON = "THROTTLE_REASON";
@Description { value: "Default jwt header name" }
@final public string JWT_HEADER_NAME = "X-JWT-Assertion";
@Description { value: "Production key type value" }
@final public string PRODUCTION_KEY_TYPE = "PRODUCTION";
@Description { value: "Authentication level any" }
@final public string ANY_AUTHENTICATION_LEVEL = "Any";
@Description { value: "API Name attribute" }
@final public string API_VERSION = "apiVersion";
@Description { value: "User name is Unknown" }
@final public string USER_NAME_UNKNOWN = "Unknown";

@Description { value: "Filter has return false" }
@final public string FILTER_FAILED = "filter_failed";
@Description { value: "remote address as IP" }
@final public string REMOTE_ADDRESS = "remote_address";
@Description { value: "error code attribute" }
@final public string ERROR_CODE = "error_code";
@Description { value: "error message attribute" }
@final public string ERROR_MESSAGE = "error_message";
@Description { value: "error description attribute" }
@final public string ERROR_DESCRIPTION = "error_description";
@final public string HTTP_STATUS_CODE = "status_code";

@Description { value: "API name attribute" }
@final public string API_NAME = "api_name";
@Description { value: "API context attribute" }
@final public string API_CONTEXT = "api_context";

@final public string AUTHN_FILTER = "AUTHN_FILTER";
@final public string AUTHZ_FILTER = "AUTHZ_FILTER";
@final public string SUBSCRIPTION_FILTER = "SUBSCRIPTION_FILTER";
@final public string THROTTLE_FILTER = "THROTTLE_FILTER";
@final public string ANALYTICS_FILTER = "ANALYTICS_FILTER";
@final public string MUTUAL_SSL_FILTER = "MUTUAL_SSL_FILTER";

@final public string SERVICE_TYPE_ATTR = "SERVICE_TYPE";
@final public string KEY_TYPE_ATTR = "KEY_TYPE";
@final public string RESOURCE_NAME_ATTR = "RESOURCE_NAME";
@final public string ACCESS_TOKEN_ATTR = "ACCESS_TOKEN";
@final public string HOST_HEADER_NAME = "Host";
@final public string HOSTNAME_PROPERTY = "hostname";
@final public string PROTOCOL_PROPERTY = "protocol";
@final public string APPLICATION_OWNER_PROPERTY = "applicationOwner";
@final public string API_CREATOR_TENANT_DOMAIN_PROPERTY = "apiCreatorTenantDomain";
@final public string API_TIER_PROPERTY = "apiTier";
@final public string API_METHOD_PROPERTY = "apiMethod";
@final public string CONTINUE_ON_TROTTLE_PROPERTY = "throttledOut";
@final public string USER_AGENT_PROPERTY = "userAgent";
@final public string USER_IP_PROPERTY = "userIp";
@final public string REQUEST_TIME_PROPERTY = "requestTimestamp";
@final public string GATEWAY_TYPE_PROPERTY = "gatewayType";
@final public string GATEWAY_TYPE = "MICRO";


@final public string ERROR_RESPONSE = "error_response";
@final public string ERROR_RESPONSE_CODE = "error_response_code";
@final public string USERNAME = "username";
@final public string PASSWORD = "password";
@final public string ENABLE = "enable";


//Analytics filter related constants
@final public string ZIP_EXTENSION = ".zip";
@Description { value: "Endpoint URL of the uploading web application" }
@final public string UPLOADING_URL = "uploadingUrl";
@Description { value: "Name of the file which the events are getting written into" }
@final public string API_USAGE_FILE = "api-usage-data.dat";
@Description { value: "Time span of the file uploading task" }
@final public string TIME_INTERVAL = "timeInterval";
@Description { value: "FileName header to be sent to uploading EP" }
@final public string FILE_NAME = "FileName";
@Description { value: "Accept header" }
@final public string ACCEPT = "Accept";
@Description { value: "Analytics conf heading" }
@final public string ANALYTICS = "analytics";
@Description { value: "Time inteval of the uploading task" }
@final public string UPLOADING_TIME_SPAN = "uploadingTimeSpanInMillis";
@Description { value: "Rotating time of the file" }
@final public string ROTATING_TIME = "rotatingPeriod";
@Description { value: "Uploading endpoing of the microgateway" }
@final public string UPLOADING_EP = "uploadingEndpoint";
@Description { value: "Starting time of the request" }
@final public string REQUEST_TIME = "REQUEST_TIME";
@Description { value: "Environment variable for datacenter Id" }
@final public string DATACENTER_ID = "datacenterId";
@Description { value: "Time spent on the throttling filter" }
@final public string THROTTLE_LATENCY = "THROTTLE_LATENCY";
@Description { value: "Time spent on the authentication filter" }
@final public string SECURITY_LATENCY_AUTHN = "SECURITY_LATENCY_AUTHN";
@Description { value: "Time spent on the authorization filter" }
@final public string SECURITY_LATENCY_AUTHZ = "SECURITY_LATENCY_AUTHZ";
@Description { value: "Time spent on the authorization response filter" }
@final public string SECURITY_LATENCY_AUTHZ_RESPONSE = "SECURITY_LATENCY_AUTHZ_RESPONSE";
@Description { value: "Time spent on the subscription filter" }
@final public string SECURITY_LATENCY_SUBS = "SECURITY_LATENCY_SUBS";
@Description { value: "HTTP Method" }
@final public string METHOD = "METHOD";
@Description { value: "Time stamp before sending to backend" }
@final public string TS_REQUEST_OUT = "timeStampRequestOut";
@Description { value: "Time stamp after response came in" }
@final public string TS_RESPONSE_IN = "timeStampResponseIn";
@Description { value: "API usage data path variable" }
@final public string API_USAGE_PATH = "api.usage.data.path";
@Description { value: "API usage data directory" }
@final public string API_USAGE_DIR = "api-usage-data";
@Description { value: "Key of file uploading config" }
@final public string FILE_UPLOAD_TASK = "task.uploadFiles";
@Description { value: "Destination" }
@final public string DESTINATION = "destination";

// config constants
@Description { value: "Key manager related configs" }
@final public string KM_CONF_INSTANCE_ID = "keyManager";
@Description { value: "Throttling related configs" }
@final public string THROTTLE_CONF_INSTANCE_ID = "Throttling";
@Description { value: "Key manager server URL parameter" }
@final public string KM_SERVER_URL = "serverUrl";
@Description { value: "Key manager oauth2 endpoint contexs" }
@final public string KM_TOKEN_CONTEXT = "/oauth2";
@Description { value: "time stamp skew for auth caches" }
@final public string TIMESTAMP_SKEW = "timestampSkew";
@Description { value: "Hostname verification enabled or not" }
@final public string ENABLE_HOSTNAME_VERIFICATION = "verifyHostname";


@Description { value: "Block condition state" }
@final public string BLOCKING_CONDITION_STATE = "state";
@Description { value: "Block condition key" }
@final public string BLOCKING_CONDITION_KEY = "blockingCondition";
@Description { value: "Block Condition Value" }
@final public string BLOCKING_CONDITION_VALUE = "conditionValue";

@Description { value: "Listener endpoint related configs" }
@final public string LISTENER_CONF_INSTANCE_ID = "listenerConfig";
@Description { value: "Listener endpoint host" }
@final public string LISTENER_CONF_HOST = "host";
@Description { value: "Listener endpoint http port" }
@final public string LISTENER_CONF_HTTP_PORT = "httpPort";
@Description { value: "Listener endpoint https port" }
@final public string LISTENER_CONF_HTTPS_PORT = "httpsPort";
@Description { value: "Listener endpoint key store path" }
@final public string LISTENER_CONF_KEY_STORE_PATH = "keyStore.path";
@Description { value: "Listener endpoint key store password" }
@final public string LISTENER_CONF_KEY_STORE_PASSWORD = "keyStore.password";
@Description { value: "The port which exposes /token,/revoke, /authorize and etc endpoints" }
@final public string TOKEN_LISTENER_PORT = "tokenListenerPort";
@Description { value: "Set of filters to be enabled" }
@final public string FILTERS = "filters";

@Description { value: "Mutual SSL related configs" }
@final public string MTSL_CONF_INSTANCE_ID = "mutualSSLConfig";
@Description { value: "MutualSSL protocol name" }
@final public string MTSL_CONF_PROTOCOL_NAME = "protocolName";
@Description { value: "MutualSSL protocol versions" }
@final public string MTSL_CONF_PROTOCOL_VERSIONS = "protocolVersions";
@Description { value: "MutualSSL ciphers" }
@final public string MTSL_CONF_CIPHERS = "ciphers";
@Description { value: "MutualSSL Verification" }
@final public string MTSL_CONF_SSLVERIFYCLIENT = "sslVerifyClient";

@Description { value: "Authentication related configs" }
@final public string AUTH_CONF_INSTANCE_ID = "authConfig";
@Description { value: "The authoization header config name" }
@final public string AUTH_HEADER_NAME = "authorizationHeader";
@Description { value: "Config name to remove auth header from out going message" }
@final public string REMOVE_AUTH_HEADER_FROM_OUT_MESSAGE = "removeAuthHeaderFromOutMessage";

@Description { value: "JWT Token related configs" }
@final public string JWT_INSTANCE_ID = "jwtTokenConfig";
@Description { value: "JWT issuer" }
@final public string ISSUER = "issuer";
@Description { value: "JWT audience" }
@final public string AUDIENCE = "audience";
@Description { value: "jwt signed cert alias" }
@final public string CERTIFICATE_ALIAS = "certificateAlias";
@Description { value: "trust store  path" }
@final public string TRUST_STORE_PATH = "trustStore.path";
@Description { value: "Trust store password" }
@final public string TRSUT_STORE_PASSWORD = "trustStore.password";

@Description { value: "Caching configs" }
@final public string CACHING_ID = "caching";
@Description { value: "Token cache enabled or not " }
@final public string TOKEN_CACHE_ENABLED = "enabled";
@Description { value: "Token cache expirt time " }
@final public string TOKEN_CACHE_EXPIRY = "tokenCache.expiryTime";
@Description { value: "Token cache capacity" }
@final public string TOKEN_CACHE_CAPACITY = "tokenCache.capacity";
@Description { value: "Token cache eviction factor" }
@final public string TOKEN_CACHE_EVICTION_FACTOR = "tokenCache.evictionFactor";

@Description { value: "JWT  related configs" }
@final public string JWT_CONFIG_INSTANCE_ID = "jwtConfig";
@Description { value: "JWT  header name" }
@final public string JWT_HEADER = "header";
@final public string EXPECT_HEADER = "Expect";

// end of config constants

@Description { value: "Is Throttled" }
@final public string IS_THROTTLED = "isThrottled";
@Description { value: "Expiry TimeStamp" }
@final public string EXPIRY_TIMESTAMP = "expiryTimeStamp";
@final string TRUE = "true";
@final string REQUEST_BLOCKED = "REQUEST_BLOCKED";
@final string THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED = "API_LIMIT_EXCEEDED";
@final string THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED = "RESOURCE_LIMIT_EXCEEDED";
@final string THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED = "SUBSCRIPTION_LIMIT_EXCEEDED";
@final string THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED = "APPLICATION_LIMIT_EXCEEDED";

@final string INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error occured";


// http codes
@final public int INTERNAL_SERVER_ERROR = 500;
@final public int FORBIDDEN = 403;
@final public int UNAUTHORIZED = 401;
@final public int THROTTLED_OUT = 429;

// end of http codes


@final string PATH_SEPERATOR = "/";

//http2 constants
@Description { value: "HTTP2 related constants"}
@final public string HTTP2_INSTANCE_ID = "http2";
@final public string HTTP2_PROPERTY = "enable";
@final public string HTTP2 = "2.0";
@final public string HTTP11 = "1.1";


// logging keys
@final string KEY_GW_LISTNER = "APIGatewayListener";
@final string KEY_AUTHN_FILTER = "AuthnFilter";
@final string KEY_AUTHZ_FILTER = "AuthzFilter";
@final string KEY_SUBSCRIPTION_FILTER = "SubscriptionFilter";
@final string KEY_THROTTLE_FILTER = "ThrottleFilter";
@final string KEY_ANALYTICS_FILTER = "AnalyticsFilter";
@final string KEY_MUTUAL_SSL_FILTER = "MutualSSLFilter";
@final string KEY_BASIC_AUTH_FILTER = "BasicAuthFilter";
@final string KEY_THROTTLE_UTIL = "ThrottleUtil";
@final string KEY_GW_CACHE = "GatewayCache";
@final string KEY_UTILS = "Utils";
@final string KEY_OAUTH_PROVIDER = "OAuthAuthProvider";
@final string KEY_UPLOAD_TASK = "UploadTimerTask";
@final string KEY_ROTATE_TASK = "RotateTimerTask";


@final public int DEFAULT_LISTENER_TIMEOUT = 120000; //2 mins
