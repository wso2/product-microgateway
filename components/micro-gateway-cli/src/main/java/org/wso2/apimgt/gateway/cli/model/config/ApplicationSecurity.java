/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Application security.
 */
public class ApplicationSecurity {

    @JsonProperty("security-types")
    private List<String> securityTypes = new ArrayList<>();
    // default set to null to make it as undefined
    private Boolean optional = null;

    public void setSecurityTypes(List<String> securityTypes) {
        this.securityTypes = securityTypes;
    }

    public List<String> getSecurityTypes() {
        return this.securityTypes;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Return if application security is optional(true), mandatory(false) or undefined(null)
     * @return Boolean isOptional
     */
    public Boolean isOptional() {
        return this.optional;
    }
}
