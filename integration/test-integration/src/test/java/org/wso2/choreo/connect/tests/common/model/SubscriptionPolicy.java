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

package org.wso2.choreo.connect.tests.common.model;

import java.util.List;
import java.util.UUID;

/**
 * Subscription policy model
 */
public class SubscriptionPolicy {
    private String policyId = UUID.randomUUID().toString();
    private String policyName;
    private String displayName;
    private String description;
    private boolean isDeployed = true;
    private int rateLimitCount = 0;
    private String rateLimitTimeUnit = null;
    private List<String> customAttributes;
    boolean stopOnQuotaReach = true;
    private String billingPlan = "FREE";
    private DefaultLimit defaultLimit = new DefaultLimit();

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDeployed() {
        return isDeployed;
    }

    public void setDeployed(boolean deployed) {
        isDeployed = deployed;
    }

    public int getRateLimitCount() {
        return rateLimitCount;
    }

    public void setRateLimitCount(int rateLimitCount) {
        this.rateLimitCount = rateLimitCount;
    }

    public String getRateLimitTimeUnit() {
        return rateLimitTimeUnit;
    }

    public void setRateLimitTimeUnit(String rateLimitTimeUnit) {
        this.rateLimitTimeUnit = rateLimitTimeUnit;
    }

    public List<String> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<String> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public boolean isStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(boolean stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }

    public String getBillingPlan() {
        return billingPlan;
    }

    public void setBillingPlan(String billingPlan) {
        this.billingPlan = billingPlan;
    }

    public String getType() {
        return this.defaultLimit.getType();
    }

    public void setType(String type) {
        this.defaultLimit.setType(type);
    }

    public String getTimeUnit() {
        return this.defaultLimit.getTimeUnit();
    }

    public void setTimeUnit(String timeUnit) {
        this.defaultLimit.setTimeUnit(timeUnit);
    }

    public int getUnitTime() {
        return this.defaultLimit.getUnitTime();
    }

    public void setUnitTime(int unitTime) {
        this.defaultLimit.setUnitTime(unitTime);
    }

    public int getRequestCount() {
        return this.defaultLimit.getRequestCount();
    }

    public void setRequestCount(int requestCount) {
        this.defaultLimit.setRequestCount(requestCount);
        ;
    }
}