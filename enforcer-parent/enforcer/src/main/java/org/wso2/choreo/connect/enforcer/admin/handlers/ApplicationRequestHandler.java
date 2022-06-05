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
import org.apache.commons.lang.StringUtils;
import org.wso2.choreo.connect.enforcer.admin.AdminUtils;
import org.wso2.choreo.connect.enforcer.constants.AdminConstants;
import org.wso2.choreo.connect.enforcer.models.Application;
import org.wso2.choreo.connect.enforcer.models.ApplicationKeyMapping;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;
import org.wso2.choreo.connect.enforcer.models.admin.ApplicationInfo;
import org.wso2.choreo.connect.enforcer.models.admin.ApplicationInfoList;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler implementation for Application requests
 */
public class ApplicationRequestHandler extends RequestHandler {

    @Override
    public ResponsePayload handleRequest(String[] params, String requestType) throws Exception {

        List<Application> applicationList;
        String name = null;
        String organizationID = null;
        String uuid = null;
        String consumerKey = null;
        List<ApplicationInfo> applicationInfoList = new ArrayList<>();
        // If params is null, return all the applications
        if (params != null) {
            for (String param : params) {
                String[] parameterParts = param.split("=");
                switch (parameterParts[0]) {
                    case AdminConstants.Parameters.NAME:
                        name = parameterParts[1];
                        break;
                    case AdminConstants.Parameters.APP_UUID:
                        uuid = parameterParts[1];
                        break;
                    case AdminConstants.Parameters.ORGANIZATION_ID:
                        organizationID = parameterParts[1];
                        break;
                    case AdminConstants.Parameters.CONSUMER_KEY:
                        consumerKey = parameterParts[1];
                        break;
                    default:
                        break;
                }
            }
        }
        if (StringUtils.isEmpty(organizationID)) {
            String error = "{\"error\": true, \"message\":\"Organization id should not be empty.\"}";
            return AdminUtils.buildResponsePayload(error, HttpResponseStatus.BAD_REQUEST, true);
        }
        applicationList = super.dataStore.getMatchingApplications(name, organizationID, uuid);
        for (Application application : applicationList) {
            List<ApplicationKeyMapping> keyMappingList = dataStore.getMatchingKeyMapping(application.getUUID(),
                    consumerKey);
            for (ApplicationKeyMapping applicationKeyMapping : keyMappingList) {
                ApplicationInfo applicationInfo = AdminUtils.toApplicationInfo(application, applicationKeyMapping);
                applicationInfoList.add(applicationInfo);
            }
        }
        ApplicationInfoList applicationInfos = AdminUtils.toApplicationInfoList(applicationInfoList);
        return AdminUtils.buildResponsePayload(applicationInfos, HttpResponseStatus.OK, false);
    }
}
