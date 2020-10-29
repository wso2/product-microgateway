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

package org.wso2.micro.gateway.filter.core.dto;

import java.util.Properties;

/**
 * Holds the configurations related to connecting with APIM event hub node
 */
public class EventHubConfigurationDto {
    private boolean enabled;
    private String serviceUrl;
    private int initDelay = 0;
    private String username;
    private char[] password;
    private EventHubReceiverConfiguration eventHubReceiverConfiguration;
    private EventHubPublisherConfiguration eventHubPublisherConfiguration;

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(boolean enabled) {

        this.enabled = enabled;
    }

    public String getServiceUrl() {

        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {

        this.serviceUrl = serviceUrl;
    }

    public int getInitDelay() {

        return initDelay;
    }

    public void setInitDelay(int initDelay) {

        this.initDelay = initDelay;
    }

    public EventHubReceiverConfiguration getEventHubReceiverConfiguration() {

        return eventHubReceiverConfiguration;
    }

    public void setEventHubReceiverConfiguration(EventHubReceiverConfiguration eventHubReceiverConfiguration) {

        this.eventHubReceiverConfiguration = eventHubReceiverConfiguration;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public void setPassword(char[] password) {

        this.password = password;
    }

    public String getPassword() {

        return String.valueOf(password);
    }

    public EventHubPublisherConfiguration getEventHubPublisherConfiguration() {

        return eventHubPublisherConfiguration;
    }

    public void setEventHubPublisherConfiguration(EventHubPublisherConfiguration eventHubPublisherConfiguration) {

        this.eventHubPublisherConfiguration = eventHubPublisherConfiguration;
    }

    /**
     * Holds the configurations related to APIM event hub receiver.
     */
    public static class EventHubReceiverConfiguration {
        private Properties jmsConnectionParameters = new Properties();

        public Properties getJmsConnectionParameters() {

            return jmsConnectionParameters;
        }

        public void setJmsConnectionParameters(Properties jmsConnectionParameters) {

            this.jmsConnectionParameters = jmsConnectionParameters;
        }
    }

    /**
     * Holds the configurations related to APIM event hub publisher.
     */
    public static class EventHubPublisherConfiguration {

        private String type = "Binary";
        private String receiverUrlGroup = "tcp://localhost:9611";
        private String authUrlGroup = "ssl://localhost:9711";

        public String getType() {

            return type;
        }

        public void setType(String type) {

            this.type = type;
        }

        public String getReceiverUrlGroup() {

            return receiverUrlGroup;
        }

        public void setReceiverUrlGroup(String receiverUrlGroup) {

            this.receiverUrlGroup = receiverUrlGroup;
        }

        public String getAuthUrlGroup() {

            return authUrlGroup;
        }

        public void setAuthUrlGroup(String authUrlGroup) {

            this.authUrlGroup = authUrlGroup;
        }
    }

}

