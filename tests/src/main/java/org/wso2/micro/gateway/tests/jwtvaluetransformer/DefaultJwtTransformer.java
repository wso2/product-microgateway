/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.jwtvaluetransformer;

import org.wso2.micro.gateway.jwt.transformer.JWTValueTransformer;

import java.util.Map;

/**
 * This class is for default Jwt transformer.
 */
public class DefaultJwtTransformer implements JWTValueTransformer {

    @Override
    public Map<String, Object> transformJWT(Map<String, Object> jwtClaims) {
        String scope = "";
        if (jwtClaims.containsKey("scope")) {
            if (jwtClaims.get("scope") instanceof Object[]) {
                for (int i = 0; i < ((Object[]) jwtClaims.get("scope")).length; i++) {
                    scope += ((Object[]) jwtClaims.get("scope"))[i] + " ";
                }
                scope = scope.trim();
            }
            jwtClaims.put("scope", scope);
        }
        return jwtClaims;
    }
}
