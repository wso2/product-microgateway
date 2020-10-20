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

import org.wso2.micro.gateway.filter.core.constants.APIConstants;

import java.util.Set;
import java.util.concurrent.locks.Condition;

/**
 * Entity for keeping details related to ConditionGroups.
 */
public class APIPolicyConditionGroup {

    private int policyId = -1;
    private String quotaType;
    private int conditionGroupId = -1;
    private Set<Condition> condition;

    public int getPolicyId() {

        return policyId;
    }

    public void setPolicyId(int policyId) {

        this.policyId = policyId;
    }

    public int getConditionGroupId() {

        return conditionGroupId;
    }

    public void setConditionGroupId(int conditionGroupId) {

        this.conditionGroupId = conditionGroupId;
    }

    public String getQuotaType() {

        return quotaType;
    }

    public void setQuotaType(String quotaType) {

        this.quotaType = quotaType;
    }

    public Set<Condition> getCondition() {

        return condition;
    }

    public void setCondition(Set<Condition> condition) {

        this.condition = condition;
    }

    public boolean isContentAware() {

        if (APIConstants.BANDWIDTH_TYPE.equals(quotaType)) {
            return true;
        }
        if (condition != null) {
            condition.stream().anyMatch(conditionDTO ->
                    APIConstants.BANDWIDTH_TYPE.equals(quotaType)
            );
            return false;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        APIPolicyConditionGroup conditionGroup = (APIPolicyConditionGroup) obj;
        return conditionGroup.policyId == policyId &&
                conditionGroup.conditionGroupId == conditionGroupId;

    }

    @Override
    public int hashCode() {

        return (policyId == -1 || conditionGroupId == -1) ? super.hashCode() : policyId * conditionGroupId;
    }
}

