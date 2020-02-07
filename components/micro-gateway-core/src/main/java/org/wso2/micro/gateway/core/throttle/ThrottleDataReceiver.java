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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Throttle data receiver class accepts all the request events and submit the throttle data to a thread pool to
 * calculate throttle counters against each unique throttle key.
 */
public class ThrottleDataReceiver {
    private static ThrottleDataCleanUpTask throttleDataCleanUpTask;

    private static final Logger log = LoggerFactory.getLogger("ballerina");

    public static ThrottleCounter getThrottleCounter() {
        return throttleCounter;
    }

    private static volatile ThrottleCounter throttleCounter = null;

    private static ExecutorService executor;

    /**
     * This method will initialize throttle data counters. Inside this we will start executor and initialize data
     * counter which we used to maintain throttle count against each unique keys.
     */
    public static void initThrottleDataReceiver(int processPoolMaxIdle, int processPoolInitIdleCapacity,
            int processThreadPoolCoreSize, int processThreadPoolMaximumSize, int processThreadPoolKeepAliveTime,
            int throttleFrequency) {
        ThrottleConfigHolder.getInstance()
                .setData(processPoolMaxIdle, processPoolInitIdleCapacity, processThreadPoolCoreSize,
                        processThreadPoolMaximumSize, processThreadPoolKeepAliveTime, throttleFrequency);
        initThrottleExecutors();
        throttleDataCleanUpTask = new ThrottleDataCleanUpTask();
    }

    private static void initThrottleExecutors() {
        ThrottleConfigHolder throttleConfigHolder = ThrottleConfigHolder.getInstance();
        executor = new ThreadPoolExecutor(throttleConfigHolder.getProcessThreadPoolCoreSize(),
                throttleConfigHolder.getProcessThreadPoolMaximumSize(),
                throttleConfigHolder.getProcessThreadPoolKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>() {
                });
        throttleCounter = new ThrottleCounter();
    }

    /**
     * This method used to pass throttle data and let it run within separate thread.
     */
    public static void processNonThrottledEvent(MapValue throttleEvent) {
        //check for a dto
        try {
            DataProcessAgent agent = new DataProcessAgent();
            agent.setDataReference(throttleEvent);
            executor.execute(agent);
        } catch (Exception e) {
            log.error("Error while processing throttling event", e);
        }
    }



    public static boolean isResourceThrottled(String resourceKey) {
        return ThrottleCounter.isResourceThrottled(resourceKey);
    }

    public static boolean isAppLevelThrottled(String appKey) {
        return ThrottleCounter.isAppLevelThrottled(appKey);
    }

    public static boolean isSubcriptionLevelThrottled(String subscriptionKey) {
        return ThrottleCounter.isSubscriptionLevelThrottled(subscriptionKey);
    }

    public static ThrottleDataCleanUpTask getThrottleDataCleanUpTask() {
        return throttleDataCleanUpTask;
    }
}

