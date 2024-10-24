/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apimgt.gateway.cli.model.rest.apim4x;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * DeploymentDescriptorDto represents deployment descriptor file created
 * inside the zip file of API zip files.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentDescriptorDto {
    private Set<ApiProjectDto> deployments = new HashSet<>();

    public Set<ApiProjectDto> getDeployments() {
        return deployments;
    }

    public void setDeployments(Set<ApiProjectDto> deployments) {
        this.deployments = deployments;
    }
}
