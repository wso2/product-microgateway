/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for executing data processing logic. This class implements runnable interface and
 * need to execute using thread pool executor.
 */
public class DataProcessAgent implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DataProcessAgent.class);

    private static String streamID = "org.wso2.throttle.request.stream:1.0.0";
    private ThrottleCounter throttleCounter;

    private String apiKey;
    private String appKey;
    private boolean stopOnQuota;
    private String subscriptionKey;
    private long appTierCount;
    private int appTierUnitTime;
    private String appTierTimeUnit;
    private long apiTierCount;
    private int apiTierUnitTime;
    private long subscriptionTierCount;
    private int subscriptionTierUnitTime;
    private String subscriptionTierTimeUnit;
    private String resourceKey;
    private long resourceTierCount;
    private int resourceTierUnitTime;
    private String resourceTierTimeUnit;
    private long timestamp;
    private String apiTier;
    private String appTier;
    private String resourceTier;
    private String subscriptionTier;


    public DataProcessAgent() {

        throttleCounter = getDataPublisher();
    }

    /**
     * This method will use to set throttle data.
     */
    public void setDataReference(String apiKey, String appKey, String stopOnQuota, String subscriptionKey,
            String appTierCount, String appTierUnitTime, String appTierTimeUnit, String subscriptionTierCount,
            String subscriptionTierUnitTime, String subscriptionTierTimeUnit, String resourceKey,
            String resourceTierCount, String resourceTierUnitTime, String resourceTierTimeUnit, String timestamp,
            String appTier, String apiTier, String resourceTier, String subscriptionTier) {

        this.appKey = appKey;
        this.appTierCount = Long.parseLong(appTierCount);
        this.appTierUnitTime = Integer.parseInt(appTierUnitTime);
        this.appTierTimeUnit = appTierTimeUnit;
        this.apiKey = apiKey;
        //this.apiTierCount = apiTierCount;
        //this.apiTierUnitTime = apiTierUnitTime;
        this.subscriptionKey = subscriptionKey;
        this.subscriptionTierCount = Long.parseLong(subscriptionTierCount);
        this.subscriptionTierUnitTime = Integer.parseInt(subscriptionTierUnitTime);
        this.subscriptionTierTimeUnit = subscriptionTierTimeUnit;
        this.resourceKey = resourceKey;
        this.resourceTierCount = Long.parseLong(resourceTierCount);
        this.resourceTierUnitTime = Integer.parseInt(resourceTierUnitTime);
        this.resourceTierTimeUnit = resourceTierTimeUnit;
        this.stopOnQuota = Boolean.parseBoolean(stopOnQuota);
        this.timestamp = Long.parseLong(timestamp);
        this.appTier = appTier;
        this.apiTier = apiTier;
        this.resourceTier = resourceTier;
        this.subscriptionTier = subscriptionTier;
    }

    /**
     * This method will clean data references. This method should call whenever we return data process and publish
     * agent back to pool. Every time when we add new property we need to implement cleaning logic as well.
     */
    public void clearDataReference() {
        this.appKey = null;
        this.appTierCount = 0;
        this.appTierUnitTime = 0;
        this.appTierTimeUnit = null;
        this.apiKey = null;
        this.apiTierCount = 0;
        this.apiTierUnitTime = 0;
        this.subscriptionKey = null;
        this.subscriptionTierCount = 0;
        this.subscriptionTierUnitTime = 0;
        this.subscriptionTierTimeUnit = null;
        this.resourceKey = null;
        this.resourceTierCount = 0;
        this.resourceTierUnitTime = 0;
        this.resourceTierTimeUnit = null;
        this.stopOnQuota = false;
        this.timestamp = 0;
        this.apiTier = null;
        this.appTier = null;
        this.resourceTier = null;
        this.subscriptionTier = null;
    }

    public void run() {
        throttleCounter.updateCounters(apiKey, appKey, stopOnQuota, subscriptionKey, appTierCount, appTierUnitTime,
                appTierTimeUnit, apiTierCount, apiTierUnitTime, subscriptionTierCount, subscriptionTierUnitTime,
                subscriptionTierTimeUnit, resourceKey, resourceTierCount, resourceTierUnitTime, resourceTierTimeUnit,
                timestamp, appTier, apiTier, resourceTier, subscriptionTier);
    }

    private ThrottleCounter getDataPublisher() {
        return ThrottleDataReceiver.getThrottleCounter();
    }
}
