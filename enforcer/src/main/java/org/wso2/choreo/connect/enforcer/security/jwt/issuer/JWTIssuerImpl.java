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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.security.TokenValidationContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default JWT issuer implementation for tokens.
 */
public class JWTIssuerImpl extends AbstractJWTIssuer {
    private static final Log log = LogFactory.getLog(JWTIssuerImpl.class);
    private static final String KEY_TYPE = "PRODUCTION";
    private static final String AUD_VALUE = "http://org.wso2.apimgt/gateway";

    @Override
    public Map<String, String> populateStandardClaims(TokenValidationContext validationContext)
            throws JWTGeneratorException {

        long currentTime = System.currentTimeMillis();
        // Generating expiring timestamp
        long expireIn = currentTime + super.jwtIssuerConfigurationDto.getTtl() * 1000;
        Map<String, String> claims = new LinkedHashMap<String, String>(20);

        String dialect = getDialectURI();

        // dialect is either empty or '/' do not append a backslash. otherwise append a backslash '/'
        if (!"".equals(dialect) && !"/".equals(dialect)) {
            dialect = dialect + "/";
        }

        claims.put("iss", super.jwtIssuerConfigurationDto.getIssuer());
        claims.put("exp", String.valueOf(expireIn));
        claims.put("iat", String.valueOf(currentTime));
        claims.put("aud", AUD_VALUE);
        claims.put(dialect + "keytype", KEY_TYPE);
        // in test key we provide the requested scope without any authorization checks.
        if (validationContext.getAttribute(APIConstants.JwtTokenConstants.SCOPE) != null) {
            claims.put(APIConstants.JwtTokenConstants.SCOPE,
                    validationContext.getAttribute(APIConstants.JwtTokenConstants.SCOPE).toString());
        }

        String endUserName = validationContext.getValidationInfoDTO().getEndUserName();

        if (StringUtils.isNotEmpty(endUserName)) {
            claims.put(dialect + "sub", endUserName);
        }
        return claims;
    }
}
