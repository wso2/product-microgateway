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

package org.wso2.choreo.connect.enforcer.util;

import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.SecurityInfo;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;

import java.util.Base64;

/**
 * Util methods related to backend endpoint security.
 */
public class EndpointSecurityUtils {

    /**
     * Adds the backend endpoint security header to the given requestContext.
     *
     * @param requestContext requestContext instance to add the backend endpoint security header
     */
    public static void addEndpointSecurity(RequestContext requestContext) {
        SecurityInfo securityInfo = null;
        String keyType = "";
        if (requestContext.getAuthenticationContext() != null) {
            keyType = requestContext.getAuthenticationContext().getKeyType();
        }
        if (requestContext.getMatchedAPI().getEndpointSecurity() != null) {
            if (APIConstants.API_KEY_TYPE_PRODUCTION.equals(keyType)) {
                securityInfo = requestContext.getMatchedAPI().getEndpointSecurity().
                        getProductionSecurityInfo();
            } else {
                securityInfo = requestContext.getMatchedAPI().getEndpointSecurity().
                        getSandBoxSecurityInfo();
            }
        }
        if (securityInfo != null && securityInfo.isEnabled() &&
                APIConstants.AUTHORIZATION_HEADER_BASIC.
                        equalsIgnoreCase(securityInfo.getSecurityType())) {
            requestContext.getRemoveHeaders().remove(APIConstants.AUTHORIZATION_HEADER_DEFAULT
                    .toLowerCase());
            requestContext.addOrModifyHeaders(APIConstants.AUTHORIZATION_HEADER_DEFAULT,
                    APIConstants.AUTHORIZATION_HEADER_BASIC + ' ' +
                            Base64.getEncoder().encodeToString((securityInfo.getUsername() +
                                    ':' + String.valueOf(securityInfo.getPassword())).getBytes()));
        }
    }
}
