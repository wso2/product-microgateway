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
package org.wso2.choreo.connect.enforcer.admin.handlers;

import com.nimbusds.jwt.SignedJWT;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import org.wso2.choreo.connect.enforcer.admin.AdminUtils;
import org.wso2.choreo.connect.enforcer.constants.AdminConstants;
import org.wso2.choreo.connect.enforcer.models.ResponsePayload;
import org.wso2.choreo.connect.enforcer.models.RevokedToken;
import org.wso2.choreo.connect.enforcer.models.RevokedTokenList;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.RevokedJWTDataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Request handler implementation for revoked tokens
 */
public class RevokedTokensRequestHandler extends RequestHandler {
    @Override
    public ResponsePayload handleRequest(String[] params, String requestType) throws Exception {

        List<RevokedToken> revokedTokens = new ArrayList<>();
        RevokedJWTDataHolder revokedJWTDataHolder = RevokedJWTDataHolder.getInstance();
        Map<String, Long> revokedJWTMap = revokedJWTDataHolder.getRevokedJWTMap();
        String token;

        if (params != null) {
            for (String param : params) {
                String[] tokenParam = param.split("=");
                if (AdminConstants.Parameters.TOKEN.equals(tokenParam[0])) {
                    token = tokenParam[1];
                    String[] tokenParts = token.split("\\.");
                    if (tokenParts.length > 2) {
                        // this is a jwt token... Get the jti
                        SignedJWT signedJWT = SignedJWT.parse(token);
                        token = signedJWT.getJWTClaimsSet().getJWTID();

                        if (revokedJWTMap.containsKey(token)) {
                            RevokedToken revokedToken = new RevokedToken();
                            revokedToken.setToken(token);
                            revokedToken.setExpiredTime(revokedJWTMap.get(token));
                            revokedTokens.add(revokedToken);
                        }
                    }
                }
            }

        } else {
            // Return all the tokens...
            for (Map.Entry<String, Long> e : revokedJWTMap.entrySet()) {
                RevokedToken revokedToken = new RevokedToken();
                revokedToken.setToken(e.getKey());
                revokedToken.setExpiredTime(e.getValue());
                revokedTokens.add(revokedToken);
            }
        }

        RevokedTokenList revokedTokenList = AdminUtils.toRevokedTokenList(revokedTokens);
        return AdminUtils.buildResponsePayload(revokedTokenList, HttpResponseStatus.OK, false);
    }
}
