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

import java.util.ArrayList;
import java.util.List;

public class ApplicationThrottlePolicyListDTO {


    private Integer count = null;
    private List<ApplicationThrottlePolicyDTO> list = new ArrayList<ApplicationThrottlePolicyDTO>();


    /**
     * Number of Application Throttling Policies returned.\n
     **/
    @JsonProperty("count")
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }


    @JsonProperty("list")
    public List<ApplicationThrottlePolicyDTO> getList() {
        return list;
    }

    public void setList(List<ApplicationThrottlePolicyDTO> list) {
        this.list = list;
    }
}
