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
package org.wso2.micro.gateway.filter.core.api.config;

import org.wso2.micro.gateway.filter.core.constants.APIConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the metadata related to the resources/operations of an API. Common collection to hold data about
 * any API type like REST, gRPC and etc.
 */
public class ResourceConfig {

    private String path;
    private HttpMethods method;
    private List<String> securitySchemas = new ArrayList<>();
    private String tier = APIConstants.UNLIMITED_TIER;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpMethods getMethod() {
        return method;
    }

    public void setMethod(HttpMethods method) {
        this.method = method;
    }

    public List<String> getSecuritySchemas() {
        return securitySchemas;
    }

    public void setSecuritySchemas(List<String> securitySchemas) {
        this.securitySchemas = securitySchemas;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    /**
     * ENUM to hold http operations
     */
    public enum HttpMethods {
        GET("get"), POST("post"), PUT("put"), DELETE("delete"), HEAD("head"), OPTION("option");

        private String value;

        private HttpMethods(String value) {
            this.value = value;
        }
    }
}


