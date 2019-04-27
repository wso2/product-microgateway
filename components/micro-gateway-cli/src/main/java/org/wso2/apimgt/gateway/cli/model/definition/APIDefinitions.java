/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.model.definition;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents the DTO for multiple APIs in Microgateway Definition.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIDefinitions {
    private Map<String, APIDefinition> apis = new LinkedHashMap<>();

    @JsonAnySetter
    public void setApis(String key, APIDefinition api) {
        apis.put(key, api);
    }

    /**
     * get all the api information from the root level
     *
     * @return map with (basepath, api information object)
     */
    public Map<String, APIDefinition> getApisMap() {
        return apis;
    }

    public APIDefinition getApiFromBasepath(String basepath) {
        return apis.get(basepath);
    }

    public String getBasepathFromAPI(String apiName, String apiVersion) {
        for (Map.Entry<String, APIDefinition> apiEntry : apis.entrySet()) {
            if (apiEntry.getValue().getTitle().equals(apiName) && apiEntry.getValue().getVersion().equals(apiVersion)) {
                //because if this method is called, it means that API information is utilized.
                apiEntry.getValue().setIsDefinitionUsed(true);
                return apiEntry.getKey();
            }
        }
        return null;
    }
}
