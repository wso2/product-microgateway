/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apimgt.gateway.cli.model.rest.policy;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.LinkedHashMap;

import javax.validation.constraints.NotNull;

/**
 * Policy definition mapper for WSO2 APIM throttle policy.
 */
public class ThrottlePolicyMapper {
    @NotNull
    private String name = null;
    @NotNull
    private Long count = null;
    @NotNull
    private String timeUnit = null;
    @NotNull
    private Integer unitTime = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(Integer unitTime) {
        this.unitTime = unitTime;
    }

    @JsonAnySetter
    public void setValues(String key, LinkedHashMap<String, String> value) {
        this.name = key;
        this.count = Long.parseLong(value.get("count"));
        this.timeUnit = value.get("timeUnit");
        this.unitTime = Integer.parseInt(value.get("unitTime"));
    }
}
