/*
 * Copyright (c) 2024, WSO2 LLC (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.enforcer.util;

import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;

import java.util.HashMap;
import java.util.Map;

/**
  * Utility functions used for internal API Key Authentication.
  */
public class InternalAPIKeyUtils {
  
         public static void addWSProtocolResponseHeaderIfRequired(RequestContext requestContext) {
        String secProtocolHeader =  requestContext.getHeaders().get(HttpConstants.WEBSOCKET_PROTOCOL_HEADER);
        if (secProtocolHeader != null) {
                String[] secProtocolHeaderValues = secProtocolHeader.split(",");
                if (secProtocolHeaderValues[0].equals(Constants.WS_API_KEY_IDENTIFIER) &&
                        secProtocolHeaderValues.length == 2) {
                    Map<String, String> responseHeadersToAddMap = requestContext.getResponseHeadersToAddMap();

                    if (responseHeadersToAddMap == null) {
                            responseHeadersToAddMap = new HashMap<>();
                    }
                    responseHeadersToAddMap.put(
                            HttpConstants.WEBSOCKET_PROTOCOL_HEADER, Constants.WS_API_KEY_IDENTIFIER);
                    requestContext.setResponseHeadersToAddMap(responseHeadersToAddMap);
                }
        }
    }
 }
