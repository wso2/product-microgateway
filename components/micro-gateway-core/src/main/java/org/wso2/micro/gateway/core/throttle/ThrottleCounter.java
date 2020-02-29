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
            long appTierCount, long appTierUnitTime, String appTierTimeUnit, long apiTierCount, long apiTierUnitTime,
            long subscriptionTierCount, long subscriptionTierUnitTime, String subcriptionTierTimeUnit,
            String resourceKey, long resourceTierCount, long resourceTierUnitTime, String resourceTierTimeUnit,
            long timestamp) {
        updateMapCounters(resourceLevelCounter, resourceKey, stopOnQuota, resourceTierCount, resourceTierUnitTime,
                resourceTierTimeUnit, timestamp, ThrottleData.ThrottleType.RESOURCE);
        updateMapCounters(applicationLevelCounter, appKey, stopOnQuota, appTierCount, appTierUnitTime, appTierTimeUnit,
                timestamp, ThrottleData.ThrottleType.APP);
        updateMapCounters(subscriptionLevelCounter, subscriptionKey, stopOnQuota, subscriptionTierCount,
                subscriptionTierUnitTime, subcriptionTierTimeUnit, timestamp, ThrottleData.ThrottleType.SUBSCRIPTION);
    }

    private void updateMapCounters(Map<String, ThrottleData> counterMap, String throttleKey, boolean stopOnQuota,
            long limit, long unitTime, String timeUnit, long timestamp, ThrottleData.ThrottleType throttleType) {
        ThrottleData existingThrottleData = counterMap.computeIfPresent(throttleKey, (key, throttleData) -> {
            if (limit > 0 && throttleData.getCount().incrementAndGet() >= limit) {
                throttleData.setThrottled(true);
            } else {
                throttleData.setThrottled(false);
            }
            if (timestamp > throttleData.getWindowStartTime() + throttleData.getUnitTime()) {
                throttleData.getCount().set(1);
                long startTime = timestamp - (timestamp % getTimeInMilliSeconds(1, timeUnit));
                throttleData.setWindowStartTime(startTime);
                throttleData.setThrottled(false);
            }
            if (log.isDebugEnabled()) {
                log.debug("Throttle count for the key '" + throttleKey + "' is " + throttleData.getCount());
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
                throttleData.getCount().set(0);
                throttleData.setThrottleKey(key);
                ThrottleDataReceiver.getThrottleDataCleanUpTask().addThrottleData(throttleData);
                if (log.isDebugEnabled()) {
                    log.debug("Throttle key inserted " + throttleKey);
                }
                return throttleData;
            });
            //There can be scenarios where the two threads stops at computeIfAbsent and one thread adds it to the map
            //and the second thread will go without incrementing the count. This additional computation is done to avoid
            // that scenario
            counterMap.computeIfPresent(throttleKey, (key, throttleData) -> {
                throttleData.getCount().incrementAndGet();
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
                counterMap.computeIfPresent(throttleKey, (key, throttleData1) -> {
                    throttleData1.setThrottled(false);
                    return throttleData1;
                });
                if (log.isDebugEnabled()) {
                    log.debug("Throttle window has expired. CurrentTime : " + currentTime + "\n Window start time : "
                            + throttleData.getWindowStartTime() + "\n Unit time : " + throttleData.getUnitTime());
                }
                return false;
            }
            return throttleData.isThrottled();
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
