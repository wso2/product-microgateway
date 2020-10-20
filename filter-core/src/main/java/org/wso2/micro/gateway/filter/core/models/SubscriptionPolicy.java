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

package org.wso2.micro.gateway.filter.core.models;

import org.wso2.micro.gateway.filter.core.subscription.SubscriptionDataStoreUtil;

/**
 * Entity to represent a Subscription Throttling Policy.
 */
public class SubscriptionPolicy extends Policy {

    private Integer rateLimitCount = null;
    private String rateLimitTimeUnit = null;
    private Boolean stopOnQuotaReach = null;
    private Integer graphQLMaxComplexity = null;
    private Integer graphQLMaxDepth = null;

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

    public boolean isStopOnQuotaReach() {

        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(boolean stopOnQuotaReach) {

        this.stopOnQuotaReach = stopOnQuotaReach;
    }

    public int getGraphQLMaxComplexity() {
        return graphQLMaxComplexity;
    }

    public void setGraphQLMaxComplexity(int graphQLMaxComplexity) {
        this.graphQLMaxComplexity = graphQLMaxComplexity;
    }

    public int getGraphQLMaxDepth() {
        return graphQLMaxDepth;
    }

    public void setGraphQLMaxDepth(int graphQLMaxDepth) {
        this.graphQLMaxDepth = graphQLMaxDepth;
    }
    @Override
    public String getCacheKey() {

        return PolicyType.SUBSCRIPTION + SubscriptionDataStoreUtil.getPolicyCacheKey(getName(), getTenantId());

    }

    @Override
    public String toString() {
        return "SubscriptionPolicy [rateLimitCount=" + rateLimitCount + ", rateLimitTimeUnit=" + rateLimitTimeUnit
                + ", stopOnQuotaReach=" + stopOnQuotaReach + ", getId()=" + getId() + ", getQuotaType()="
                + getQuotaType() + ", isContentAware()=" + isContentAware() + ", getTenantId()=" + getTenantId()
                + ", getName()=" + getName() + "]";
    }
}

