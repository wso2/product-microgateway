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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.validation.constraints.NotNull;

/**
 * Data mapper for WSO2 APIM throttle limits.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RequestCountLimitDTO.class, name = "RequestCountLimit"),
        @JsonSubTypes.Type(value = BandwidthLimitDTO.class, name = "BandwidthLimit"),
})
public class ThrottleLimitDTO {

    /**
     * Throttle limit type.
     */
    public enum TypeEnum {
        RequestCountLimit, BandwidthLimit,
    }

    /**
     * Type of the throttling limit. Allowed values are \"RequestCountLimit\" and \"BandwidthLimit\".\n
     * Please see schemas of each of those throttling limit types in Definitions section.\n
     **/
    @JsonProperty("type")
    public TypeEnum getType() {
        return type;
    }

    @NotNull
    private TypeEnum type = null;
    @NotNull
    private String timeUnit = null;
    @NotNull
    private Integer unitTime = null;

    public void setType(TypeEnum type) {
        this.type = type;
    }


    /**
     * Unit of the time. Allowed values are \"sec\", \"min\", \"hour\", \"day\"
     **/
    @JsonProperty("timeUnit")
    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }


    /**
     * Time limit that the throttling limit applies.
     **/
    @JsonProperty("unitTime")
    public Integer getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(Integer unitTime) {
        this.unitTime = unitTime;
    }

}
