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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.filter.core.listener.events;

import java.util.Objects;

/**
 * An Event Object which can holds the data related to API which are required for the validation purpose in a gateway.
 */
public class APIEvent extends Event {

    private String apiName;
    private int apiId;
    private String apiVersion;
    private String apiContext;
    private String apiProvider;
    private String apiType;
    private String apiStatus;

    public APIEvent(String eventId, long timestamp, String type, int tenantId, String tenantDomain,
                    String apiName, int apiId, String apiVersion, String apiType, String apiContext,
                    String apiProvider, String apiStatus) {
        this.eventId = eventId;
        this.timeStamp = timestamp;
        this.type = type;
        this.tenantId = tenantId;
        this.apiId = apiId;
        this.apiVersion = apiVersion;
        this.apiName = apiName;
        this.apiType = apiType;
        this.apiContext = apiContext;
        this.apiProvider = apiProvider;
        this.apiStatus = apiStatus;
        this.tenantDomain = tenantDomain;
    }

    @Override
    public String toString() {
        return "APIEvent{" +
                "apiName='" + apiName + '\'' +
                ", apiId=" + apiId +
                ", apiVersion='" + apiVersion + '\'' +
                ", apiContext='" + apiContext + '\'' +
                ", apiStatus='" + apiStatus + '\'' +
                ", apiProvider='" + apiProvider + '\'' +
                ", apiType='" + apiType + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timeStamp=" + timeStamp +
                ", type='" + type + '\'' +
                ", tenantId=" + tenantId + '\'' +
                ", tenantDomain=" + tenantDomain +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof APIEvent)) {
            return false;
        }
        APIEvent apiEvent = (APIEvent) o;
        return getApiId() == apiEvent.getApiId() &&
                getApiName().equals(apiEvent.getApiName()) &&
                getApiVersion().equals(apiEvent.getApiVersion()) &&
                getApiContext().equals(apiEvent.getApiContext()) &&
                getApiStatus().equals(apiEvent.getApiStatus()) &&
                getApiProvider().equals(apiEvent.getApiProvider()) &&
                getApiType().equals(apiEvent.getApiType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApiName(), getApiId(), getApiVersion(), getApiContext(), getApiStatus(),
                getApiProvider(), getApiType());
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public int getApiId() {
        return apiId;
    }

    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiContext() {
        return apiContext;
    }

    public void setApiContext(String apiContext) {
        this.apiContext = apiContext;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getApiStatus() {
        return apiStatus;
    }

    public void setApiStatus(String apiStatus) {
        this.apiStatus = apiStatus;
    }
}
