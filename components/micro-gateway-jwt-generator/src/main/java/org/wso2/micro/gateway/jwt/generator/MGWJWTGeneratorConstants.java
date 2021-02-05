/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.jwt.generator;

/**
 * Constants for Microgateway JWT generator module
 */
public class MGWJWTGeneratorConstants {
    public static final String ISSUER_CLAIM = "iss";
    public static final String AUDIENCE_CLAIM = "aud";
    public static final String NBF_CLAIM = "nbf";
    public static final String JTI_CLAIM = "jti";
    public static final String IAT_CLAIM = "iat";
    public static final String EXP_CLAIM = "exp";
    public static final String SUB_CLAIM = "sub";
    public static final String SCOPES_CLAIM = "scopes";
    public static final String APPLICATION_CLAIM = "application";
    public static final String APPLICATION_ID_CLAIM = "id";
    public static final String APPLICATION_UUID_CLAIM = "uuid";
    public static final String APPLICATION_OWNER_CLAIM = "owner";
    public static final String APPLICATION_NAME_CLAIM = "name";
    public static final String APPLICATION_TIER_CLAIM = "tier";
    public static final String API_NAME_CLAIM = "apiName";
    public static final String SUBSCRIBER_TENANT_DOMAIN_CLAIM = "subscriberTenantDomain";
    public static final String API_CONTEXT_CLAIM = "apiContext";
    public static final String API_VERSION_CLAIM = "apiVersion";
    public static final String API_TIER_CLAIM = "apiTier";
    public static final String KEY_TYPE_CLAIM = "keytype";
    public static final String TIER_INFO_CLAIM = "tierInfo";
    public static final String SUBSCRIBED_APIS_CLAIM = "subscribedAPIs";
    public static final String CUSTOM_CLAIMS = "customClaims";
    public static final String KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String AUTH_APPLICATION_USER_LEVEL_TOKEN = "Application_User";
}
