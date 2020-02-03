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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Throttle data receiver class accepts all the request events and submit the throttle data to a thread pool to
 * calculate throttle counters against each unique throttle key.
 */
public class ThrottleDataReceiver {
    private static ThrottleDataPublisherPool dataPublisherPool;
    private static final ThrottleDataCleanUpTask throttleDataCleanUpTask = new ThrottleDataCleanUpTask();

    private static final Logger log = LoggerFactory.getLogger(ThrottleDataReceiver.class);

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
    }

    private static void initThrottleExecutors() {
        ThrottleConfigHolder throttleConfigHolder = ThrottleConfigHolder.getInstance();
        dataPublisherPool = ThrottleDataPublisherPool.getInstance();
        executor = new DataProcessThreadPoolExecutor(throttleConfigHolder.getProcessThreadPoolCoreSize(),
                throttleConfigHolder.getProcessThreadPoolMaximumSize(),
                throttleConfigHolder.getProcessThreadPoolKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>() {
                });
        throttleCounter = new ThrottleCounter();
    }

    /**
     * This method used to pass throttle data and let it run within separate thread.
     *
     */
    public static void processNonThrottledEvent(String apiKey, String appKey, String stopOnQuota,
            String subscriptionKey, String appTierCount, String appTierUnitTime, String appTierTimeUnit,
            String subscriptionTierCount, String subscriptionTierUnitTime, String subscriptionTierTimeUnit,
            String resourceKey, String resourceTierCount, String resourceTierUnitTime, String resourceTierTimeUnit,
            String timestamp, String appTier, String apiTier, String resourceTier, String subscriptionTier) {
        try {
            if (dataPublisherPool != null) {
                log.info("Throttle event recieved");
                DataProcessAgent agent = dataPublisherPool.get();
                agent.setDataReference(apiKey, appKey, stopOnQuota, subscriptionKey, appTierCount, appTierUnitTime,
                        appTierTimeUnit, subscriptionTierCount, subscriptionTierUnitTime, subscriptionTierTimeUnit,
                        resourceKey, resourceTierCount, resourceTierUnitTime, resourceTierTimeUnit, timestamp, appTier,
                        apiTier, resourceTier, subscriptionTier);

                executor.execute(agent);

            } else {
                log.debug("Throttle data publisher pool is not initialized.");
            }
        } catch (Exception e) {
            log.error("Error while publishing throttling events to global policy server", e);
        }
    }

    /**
     * This class will act as thread pool executor and after executing each thread it will return runnable
     * object back to pool. This implementation specifically used to minimize number of objects created during
     * runtime. In this queuing strategy the submitted task will wait in the queue if the corePoolsize threads are
     * busy and the task will be allocated if any of the threads become idle.Thus ThreadPool will always have number
     * of threads running  as mentioned in the corePoolSize.
     * LinkedBlockingQueue without the capacity can be used for this queuing strategy.If the corePoolsize of the
     * thread pool is less and there are more number of time consuming task were submitted,there is more possibility
     * that the task has to wait in the queue for more time before it is run by any of the ideal thread.
     * So tuning core pool size is something we need to tune properly.
     * Also no task will be rejected in Threadpool until the thread pool was shutdown.
     */
    private static class DataProcessThreadPoolExecutor extends ThreadPoolExecutor {
        public DataProcessThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                LinkedBlockingDeque<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        protected void afterExecute(java.lang.Runnable r, java.lang.Throwable t) {
            try {
                DataProcessAgent agent = (DataProcessAgent) r;
                ThrottleDataReceiver.dataPublisherPool.release(agent);
            } catch (Exception e) {
                log.error("Error while returning Throttle data publishing agent back to pool" + e.getMessage());
            }
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

