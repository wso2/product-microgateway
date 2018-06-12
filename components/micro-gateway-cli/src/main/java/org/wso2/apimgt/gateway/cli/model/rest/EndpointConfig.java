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

import java.util.ArrayList;
import java.util.List;

public class EndpointConfig {
    private String endpointType;
    private List<Endpoint> prodEndpoints = null;
    private List<Endpoint> prodFailoverEndpoints = null;
    private List<Endpoint> sandEndpoints = null;
    private List<Endpoint> sandFailoverEndpoints = null;

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public List<Endpoint> getProdEndpoints() {
        return prodEndpoints;
    }

    public void setProdEndpoints(List<Endpoint> prodEndpoints) {
        this.prodEndpoints = prodEndpoints;
    }

    public List<Endpoint> getProdFailoverEndpoints() {
        return prodFailoverEndpoints;
    }

    public void setProdFailoverEndpoints(List<Endpoint> prodFailoverEndpoints) {
        this.prodFailoverEndpoints = prodFailoverEndpoints;
    }

    public List<Endpoint> getSandEndpoints() {
        return sandEndpoints;
    }

    public void setSandEndpoints(List<Endpoint> sandEndpoints) {
        this.sandEndpoints = sandEndpoints;
    }

    public List<Endpoint> getSandFailoverEndpoints() {
        return sandFailoverEndpoints;
    }

    public void setSandFailoverEndpoints(List<Endpoint> sandFailoverEndpoints) {
        this.sandFailoverEndpoints = sandFailoverEndpoints;
    }

    public void addSandEndpoint(Endpoint endpoint) {
        if (sandEndpoints == null) {
            sandEndpoints = new ArrayList<>();
        }
        sandEndpoints.add(endpoint);
    }

    public void addProdEndpoint(Endpoint endpoint) {
        if (prodEndpoints == null) {
            prodEndpoints = new ArrayList<>();
        }
        prodEndpoints.add(endpoint);
    }

    public void addProdFailoverEndpoint(Endpoint endpoint) {
        if (prodFailoverEndpoints == null) {
            prodFailoverEndpoints = new ArrayList<>();
        }
        prodFailoverEndpoints.add(endpoint);
    }

    public void addSandFailoverEndpoint(Endpoint endpoint) {
        if (sandFailoverEndpoints == null) {
            sandFailoverEndpoints = new ArrayList<>();
        }
        sandFailoverEndpoints.add(endpoint);
    }
}
