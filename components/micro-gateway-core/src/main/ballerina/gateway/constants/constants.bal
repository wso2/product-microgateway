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

 public const string AUTHN_SCHEME_BASIC = "basic";
 public const string AUTH_SCHEME_JWT = "jwt";
 public const string AUTH_SCHEME_OAUTH2 = "oauth2";
 public const string AUTH_PROVIDER_CONFIG = "config";
 public const string AUTH_HEADER = "Authorization";
 public const string TEMP_AUTH_HEADER = "WSO2-Authorization";
 public const string AUTH_SCHEME_BASIC = "Basic";
 public const string AUTH_SCHEME_BEARER = "Bearer";
 public const string ANN_PACKAGE = "ballerina/http";
 public const string RESOURCE_ANN_NAME = "ResourceConfig";
 public const string SERVICE_ANN_NAME = "ServiceConfig";
 public const string API_ANN_NAME = "API";
 public const string SKIP_FILTERS_ANN_NAME = "SkipFilters";
 public const string GATEWAY_ANN_PACKAGE = "wso2/gateway";

 public const string BASIC_PREFIX_WITH_SPACE = "Basic ";
 public const string AUTHORIZATION_HEADER = "Authorization";
 public const string COOKIE_HEADER = "Cookie";
 public const string CONTENT_TYPE_HEADER = "Content-Type";
 public const string X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
 public const string APPLICATION_JSON = "application/json";
 public const string TEXT_XML = "text/xml";
 public const string SOAP_ACTION = "SOAPAction";
 public const string VALIDATE_KEY_SOAP_ACTION = "urn:validateKey";
 public const string KEY_VALIDATION_SERVICE_CONTEXT = "/services/APIKeyValidationService";
 public const string UTF_8 = "UTF-8";
 public const string INVALID_COOKIE = "Cookie is Invalid";

 public const string X_FORWARD_FOR_HEADER = "X-FORWARDED-FOR";
 public const string KEY_VALIDATION_RESPONSE = "KEY_VALIDATION_RESPONSE";
 public const string AUTHENTICATION_CONTEXT = "AUTHENTICATION_CONTEXT";
 public const string SUPER_TENANT_DOMAIN_NAME = "carbon.super";
 public int SUPER_TENANT_ID = -1234;
 public const string THROTTLE_KEY = "throttleKey";
 public const string RESOURCE_TIER_ANN_PACKAGE = "ballerina.gateway";
 public const string RESOURCE_TIER_ANN_NAME = "ResourceTier";
 public const string UNLIMITED_TIER = "Unlimited";
 public const string UNAUTHENTICATED_TIER = "Unauthenticated";
 public const string END_USER_ANONYMOUS = "anonymous";
 public const string ANONYMOUS_USER_TENANT_DOMAIN = "anonymous";
 public const string ANONYMOUS_APP_NAME = "anonymous";
 public const string ANONYMOUS_APP_ID = "anonymous";
 public const string ANONYMOUS_APP_OWNER = "anonymous";
 public const string ANONYMOUS_CONSUMER_KEY = "anonymous";
 public const string MESSAGE_ID = "MESSAGE_ID";
 public const string IS_THROTTLE_OUT = "IS_THROTTLE_OUT";
 public const string ALLOWED_ON_QUOTA_REACHED = "ALLOWED_ON_QUOTA_REACHED";
 public const string IS_SECURED = "IS_SECURED";
 public const string THROTTLE_OUT_REASON = "THROTTLE_REASON";
 public const string JWT_HEADER_NAME = "X-JWT-Assertion";
 public const string PRODUCTION_KEY_TYPE = "PRODUCTION";
 public const string ANY_AUTHENTICATION_LEVEL = "Any";
 public const string API_VERSION = "apiVersion";
 public const string USER_NAME_UNKNOWN = "Unknown";
 public const string UNKNOWN_VALUE = "__unknown__";
 public const string STATUS = "status";
 public const string PASSED = "passed";

 public const string FILTER_FAILED = "filter_failed";
 public const string REMOTE_ADDRESS = "remote_address";
 public const string ERROR_CODE = "error_code";
 public const string ERROR_MESSAGE = "error_message";
 public const string ERROR_DESCRIPTION = "error_description";
 public const string HTTP_STATUS_CODE = "status_code";

 public const string API_NAME = "api_name";
 public const string API_CONTEXT = "api_context";

 public const string AUTHN_FILTER = "AUTHN_FILTER";
 public const string AUTHZ_FILTER = "AUTHZ_FILTER";
 public const string SUBSCRIPTION_FILTER = "SUBSCRIPTION_FILTER";
 public const string THROTTLE_FILTER = "THROTTLE_FILTER";
 public const string ANALYTICS_FILTER = "ANALYTICS_FILTER";
 public const string MUTUAL_SSL_FILTER = "MUTUAL_SSL_FILTER";
 public const string VALIDATION_FILTER = "VALIDATION_FILTER";

 public const string SERVICE_TYPE_ATTR = "SERVICE_TYPE";
 public const string KEY_TYPE_ATTR = "KEY_TYPE";
 public const string RESOURCE_NAME_ATTR = "RESOURCE_NAME";
 public const string ACCESS_TOKEN_ATTR = "ACCESS_TOKEN";
 public const string HOST_HEADER_NAME = "Host";
 public const string HOSTNAME_PROPERTY = "hostname";
 public const string PROTOCOL_PROPERTY = "protocol";
 public const string APPLICATION_OWNER_PROPERTY = "applicationOwner";
 public const string API_CREATOR_TENANT_DOMAIN_PROPERTY = "apiCreatorTenantDomain";
 public const string API_TIER_PROPERTY = "apiTier";
 public const string API_METHOD_PROPERTY = "apiMethod";
 public const string CONTINUE_ON_TROTTLE_PROPERTY = "throttledOut";
 public const string USER_AGENT_PROPERTY = "userAgent";
 public const string USER_IP_PROPERTY = "userIp";
 public const string REQUEST_TIME_PROPERTY = "requestTimestamp";
 public const string GATEWAY_TYPE_PROPERTY = "gatewayType";
 public const string GATEWAY_TYPE = "MICRO";


 public const string ERROR_RESPONSE = "error_response";
 public const string ERROR_RESPONSE_CODE = "error_response_code";
 public const string USERNAME = "username";
 public const string PASSWORD = "password";
 public const string ENABLE = "enable";
 public const string REQUIRE = "require";


