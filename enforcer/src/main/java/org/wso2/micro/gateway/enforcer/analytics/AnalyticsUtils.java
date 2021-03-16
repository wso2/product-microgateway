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

package org.wso2.micro.gateway.enforcer.analytics;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.constants.AnalyticsConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

/**
 * Common Utility functions
 */
public class AnalyticsUtils {

    public static String getAPIId(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        if (authContext == null || StringUtils.isEmpty(authContext.getApiUUID())) {
            return generateHash(requestContext.getMathedAPI().getAPIConfig().getName(),
                    requestContext.getMathedAPI().getAPIConfig().getVersion());
        }
        return authContext.getApiUUID();
    }

    private static String generateHash(String apiName, String apiVersion) {
        return DigestUtils.md5Hex(apiName + ":" + apiVersion);
    }

    public static String setDefaultIfNull(String value) {
        return value == null ? AnalyticsConstants.DEFAULT_FOR_UNASSIGNED : value;
    }

    /**
     * Extracts Authentication Context from the request Context. If Authentication Context is not available,
     * new Authentication Context object will be created with authenticated property is set to false.
     *
     * @param requestContext {@code RequestContext} object
     * @return {@code AuthenticationContext} object
     */
    public static AuthenticationContext getAuthenticationContext(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        // When authentication failure happens authContext remains null
        if (authContext == null) {
            authContext = new AuthenticationContext();
            authContext.setAuthenticated(false);
        }
        return authContext;
    }
}
