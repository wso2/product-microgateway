/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.constants;

/**
 * MetadataConstants class contains all the Metadata Key Values added from enforcer.
 */
public class MetadataConstants {
    public static final String EXT_AUTH_METADATA_CONTEXT_KEY = "envoy.filters.http.ext_authz";

    public static final String WSO2_METADATA_PREFIX = "x-wso2-";

    public static final String API_PROJECT_ID_KEY = WSO2_METADATA_PREFIX + "project-id";
    public static final String API_COMPONENT_ID_KEY = WSO2_METADATA_PREFIX + "component-id";
    public static final String API_VERSION_ID_KEY = WSO2_METADATA_PREFIX + "version-id";
    public static final String INSIGHTS_ERROR_TYPE = WSO2_METADATA_PREFIX + "error-type";
    public static final String INSIGHTS_ERROR_CODE = WSO2_METADATA_PREFIX + "error-code";
    public static final String INSIGHTS_ERROR_MESSAGE = WSO2_METADATA_PREFIX + "error-message";

    public static final String API_ID_KEY = WSO2_METADATA_PREFIX + "api-id";
    public static final String API_CREATOR_KEY = WSO2_METADATA_PREFIX + "api-creator";
    public static final String API_NAME_KEY = WSO2_METADATA_PREFIX + "api-name";
    public static final String API_VERSION_KEY = WSO2_METADATA_PREFIX + "api-version";
    public static final String API_USER_NAME_KEY = WSO2_METADATA_PREFIX + "user-name";
    public static final String API_CONTEXT_KEY = WSO2_METADATA_PREFIX + "api-context";
    public static final String API_TYPE_KEY = WSO2_METADATA_PREFIX + "api-type";
    public static final String API_CREATOR_TENANT_DOMAIN_KEY = WSO2_METADATA_PREFIX + "api-creator-tenant-domain";
    public static final String API_ORGANIZATION_ID = WSO2_METADATA_PREFIX + "api-organization-id";
    public static final String API_ENVIRONMENT_ID = WSO2_METADATA_PREFIX + "api-environment-id";
    public static final String CHOREO_ENVIRONMENT_ID = WSO2_METADATA_PREFIX + "choreo-environment-id";

    public static final String APP_ID_KEY = WSO2_METADATA_PREFIX + "application-id";
    public static final String APP_UUID_KEY = WSO2_METADATA_PREFIX + "application-uuid";
    public static final String APP_KEY_TYPE_KEY = WSO2_METADATA_PREFIX + "application-key-type";
    public static final String APP_NAME_KEY = WSO2_METADATA_PREFIX + "application-name";
    public static final String APP_OWNER_KEY = WSO2_METADATA_PREFIX + "application-owner";

    public static final String CORRELATION_ID_KEY = WSO2_METADATA_PREFIX + "correlation-id";
    public static final String REGION_KEY = WSO2_METADATA_PREFIX + "region";

    public static final String API_RESOURCE_TEMPLATE_KEY = WSO2_METADATA_PREFIX + "api-resource-template";

    public static final String DESTINATION = WSO2_METADATA_PREFIX + "destination";

    public static final String ERROR_CODE_KEY = "ErrorCode";

    public static final String CHOREO_CONNECT_ENFORCER_REPLY = "choreo-connect-enforcer-reply";
    public static final String CHOREO_CONNECT_ENFORCER_REPLY_OK = "Ok";
    public static final String GATEWAY_URL = WSO2_METADATA_PREFIX + "x-original-gw-url";
    public static final String DEPLOYMENT_TYPE = WSO2_METADATA_PREFIX + "deployment-type";
}
