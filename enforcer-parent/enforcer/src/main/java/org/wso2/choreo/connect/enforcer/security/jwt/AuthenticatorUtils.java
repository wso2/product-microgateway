/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import net.minidev.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions shared between different authenticators.
 */
public class AuthenticatorUtils {

    public static void populateTierInfo(APIKeyValidationInfoDTO validationInfo, JWTClaimsSet payload,
                                        String subTier) {
        validationInfo.setTier(subTier);
        if (payload.getClaim(APIConstants.JwtTokenConstants.TIER_INFO) != null) {
            JSONObject tierInfo = (JSONObject) payload.getClaim(
                    APIConstants.JwtTokenConstants.TIER_INFO);
            if (tierInfo.get(subTier) != null) {
                JSONObject subTierInfo = (JSONObject) tierInfo.get(subTier);
                validationInfo.setStopOnQuotaReach((Boolean)
                        subTierInfo.get(APIConstants.JwtTokenConstants.STOP_ON_QUOTA_REACH));
            }
        }
    }

    public static void addWSProtocolResponseHeaderIfRequired(RequestContext requestContext, String protocolKeyword) {
        String secProtocolHeader = requestContext.getHeaders().get(HttpConstants.WEBSOCKET_PROTOCOL_HEADER);
        if (secProtocolHeader != null) {
            String[] secProtocolHeaderValues = secProtocolHeader.split(",");
            if (secProtocolHeaderValues[0].equals(protocolKeyword) &&
                    secProtocolHeaderValues.length == 2) {
                Map<String, String> responseHeadersToAddMap = requestContext
                        .getResponseHeadersToAddMap();

                if (responseHeadersToAddMap == null) {
                    responseHeadersToAddMap = new HashMap<>();
                }
                responseHeadersToAddMap.put(
                        HttpConstants.WEBSOCKET_PROTOCOL_HEADER,
                        protocolKeyword);
                requestContext.setResponseHeadersToAddMap(responseHeadersToAddMap);
            }
        }
    }
}
