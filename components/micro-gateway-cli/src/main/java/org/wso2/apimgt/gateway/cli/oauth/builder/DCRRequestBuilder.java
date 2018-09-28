/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.oauth.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.wso2.apimgt.gateway.cli.constants.TokenManagementConstants;
import org.wso2.apimgt.gateway.cli.model.oauth.DCRRequest;

import java.util.Arrays;

public class DCRRequestBuilder {
    private DCRRequest dcrRequest;
    private ObjectNode request;

    public DCRRequestBuilder() {
        this.dcrRequest = new DCRRequest();
        this.request = new ObjectMapper().createObjectNode();
    }

    public DCRRequest build() {
        return this.dcrRequest;
    }

    public String requestBody() {
        return this.request.toString();
    }

    public DCRRequestBuilder setCallbackUrl(String callbackUrl) {
        this.request.put(TokenManagementConstants.CALLBACK_URL, callbackUrl);
        this.dcrRequest.setCallbackUrl(callbackUrl);
        return this;
    }

    public DCRRequestBuilder setClientName(String clientName) {
        this.request.put(TokenManagementConstants.CLIENT_NAME, clientName);
        this.dcrRequest.setClientName(clientName);
        return this;
    }

    public DCRRequestBuilder setOwner(String owner) {
        this.request.put(TokenManagementConstants.OWNER, owner);
        this.dcrRequest.setOwner(owner);
        return this;
    }

    public DCRRequestBuilder setGrantTypes(String[] grantTypes) {
        String grantTypesStr = String.join(" ", grantTypes);
        this.dcrRequest.setGrantTypes(grantTypes);
        //this.request.put(TokenManagementConstants.GRANT_TYPE, grantTypesStr);
        ArrayNode grantTypeArray = this.request.putArray(TokenManagementConstants.GRANT_TYPE);
        Arrays.stream(grantTypes).forEach( val -> grantTypeArray.add(val));
        return this;
    }

    public DCRRequestBuilder setTokenScope(String tokenScope) {
        this.request.put(TokenManagementConstants.TOKEN_SCOPE, tokenScope);
        this.dcrRequest.setTokenScope(tokenScope);
        return this;
    }

    public DCRRequestBuilder setSaasApp(boolean saasApp) {
        this.request.put(TokenManagementConstants.SAAS_APP, saasApp);
        this.dcrRequest.setSaasApp(saasApp);
        return this;
    }
}
