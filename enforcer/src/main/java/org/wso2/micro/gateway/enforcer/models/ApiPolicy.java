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

package org.wso2.micro.gateway.enforcer.models;

import org.wso2.micro.gateway.enforcer.common.CacheableEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds details about API/resource level Policy.
 */
public class ApiPolicy extends Policy {

    private List<APIPolicyConditionGroup> conditionGroups = new ArrayList<>();
    private String applicableLevel;

    @Override
    public String getCacheKey() {

        return PolicyType.API + CacheableEntity.DELEM_PERIOD + super.getCacheKey();
    }

    public List<APIPolicyConditionGroup> getConditionGroups() {
        return conditionGroups;
    }

    public void setConditionGroups(List<APIPolicyConditionGroup> conditionGroups) {
        this.conditionGroups = conditionGroups;
    }
    @Override
    public boolean isContentAware() {
        boolean isContentAware = super.isContentAware();
        for (APIPolicyConditionGroup apiPolicyConditionGroup : conditionGroups) {
            isContentAware = isContentAware || apiPolicyConditionGroup.isContentAware();
        }
        return isContentAware;
    }

    public String getApplicableLevel() {
        return applicableLevel;
    }

    public void setApplicableLevel(String applicableLevel) {
        this.applicableLevel = applicableLevel;
    }
}

