/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.config.dto;

import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.AgentConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * This contains throttle configurations.
 */
public class ThrottleAgentConfigDto {
    String username;
    String password;
    List<ThrottleURLGroupDto> urlGroup = new ArrayList<>();
    ThrottlePublisherConfigDto publisher;
    AgentConfiguration agent;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<ThrottleURLGroupDto> getUrlGroup() {
        return urlGroup;
    }

    public void setUrlGroup(List<ThrottleURLGroupDto> urlGroup) {
        this.urlGroup = urlGroup;
    }

    public ThrottlePublisherConfigDto getPublisher() {
        return publisher;
    }

    public void setPublisher(ThrottlePublisherConfigDto publisher) {
        this.publisher = publisher;
    }

    public AgentConfiguration getAgent() {
        return agent;
    }

    public void setAgent(AgentConfiguration agent) {
        this.agent = agent;
    }
}
