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

//import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for maintaining the throttle counters for various throttle policies.
 */
public class ThrottleCounter {
    private static final Logger log = LoggerFactory.getLogger(ThrottleCounter.class);

    private static final Map<String, ThrottleData> resourceLevelCounter = new ConcurrentHashMap<>();
    private static final Map<String, ThrottleData> applicationLevelCounter = new ConcurrentHashMap<>();
    private static final Map<String, ThrottleData> subscriptionLevelCounter = new ConcurrentHashMap<>();

    public void updateCounters(String apiKey, String appKey, boolean stopOnQuota, String subscriptionKey,
            long appTierCount, int appTierUnitTime, String appTierTimeUnit, long apiTierCount, int apiTierUnitTime,
            long subscriptionTierCount, int subcriptionTierUnitTime, String subcriptionTierTimeUnit, String resourceKey,
            long resourceTierCount, int resourceTierUnitTime, String resourceTierTimeUnit, long timestamp,
            String appTier, String apiTier, String resourceTier, String subscriptionTier) {
        String resourceMapKey = resourceTier + ":" + resourceKey;
        String applicationMapKey = appTier + ":" + appKey;
        String subscriptionMapKey = subscriptionTier + ":" + subscriptionKey;
        updateMapCounters(resourceLevelCounter, resourceMapKey, stopOnQuota, resourceTierCount,
                resourceTierUnitTime, resourceTierTimeUnit, timestamp,
                ThrottleData.ThrottleType.RESOURCE);
        updateMapCounters(applicationLevelCounter, applicationMapKey, stopOnQuota, appTierCount,
                appTierUnitTime, appTierTimeUnit, timestamp, ThrottleData.ThrottleType.APP);
        updateMapCounters(subscriptionLevelCounter, subscriptionMapKey, stopOnQuota, subscriptionTierCount,
                subcriptionTierUnitTime, subcriptionTierTimeUnit, timestamp,
                ThrottleData.ThrottleType.SUBSCRIPTION);
    }

    private void updateMapCounters(Map<String, ThrottleData> counterMap, String throttleKey, boolean stopOnQuota,
            long limit, long unitTime, String timeUnit, long timestamp, ThrottleData.ThrottleType throttleType) {
        //PrintStream out = System.out;

        ThrottleData existingThrottleData = counterMap.computeIfPresent(throttleKey, (key, throttleData) -> {
            //out.println(" ######### throttleKey : " + key);
            //out.println(" ######### count : " + throttleData.getCount());
            //log.info("$$$$$$$$$$$$$$$$$$$$$$$$$");
            //out.println("##### timestamp : " + timestamp);
            //out.println("######## window start time :" + throttleData.getWindowStartTime());
            if (limit > 0 && throttleData.getCount().incrementAndGet() > limit) {
                throttleData.setThrottled(true);
            } else {
                throttleData.setThrottled(false);
            }
            if (timestamp > throttleData.getWindowStartTime() + throttleData.getUnitTime()) {
                throttleData.getCount().set(0);
                long startTime = timestamp - (timestamp % getTimeInMilliSeconds(1, timeUnit));
                throttleData.setWindowStartTime(startTime);
                //out.println("$$$$$$$$$$$$$$ window resetted. New start time" + startTime);
            }
            return throttleData;
        });
        if (existingThrottleData == null) {
            counterMap.computeIfAbsent(throttleKey, key -> {
                ThrottleData throttleData = new ThrottleData();
                long startTime = timestamp - (timestamp % getTimeInMilliSeconds(1, timeUnit));
                throttleData.setWindowStartTime(startTime);
                throttleData.setStopOnQuota(stopOnQuota);
                throttleData.setUnitTime(getTimeInMilliSeconds(unitTime, timeUnit));
                throttleData.setThrottleType(throttleType);
                ThrottleDataReceiver.getThrottleDataCleanUpTask().addThrottleData(throttleData);
                return throttleData;
            });
        }
    }

    static boolean isResourceThrottled(String resourceKey) {
        return isRequestThrottled(resourceLevelCounter, resourceKey);
    }

    static boolean isAppLevelThrottled(String appKey) {
        return isRequestThrottled(applicationLevelCounter, appKey);
    }

    static boolean isSubscriptionLevelThrottled(String subscriptionKey) {
        return isRequestThrottled(subscriptionLevelCounter, subscriptionKey);
    }

    static void removeFromResourceCounterMap(String key) {
        resourceLevelCounter.remove(key);
    }

    static void removeFromApplicationCounterMap(String key) {
        applicationLevelCounter.remove(key);
    }

    static void removeFromSubscriptionCounterMap(String key) {
        subscriptionLevelCounter.remove(key);
    }

    private static boolean isRequestThrottled(Map<String, ThrottleData> counterMap, String throttleKey) {
        if (counterMap.containsKey(throttleKey)) {
            long currentTime = System.currentTimeMillis();
            ThrottleData throttleData = counterMap.get(throttleKey);
            if (currentTime > throttleData.getWindowStartTime() + throttleData.getUnitTime()) {
                return false;
            }
            return counterMap.get(throttleKey).isThrottled();
        }
        return false;
    }

    private long getTimeInMilliSeconds(long unitTime, String timeUnit) {
        long milliSeconds;
        if ("min".equalsIgnoreCase(timeUnit)) {
            milliSeconds = TimeUnit.MINUTES.toMillis(unitTime);
        } else if ("hour".equalsIgnoreCase(timeUnit)) {
            milliSeconds = TimeUnit.HOURS.toMillis(unitTime);
        } else if ("day".equalsIgnoreCase(timeUnit)) {
            milliSeconds = TimeUnit.DAYS.toMillis(unitTime);
        } else if ("week".equalsIgnoreCase(timeUnit)) {
            milliSeconds = 7 * TimeUnit.DAYS.toMillis(unitTime);
        } else if ("month".equalsIgnoreCase(timeUnit)) {
            milliSeconds = 30 * TimeUnit.DAYS.toMillis(unitTime);
        } else if ("year".equalsIgnoreCase(timeUnit)) {
            milliSeconds = 365 * TimeUnit.DAYS.toMillis(unitTime);
        } else {
            throw new RuntimeException("Unsupported time unit provided");
        }
        return milliSeconds;
    }
}
