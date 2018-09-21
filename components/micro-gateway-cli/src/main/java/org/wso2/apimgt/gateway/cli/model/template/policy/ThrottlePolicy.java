/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.apimgt.gateway.cli.model.template.policy;

import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.RequestCountLimitDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ThrottleLimitDTO;

import java.util.concurrent.TimeUnit;

public class ThrottlePolicy {

    private String policyType;
    private String policyKey;
    private String name;
    //unit time in milliSeconds
    private long unitTime;
    private String srcPackage;
    private String modelPackage;
    private String funcName;
    private long count;
    private String tierType;
    private boolean stopOnQuotaReach;

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(long unitTime) {
        this.unitTime = unitTime;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getSrcPackage() {
        return srcPackage;
    }

    public void setSrcPackage(String srcPackage) {
        this.srcPackage = srcPackage;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public String getPolicyKey() {
        return policyKey;
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    public String getTierType() {
        return tierType;
    }

    public void setTierType(String tierType) {
        this.tierType = tierType;
    }

    public boolean isStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(boolean stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }

    public ThrottlePolicy buildContext(
            ApplicationThrottlePolicyDTO applicationPolicy) {
        this.policyType = GeneratorConstants.APPLICATION_POLICY_TYPE;
        this.name = applicationPolicy.getPolicyName();
        ThrottleLimitDTO requestCountLimitDTO =  applicationPolicy.getDefaultLimit();
        this.count = requestCountLimitDTO.getRequestCountLimit().getRequestCount();
        this.unitTime = getTimeInMilliSeconds(requestCountLimitDTO.getUnitTime(), requestCountLimitDTO.getTimeUnit());
        this.funcName = GeneratorConstants.APPLICATION_INIT_FUNC_PREFIX + applicationPolicy.getPolicyName()
                + GeneratorConstants.INIT_FUNC_SUFFIX;
        this.policyKey = GeneratorConstants.APPLICATION_KEY;
        this.tierType = GeneratorConstants.APPLICATION_TIER_TYPE;
        this.stopOnQuotaReach = true;
        return this;
    }

    public ThrottlePolicy buildContext(SubscriptionThrottlePolicyDTO applicationPolicy) {
        this.policyType = GeneratorConstants.SUBSCRIPTION_POLICY_TYPE;
        this.name = applicationPolicy.getPolicyName();
        ThrottleLimitDTO requestCountLimitDTO =  applicationPolicy.getDefaultLimit();
        this.count = requestCountLimitDTO.getRequestCountLimit().getRequestCount();;
        this.unitTime = getTimeInMilliSeconds(requestCountLimitDTO.getUnitTime(), requestCountLimitDTO.getTimeUnit());
        this.funcName = GeneratorConstants.SUBSCRIPTION_INIT_FUNC_PREFIX + applicationPolicy.getPolicyName()
                + GeneratorConstants.INIT_FUNC_SUFFIX;
        this.policyKey = GeneratorConstants.SUBSCRIPTION_KEY;
        this.tierType = GeneratorConstants.SUBSCRIPTION_TIER_TYPE;
        this.stopOnQuotaReach = applicationPolicy.getStopOnQuotaReach();
        return this;
    }

    public ThrottlePolicy srcPackage(String srcPackage) {
        if (srcPackage != null) {
            this.srcPackage = srcPackage.replaceFirst("\\.", "/");
        }
        return this;
    }

    public ThrottlePolicy modelPackage(String modelPackage) {
        if (modelPackage != null) {
            this.modelPackage = modelPackage.replaceFirst("\\.", "/");
        }
        return this;
    }

    private long getTimeInMilliSeconds(int unitTime, String timeUnit) {
        long milliSeconds;
        if ("min".equalsIgnoreCase(timeUnit)) {
            milliSeconds = TimeUnit.MINUTES.toMillis(unitTime);
        } else if ("hour".equalsIgnoreCase(timeUnit)) {
            milliSeconds = TimeUnit.HOURS.toMillis(unitTime);
        } else if ("day".equalsIgnoreCase(timeUnit)) {
            milliSeconds = TimeUnit.DAYS.toMillis(unitTime);
        } else if ("week".equalsIgnoreCase(timeUnit)) {
            milliSeconds = 7 * TimeUnit.DAYS.toMillis(unitTime);
        } else if ("month".equalsIgnoreCase(timeUnit)) {
            milliSeconds = 30 * TimeUnit.DAYS.toMillis(unitTime);
        } else if ("year".equalsIgnoreCase(timeUnit)) {
            milliSeconds = 365 * TimeUnit.DAYS.toMillis(unitTime);
        } else {
            throw new RuntimeException("Unsupported time unit provided");
        }
        return milliSeconds;
    }
}
