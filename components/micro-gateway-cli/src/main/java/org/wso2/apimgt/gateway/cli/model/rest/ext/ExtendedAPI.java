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
package org.wso2.apimgt.gateway.cli.model.rest.ext;

import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.APIDetailedDTO;

import java.util.List;

/**
 * Data mapper object defining a MGW API using OpenApi definition.
 */
public class ExtendedAPI extends APIDetailedDTO {
    //API Level endpoint configuration
    private MgwEndpointConfigDTO endpointConfigRepresentation = null;
    //Basepath
    private String specificBasepath = null;
    //Security
    private String mgwApiSecurity = null;
    //Scopes
    private String mgwApiScope = null;
    //support apim application level security
    private List<String> apiSecurityByExtension = null;

    public MgwEndpointConfigDTO getEndpointConfigRepresentation() {
        return endpointConfigRepresentation;
    }

    public void setEndpointConfigRepresentation(MgwEndpointConfigDTO endpointConfigRepresentation) {
        this.endpointConfigRepresentation = endpointConfigRepresentation;
    }

    public String getSpecificBasepath() {
        return specificBasepath;
    }

    public void setSpecificBasepath(String specificBasepath) {
        this.specificBasepath = specificBasepath;
    }

    public String getMgwApiSecurity() {
        return mgwApiSecurity;
    }

    public void setMgwApiSecurity(String mgwApiSecurity) {
        this.mgwApiSecurity = mgwApiSecurity;
    }

    public void setMgwApiScope(String mgwApiScope) {
        this.mgwApiScope = mgwApiScope;
    }

    public String getMgwApiScope() {
        return mgwApiScope;
    }

    public void setAPISecurityByExtension(List<String> apiSecurityByExtension) {
        this.apiSecurityByExtension = apiSecurityByExtension;
    }

    public List<String> getAPISecurityByExtension() {
        return apiSecurityByExtension;
    }
}
