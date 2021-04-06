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

import org.wso2.choreo.connect.enforcer.constants.Constants;

import java.util.Properties;

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

    public ThrottleAgentConfigDto getThrottleAgent() {
        return throttleAgent;
    }

    public void setThrottleAgent(ThrottleAgentConfigDto throttleAgent) {
        this.throttleAgent = throttleAgent;
    }

    /**
     * Build jms listener configuration property bag. This is done this way to
     * get the properties after resolving env variables. if we create the property
     * before resolving env vars, we lose the env value replacement.
     *
     * @return jms connection parameters as {@link Properties}
     */
    public Properties buildListenerProperties() {
        Properties props = new Properties();
        props.put(Constants.PROP_INIT_CONTEXT_FACTORY, this.jmsConnectionInitialContextFactory);
        props.put(Constants.PROP_CON_FACTORY, this.jmsConnectionProviderUrl);
        props.put(Constants.PROP_DESTINATION_TYPE, Constants.DEFAULT_DESTINATION_TYPE);
        props.put(Constants.PROP_CON_FACTORY_JNDI_NAME, Constants.DEFAULT_CON_FACTORY_JNDI_NAME);
        return props;
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
}
