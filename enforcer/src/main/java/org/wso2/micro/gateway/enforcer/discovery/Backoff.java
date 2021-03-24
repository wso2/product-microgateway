/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.enforcer.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple exponential backoff logic implementation to decide and limit the retries.
 */
public class Backoff {
    private static final Logger log = LogManager.getLogger(Backoff.class);
    public static final double DEFAULT_EXPONENT = 2;

    private final long maxRetries;
    private int retryCount;

    public Backoff(int maxRetries) {
        this.maxRetries = maxRetries;
        this.retryCount = 0;
    }

    public boolean shouldRetry() {
        return this.retryCount <= this.maxRetries;
    }

    public void errorOccurred() {
        ++this.retryCount;
        if (!shouldRetry()) {
            log.warn("No more retries available");
            return;
        }
        waitUntilNextTry();
    }

    private long getWaitTime() {
        return  ((long) Math.pow(DEFAULT_EXPONENT, this.retryCount) * 1000L);
    }

    private void waitUntilNextTry() {
        try {
            Thread.sleep(getWaitTime());
        } catch (InterruptedException e) {
            log.error("Error occurred while waiting for next retry", e);
        }
    }
}
