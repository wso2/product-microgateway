// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// default should bind to 0.0.0.0, not localhost. Else will not work in dockerized environments.
public const string DEFAULT_CONF_HOST = "0.0.0.0";
public const int DEFAULT_HTTP_PORT = 9090;
public const int DEFAULT_HTTPS_PORT = 9095;
public const string DEFAULT_HOSTNAME = "*";
public const int DEFAULT_PORT = 443;
public const string DEFAULT_KEY_STORE_PATH = "${mgw-runtime.home}/runtime/bre/security/ballerinaKeystore.p12";
public const string DEFAULT_KEY_STORE_PASSWORD = "ballerina";
public const string DEFAULT_TRUST_STORE_PATH = "${mgw-runtime.home}/runtime/bre/security/ballerinaTruststore.p12";
public const string DEFAULT_TRUST_STORE_PASSWORD = "ballerina";
public const int DEFAULT_TOKEN_LISTENER_PORT = 9096;
public const string DEFAULT_KEEP_ALIVE = "AUTO";
public const int DEFAULT_MAX_PIPELINED_REQUESTS = 10;
public const int DEFAULT_MAX_URI_LENGTH = 4096;
public const int DEFAULT_MAX_HEADER_SIZE = 8192;
public const int DEFAULT_MAX_ENTITY_BODY_SIZE = -1;

public const string DEFAULT_AUTH_HEADER_NAME = "Authorization";
public const boolean DEFAULT_REMOVE_AUTH_HEADER_FROM_OUT_MESSAGE = true;

public const string DEFAULT_KM_SERVER_URL = "https://localhost:9443";
public const string DEFAULT_KM_TOKEN_CONTEXT = "oauth2";
public const int DEFAULT_TIMESTAMP_SKEW = 5000;
public const boolean DEFAULT_EXTERNAL = false;
public const string DEFAULT_KM_CONF_ISSUER = "https://localhost:9443/oauth2/token";
public const boolean DEFAULT_KM_CONF_ENABLE_INTROSPECT_FOR_FAILED_JWT = false;

public const boolean DEFAULT_KM_REMOTE_USER_CLAIM_RETRIEVAL_ENABLED = false;

public const boolean DEFAULT_VALIDATE_SUBSCRIPTIONS = false;

public const boolean DEFAULT_KM_CONF_SECURITY_BASIC_ENABLED = true;
public const string DEFAULT_USERNAME = "admin";
public const string DEFAULT_PASSWORD = "admin";

public const string DEFAULT_PILOT_SERVER_URL = "https://localhost:9443";
public const string DEFAULT_PILOT_INT_CONTEXT = "/internal/data/v1/";
public const string DEFAULT_PILOT_USERNAME = "admin";
public const string DEFAULT_PILOT_PASSWORD = "admin";

public const boolean DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED = false;
public const string DEFAULT_KM_CONF_SECURITY_OAUTH2_CREDENTIAL_BEARER = "AUTH_HEADER_BEARER";
public const string DEFAULT_KM_CONF_SECURITY_OAUTH2 = "";
public const boolean DEFAULT_KM_CONF_IS_LEGACY_KM = false;

public const string DEFAULT_JWT_ISSUER = "https://localhost:9443/oauth2/token";
public const string DEFAULT_ISSUER_CLASSNAME = "DefaultJWTTransformer";
public const string DEFAULT_CONSUMER_KEY_CLAIM = "aud";
public const boolean DEFAULT_VALIDATE_SUBSCRIPTION = false;
public const string DEFAULT_AUDIENCE = "http://org.wso2.apimgt/gateway";
public const string DEFAULT_CERTIFICATE_ALIAS = "wso2apim310";
public const string DEFAULT_JWT_HEADER_NAME = "X-JWT-Assertion";
public const boolean DEFAULT_JWT_REMOTE_USER_CLAIM_RETRIEVAL_ENABLED = false;

public const boolean DEFAULT_CACHING_ENABLED = true;
public const int DEFAULT_TOKEN_CACHE_EXPIRY = 900000;
public const int DEFAULT_TOKEN_EXPIRY = 3600;
public const int DEFAULT_TOKEN_CACHE_CAPACITY = 10000;
public const float DEFAULT_TOKEN_CACHE_EVICTION_FACTOR = 0.25;

//note, for analytics some configuration default values are not set. They are read directly from conf.
public const boolean DEFAULT_ANALYTICS_ENABLED = false;
public const int DEFAULT_UPLOADING_TIME_SPAN_IN_MILLIS = 600000;
public const int DEFAULT_INITIAL_DELAY_IN_MILLIS = 5000;
public const string DEFAULT_UPLOADING_EP = "https://localhost:9444/analytics/v1.0/usage/upload-file";
public const int DEFAULT_ROTATING_PERIOD_IN_MILLIS =  600000;
public const boolean DEFAULT_TASK_UPLOAD_FILES_ENABLED = true;
public const string DEFAULT_AM_ANALYTICS_VERSION = "3.2.0";
public const string DEFAULT_AM_ANALYTICS_VERSION_300 = "3.0.0";
public const string DEFAULT_AM_ANALYTICS_VERSION_310 = "3.1.0";
//constants for gRPC analytics
public const string DEFAULT_GRPC_ENDPOINT_URL = "https://localhost:9806";
public const int DEFAULT_GRPC_RECONNECT_TIME_IN_MILLES = 6000;
public const int DEFAULT_GRPC_TIMEOUT_IN_MILLIS = 2147483647;
//constants for Choreo based analytics
public const string DEFAULT_CHOREO_ANALYTICS_CONFIG_ENDPOINT = "https://analytics-event-auth.choreo.dev/auth/v1";
public const string DEFAULT_CHOREO_ANALYTICS_AUTH_TOKEN = "";

