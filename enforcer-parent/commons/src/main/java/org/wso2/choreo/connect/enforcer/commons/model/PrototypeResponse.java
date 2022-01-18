/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.List;

/**
 * Defines prototype response structure used in the prototyped APIs.
 */
public class PrototypeResponse {
    private String value;
    private int code;
    private List<PrototypeHeader> headers;

    private PrototypePayload payload;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public PrototypePayload getPayload() {
        return payload;
    }

    public void setPayload(PrototypePayload payload) {
        this.payload = payload;
    }

    public List<PrototypeHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<PrototypeHeader> headers) {
        this.headers = headers;
    }
}
