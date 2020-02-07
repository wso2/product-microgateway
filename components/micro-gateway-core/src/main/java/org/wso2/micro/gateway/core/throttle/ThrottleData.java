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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to hold throttle counter data against a specific key
 */
public class ThrottleData {

    /**
     * Enum to hold throttle types supported by gateway.
     */
    public enum ThrottleType {
        APP, SUBSCRIPTION, RESOURCE
    }

    private long windowStartTime = 0;
    private long unitTime;
    private AtomicLong count = new AtomicLong();
    private long remainingQuota;
    private boolean stopOnQuota = true;
    private boolean throttled = false;
    private String throttleKey;
    private ThrottleType throttleType;

    public long getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(long windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public long getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(long unitTime) {
        this.unitTime = unitTime;
    }

    public AtomicLong getCount() {
        return count;
    }

    public void setCount(AtomicLong count) {
        this.count = count;
    }

    public long getRemainingQuota() {
        return remainingQuota;
    }

    public void setRemainingQuota(long remainingQuota) {
        this.remainingQuota = remainingQuota;
    }

    public boolean isStopOnQuota() {
        return stopOnQuota;
    }

    public void setStopOnQuota(boolean stopOnQuota) {
        this.stopOnQuota = stopOnQuota;
    }

    public boolean isThrottled() {
        return throttled;
    }

    public void setThrottled(boolean throttled) {
        this.throttled = throttled;
    }

    public ThrottleType getThrottleType() {
        return throttleType;
    }

    public void setThrottleType(ThrottleType throttleType) {
        this.throttleType = throttleType;
    }

    public void setThrottleKey(String throttleKey) {
        this.throttleKey = throttleKey;
    }

    public boolean cleanThrottleData(long timeStamp) {
        if (this.windowStartTime + this.unitTime < timeStamp) {
            switch (getThrottleType()) {
                case APP: {
                    ThrottleCounter.removeFromApplicationCounterMap(this.throttleKey);
                    break;
                }
                case RESOURCE: {
                    ThrottleCounter.removeFromResourceCounterMap(this.throttleKey);
                    break;
                }
                case SUBSCRIPTION: {
                    ThrottleCounter.removeFromSubscriptionCounterMap(this.throttleKey);
                    break;
                }
            }
            return true;
        }
        return false;
    }
}
