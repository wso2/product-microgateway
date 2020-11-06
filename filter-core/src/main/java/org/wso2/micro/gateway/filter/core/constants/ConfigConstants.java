/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * This class holds the constant keys related to the Microgateway Configurations which will be read from the config file
 * or dynamically configured via the control plane.
 */
public class ConfigConstants {
    // JWT Token Config
    public static final String JWT_TOKEN_CONFIG = "jwtTokenConfig";
    public static final String JWT_TOKEN_ISSUER = "issuer";
    public static final String JWT_TOKEN_CERTIFICATE_ALIAS = "certificateAlias";
    public static final String JWT_TOKEN_JWKS_URL = "jwksURL";
    public static final String JWT_TOKEN_VALIDATE_SUBSCRIPTIONS = "validateSubscription";
    public static final String JWT_TOKEN_CONSUMER_KEY_CLAIM = "consumerKeyClaim";
    public static final String JWT_TOKEN_CLAIM_MAPPER_CLASS_NAME = "claimMapperClassName";
    public static final String JWT_TOKEN_ENABLE_REMOTE_USER_CLAIM_RETRIEVAL = "remoteUserClaimRetrievalEnabled";
    public static final String JWT_TOKEN_CLAIMS = "claims";

    // Event hub configuration
    public static final String EVENT_HUB_ENABLE = "apim.eventHub.enable";
    public static final String EVENT_HUB_SERVICE_URL = "apim.eventHub.serviceUrl";
    public static final String EVENT_HUB_USERNAME = "apim.eventHub.username";
    public static final String EVENT_HUB_PASSWORD = "apim.eventHub.password";
    public static final String EVENT_HUB_EVENT_LISTENING_ENDPOINT = "apim.eventHub.eventListeningEndpoints";

    //KeyStore and Trust Store configuration
    public static final String MGW_KEY_STORE_LOCATION = "keystore.location";
    public static final String MGW_KEY_STORE_TYPE = "keystore.type";
    public static final String MGW_KEY_STORE_PASSWORD = "keystore.password";
    public static final String MGW_TRUST_STORE_LOCATION = "truststore.location";
    public static final String MGW_TRUST_STORE_TYPE = "truststore.type";
    public static final String MGW_TRUST_STORE_PASSWORD = "truststore.password";
}
