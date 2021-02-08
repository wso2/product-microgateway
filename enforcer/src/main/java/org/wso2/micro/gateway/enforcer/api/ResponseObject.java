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

package org.wso2.micro.gateway.enforcer.api;

import java.util.Map;

/**
 * Holds the response data to build the response object to be sent to the envoy proxy.
 */
public class ResponseObject {
    private final String correlationID;
    private int statusCode;
    private String errorCode;
    private String errorDescription;
    private Map<String, String> headerMap;
    private boolean isDirectResponse = false;

    public ResponseObject(String correlationID) {
        this.correlationID = correlationID;
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    public Map<String, String> getHeaderMap() {
        return headerMap;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public boolean isDirectResponse() {
        return isDirectResponse;
    }

    public void setDirectResponse(boolean directResponse) {
        isDirectResponse = directResponse;
    }

    public String getCorrelationID() {
        return correlationID;
    }
}
