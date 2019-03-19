/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.constants;

public class RESTServiceConstants {

    public static final String ENDPOINT_TYPE = "endpoint_type";
    public static final String HTTP = "http";
    public static final String FAILOVER = "failover";
    public static final String PRODUCTION_ENDPOINTS = "production_endpoints";
    public static final String SANDBOX_ENDPOINTS = "sandbox_endpoints";
    public static final String URL = "url";
    public static final String PRODUCTION_FAILOVERS = "production_failovers";
    public static final String SANDBOX_FAILOVERS = "sandbox_failovers";
    public static final String LOAD_BALANCE = "load_balance";
    public static final String UNLIMITED = "Unlimited";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String GET = "GET";
    public static final String APIS_GET_URI =
            "apis?query=label:" + GatewayCliConstants.LABEL_PLACEHOLDER + "%20status:PUBLISHED&expand=true&limit=500";
    public static final String API_GET_BY_NAME_VERSION_URI =
            "apis?query=name:" + GatewayCliConstants.API_NAME_PLACEHOLDER + "%20version:"
                    + GatewayCliConstants.VERSION_PLACEHOLDER + "%20status:PUBLISHED&expand=true";

    public static final String CONFIG_REST_VERSION = "v0.14";
    public static final String CONFIG_PUBLISHER_ENDPOINT = "{baseURL}/api/am/publisher/{restVersion}";
    public static final String CONFIG_ADMIN_ENDPOINT = "{baseURL}/api/am/admin/{restVersion}";
    public static final String CONFIG_REGISTRATION_ENDPOINT = "{baseURL}/client-registration/{restVersion}/register";
    public static final String CONFIG_TOKEN_ENDPOINT = "{baseURL}/oauth2/token";
    public static final String REST_VERSION_TAG="{restVersion}";
    public static final String BASE_URL_TAG="{baseURL}";

    public static final String DEFAULT_HOST = "https://localhost:9443";
    public static final String DEFAULT_TRUSTSTORE_PATH = "lib/platform/bre/security/ballerinaTruststore.p12";
    public static final String DEFAULT_TRUSTSTORE_PASS = "ballerina";
    public static final String DEFAULT_KEYSTORE_PATH = "lib/platform/bre/security/ballerinaKeystore.p12";
    public static final String DEFAULT_KEYSTORE_PASS = "ballerina";

    public static final String CERTIFICATE_ALIAS = "Alias";
    public static final String CERTIFICATE_TIER = "Tier";

}
