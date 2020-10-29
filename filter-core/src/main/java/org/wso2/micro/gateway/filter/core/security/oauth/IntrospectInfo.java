/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.micro.gateway.filter.core.security.oauth;

import com.google.gson.annotations.SerializedName;

public class IntrospectInfo {

    @SerializedName("active")
    private boolean active;
    @SerializedName("client_id")
    private String clientId;
    @SerializedName("device_id")
    private String deviceId;
    @SerializedName("exp")
    private long expiry;
    @SerializedName("iat")
    private long iat;
    @SerializedName("nbf")
    private long nbf;
    @SerializedName("scope")
    private String scope;
    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("username")
    private String username;

    public boolean isActive() {

        return active;
    }

    public void setActive(boolean active) {

        this.active = active;
    }

    public String getClientId() {

        return clientId;
    }

    public void setClientId(String clientId) {

        this.clientId = clientId;
    }

    public String getDeviceId() {

        return deviceId;
    }

    public void setDeviceId(String deviceId) {

        this.deviceId = deviceId;
    }

    public long getExpiry() {

        return expiry;
    }

    public void setExpiry(long expiry) {

        this.expiry = expiry;
    }

    public long getIat() {

        return iat;
    }

    public void setIat(long iat) {

        this.iat = iat;
    }

    public long getNbf() {

        return nbf;
    }

    public void setNbf(long nbf) {

        this.nbf = nbf;
    }

    public String getScope() {

        return scope;
    }

    public void setScope(String scope) {

        this.scope = scope;
    }

    public String getTokenType() {

        return tokenType;
    }

    public void setTokenType(String tokenType) {

        this.tokenType = tokenType;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }
}
