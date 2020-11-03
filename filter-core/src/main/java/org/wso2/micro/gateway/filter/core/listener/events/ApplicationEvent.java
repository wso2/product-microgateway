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

package org.wso2.micro.gateway.filter.core.listener.events;

import java.util.Map;
import java.util.Objects;

/**
 * An Event Object which can holds the data related to Application which are required
 * for the validation purpose in a gateway.
 */
public class ApplicationEvent extends Event {
    private String uuid;
    private int applicationId;
    private String applicationName;
    private String tokenType;
    private String applicationPolicy;
    private String groupId;
    private Map<String, String> attributes;
    private String subscriber;

    public ApplicationEvent(String eventId, long timestamp, String type, int tenantId, String tenantDomain,
                            int applicationId, String uuid, String applicationName, String tokenType,
                            String applicationPolicy, String groupId, Map<String, String> attributes,
                            String subscriber) {
        this.eventId = eventId;
        this.timeStamp = timestamp;
        this.type = type;
        this.tenantId = tenantId;
        this.applicationId = applicationId;
        this.uuid = uuid;
        this.applicationName = applicationName;
        this.tokenType = tokenType;
        this.applicationPolicy = applicationPolicy;
        this.groupId = groupId;
        this.tenantDomain = tenantDomain;
        this.attributes = attributes;
        this.subscriber = subscriber;
    }

    @Override
    public String toString() {
        return "ApplicationEvent{" +
                "applicationId=" + applicationId +
                ", applicationName='" + applicationName + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", applicationPolicy='" + applicationPolicy + '\'' +
                ", groupId='" + groupId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timeStamp=" + timeStamp +
                ", type='" + type + '\'' +
                ", tenantId=" + tenantId + '\'' +
                ", tenantDomain=" + tenantDomain +
                ", subscriber=" + subscriber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationEvent)) {
            return false;
        }
        ApplicationEvent that = (ApplicationEvent) o;
        return getApplicationId() == that.getApplicationId() &&
                getApplicationName().equals(that.getApplicationName()) &&
                getTokenType().equals(that.getTokenType()) &&
                getGroupId().equals(that.getGroupId()) &&
                getApplicationPolicy().equals(that.getApplicationPolicy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationId(), getApplicationName(),
                getTokenType(), getApplicationPolicy(), getGroupId());
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getApplicationPolicy() {
        return applicationPolicy;
    }

    public void setApplicationPolicy(String applicationPolicy) {
        this.applicationPolicy = applicationPolicy;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Map<String, String> getAttributes() {

        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {

        this.attributes = attributes;
    }

    public String getUuid() {

        return uuid;
    }

    public void setUuid(String uuid) {

        this.uuid = uuid;
    }

    public String getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }
}
