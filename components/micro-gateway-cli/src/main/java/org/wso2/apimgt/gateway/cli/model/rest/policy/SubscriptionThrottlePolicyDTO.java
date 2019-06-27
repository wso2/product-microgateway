/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.rest.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.hashing.Hash;

import java.util.ArrayList;
import java.util.List;

/**
 * Data mapper for WSO2 APIM subscription throttle policy.
 */
public class SubscriptionThrottlePolicyDTO extends ThrottlePolicyDTO {

    private ThrottleLimitDTO defaultLimit = null;
    private Integer rateLimitCount = null;
    private String rateLimitTimeUnit = null;
    private List<CustomAttributeDTO> customAttributes = new ArrayList<CustomAttributeDTO>();
    private Boolean stopOnQuotaReach = false;
    private String billingPlan = null;

    @Hash
    @JsonProperty("defaultLimit")
    public ThrottleLimitDTO getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(ThrottleLimitDTO defaultLimit) {
        this.defaultLimit = defaultLimit;
    }


    /**
     * Burst control request count
     **/
    @JsonProperty("rateLimitCount")
    public Integer getRateLimitCount() {
        return rateLimitCount;
    }

    public void setRateLimitCount(Integer rateLimitCount) {
        this.rateLimitCount = rateLimitCount;
    }


    /**
     * Burst control time unit
     **/
    @JsonProperty("rateLimitTimeUnit")
    public String getRateLimitTimeUnit() {
        return rateLimitTimeUnit;
    }

    public void setRateLimitTimeUnit(String rateLimitTimeUnit) {
        this.rateLimitTimeUnit = rateLimitTimeUnit;
    }


    /**
     * Custom attributes added to the Subscription Throttling Policy\n
     **/
    @JsonProperty("customAttributes")
    public List<CustomAttributeDTO> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(List<CustomAttributeDTO> customAttributes) {
        this.customAttributes = customAttributes;
    }


    /**
     * This indicates the action to be taken when a user goes beyond the allocated quota. If checked, the user's
     * requests will be dropped. If unchecked, the requests will be allowed to pass through.\n
     **/
    @Hash
    @JsonProperty("stopOnQuotaReach")
    public Boolean getStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(Boolean stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }


    /**
     * define whether this is Paid or a Free plan. Allowed values are FREE or COMMERCIAL.\n
     **/
    @JsonProperty("billingPlan")
    public String getBillingPlan() {
        return billingPlan;
    }

    public void setBillingPlan(String billingPlan) {
        this.billingPlan = billingPlan;
    }
}
