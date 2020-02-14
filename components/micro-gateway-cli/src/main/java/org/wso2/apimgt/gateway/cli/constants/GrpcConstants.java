/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

/**
 * Constants Related to Grpc Implementation.
 */
public class GrpcConstants {
    //todo: change underscore to the hyphen
    public static final String PRODUCTION_ENDPOINTS = "x_wso2_production_endpoints";
    public static final String SANDBOX_ENDPOINTS = "x_wso2_sandbox_endpoints";
    public static final String THROTTLING_TIER = "x_wso2_throttling_tier";
    public static final String SECURITY = "x_wso2_security";
    public static final String SCOPES = "x_wso2_scopes";
    public static final String METHOD_THROTTLING_TIER = "x_wso2_method_throttling_tier";
    public static final String METHOD_SCOPES = "x_wso2_method_scopes";
    public static final String URL_SEPARATOR = "/";
}
