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
package org.wso2.choreo.connect.enforcer.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.models.Application;
import org.wso2.choreo.connect.enforcer.models.ApplicationKeyMapping;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicy;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicyList;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;
import org.wso2.choreo.connect.enforcer.models.RevokedToken;
import org.wso2.choreo.connect.enforcer.models.RevokedTokenList;
import org.wso2.choreo.connect.enforcer.models.Subscription;
import org.wso2.choreo.connect.enforcer.models.SubscriptionList;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicy;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicyList;
import org.wso2.choreo.connect.enforcer.models.admin.APIInfo;
import org.wso2.choreo.connect.enforcer.models.admin.ApplicationInfo;
import org.wso2.choreo.connect.enforcer.models.admin.ApplicationInfoList;
import org.wso2.choreo.connect.enforcer.models.admin.BasicAPIInfo;
import org.wso2.choreo.connect.enforcer.models.admin.SubscriptionInfo;

import java.util.List;

/**
 * Utility class for internal admin functions
 */
public class AdminUtils {

    public static APIInfo toAPIInfo(API api, List<SubscriptionInfo> subscriptionInfoList) {
        APIInfo apiInfo = new APIInfo();
        apiInfo.setSubscriptions(subscriptionInfoList);
        apiInfo.setApiUUID(api.getAPIConfig().getUuid());
        apiInfo.setContext(api.getAPIConfig().getBasePath());
        apiInfo.setName(api.getAPIConfig().getName());
        apiInfo.setLcState(api.getAPIConfig().getApiLifeCycleState());
        apiInfo.setTier(api.getAPIConfig().getTier());
        apiInfo.setVersion(api.getAPIConfig().getVersion());
        apiInfo.setProvider(api.getAPIConfig().getApiProvider());
        apiInfo.setApiType(api.getAPIConfig().getApiType());
        return apiInfo;
    }

    public static BasicAPIInfo toBasicAPIInfo(API api, boolean isDefaultVersion) {
        BasicAPIInfo apiInfo = new BasicAPIInfo();
        apiInfo.setApiUUID(api.getAPIConfig().getUuid());
        apiInfo.setContext(api.getAPIConfig().getBasePath());
        apiInfo.setName(api.getAPIConfig().getName());
        apiInfo.setLcState(api.getAPIConfig().getApiLifeCycleState());
        apiInfo.setDefaultVersion(isDefaultVersion);
        apiInfo.setApiType(api.getAPIConfig().getApiType());
        apiInfo.setVersion(api.getAPIConfig().getVersion());
        apiInfo.setProvider(api.getAPIConfig().getApiProvider());
        apiInfo.setPolicy(api.getAPIConfig().getTier());
        return apiInfo;
    }

    public static ApplicationInfo toApplicationInfo(Application application,
                                                    ApplicationKeyMapping applicationKeyMapping) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.setConsumerKey(applicationKeyMapping.getConsumerKey());
        applicationInfo.setUuid(application.getUUID());
        applicationInfo.setName(application.getName());
        applicationInfo.setPolicy(application.getPolicy());
        applicationInfo.setId(application.getId());
        applicationInfo.setSubId(application.getSubId());
        applicationInfo.setSubName(application.getSubName());
        applicationInfo.setTenantDomain(application.getTenantDomain());
        return applicationInfo;
    }

    public static SubscriptionInfo toSubscriptionInfo(Subscription subscription,
                                                      ApplicationInfo applicationInfo) {
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setApplicationInfo(applicationInfo);
        subscriptionInfo.setApiUUID(subscription.getApiUUID());
        subscriptionInfo.setAppUUID(subscription.getAppUUID());
        subscriptionInfo.setPolicyId(subscription.getPolicyId());
        subscriptionInfo.setSubscriptionId(subscription.getSubscriptionId());
        subscriptionInfo.setSubscriptionState(subscription.getSubscriptionState());
        return subscriptionInfo;
    }

    public static ApplicationInfoList toApplicationInfoList(List<ApplicationInfo> applications) {
        ApplicationInfoList applicationInfoList = new ApplicationInfoList();
        applicationInfoList.setCount(applications.size());
        applicationInfoList.setList(applications);
        return applicationInfoList;
    }

    public static SubscriptionList toSubscriptionsList(List<Subscription> subscriptions) {
        SubscriptionList subscriptionList = new SubscriptionList();
        subscriptionList.setCount(subscriptions.size());
        subscriptionList.setList(subscriptions);
        return subscriptionList;
    }

    public static ApplicationPolicyList toApplicationPolicyList(List<ApplicationPolicy> applicationPolicies) {
        ApplicationPolicyList applicationPolicyList = new ApplicationPolicyList();
        applicationPolicyList.setCount(applicationPolicies.size());
        applicationPolicyList.setList(applicationPolicies);
        return applicationPolicyList;
    }

    public static SubscriptionPolicyList toSubscriptionPolicyList(List<SubscriptionPolicy> subscriptionPolicies) {
        SubscriptionPolicyList subscriptionPolicyList = new SubscriptionPolicyList();
        subscriptionPolicyList.setCount(subscriptionPolicies.size());
        subscriptionPolicyList.setList(subscriptionPolicies);
        return subscriptionPolicyList;
    }

    public static ResponsePayload buildResponsePayload(Object dataModel, HttpResponseStatus status, boolean isError)
            throws JsonProcessingException {

        String jsonPayload;
        ObjectMapper objectMapper = new ObjectMapper();
        if (!(dataModel instanceof String)) {
            jsonPayload = objectMapper.writeValueAsString(dataModel);
        } else {
            jsonPayload = (String) dataModel;
        }
        ResponsePayload responsePayload = new ResponsePayload();
        responsePayload.setContent(jsonPayload);
        responsePayload.setError(isError);
        responsePayload.setStatus(status);
        return responsePayload;
    }

    public static RevokedTokenList toRevokedTokenList(List<RevokedToken> revokedTokens) {
        RevokedTokenList revokedTokenList = new RevokedTokenList();
        revokedTokenList.setCount(revokedTokens.size());
        revokedTokenList.setTokens(revokedTokens);
        return revokedTokenList;
    }
}
