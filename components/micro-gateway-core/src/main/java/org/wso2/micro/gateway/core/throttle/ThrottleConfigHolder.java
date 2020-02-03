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

/**
 * Static holder single pattern implementation to hold the configurations related local throttle processing,
 */
public class ThrottleConfigHolder {

    private int processPoolMaxIdle = 1000, processPoolInitIdleCapacity = 200, processThreadPoolCoreSize = 200,
            processThreadPoolMaximumSize = 1000, processThreadPoolKeepAliveTime = 200, throttleFrequency = 3600;

    private static class InnerConfigHolder {
        private static final ThrottleConfigHolder instance = new ThrottleConfigHolder();
    }

    private ThrottleConfigHolder() {
    }

    public static ThrottleConfigHolder getInstance() {
        return InnerConfigHolder.instance;
    }

    public void setData(int processPoolMaxIdle, int processPoolInitIdleCapacity, int processThreadPoolCoreSize,
            int processThreadPoolMaximumSize, int processThreadPoolKeepAliveTime, int throttleFrequency) {
        this.processPoolMaxIdle = processPoolMaxIdle;
        this.processPoolInitIdleCapacity = processPoolInitIdleCapacity;
        this.processThreadPoolCoreSize = processThreadPoolCoreSize;
        this.processThreadPoolMaximumSize = processThreadPoolMaximumSize;
        this.processThreadPoolKeepAliveTime = processThreadPoolKeepAliveTime;
        this.throttleFrequency = throttleFrequency;
    }

    public int getProcessPoolMaxIdle() {
        return processPoolMaxIdle;
    }

    public int getProcessPoolInitIdleCapacity() {
        return processPoolInitIdleCapacity;
    }

    public int getProcessThreadPoolCoreSize() {
        return processThreadPoolCoreSize;
    }

    public int getProcessThreadPoolMaximumSize() {
        return processThreadPoolMaximumSize;
    }

    public int getProcessThreadPoolKeepAliveTime() {
        return processThreadPoolKeepAliveTime;
    }

    public int getThrottleFrequency() {
        return throttleFrequency;
    }
}