//Analytics filter related constants
 public const string ZIP_EXTENSION = ".zip";
 public const string UPLOADING_URL = "uploadingUrl";
 public const string API_USAGE_FILE = "api-usage-data.dat";
 public const string TIME_INTERVAL = "timeInterval";
 public const string FILE_NAME = "FileName";
 public const string ACCEPT = "Accept";
 public const string ANALYTICS = "analytics";
 public const string UPLOADING_TIME_SPAN = "uploadingTimeSpanInMillis";
 public const string ROTATING_TIME = "rotatingPeriod";
 public const string UPLOADING_EP = "uploadingEndpoint";
 public const string REQUEST_TIME = "REQUEST_TIME";
 public const string DATACENTER_ID = "datacenterId";
 public const string THROTTLE_LATENCY = "THROTTLE_LATENCY";
 public const string SECURITY_LATENCY_AUTHN = "SECURITY_LATENCY_AUTHN";
 public const string SECURITY_LATENCY_AUTHZ = "SECURITY_LATENCY_AUTHZ";
 public const string SECURITY_LATENCY_AUTHZ_RESPONSE = "SECURITY_LATENCY_AUTHZ_RESPONSE";
 public const string SECURITY_LATENCY_SUBS = "SECURITY_LATENCY_SUBS";
 public const string SECURITY_LATENCY_VALIDATION = "SECURITY_LATENCY_VALIDATION";
 public const string METHOD = "METHOD";
 public const string TS_REQUEST_OUT = "timeStampRequestOut";
 public const string TS_RESPONSE_IN = "timeStampResponseIn";
 public const string API_USAGE_PATH = "api.usage.data.path";
 public const string API_USAGE_DIR = "api-usage-data";
 public const string FILE_UPLOAD_TASK = "task.uploadFiles";
 public const string DESTINATION = "destination";

