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

public class BandwidthLimitDTO extends ThrottleLimitDTO {

    private Long dataAmount = null;
    private String dataUnit = null;


    /**
     * Amount of data allowed to be transfered
     **/
    @JsonProperty("dataAmount")
    public Long getDataAmount() {
        return dataAmount;
    }

    public void setDataAmount(Long dataAmount) {
        this.dataAmount = dataAmount;
    }


    /**
     * Unit of data allowed to be transfered. Allowed values are \"KB\", \"MB\" and \"GB\"
     **/
    @JsonProperty("dataUnit")
    public String getDataUnit() {
        return dataUnit;
    }

    public void setDataUnit(String dataUnit) {
        this.dataUnit = dataUnit;
    }

}
