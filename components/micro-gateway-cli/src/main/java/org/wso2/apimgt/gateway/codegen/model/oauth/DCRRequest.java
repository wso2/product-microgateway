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
package org.wso2.apimgt.gateway.codegen.token.bean;

public class DCRRequest {
    private String callbackUrl;
    private String clientName;
    private String owner;
    private String[] grantTypes;
    private String tokenScope;
    private boolean saasApp;

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String[] getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String[] grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getTokenScope() {
        return tokenScope;
    }

    public void setTokenScope(String tokenScope) {
        this.tokenScope = tokenScope;
    }

    public boolean isSaasApp() {
        return saasApp;
    }

    public void setSaasApp(boolean saasApp) {
        this.saasApp = saasApp;
    }
}
