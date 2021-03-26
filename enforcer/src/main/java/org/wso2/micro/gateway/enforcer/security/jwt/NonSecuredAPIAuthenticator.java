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

package org.wso2.micro.gateway.enforcer.security.jwt;

import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.exception.APISecurityException;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;
import org.wso2.micro.gateway.enforcer.security.Authenticator;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

/**
 * Implements the authenticator interface to authenticate non-secured APIs.
 */

public class NonSecuredAPIAuthenticator implements Authenticator {

    @Override public boolean canAuthenticate(RequestContext requestContext) {
        // Retrieve the authType value, if it is "None", they you can proceed directly with the authentication.
        if (isDisableSecurity(requestContext.getMatchedResourcePath())) {
            return true;
        }
        return false;
    }

    @Override public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        return FilterUtils.generateAuthenticationContext(requestContext);
    }

    @Override public int getPriority() {
        return -20;
    }

    /**
     * This method retrieve the proper auth type for the given request context.
     * AuthType can be deduced from API level and resource level. If both are defined,
     * resource level gets the precedence.
     * If nothing declared, it will return the authType as "default".
     * @param matchingResource matching resource related configurations
     * @return value of the authType from API definition. If not present "default"
     */
    private boolean isDisableSecurity(ResourceConfig matchingResource) {

        return matchingResource.isDisableSecurity();
    }
}
