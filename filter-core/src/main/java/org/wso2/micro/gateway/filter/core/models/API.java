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

package org.wso2.micro.gateway.filter.core.models;

import org.wso2.micro.gateway.filter.core.common.CacheableEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity for keeping API related information.
 */
public class API implements CacheableEntity<String> {

    private Integer apiId = null;
    private String provider = null;
    private String name = null;
    private String version = null;
    private String context = null;
    private String policy = null;
    private String apiType = null;
    private boolean isDefaultVersion = false;

    private List<URLMapping> urlMappings = new ArrayList<>();


    public void addResource(URLMapping resource) {

        urlMappings.add(resource);
    }

    public List<URLMapping> getResources() {

        return urlMappings;
    }

    public void removeResource(URLMapping resource) {
        urlMappings.remove(resource);
    }

    public String getContext() {

        return context;
    }

    public void setContext(String context) {

        this.context = context;
    }

    public String getApiTier() {

        return policy;
    }

    public void setApiTier(String apiTier) {

        this.policy = apiTier;
    }

    public int getApiId() {

        return apiId;
    }

    public void setApiId(int apiId) {

        this.apiId = apiId;
    }

    public String getApiProvider() {

        return provider;
    }

    public void setApiProvider(String apiProvider) {

        this.provider = apiProvider;
    }

    public String getApiName() {

        return name;
    }

    public void setApiName(String apiName) {

        this.name = apiName;
    }

    public String getApiVersion() {

        return version;
    }

    public void setApiVersion(String apiVersion) {

        this.version = apiVersion;
    }

    public String getCacheKey() {

        return context + DELEM_PERIOD + version;
    }

    public String getApiType() {

        return apiType;
    }

    public void setApiType(String apiType) {

        this.apiType = apiType;
    }

    @Override
    public String toString() {
        return "API [apiId=" + apiId + ", provider=" + provider + ", name=" + name + ", version=" + version
                + ", context=" + context + ", policy=" + policy + ", apiType=" + apiType + ", urlMappings="
                + urlMappings + "]";
    }

    public boolean isDefaultVersion() {
        return isDefaultVersion;
    }

    public void setDefaultVersion(boolean isDefaultVersion) {
        this.isDefaultVersion = isDefaultVersion;
    }

}

