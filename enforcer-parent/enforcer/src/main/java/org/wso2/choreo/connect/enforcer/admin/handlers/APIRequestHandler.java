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
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.constants.AdminConstants;
import org.wso2.choreo.connect.enforcer.models.Application;
import org.wso2.choreo.connect.enforcer.models.ApplicationKeyMapping;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;
import org.wso2.choreo.connect.enforcer.models.Subscription;
import org.wso2.choreo.connect.enforcer.models.admin.APIInfo;
import org.wso2.choreo.connect.enforcer.models.admin.APIList;
import org.wso2.choreo.connect.enforcer.models.admin.ApplicationInfo;
import org.wso2.choreo.connect.enforcer.models.admin.BasicAPIInfo;
import org.wso2.choreo.connect.enforcer.models.admin.SubscriptionInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler implementation for API requests.
 */
public class APIRequestHandler extends RequestHandler {

    @Override
    public ResponsePayload handleRequest(String[] params, String requestType) throws Exception {

        if (AdminConstants.API_TYPE.equals(requestType)) {
            return getAPIs(params);
        }
        return getAPIInfo(params);
    }

    /**
     * Method to get apis which match the given parameters
     *
     * @param params Array of parameters
     * @return ResponsePayload with APIs as APIList object.
     */
    private ResponsePayload getAPIs(String[] params) throws JsonProcessingException {
        List<API> apis;
        String name = null;
        String version = null;
        String context = null;
        String uuid = null;
        if (params != null) {
            for (String param : params) {
                String[] keyVal = param.split("=");
                switch (keyVal[0]) {
                    case AdminConstants.Parameters.NAME:
                        name = keyVal[1];
                        break;
                    case AdminConstants.Parameters.CONTEXT:
                        context = keyVal[1];
                        break;
                    case AdminConstants.Parameters.VERSION:
                        version = keyVal[1];
                        break;
                    case AdminConstants.Parameters.API_UUID:
                        uuid = keyVal[1];
                        break;
                    default:
                        break;
                }
            }
        }
        apis = APIFactory.getInstance().getMatchingAPIs(name, context, version, uuid);
        APIList apiList = new APIList();
        apiList.setCount(apis.size());
        List<BasicAPIInfo> modelAPIs = new ArrayList<>(apis.size());
        for (API api : apis) {
            // IsDefaultVersion is not populated as it is not required for the enforcer.
            modelAPIs.add(AdminUtils.toBasicAPIInfo(api, false));
        }
        apiList.setList(modelAPIs);
        return AdminUtils.buildResponsePayload(apiList, HttpResponseStatus.OK, false);

    }

    /**
     * Method to get the api information (along with subscription,
     * application and key mapping) for a provided api context and version
     *
     * @param params API Context and API version
     * @return APIInfo in as a ResponsePayload object.
     * @throws JsonProcessingException If the object cannot be converted into a JSON
     */
    private ResponsePayload getAPIInfo(String[] params) throws JsonProcessingException {
        APIInfo apiInfo;
        String context = null;
        String version = null;
        if (params == null || params.length < 2) {
            // Bad request
            String responseMessage = "{" +
                    "\"error\": true, " +
                    "\"message\": \"Query parameters context and version could not be empty\"}";
            return AdminUtils.buildResponsePayload(responseMessage, HttpResponseStatus.BAD_REQUEST, true);
        }
        for (String param : params) {
            if (param.contains(AdminConstants.Parameters.CONTEXT)) {
                context = param.split("=")[1];
            }
            if (param.contains(AdminConstants.Parameters.VERSION)) {
                version = param.split("=")[1];
            }
        }
        List<API> apis = APIFactory.getInstance().getMatchingAPIs(null, context, version, null);
        String apiUUID;
        if (apis.size() > 0) {
            apiUUID = apis.get(0).getAPIConfig().getUuid();
            List<Subscription> matchingSubscriptions = dataStore.
                    getMatchingSubscriptions(null, apiUUID, null);
            List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
            // For each subscription, build the Subscription info with application and key mapping.
            for (Subscription subscription : matchingSubscriptions) {
                Application matchingApplication = dataStore.getApplicationById(subscription.getAppUUID());
                List<ApplicationKeyMapping> applicationKeyMapping;
                ApplicationInfo applicationInfo = null;
                if (matchingApplication != null) {
                    applicationKeyMapping = dataStore.getMatchingKeyMapping(matchingApplication.getUUID(), null);
                    if (applicationKeyMapping.size() > 0) {
                        // should be only one entry
                        applicationInfo = AdminUtils.toApplicationInfo(matchingApplication,
                                applicationKeyMapping.get(0));
                    }
                }
                SubscriptionInfo subscriptionInfo = AdminUtils.toSubscriptionInfo(subscription, applicationInfo);
                subscriptionInfoList.add(subscriptionInfo);
            }

            apiInfo = AdminUtils.toAPIInfo(apis.get(0), subscriptionInfoList);
            return AdminUtils.buildResponsePayload(apiInfo, HttpResponseStatus.OK, false);
        } else {
            // No api found for the provided search parameters...
            // return empty response.
            String message = "{\"error\": true,\"message\":\"No API information found for context "
                    + context + " and version " + version + "\"}";
            return AdminUtils.buildResponsePayload(message, HttpResponseStatus.NOT_FOUND, true);
        }
    }
}
