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
import org.apache.commons.lang3.StringUtils;

/**
 * EnvironmentDto represents a gateway environment.
 * name: Name of the environment.
 * vhost: Deployed Vhost.
 * deployedTimeStamp: API deployed time.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentDto {
    private String name;
    private String vhost;

    private long deployedTimeStamp;

    private String type;

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public long getDeployedTimeStamp() {
        return deployedTimeStamp;
    }

    public void setDeployedTimeStamp(long deployedTimeStamp) {
        this.deployedTimeStamp = deployedTimeStamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof EnvironmentDto)) {
            return false;
        }

        EnvironmentDto environmentDto = (EnvironmentDto) obj;
        // check only name (environment name or label name)
        return StringUtils.equals(this.name, environmentDto.name);
    }

    @Override
    public int hashCode() {
        return (name == null ? 0 : name.hashCode());
    }
}