public const boolean DEFAULT_HTTP2_ENABLED = true;

public const string DEFAULT_PROTOCOL_NAME = "TLS";
public const string DEFAULT_PROTOCOL_VERSIONS = "TLSv1.2,TLSv1.1";
public const string DEFAULT_CIPHERS = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, " 
+ "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256, " 
+ "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, " 
+ "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA, " 
+ "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA, " 
+ " TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, " 
+ "TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256, " 
+ "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 , " 
+ "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA, " 
+ "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA, " 
+ "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV";
public const string DEFAULT_SSL_VERIFY_CLIENT = "optional";
public const string DEFAULT_MTSL_CONF_CERT_HEADER_NAME = "X-WSO2-CLIENT-CERTIFICATE";

public const boolean DEFAULT_REQUEST_VALIDATION_ENABLED = false;
public const boolean DEFAULT_RESPONSE_VALIDATION_ENABLED = false;

// Local throttling related constants
public const int DEFAULT_PROCESS_THREAD_POOL_CORE_SIZE = 200;
public const int DEFAULT_PROCESS_THREAD_POOL_MAXIMUM_SIZE = 1000;
public const int DEFAULT_PROCESS_THREAD_POOL_KEEP_ALIVE_TIME = 200;
public const int DEFAULT_THROTTLE_CLEANUP_FREQUENCY = 3600000;

public const boolean DEFAULT_GLOBAL_TM_EVENT_PUBLISH_ENABLED = false;
public const boolean DEFAULT_HEADER_CONDITIONS_ENABLED = false;
public const boolean DEFAULT_QUERY_CONDITIONS_ENABLED = false;
public const boolean DEFAULT_JWT_CONDITIONS_ENABLED = false;
public const string DEFAULT_JMS_CONNECTION_INITIAL_CONTEXT_FACTORY = "wso2mbInitialContextFactory";
public const string DEFAULT_JMS_CONNECTION_PROVIDER_URL = "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'";
public const string DEFAULT_JMS_CONNECTION_USERNAME = "";
public const string DEFAULT_JMS_CONNECTION_PASSWORD = "";
public const string DEFAULT_THROTTLE_ENDPOINT_URL = "https://localhost:9443/endpoints";
public const string DEFAULT_THROTTLE_ENDPOINT_BASE64_HEADER = "admin:admin";
public const string DEFAULT_THROTTLE_KEY_TEMPLATE_URL = "https://localhost:9443/internal/data/v1";

//global throttling - binary publisher related constants
public const boolean DEFAULT_TM_BINARY_PUBLISHER_ENABLED = true;
public const string DEFAULT_TM_RECEIVER_URL_GROUP = "tcp://localhost:9611";
public const string DEFAULT_TM_AUTH_URL_GROUP = "ssl://localhost:9711";
public const string DEFAULT_TM_USERNAME = "admin";
public const string DEFAULT_TM_PASSWORD = "admin";
public const int DEFAULT_TM_PUBLISHER_POOL_MAX_IDLE = 1000;
public const int DEFAULT_TM_PUBLISHER_POOL_INIT_IDLE_CAPACITY = 200;
public const int DEFAULT_TM_PUBLISHER_THREAD_POOL_CORE_SIZE = 200;
public const int DEFAULT_TM_PUBLISHER_THREAD_POOL_MAXIMUM_SIZE = 1000;
public const int DEFAULT_TM_PUBLISHER_THREAD_POOL_KEEP_ALIVE_TIME = 200;

