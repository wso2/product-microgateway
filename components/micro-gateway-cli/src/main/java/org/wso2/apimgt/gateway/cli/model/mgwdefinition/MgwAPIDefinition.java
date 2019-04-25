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
package org.wso2.apimgt.gateway.cli.model.mgwdefinition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

/**
 * This class represents the DTO for Single API in Microgateway Definition.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MgwAPIDefinition {
    private String title;
    private String version;
    private EndpointListRouteDTO prodEpList;
    private EndpointListRouteDTO sandEpList;
    private String requestInterceptor;
    private String responseInterceptor;
    private MgwPathsDefinition pathsDefinition;
    private String security; //todo: bring enum
    private APICorsConfigurationDTO corsConfiguration;
    //to identify whether it has been used
    private boolean isDefinitionUsed = false;

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("production_endpoint")
    public EndpointListRouteDTO getProdEpList() {
        return prodEpList;
    }

    public void setProdEpList(EndpointListRouteDTO prodEpList) {
        this.prodEpList = prodEpList;
    }

    @JsonProperty("sandbox_endpoint")
    public EndpointListRouteDTO getSandEpList() {
        return sandEpList;
    }

    public void setSandEpList(EndpointListRouteDTO sandEpList) {
        this.sandEpList = sandEpList;
    }

    @JsonProperty("security")
    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    @JsonProperty("cors")
    public APICorsConfigurationDTO getCorsConfiguration() {
        return corsConfiguration;
    }

    public void setCorsConfiguration(APICorsConfigurationDTO corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    @JsonProperty("resources")
    public MgwPathsDefinition getPathsDefinition() {
        return pathsDefinition;
    }

    public void setPathsDefinition(MgwPathsDefinition pathsDefinition) {
        this.pathsDefinition = pathsDefinition;
    }

    @JsonProperty("request-interceptor")
    public String getRequestInterceptor() {
        return requestInterceptor;
    }

    public void setRequestInterceptor(String requestInterceptor) {
        this.requestInterceptor = requestInterceptor;
    }

    @JsonProperty("response-interceptor")
    public String getResponseInterceptor() {
        return responseInterceptor;
    }

    public void setResponseInterceptor(String responseInterceptor) {
        this.responseInterceptor = responseInterceptor;
    }

    /**
     * Identify if the API information used or not.
     *
     * @return true if the API information is used inside
     */
    public boolean getIsUsed(){
        return isUsed;
    }

    /**
     * Set the IsUsed if the API information is used
     *
     * @param flag true if the API information is used
     */
    public void setIsUsed(boolean flag){
        isUsed = flag;
    }
}
