/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.filter.core.auth;

import org.wso2.micro.gateway.filter.core.Filter;
import org.wso2.micro.gateway.filter.core.api.RequestContext;
import org.wso2.micro.gateway.filter.core.api.config.APIConfig;
import org.wso2.micro.gateway.filter.core.auth.jwt.JWTAuthenticator;
import org.wso2.micro.gateway.filter.core.exception.APISecurityException;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the filter handling the authentication for the requests flowing through the gateway.
 */
public class AuthFilter implements Filter {
    private List<Authenticator> authenticators = new ArrayList<>();

    @Override public void init(APIConfig apiConfig) {
        //TODO: Check security schema and add relevant authenticators .
        Authenticator jwtAuthenticator = new JWTAuthenticator();
        authenticators.add(jwtAuthenticator);
    }

    @Override public boolean handleRequest(RequestContext requestContext) {
        try {
            for (Authenticator authenticator : authenticators) {
                if (authenticator.canAuthenticate(requestContext)) {
                    AuthenticationContext authenticate = authenticator.authenticate(requestContext);
                    if (authenticate.isAuthenticated()) {
                        return true;
                    }
                }
            }
        } catch (APISecurityException e) {
            //TODO : handle or log the error
        }
        return false;
    }
}
