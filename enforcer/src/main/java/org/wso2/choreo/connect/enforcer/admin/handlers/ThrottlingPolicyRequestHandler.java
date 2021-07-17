/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.admin.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import org.wso2.choreo.connect.enforcer.admin.AdminUtils;
import org.wso2.choreo.connect.enforcer.constants.AdminConstants;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicy;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicyList;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicy;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicyList;

import java.util.List;

/**
 * Request Handler implementation for throttling policies (Application and subscription)
 */
public class ThrottlingPolicyRequestHandler extends RequestHandler {
    @Override
    public ResponsePayload handleRequest(String[] params, String requestType) throws Exception {
        String policyName = null;
        ResponsePayload responsePayload;
        if (params != null) {
            for (String param : params) {
                String[] keyVal = param.split("=");
                if (AdminConstants.Parameters.NAME.equals(keyVal[0])) {
                    policyName = keyVal[1];
                }
            }
        }
        if (AdminConstants.APPLICATION_THROTTLING_POLICY_TYPE.equals(requestType)) {
            responsePayload = getApplicationPolicies(policyName);
        } else {
            responsePayload = getSubscriptionPolicies(policyName);
        }
        return responsePayload;
    }

    private ResponsePayload getApplicationPolicies(String policyName) throws JsonProcessingException {
        List<ApplicationPolicy> applicationPolicies = super.dataStore.getMatchingApplicationPolicies(policyName);
        ApplicationPolicyList applicationPolicyList = AdminUtils.toApplicationPolicyList(applicationPolicies);
        return AdminUtils.buildResponsePayload(applicationPolicyList, HttpResponseStatus.OK, false);
    }

    private ResponsePayload getSubscriptionPolicies(String policyName) throws JsonProcessingException {
        List<SubscriptionPolicy> subscriptionPolicies = super.dataStore.getMatchingSubscriptionPolicies(policyName);
        SubscriptionPolicyList subscriptionPolicyList = AdminUtils.toSubscriptionPolicyList(subscriptionPolicies);
        return AdminUtils.buildResponsePayload(subscriptionPolicyList, HttpResponseStatus.OK, false);

    }
}
