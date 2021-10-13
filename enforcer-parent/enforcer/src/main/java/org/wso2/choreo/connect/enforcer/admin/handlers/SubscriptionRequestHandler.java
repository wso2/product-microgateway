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

import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import org.wso2.choreo.connect.enforcer.admin.AdminUtils;
import org.wso2.choreo.connect.enforcer.constants.AdminConstants;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;
import org.wso2.choreo.connect.enforcer.models.Subscription;
import org.wso2.choreo.connect.enforcer.models.SubscriptionList;

import java.util.List;

/**
 * Request Handler implementation for Subscriptions
 */
public class SubscriptionRequestHandler extends RequestHandler {
    @Override
    public ResponsePayload handleRequest(String[] params, String requestType) throws Exception {
        String apiUUID = null;
        String appUUID = null;
        String state = null;

        if (params != null) {
            for (String param : params) {
                String[] paramKeyVal = param.split("=");
                switch (paramKeyVal[0]) {
                    case AdminConstants.Parameters.API_UUID:
                        apiUUID = paramKeyVal[1];
                        break;
                    case AdminConstants.Parameters.APP_UUID:
                        appUUID = paramKeyVal[1];
                        break;
                    case AdminConstants.Parameters.STATE:
                        state = paramKeyVal[1];
                        break;
                    default:
                        break;
                }
            }
        }
        List<Subscription> subscriptions = super.dataStore.getMatchingSubscriptions(appUUID, apiUUID, state);
        SubscriptionList subscriptionList = AdminUtils.toSubscriptionsList(subscriptions);
        return AdminUtils.buildResponsePayload(subscriptionList, HttpResponseStatus.OK, false);
    }
}
