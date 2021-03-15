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

package org.wso2.micro.gateway.enforcer.constants;

/**
 * This class holds the constant keys related to the Microgateway.
 */
public class Constants {
    public static final String CONFIG_TYPE_URL = "type.googleapis.com/wso2.discovery.config.enforcer.Config";
    public static final String API_TYPE_URL = "type.googleapis.com/wso2.discovery.api.Api";
    public static final String SUBSCRIPTION_LIST_TYPE_URL =
            "type.googleapis.com/wso2.discovery.subscription.SubscriptionList";
    public static final String API_LIST_TYPE_URL = "type.googleapis.com/wso2.discovery.subscription.APIList";
    public static final String APPLICATION_LIST_TYPE_URL =
            "type.googleapis.com/wso2.discovery.subscription.ApplicationList";
    public static final String APPLICATION_POLICY_LIST_TYPE_URL =
            "type.googleapis.com/wso2.discovery.subscription.ApplicationPolicyList";
    public static final String SUBSCRIPTION_POLICY_LIST_TYPE_URL =
            "type.googleapis.com/wso2.discovery.subscription.SubscriptionPolicyList";
    public static final String APPLICATION_KEY_MAPPING_LIST_TYPE_URL =
            "type.googleapis.com/wso2.discovery.subscription.ApplicationKeyMappingList";
    public static final String KEY_MANAGER_TYPE_URL =
            "type.googleapis.com/wso2.discovery.keymgt.KeyManagerConfig";
    public static final String REVOKED_TOKEN_TYPE_URL =
            "type.googleapis.com/wso2.discovery.keymgt.RevokedToken";
    public static final int MAX_XDS_RETRIES = 3;

    // Config constants
    public static final String EVENT_HUB_EVENT_LISTENING_ENDPOINT = "eventListeningEndpoints";

    public static final String BEGINING_OF_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n";
    public static final String END_OF_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    public static final String RSA = "RSA";

    public static final String LOADBALANCE = "loadbalance";
    public static final String FAILOVER = "failover";
    public static final String TM_BINARY_LOADBALANCE_SEPARATOR = ",";
    public static final String TM_BINARY_FAILOVER_SEPARATOR = "|";
    public static final String UNKNOWN_VALUE = "__unknown__";

    public static final String OBJECT_THIS_NOTATION = "this$";
    public static final String ENV_PREFIX = "$env{";
    public static final String START_BRACKET = "{";
    public static final String END_BRACKET = "}";
}
