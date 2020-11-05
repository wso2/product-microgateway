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

import org.wso2.micro.gateway.filter.core.common.CacheableEntity;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;
import org.wso2.micro.gateway.filter.core.subscription.SubscriptionDataStoreUtil;

/**
 * Top level entity for representing a Throttling Policy.
 */
public class Policy implements CacheableEntity<String> {

    /**
     * ENUM to hold type of policies.
     */
    public enum PolicyType {
        SUBSCRIPTION,
        APPLICATION,
        API
    }
    private Integer id = null;
    private Integer tenantId = null;
    private String name = null;
    private String quotaType = null;

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public String getQuotaType() {

        return quotaType;
    }

    public void setQuotaType(String quotaType) {

        this.quotaType = quotaType;
    }

    public boolean isContentAware() {

        return APIConstants.BANDWIDTH_TYPE.equals(quotaType);
    }

    public int getTenantId() {

        return tenantId;
    }

    public void setTenantId(int tenantId) {

        this.tenantId = tenantId;
    }

    public String getName() {

        return name;
    }

    public void setTierName(String name) {

        this.name = name;
    }

    @Override
    public String getCacheKey() {

        return SubscriptionDataStoreUtil.getPolicyCacheKey(getName(), getTenantId());
    }
}
