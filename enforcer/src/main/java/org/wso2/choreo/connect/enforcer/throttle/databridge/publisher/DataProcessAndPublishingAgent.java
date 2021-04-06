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

package org.wso2.choreo.connect.enforcer.throttle.databridge.publisher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.DataPublisher;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.ThrottleEventConstants;

import java.util.Map;

/**
 * This class is responsible for executing data publishing logic. This class implements runnable interface and
 * need to execute using thread pool executor. Primary task of this class it is accept message context as parameter
 * and perform time consuming data extraction and publish event to data publisher. Having data extraction and
 * transformation logic in this class will help to reduce overhead added to main message flow.
 */
public class DataProcessAndPublishingAgent implements Runnable {
    private static final Logger log = LogManager.getLogger(DataProcessAndPublishingAgent.class);

    private static String streamID = "org.wso2.throttle.request.stream:1.0.0";
    private DataPublisher dataPublisher;

    String messageId;
    String applicationLevelThrottleKey;
    String applicationLevelTier;
    String apiLevelThrottleKey;
    String apiLevelTier;
    String subscriptionLevelThrottleKey;
    String subscriptionLevelTier;
    String resourceLevelThrottleKey;
    String authorizedUser;
    String resourceLevelTier;
    String apiContext;
    String apiVersion;
    String appTenant;
    String apiTenant;
    String apiName;
    String appId;
    String properties;

    public DataProcessAndPublishingAgent() {
        dataPublisher = getDataPublisher();
    }

    /**
     * This method will clean data references. This method should call whenever we return data process and publish
     * agent back to pool. Every time when we add new property we need to implement cleaning logic as well.
     */
    public void clearDataReference() {
        this.messageId = null;
        this.applicationLevelThrottleKey = null;
        this.applicationLevelTier = null;
        this.apiLevelThrottleKey = null;
        this.apiLevelTier = null;
        this.subscriptionLevelThrottleKey = null;
        this.subscriptionLevelTier = null;
        this.resourceLevelThrottleKey = null;
        this.resourceLevelTier = null;
        this.authorizedUser = null;
        this.apiContext = null;
        this.apiVersion = null;
        this.appTenant = null;
        this.apiTenant = null;
        this.appId = null;
        this.apiName = null;
    }

    /**
     * This method will use to set message context.
     */
    public void setDataReference(Map<String, String> throttleEvent) {
        this.messageId = throttleEvent.get(ThrottleEventConstants.MESSAGE_ID);
        this.applicationLevelThrottleKey = throttleEvent.get(ThrottleEventConstants.APP_KEY);
        this.applicationLevelTier = throttleEvent.get(ThrottleEventConstants.APP_TIER);
        this.apiLevelThrottleKey = throttleEvent.get(ThrottleEventConstants.API_KEY);
        this.apiLevelTier = throttleEvent.get(ThrottleEventConstants.API_TIER);
        this.subscriptionLevelThrottleKey = throttleEvent.get(ThrottleEventConstants.SUBSCRIPTION_KEY);
        this.subscriptionLevelTier = throttleEvent.get(ThrottleEventConstants.SUBSCRIPTION_TIER);
        this.resourceLevelThrottleKey = throttleEvent.get(ThrottleEventConstants.RESOURCE_KEY);
        this.resourceLevelTier = throttleEvent.get(ThrottleEventConstants.RESOURCE_TIER);
        this.authorizedUser = throttleEvent.get(ThrottleEventConstants.USER_ID);
        this.apiContext = throttleEvent.get(ThrottleEventConstants.API_CONTEXT);
        this.apiVersion = throttleEvent.get(ThrottleEventConstants.API_VERSION);
        this.appTenant = throttleEvent.get(ThrottleEventConstants.APP_TENANT);
        this.apiTenant = throttleEvent.get(ThrottleEventConstants.API_TENANT);
        this.appId = throttleEvent.get(ThrottleEventConstants.APP_ID);
        this.apiName = throttleEvent.get(ThrottleEventConstants.API_NAME);
        this.properties = throttleEvent.get(ThrottleEventConstants.PROPERTIES);
    }

    public void run() {

        Object[] objects = new Object[]{messageId,
                this.applicationLevelThrottleKey, this.applicationLevelTier,
                this.apiLevelThrottleKey, this.apiLevelTier,
                this.subscriptionLevelThrottleKey, this.subscriptionLevelTier,
                this.resourceLevelThrottleKey, this.resourceLevelTier,
                this.authorizedUser, this.apiContext, this.apiVersion,
                this.appTenant, this.apiTenant, this.appId, this.apiName, properties};
        Event event = new Event(streamID, System.currentTimeMillis(), null, null, objects);
        dataPublisher.tryPublish(event);
    }

    protected DataPublisher getDataPublisher() {
        return ThrottleDataPublisher.getDataPublisher();
    }
}
