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

import org.wso2.micro.gateway.interceptor.Caller;
import org.wso2.micro.gateway.interceptor.ConfigUtils;
import org.wso2.micro.gateway.interceptor.Interceptor;
import org.wso2.micro.gateway.interceptor.Request;
import org.wso2.micro.gateway.interceptor.Response;

import java.util.List;
import java.util.Map;

/**
 * Implements sample interceptor for the integration test cases used to read configurations.
 */
public class ConfigInterceptor implements Interceptor {

    private  String responseString = "";

    @Override
    public boolean interceptRequest(Caller caller, Request request) {
        String queryParam = request.getQueryParamValue("config");
        queryParam = (queryParam != null) ? queryParam : "";
        switch (queryParam) {
        case "int" :
            int port = ConfigUtils.getAsInt("listenerConfig.httpPort", 9080);
            responseString = Integer.toString(port);
            break;
        case "float":
            float portSecured = ConfigUtils.getAsFloat("listenerConfig.httpsPort", 9080);
            responseString = Float.toString(portSecured);
            break;
        case "boolean":
            boolean analyticsEnabled = ConfigUtils.getAsBoolean("analytics.enable", true);
            responseString = Boolean.toString(analyticsEnabled);
            break;
        case "array":
            List<Map<String, String>> tokenConfig = ConfigUtils.getAsList("jwtTokenConfig");
            responseString = tokenConfig.get(0).get("issuer");
            break;
        case "map":
            Map<String, String> analytics = ConfigUtils.getAsMap("analytics");
            responseString = analytics.get("enable");
            break;
        default:
            responseString = ConfigUtils.getAsString("analytics.uploadingEndpoint", "");
        }
        respondFromRequest(caller);
        return false;
    }

    @Override
    public boolean interceptResponse(Caller caller, Response response) {
        return true;
    }

    private void respondFromRequest(Caller caller) {
        Response response = new Response();
        response.setTextPayload(responseString);
        caller.respond(response);
    }
}
