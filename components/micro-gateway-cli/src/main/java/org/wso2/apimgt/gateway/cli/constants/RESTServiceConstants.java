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
            "apis?query=name:" + GatewayCliConstants.API_NAME_PLACEHOLDER + "%2Cversion:"
                    + GatewayCliConstants.VERSION_PLACEHOLDER ;
    public static final String PUB_RESOURCE_PATH = "/api/am/publisher/v1.0/";
    public static final String ADMIN_RESOURCE_PATH = "/api/am/admin/v1.0/";
    public static final String DCR_RESOURCE_PATH = "/api/identity/oauth2/dcr/v1.0/register";
    public static final String TOKEN_PATH = "/api/auth/oauth2/v1.0/token";

    public static final String DEFAULT_HOST = "https://localhost:9443/";
    public static final String DEFAULT_TRUSTSTORE_PATH = "lib/platform/bre/security/ballerinaTruststore.p12";
    public static final String DEFAULT_TRUSTSTORE_PASS = "ballerina";
}