//validation_filter related constatnts
 public const string PATHS = "paths";
 public const string PARAMETERS = "parameters";
 public const string REFERENCE = "$ref";
 public const string RESPONSES = "responses";
 public const string SCHEMA = "schema";
 public const string ITEMS = "items";
 public const string TYPE = "type";
 public const string ARRAY = "array";
 public const string STRING = "const string";
 public const string INTEGER = "integer";
 public const string NUMBER = "number";
 public const string OBJECT = "object";
 public const string NULL = "null";
 public const string BOOLEAN = "boolean";
 public const string JSON = "json";
 public const string DATE = "date";
 public const string DATE_TIME = "date-time";
 public const string INT_32 = "int32";
 public const string INT_64 = "int64";
 public const string DEFINITIONS = "#/definitions/";
 public const string COMPONENTS_SCHEMAS = "#/components/schemas/";
 public const string YYYY_MM_DD = "yyyy-MM-dd";
 public const string DISCRIMINATOR = "discriminator";
 public const string SWAGGER = "_swagger";
 public const string SEPERATOR = ".";
 public const string BASEPATH = "../../../src/";

// config constants
 public const string KM_CONF_INSTANCE_ID = "keyManager";
 public const string KM_SERVER_URL = "serverUrl";
 public const string KM_TOKEN_CONTEXT = "/oauth2";
 public const string TIMESTAMP_SKEW = "timestampSkew";
 public const string ENABLE_HOSTNAME_VERIFICATION = "verifyHostname";


 public const string BLOCKING_CONDITION_STATE = "state";
 public const string BLOCKING_CONDITION_KEY = "blockingCondition";
 public const string BLOCKING_CONDITION_VALUE = "conditionValue";

 public const string LISTENER_CONF_INSTANCE_ID = "listenerConfig";
 public const string LISTENER_CONF_HOST = "host";
 public const string LISTENER_CONF_HTTP_PORT = "httpPort";
 public const string LISTENER_CONF_HTTPS_PORT = "httpsPort";
 public const string LISTENER_CONF_KEY_STORE_PATH = "keyStore.path";
 public const string LISTENER_CONF_KEY_STORE_PASSWORD = "keyStore.password";
 public const string TOKEN_LISTENER_PORT = "tokenListenerPort";
 public const string FILTERS = "filters";

 public const string MTSL_CONF_INSTANCE_ID = "mutualSSLConfig";
 public const string MTSL_CONF_PROTOCOL_NAME = "protocolName";
 public const string MTSL_CONF_PROTOCOL_VERSIONS = "protocolVersions";
 public const string MTSL_CONF_CIPHERS = "ciphers";
 public const string MTSL_CONF_SSLVERIFYCLIENT = "sslVerifyClient";

 public const string AUTH_CONF_INSTANCE_ID = "authConfig";
 public const string AUTH_HEADER_NAME = "authorizationHeader";
 public const string REMOVE_AUTH_HEADER_FROM_OUT_MESSAGE = "removeAuthHeaderFromOutMessage";

 public const string JWT_INSTANCE_ID = "jwtTokenConfig";
 public const string ISSUER = "issuer";
 public const string AUDIENCE = "audience";
 public const string CERTIFICATE_ALIAS = "certificateAlias";
 public const string TRUST_STORE_PATH = "trustStore.path";
 public const string TRSUT_STORE_PASSWORD = "trustStore.password";

 public const string CACHING_ID = "caching";
 public const string TOKEN_CACHE_ENABLED = "enabled";
 public const string TOKEN_CACHE_EXPIRY = "tokenCache.expiryTime";
 public const string TOKEN_CACHE_CAPACITY = "tokenCache.capacity";
 public const string TOKEN_CACHE_EVICTION_FACTOR = "tokenCache.evictionFactor";

 public const string JWT_CONFIG_INSTANCE_ID = "jwtConfig";
 public const string JWT_HEADER = "header";
 public const string EXPECT_HEADER = "Expect";

 public const string VALIDATION_CONFIG_INSTANCE_ID = "validationConfig";
 public const string REQUEST_VALIDATION_ENABLED = "enableRequestValidation";
 public const string RESPONSE_VALIDATION_ENABLED = "enableResponseValidation";
 public const string SWAGGER_ABSOLUTE_PATH = "absolutePathToSwagger";

 public const string THROTTLE_CONF_INSTANCE_ID = "throttlingConfig";
 public const string GLOBAL_TM_EVENT_PUBLISH_ENABLED = "enabledGlobalTMEventPublishing";
 public const string JMS_CONNECTION_INITIAL_CONTEXT_FACTORY = "jmsConnectioninitialContextFactory";
 public const string JMS_CONNECTION_PROVIDER_URL = "jmsConnectionProviderUrl";
 public const string JMS_CONNECTION_USERNAME = "jmsConnectionUsername";
 public const string JMS_CONNECTION_PASSWORD = "jmsConnectionPassword";
 public const string THROTTLE_ENDPOINT_URL = "throttleEndpointUrl";
 public const string THROTTLE_ENDPOINT_BASE64_HEADER = "throttleEndpointbase64Header";

