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
package org.wso2.apimgt.gateway.cli.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.hashing.Hash;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIInfoDTO {

    private String id = null;
    private String name = null;
    private String description = null;
    private String context = null;
    private String version = null;
    private String provider = null;
    private String status = null;
    private String thumbnailUri = null;


    /**
     * UUID of the api registry artifact\n
     **/
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Name of the APIDetailedDTO
     **/
    @Hash
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A brief description about the APIDetailedDTO
     **/
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * A string that represents the context of the user's request
     **/
    @Hash
    @JsonProperty("context")
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * The version of the APIDetailedDTO
     **/
    @Hash
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * If the provider value is not given, the user invoking the APIDetailedDTO will be used as the provider.\n
     **/
    @JsonProperty("provider")
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * This describes in which status of the lifecycle the APIDetailedDTO is
     **/
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("thumbnailUri")
    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }
}