//global throttling - binary agent related constants
public const string DEFAULT_TM_BINARY_AGENT_PROTOCOL_VERSIONS = "TLSv1,TLSv1.1,TLSv1.2";
public const string DEFAULT_TM_BINARY_AGENT_CIPHERS = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, "
+ "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256, "
+ "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, "
+ "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA, "
+ "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA, "
+ " TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, "
+ "TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256, "
+ "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 , "
+ "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA, "
+ "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA, "
+ "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV";
public const int DEFAULT_TM_AGENT_QUEUE_SIZE = 32768;
public const int DEFAULT_TM_AGENT_BATCH_SIZE = 200;
public const int DEFAULT_TM_AGENT_THREAD_POOL_CORE_SIZE = 1;
public const int DEFAULT_TM_AGENT_SOCKET_TIMEOUT_MS = 30000;
public const int DEFAULT_TM_AGENT_THREAD_POOL_MAXIMUM_SIZE = 1;
public const int DEFAULT_TM_AGENT_THREAD_POOL_KEEP_ALIVE_TIME = 20;
public const int DEFAULT_TM_AGENT_RECONNECTION_INTERVAL = 30;
public const int DEFAULT_TM_AGENT_MAX_TRANSPORT_POOL_SIZE = 250;
public const int DEFAULT_TM_AGENT_MAX_IDLE_CONNECTIONS = 250;
public const int DEFAULT_TM_AGENT_EVICTION_TIME_PERIOD = 5500;
public const int DEFAULT_TM_AGENT_MIN_IDLE_TIME_IN_POOL = 5000;
public const int DEFAULT_TM_AGENT_SECURE_MAX_TRANSPORT_POOL_SIZE = 250;
public const int DEFAULT_TM_AGENT_SECURE_MAX_IDLE_CONNECTIONS = 250;
public const int DEFAULT_TM_AGENT_SECURE_EVICTION_TIME_PERIOD = 5500;
public const int DEFAULT_TM_AGENT_SECURE_MIN_IDLE_TIME_IN_POOL = 5000;

public const boolean DEFAULT_TOKEN_REVOCATION_ENABLED = false;
public const string DEFAULT_REALTIME_JMS_CONNECTION_TOPIC = "tokenRevocation";
public const boolean DEFAULT_PERSISTENT_USE_DEFAULT = false;
public const string DEFAULT_PERSISTENT_TYPE = "default";
public const string DEFAULT_PERSISTENT_MESSAGE_HOSTNAME = "https://127.0.0.1:2379/v2/keys/jti/";
public const string DEFAULT_PERSISTENT_MESSAGE_USERNAME = "root";
public const string DEFAULT_PERSISTENT_MESSAGE_PASSWORD = "root";

public const boolean DEFAULT_HOSTNAME_VERIFICATION_ENABLED = true;
public const int DEFAULT_HTTP_CLIENTS_MAX_ACTIVE_CONNECTIONS = -1;
public const int DEFAULT_HTTP_CLIENTS_MAX_IDLE_CONNECTIONS = 100;
public const int DEFAULT_HTTP_CLIENTS_WAIT_TIME = 30000;
public const int DEFAULT_HTTP_CLIENTS_MAX_ACTIVE_STREAMS = 50;
public const int DEFAULT_HTTP_CLIENTS_RESPONSE_MAX_STATUS_LINE_LENGTH = 4096;
public const int DEFAULT_HTTP_CLIENTS_RESPONSE_MAX_HEADER_SIZE = 8192;
public const int DEFAULT_HTTP_CLIENTS_RESPONSE_MAX_ENTITY_BODY_SIZE = -1;

public const string DEFAULT_API_KEY_ISSUER = "https://localhost:9095/apikey";
public const string DEFAULT_API_KEY_ALIAS = "ballerina";
public const boolean DEFAULT_VALIDATE_APIS_ENABLED = false;

public const boolean DEFAULT_API_KEY_ISSUER_ENABLED = true;
public const int DEFAULT_API_KEY_VALIDITY_TIME = -1;

public const boolean DEFAULT_JWT_GENERATOR_ENABLED = false;
public const string DEFAULT_JWT_GENERATOR_DIALECT = "http://wso2.org/claims";
public const string DEFAULT_JWT_GENERATOR_SIGN_ALGO = "SHA256withRSA";
public const string DEFAULT_JWT_GENERATOR_CERTIFICATE_ALIAS = "ballerina";
public const string DEFAULT_JWT_GENERATOR_PRIVATE_KEY_ALIAS = "ballerina";
public const int DEFAULT_JWT_GENERATOR_TOKEN_EXPIRY = 900000;
public const string DEFAULT_JWT_GENERATOR_TOKEN_ISSUER = "wso2.org/products/am";
public const string DEFAULT_JWT_GENERATOR_IMPLEMENTATION = "org.wso2.micro.gateway.jwt.generator.MGWJWTGeneratorImpl";
public const boolean DEFAULT_JWT_GENERATOR_TOKEN_CACHE_ENABLED = true;
public const string DEFAULT_JWT_GENERATOR_CLAIM_RETRIEVAL_IMPLEMENTATION
                                                    = "org.wso2.micro.gateway.jwt.generator.DefaultMGWClaimRetriever";

public const string DEFAULT_APIM_CREDENTIALS_USERNAME = "admin";
public const string DEFAULT_APIM_CREDENTIALS_PASSWORD = "admin";

public const int DEFAULT_SERVER_TIMESTAMP_SKEW = -1;
public const string DEFAULT_SERVER_HEADER = "ballerina";
public const boolean DEFAULT_SERVER_HEADERCONF_USER_AGENT_PRESERVE_HEADER = false;
public const boolean DEFAULT_SERVER_HEADERCONF_SERVER_PRESERVE_HEADER = true;
public const boolean DEFAULT_SERVER_HEADERCONF_PRESERVE_HEADER = false;
