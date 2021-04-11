/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.wso2.choreo.connect.enforcer.config.dto;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.throttle.databridge.publisher.DataPublisherConstants;

import java.util.Map;

/**
 * This class holds the configurations related to binary data publisher.
 */
public class ThrottlePublisherConfigDto {
    private static final Logger log = LogManager.getLogger(ThrottlePublisherConfigDto.class);

    private int maxIdleDataPublishingAgents;
    private int initIdleObjectDataPublishingAgents;
    private int publisherThreadPoolCoreSize;
    private int publisherThreadPoolMaximumSize;
    private int publisherThreadPoolKeepAliveTime;

    private String receiverUrlGroup;
    private String authUrlGroup;
    private String userName;
    private char[] password;

    public void setMaxIdleDataPublishingAgents(int maxIdleDataPublishingAgents) {
        this.maxIdleDataPublishingAgents = maxIdleDataPublishingAgents;
    }

    public void setInitIdleObjectDataPublishingAgents(int initIdleObjectDataPublishingAgents) {
        this.initIdleObjectDataPublishingAgents = initIdleObjectDataPublishingAgents;
    }

    public void setPublisherThreadPoolCoreSize(int publisherThreadPoolCoreSize) {
        this.publisherThreadPoolCoreSize = publisherThreadPoolCoreSize;
    }

    public void setPublisherThreadPoolMaximumSize(int publisherThreadPoolMaximumSize) {
        this.publisherThreadPoolMaximumSize = publisherThreadPoolMaximumSize;
    }

    public void setPublisherThreadPoolKeepAliveTime(int publisherThreadPoolKeepAliveTime) {
        this.publisherThreadPoolKeepAliveTime = publisherThreadPoolKeepAliveTime;
    }

    public void setAuthUrlGroup(String authUrlGroup) {
        this.authUrlGroup = authUrlGroup;
    }

    public void setReceiverUrlGroup(String receiverUrlGroup) {
        this.receiverUrlGroup = receiverUrlGroup;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    public int getMaxIdleDataPublishingAgents() {
        return maxIdleDataPublishingAgents;
    }

    public int getInitIdleObjectDataPublishingAgents() {
        return initIdleObjectDataPublishingAgents;
    }

    public int getPublisherThreadPoolCoreSize() {
        return publisherThreadPoolCoreSize;
    }

    public int getPublisherThreadPoolMaximumSize() {
        return publisherThreadPoolMaximumSize;
    }

    public int getPublisherThreadPoolKeepAliveTime() {
        return publisherThreadPoolKeepAliveTime;
    }

    public String getReceiverUrlGroup() {
        return receiverUrlGroup;
    }

    public String getAuthUrlGroup() {
        return authUrlGroup;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return String.valueOf(password);
    }

    public void setConfiguration(Map<String, Object> publisherConfiguration) {
        this.receiverUrlGroup = String.valueOf(publisherConfiguration.get(DataPublisherConstants.RECEIVER_URL_GROUP));
        this.authUrlGroup = String.valueOf(publisherConfiguration.get(DataPublisherConstants.AUTH_URL_GROUP));
        this.userName = String.valueOf(publisherConfiguration.get(DataPublisherConstants.USERNAME));
        this.password = String.valueOf(publisherConfiguration.get(DataPublisherConstants.PASSWORD)).toCharArray();
        try {
            this.maxIdleDataPublishingAgents =
                    Math.toIntExact((long) publisherConfiguration.get(DataPublisherConstants.MAX_IDLE));
            this.initIdleObjectDataPublishingAgents =
                    Math.toIntExact((long) publisherConfiguration.get(DataPublisherConstants.INIT_IDLE_CAPACITY));
            this.publisherThreadPoolCoreSize =
                    Math.toIntExact((long) publisherConfiguration.get(DataPublisherConstants.CORE_POOL_SIZE));
            this.publisherThreadPoolMaximumSize =
                    (Math.toIntExact((long) publisherConfiguration.get(DataPublisherConstants.MAX_POOL_SIZE)));
            this.publisherThreadPoolKeepAliveTime = Math.toIntExact((long) publisherConfiguration
                    .get(DataPublisherConstants.KEEP_ALIVE_TIME));
        } catch (ArithmeticException e) {
            log.error("Error while processing the publisher configuration.", e);
        }
    }
}
