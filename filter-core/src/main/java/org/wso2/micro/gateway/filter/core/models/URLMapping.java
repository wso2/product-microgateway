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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds details about url mapping of API resources.
 */
public class URLMapping {

    private String throttlingPolicy;
    private String authScheme;
    private String httpMethod;
    private String urlPattern;
    private List<String> scopes = new ArrayList<>();


    public String getHttpMethod() {

        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {

        this.httpMethod = httpMethod;
    }

    public String getThrottlingPolicy() {

        return throttlingPolicy;
    }

    public void setThrottlingPolicy(String throttlingPolicy) {

        this.throttlingPolicy = throttlingPolicy;
    }

    public String getAuthScheme() {

        return authScheme;
    }

    public void setAuthScheme(String authScheme) {

        this.authScheme = authScheme;
    }

    public String getUrlPattern() {

        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {

        this.urlPattern = urlPattern;
    }

    public void addScope(String scope) {
        scopes.add(scope);
    }

    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        URLMapping that = (URLMapping) o;
        return Objects.equals(throttlingPolicy, that.throttlingPolicy) &&
                Objects.equals(authScheme, that.authScheme) &&
                Objects.equals(httpMethod, that.httpMethod) &&
                Objects.equals(urlPattern, that.urlPattern);
    }

    @Override
    public int hashCode() {

        return Objects.hash(throttlingPolicy, authScheme, httpMethod, urlPattern);
    }

    @Override
    public String toString() {

        return "URLMapping {" +
                ", throttlingPolicy ='" + throttlingPolicy + '\'' +
                ", authScheme ='" + authScheme + '\'' +
                ", httpMethod ='" + httpMethod + '\'' +
                ", urlPattern ='" + urlPattern + '\'' +
                '}';
    }
}
