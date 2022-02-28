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

package org.wso2.choreo.connect.enforcer.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the response data to build the response object to be sent to the envoy proxy.
 */
public class ResponseObject {
    private final String correlationID;
    private int statusCode;
    private String errorCode;
    private String errorMessage;
    private String errorDescription;
    private Map<String, String> headerMap = new HashMap<>();
    private ArrayList<String> removeHeaderMap = new ArrayList<>();
    private Map<String, String> metaDataMap;
    private boolean isDirectResponse = false;
    private ArrayList<String> queryParamsToRemove = new ArrayList<>();
    private boolean removeAllQueryParams = false;
    private Map<String, String> queryParamsToAdd;
    private Map<String, String> queryParams;
    private String requestPath;
    private String responseContent;

    public ArrayList<String> getRemoveHeaderMap() {
        return removeHeaderMap;
    }

    public void setRemoveHeaderMap(ArrayList<String> removeHeaderMap) {
        this.removeHeaderMap = removeHeaderMap;
    }

    public ResponseObject(String correlationID) {
        this.correlationID = correlationID;
    }

    public ResponseObject() {
        this.correlationID = "xxxxx";
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public Map<String, String> getMetaDataMap() {
        return metaDataMap;
    }

    public void setMetaDataMap(Map<String, String> metaDataMap) {
        this.metaDataMap = metaDataMap;
    }

    public ArrayList<String> getQueryParamsToRemove() {
        return queryParamsToRemove;
    }
    public Map<String, String> getQueryParamsToAdd() {
        return queryParamsToAdd;
    }

    public void setQueryParamsToRemove(ArrayList<String> queryParamsToRemove) {
        this.queryParamsToRemove = queryParamsToRemove;
    }

    public void setRemoveAllQueryParams(boolean queryParamsToRemove) {
        this.removeAllQueryParams = queryParamsToRemove;
    }

    public boolean isRemoveAllQueryParams() {
        return removeAllQueryParams;
    }

    public void setQueryParamsToAdd(Map<String, String> queryParamsToAdd) {
        this.queryParamsToAdd = queryParamsToAdd;
    }

    public Map<String, String> getQueryParamMap() {
        return queryParams;
    }

    public void setQueryParamMap(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getResponsePayload() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }
}
