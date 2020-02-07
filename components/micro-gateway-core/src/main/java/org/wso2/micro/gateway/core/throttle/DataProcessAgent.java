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

import org.ballerinalang.jvm.values.MapValue;

/**
 * This class is responsible for executing data processing logic. This class implements runnable interface and
 * need to execute using thread pool executor.
 */
public class DataProcessAgent implements Runnable {

    private ThrottleCounter throttleCounter;

    private String apiKey;
    private String appKey;
    private boolean stopOnQuota;
    private String subscriptionKey;
    private long appTierCount;
    private long appTierUnitTime;
    private String appTierTimeUnit;
    private long apiTierCount;
    private long apiTierUnitTime;
    private long subscriptionTierCount;
    private long subscriptionTierUnitTime;
    private String subscriptionTierTimeUnit;
    private String resourceKey;
    private long resourceTierCount;
    private long resourceTierUnitTime;
    private String resourceTierTimeUnit;
    private long timestamp;


    public DataProcessAgent() {

        throttleCounter = getDataPublisher();
    }

    /**
     * This method will use to set throttle data.
     */
    public void setDataReference(MapValue throttleData) {

        this.appKey = throttleData.getStringValue("appKey");
        this.appTierCount = throttleData.getIntValue("appTierCount");
        this.appTierUnitTime = throttleData.getIntValue("appTierUnitTime");
        this.appTierTimeUnit = throttleData.getStringValue("appTierTimeUnit");
        this.apiKey = throttleData.getStringValue("apiKey");
        //this.apiTierCount = apiTierCount;
        //this.apiTierUnitTime = apiTierUnitTime;
        this.subscriptionKey = throttleData.getStringValue("subscriptionKey");
        this.subscriptionTierCount = throttleData.getIntValue("subscriptionTierCount");
        this.subscriptionTierUnitTime = throttleData.getIntValue("subscriptionTierUnitTime");
        this.subscriptionTierTimeUnit = throttleData.getStringValue("subscriptionTierTimeUnit");
        this.resourceKey = throttleData.getStringValue("resourceKey");
        this.resourceTierCount = throttleData.getIntValue("resourceTierCount");
        this.resourceTierUnitTime = throttleData.getIntValue("resourceTierUnitTime");
        this.resourceTierTimeUnit = throttleData.getStringValue("resourceTierTimeUnit");
        this.stopOnQuota = throttleData.getBooleanValue("stopOnQuota");
        this.timestamp = throttleData.getIntValue("timestamp");
    }

    public void run() {
        throttleCounter.updateCounters(apiKey, appKey, stopOnQuota, subscriptionKey, appTierCount, appTierUnitTime,
                appTierTimeUnit, apiTierCount, apiTierUnitTime, subscriptionTierCount, subscriptionTierUnitTime,
                subscriptionTierTimeUnit, resourceKey, resourceTierCount, resourceTierUnitTime, resourceTierTimeUnit,
                timestamp);
    }

    private ThrottleCounter getDataPublisher() {
        return ThrottleDataReceiver.getThrottleCounter();
    }
}
