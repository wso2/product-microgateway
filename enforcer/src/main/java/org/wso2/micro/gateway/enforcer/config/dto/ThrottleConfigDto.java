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

package org.wso2.micro.gateway.enforcer.config.dto;

/**
 * Throttling configuration model.
 */
public class ThrottleConfigDto {
    private boolean isGlobalPublishingEnabled;
    private boolean isHeaderConditionsEnabled;
    private boolean isQueryConditionsEnabled;
    private boolean isJwtClaimConditionsEnabled;
    private String jmsConnectionInitialContextFactory;
    private String jmsConnectionProviderUrl;
    private String jmsConnectionUsername;
    private String jmsConnectionPassword;
    private ThrottleAgentConfigDto throttleAgent;

    public boolean isGlobalPublishingEnabled() {
        return isGlobalPublishingEnabled;
    }

    public void setGlobalPublishingEnabled(boolean globalPublishingEnabled) {
        isGlobalPublishingEnabled = globalPublishingEnabled;
    }

    public boolean isHeaderConditionsEnabled() {
        return isHeaderConditionsEnabled;
    }

    public void setHeaderConditionsEnabled(boolean headerConditionsEnabled) {
        isHeaderConditionsEnabled = headerConditionsEnabled;
    }

    public boolean isQueryConditionsEnabled() {
        return isQueryConditionsEnabled;
    }

    public void setQueryConditionsEnabled(boolean queryConditionsEnabled) {
        isQueryConditionsEnabled = queryConditionsEnabled;
    }

    public boolean isJwtClaimConditionsEnabled() {
        return isJwtClaimConditionsEnabled;
    }

    public void setJwtClaimConditionsEnabled(boolean jwtClaimConditionsEnabled) {
        isJwtClaimConditionsEnabled = jwtClaimConditionsEnabled;
    }

    public String getJmsConnectionInitialContextFactory() {
        return jmsConnectionInitialContextFactory;
    }

    public void setJmsConnectionInitialContextFactory(String jmsConnectionInitialContextFactory) {
        this.jmsConnectionInitialContextFactory = jmsConnectionInitialContextFactory;
    }

    public String getJmsConnectionProviderUrl() {
        return jmsConnectionProviderUrl;
    }

    public void setJmsConnectionProviderUrl(String jmsConnectionProviderUrl) {
        this.jmsConnectionProviderUrl = jmsConnectionProviderUrl;
    }

    public String getJmsConnectionUsername() {
        return jmsConnectionUsername;
    }

    public void setJmsConnectionUsername(String jmsConnectionUsername) {
        this.jmsConnectionUsername = jmsConnectionUsername;
    }

    public String getJmsConnectionPassword() {
        return jmsConnectionPassword;
    }

    public void setJmsConnectionPassword(String jmsConnectionPassword) {
        this.jmsConnectionPassword = jmsConnectionPassword;
    }

    public ThrottleAgentConfigDto getThrottleAgent() {
        return throttleAgent;
    }

    public void setThrottleAgent(ThrottleAgentConfigDto throttleAgent) {
        this.throttleAgent = throttleAgent;
    }
}
