/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.common.model;

/**
 * API Object model
 */
public class API {
    private String id;
    private String name;
    private String version;
    private String context;
    private String prodEndpoint;
    private String sandEndpoint;
    private String provider;
    private String[] tiers;
    private String swagger;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getProdEndpoint() {
        return prodEndpoint;
    }

    public void setProdEndpoint(String prodEndpoint) {
        this.prodEndpoint = prodEndpoint;
    }

    public String getSandEndpoint() {
        return sandEndpoint;
    }

    public void setSandEndpoint(String sandEndpoint) {
        this.sandEndpoint = sandEndpoint;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String[] getTiers() {
        return tiers;
    }

    public void setTiers(String[] tiers) {
        this.tiers = tiers;
    }

    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    public void setEndpoint(String endpoint) {
        setProdEndpoint(endpoint);
        setSandEndpoint(endpoint);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
