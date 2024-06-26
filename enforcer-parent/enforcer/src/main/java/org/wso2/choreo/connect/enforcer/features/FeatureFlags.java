/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.features;

import org.wso2.choreo.connect.enforcer.constants.APIConstants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Feature flags for Choreo Connect.
 */
public class FeatureFlags {
    private static final Set<String> CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ORG;
    private static final boolean ENABLE_CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ALL_ORG;
    private static final boolean ENFORCER_GRPC_CLIENT_KEEPALIVE_ENABLED;

    static {
        final String orgEnvVar = System.getenv().getOrDefault("CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ORG", "");
        final String keepAliveEnvVar = System.getenv().getOrDefault("ENFORCER_GRPC_KEEPALIVE_ENABLED", "");
        ENFORCER_GRPC_CLIENT_KEEPALIVE_ENABLED = keepAliveEnvVar.trim().equals("true");
        CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ORG = new HashSet<>(Arrays.asList(orgEnvVar.split(",")));
        ENABLE_CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ALL_ORG = orgEnvVar.equals("*");
    }

    public static boolean isCustomSubscriptionPolicyHandlingEnabled(String orgId) {
        return ENABLE_CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ALL_ORG
                || CUSTOM_SUBSCRIPTION_POLICY_HANDLING_ORG.contains(orgId);
    }

    public static boolean isEnforcerGrpcClientKeepaliveEnabled() {
        return ENFORCER_GRPC_CLIENT_KEEPALIVE_ENABLED;
    }

    public static String getCustomSubscriptionPolicyHandlingOrg(String orgId) {
        if (isCustomSubscriptionPolicyHandlingEnabled(orgId)) {
            return orgId;
        }
        return APIConstants.SUPER_TENANT_DOMAIN_NAME;
    }
}
