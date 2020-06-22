/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.tests.interceptor;

import org.wso2.micro.gateway.core.utils.CommonUtils;
import org.wso2.micro.gateway.interceptor.Caller;
import org.wso2.micro.gateway.interceptor.Interceptor;
import org.wso2.micro.gateway.interceptor.Request;
import org.wso2.micro.gateway.interceptor.Response;
import org.wso2.micro.gateway.interceptor.Utils;

/**
 * Implements sample interceptor for the integration test cases to read the openAPIs expose via the gateway.
 */
public class OpenAPIReadInterceptor implements Interceptor {
    @Override
    public boolean interceptRequest(Caller caller, Request request) {
        String apiName = Utils.getInvocationContextAttributes().get("api_name").toString();
        String apiVersion = Utils.getInvocationContextAttributes().get("api_version").toString();
        String openAPIString = CommonUtils.getOpenAPI(apiName, apiVersion);
        respondFromRequest(caller, openAPIString);
        return false;
    }

    @Override
    public boolean interceptResponse(Caller caller, Response response) {
        return true;
    }

    private void respondFromRequest(Caller caller, String responseString) {
        Response response = new Response();
        response.setTextPayload(responseString);
        caller.respond(response);
    }

}
