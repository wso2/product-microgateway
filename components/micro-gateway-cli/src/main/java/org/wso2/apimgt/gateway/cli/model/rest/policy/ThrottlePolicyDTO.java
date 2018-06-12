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
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;

@ApiModel(description = "")
public class ThrottlePolicyDTO {

    private String policyId = null;
    @NotNull
    private String policyName = null;
    private String displayName = null;
    private String description = null;
    private Boolean isDeployed = false;


    /**
     * Id of policy
     **/
    @JsonProperty("policyId")
    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }


    /**
     * Name of policy
     **/
    @JsonProperty("policyName")
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }


    /**
     * Display name of the policy
     **/
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    /**
     * Description of the policy
     **/
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * Indicates whether the policy is deployed successfully or not.
     **/
    @JsonProperty("isDeployed")
    public Boolean getIsDeployed() {
        return isDeployed;
    }

    public void setIsDeployed(Boolean isDeployed) {
        this.isDeployed = isDeployed;
    }

}
