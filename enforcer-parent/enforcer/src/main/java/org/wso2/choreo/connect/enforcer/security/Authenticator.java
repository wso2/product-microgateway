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
package org.wso2.choreo.connect.enforcer.security;

import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;

/**
 * Defines the interface to implement an authenticator. This authenticator can be oauth2(opaque, jwt), MTLS, basic
 * and etc.
 */
public interface Authenticator {

    boolean canAuthenticate(RequestContext requestContext);

    AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException;

    /**
     * Returns a string representation of the authentication challenge imposed by this
     * authenticator. In case of an authentication failure this value will be sent back
     * to the API consumer in the form of a WWW-Authenticate header.
     *
     * @return A string representation of the authentication challenge
     */
    String getChallengeString();

    /**
     * Name of the authenticator.
     * This is used in authentication context of interceptors.
     * @return Authenticator name
     */
    String getName();

    int getPriority();
}
