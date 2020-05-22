/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.core.globalthrottle.databridge.publisher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.jvm.values.api.BMap;

/**
 * This class holds the configurations related to binary data publisher.
 */
public class PublisherConfiguration {
    private static final Logger log = LogManager.getLogger(PublisherConfiguration.class);

    private int maxIdleDataPublishingAgents = 250;
    private int initIdleObjectDataPublishingAgents = 250;

    private int publisherThreadPoolCoreSize = 200;
    private int publisherThreadPoolMaximumSize = 1000;
    private int publisherThreadPoolKeepAliveTime = 20;

    private String receiverUrlGroup = "tcp://localhost:9611";
    private String authUrlGroup = "ssl://localhost:9711";
    private String userName = "admin";
    private String password = "admin";

    private PublisherConfiguration() {
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
        return password;
    }

    private static class InnerPublisherConfiguration {
        private static final PublisherConfiguration instance = new PublisherConfiguration();
    }

    public static PublisherConfiguration getInstance() {
        return InnerPublisherConfiguration.instance;
    }

    public void setConfiguration(BMap<String, Object> publisherConfiguration) {
        this.receiverUrlGroup = String.valueOf(publisherConfiguration.get(DataPublisherConstants.RECEIVER_URL_GROUP));
        this.authUrlGroup = String.valueOf(publisherConfiguration.get(DataPublisherConstants.AUTH_URL_GROUP));
        this.userName = String.valueOf(publisherConfiguration.get(DataPublisherConstants.USERNAME));
        this.password = String.valueOf(publisherConfiguration.get(DataPublisherConstants.PASSWORD));
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
