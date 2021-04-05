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

package org.wso2.choreo.connect.enforcer.security.jwt.issuer;

import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.choreo.connect.enforcer.security.TokenValidationContext;

/**
 * This interface can be used to generate a token with the invoking user's details.
 */
public interface TokenIssuer {

    /**
     * Generates the JWT and gives as a string.
     * @param validationContext
     * @return
     * @throws JWTGeneratorException
     */
    String generateToken(TokenValidationContext validationContext) throws JWTGeneratorException;
}
