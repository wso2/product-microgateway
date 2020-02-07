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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This task is responsible for cleanup ThrottleData objects which has expired.
 *
 */
public class ThrottleDataCleanUpTask {

    private static final Logger log = LoggerFactory.getLogger(ThrottleDataCleanUpTask.class);
    private List<ThrottleData> throttleDataList = new ArrayList<>();

    public ThrottleDataCleanUpTask() {

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Throttle Cleanup Task");
                return t;
            }
        });

        int throttleFrequency = ThrottleConfigHolder.getInstance().getThrottleFrequency();

        if (log.isDebugEnabled()) {
            log.debug("Throttling Cleanup Task Frequency set to " + throttleFrequency);
        }

        executor.scheduleAtFixedRate(new CleanupTask(), throttleFrequency, throttleFrequency, TimeUnit.SECONDS);

    }

    /**
     * Add the throttle data instances to be cleaned into the cleanup array list, which is iterated by the cleanup task
     * which runs periodically.
     */
    public void addThrottleData(ThrottleData throttleData) {
        throttleDataList.add(throttleData);
    }

    private class CleanupTask implements Runnable {
        public void run() {
            long currentTimeStamp = System.currentTimeMillis();
            throttleDataList.removeIf(throttleData -> throttleData.cleanThrottleData(currentTimeStamp));
        }
    }

}
