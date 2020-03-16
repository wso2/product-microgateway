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

public const boolean DEFAULT_KM_CONF_SECURITY_BASIC_ENABLED = true;
public const string DEFAULT_USERNAME = "admin";
public const string DEFAULT_PASSWORD = "admin";

public const boolean DEFAULT_KM_CONF_SECURITY_OAUTH2_ENABLED = false;
public const string DEFAULT_KM_CONF_SECURITY_OAUTH2_CREDENTIAL_BEARER = "AUTH_HEADER_BEARER";
public const string DEFAULT_KM_CONF_SECURITY_OAUTH2 = "";

public const string DEFAULT_JWT_ISSUER = "https://localhost:9443/oauth2/token";
public const string DEFAULT_ISSUER_CLASSNAME = "DefaultJWTTransformer";
public const boolean DEFAULT_VALIDATE_SUBSCRIPTION = false;
public const string DEFAULT_AUDIENCE = "http://org.wso2.apimgt/gateway";
public const string DEFAULT_CERTIFICATE_ALIAS = "wso2apim310";
public const string DEFAULT_JWT_HEADER_NAME = "X-JWT-Assertion";

public const boolean DEFAULT_CACHING_ENABLED = true;
public const int DEFAULT_TOKEN_CACHE_EXPIRY = 900000;
public const int DEFAULT_TOKEN_CACHE_CAPACITY = 10000;
public const float DEFAULT_TOKEN_CACHE_EVICTION_FACTOR = 0.25;

//note, for analytics some configuration default values are not set. They are read directly from conf.
public const boolean DEFAULT_ANALYTICS_ENABLED = false;
public const int DEFAULT_UPLOADING_TIME_SPAN_IN_MILLIS = 600000;
public const int DEFAULT_INITIAL_DELAY_IN_MILLIS = 5000;
public const string DEFAULT_UPLOADING_EP = "https://localhost:9444/analytics/v1.0/usage/upload-file";
public const int DEFAULT_ROTATING_PERIOD_IN_MILLIS =  600000;
public const boolean DEFAULT_TASK_UPLOAD_FILES_ENABLED = true;
//constants for gRPC analytics 
public const string DEFAULT_GRPC_ENDPOINT_URL = "https://localhost:9806";
public const int DEFAULT_GRPC_RECONNECT_TIME_IN_MILLES = 6000;

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

public const boolean DEFAULT_REQUEST_VALIDATION_ENABLED = false;
public const boolean DEFAULT_RESPONSE_VALIDATION_ENABLED = false;

// Local throttling related constants
public const int DEFAULT_PROCESS_THREAD_POOL_CORE_SIZE = 200;
public const int DEFAULT_PROCESS_THREAD_POOL_MAXIMUM_SIZE = 1000;
public const int DEFAULT_PROCESS_THREAD_POOL_KEEP_ALIVE_TIME = 200;
public const int DEFAULT_THROTTLE_CLEANUP_FREQUENCY = 3600000;

public const boolean DEFAULT_GLOBAL_TM_EVENT_PUBLISH_ENABLED = false;
public const string DEFAULT_JMS_CONNECTION_INITIAL_CONTEXT_FACTORY = "wso2mbInitialContextFactory";
public const string DEFAULT_JMS_CONNECTION_PROVIDER_URL = "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'";
public const string DEFAULT_JMS_CONNECTION_USERNAME = "";
public const string DEFAULT_JMS_CONNECTION_PASSWORD = "";
public const string DEFAULT_THROTTLE_ENDPOINT_URL = "https://localhost:9443/endpoints";
public const string DEFAULT_THROTTLE_ENDPOINT_BASE64_HEADER = "admin:admin";

public const boolean DEFAULT_TOKEN_REVOCATION_ENABLED = false;
public const string DEFAULT_REALTIME_JMS_CONNECTION_TOPIC = "tokenRevocation";
public const boolean DEFAULT_PERSISTENT_USE_DEFAULT = true;
public const string DEFAULT_PERSISTENT_MESSAGE_HOSTNAME = "https://127.0.0.1:2379/v2/keys/jti/";
public const string DEFAULT_PERSISTENT_MESSAGE_USERNAME = "root";
public const string DEFAULT_PERSISTENT_MESSAGE_PASSWORD = "root";

public const boolean DEFAULT_HOSTNAME_VERIFICATION_ENABLED = true;

public const string DEFAULT_API_KEY_ISSUER = "https://localhost:9095/apikey";
public const string DEFAULT_API_KEY_ALIAS = "ballerina";
public const boolean DEFAULT_VALIDATE_APIS_ENABLED = false;

public const boolean DEFAULT_API_KEY_ISSUER_ENABLED = true;
public const int DEFAULT_API_KEY_VALIDITY_TIME = -1;