// end of config constants

 public const string IS_THROTTLED = "isThrottled";
 public const string EXPIRY_TIMESTAMP = "expiryTimeStamp";
 const string TRUE = "true";
 const string REQUEST_BLOCKED = "REQUEST_BLOCKED";
 const string THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED = "API_LIMIT_EXCEEDED";
 const string THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED = "RESOURCE_LIMIT_EXCEEDED";
 const string THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED = "SUBSCRIPTION_LIMIT_EXCEEDED";
 const string THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED = "APPLICATION_LIMIT_EXCEEDED";

 const string INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error occured";
 const string UNPROCESSABLE_ENTITY_MESSAGE = "Unable to process the entity";
 public  const string UNPROCESSABLE_ENTITY_DESCRIPTION = "Unable to process the given entity. Please check whether the provided entity is correct.";

// http codes
 public int INTERNAL_SERVER_ERROR = 500;
 public int FORBIDDEN = 403;
 public int UNAUTHORIZED = 401;
 public int THROTTLED_OUT = 429;
 public int UNPROCESSABLE_ENTITY = 422;

// end of http codes


 const string PATH_SEPERATOR = "/";

//http2 constants
 public const string HTTP2_INSTANCE_ID = "http2";
 public const string HTTP2_PROPERTY = "enable";
 public const string HTTP2 = "2.0";
 public const string HTTP11 = "1.1";


// logging keys
 const string KEY_GW_LISTNER = "APIGatewayListener";
 const string KEY_AUTHN_FILTER = "AuthnFilter";
 const string KEY_AUTHZ_FILTER = "AuthzFilter";
 const string KEY_SUBSCRIPTION_FILTER = "SubscriptionFilter";
 const string KEY_THROTTLE_FILTER = "ThrottleFilter";
 const string KEY_ANALYTICS_FILTER = "AnalyticsFilter";
 const string KEY_MUTUAL_SSL_FILTER = "MutualSSLFilter";
 const string KEY_VALIDATION_FILTER = "ValidationFilter";
 const string KEY_BASIC_AUTH_FILTER = "BasicAuthFilter";
 const string KEY_THROTTLE_UTIL = "ThrottleUtil";
 const string KEY_GW_CACHE = "GatewayCache";
 const string KEY_UTILS = "Utils";
 const string KEY_OAUTH_PROVIDER = "OAuthAuthProvider";
 const string KEY_UPLOAD_TASK = "UploadTimerTask";
 const string KEY_ROTATE_TASK = "RotateTimerTask";
 const string KEY_ETCD_UTIL = "EtcdUtil";


 public int DEFAULT_LISTENER_TIMEOUT = 120000; //2 mins
 public int DEFAULT_ETCD_TRIGGER_TIME = 10000; //10 seconds
