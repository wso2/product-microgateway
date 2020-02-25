/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transport Security http, http2 and mutualSSL.
 */
public class TransportSecurity {
    private boolean http = true;
    private boolean https = true;

    //if this is null, mutual ssl is disabled. This set client verification to optional/mandatory
    @JsonProperty("mutualssl")
    private String mutualSSL = null;

    public void setHttp(boolean http) {
        this.http = http;
    }
    public boolean getHttp() {
        return http;
    }
    public void setHttps(boolean https) {
        this.https = https;
    }
    public boolean getHttps() {
        return https;
    }
    public void setMutualSSL(String mutualSSL) {
        this.mutualSSL = mutualSSL;
    }
    public String getMutualSSL() {
        return mutualSSL;
    }
}
