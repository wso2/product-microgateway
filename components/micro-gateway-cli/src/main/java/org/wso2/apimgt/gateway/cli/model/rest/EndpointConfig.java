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

public class EndpointConfig {
    private String endpointType;
    private EndpointList prodEndpoints = null;
    private EndpointList prodFailoverEndpoints = null;
    private EndpointList sandEndpoints = null;
    private EndpointList sandFailoverEndpoints = null;

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public EndpointList getProdEndpoints() {
        return prodEndpoints;
    }

    public void setProdEndpoints(EndpointList prodEndpoints) {
        this.prodEndpoints = prodEndpoints;
    }

    public EndpointList getProdFailoverEndpoints() {
        return prodFailoverEndpoints;
    }

    public void setProdFailoverEndpoints(EndpointList prodFailoverEndpoints) {
        this.prodFailoverEndpoints = prodFailoverEndpoints;
    }

    public EndpointList getSandEndpoints() {
        return sandEndpoints;
    }

    public void setSandEndpoints(EndpointList sandEndpoints) {
        this.sandEndpoints = sandEndpoints;
    }

    public EndpointList getSandFailoverEndpoints() {
        return sandFailoverEndpoints;
    }

    public void setSandFailoverEndpoints(EndpointList sandFailoverEndpoints) {
        this.sandFailoverEndpoints = sandFailoverEndpoints;
    }

    public void addSandEndpoint(Endpoint endpoint) {
        if (sandEndpoints == null) {
            sandEndpoints = new EndpointList(EndpointUrlTypeEnum.SAND);
        }
        sandEndpoints.addEndpoint(endpoint);
    }

    public void addProdEndpoint(Endpoint endpoint) {
        if (prodEndpoints == null) {
            prodEndpoints = new EndpointList(EndpointUrlTypeEnum.PROD);
        }
        prodEndpoints.addEndpoint(endpoint);
    }

    public void addProdFailoverEndpoint(Endpoint endpoint) {
        if (prodFailoverEndpoints == null) {
            prodFailoverEndpoints = new EndpointList(EndpointUrlTypeEnum.PROD);
        }
        prodFailoverEndpoints.addEndpoint(endpoint);
    }

    public void addSandFailoverEndpoint(Endpoint endpoint) {
        if (sandFailoverEndpoints == null) {
            sandFailoverEndpoints = new EndpointList(EndpointUrlTypeEnum.SAND);
        }
        sandFailoverEndpoints.addEndpoint(endpoint);
    }
}
