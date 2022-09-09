/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that holds the  thread pool that serves the external auth requests coming from the router component.
 */
public class EnforcerWorkerPool {
    private final BlockingQueue<Runnable> blockingQueue;
    private final ThreadPoolExecutor executor;
    private static final Logger log = LogManager.getLogger(EnforcerWorkerPool.class);

    public EnforcerWorkerPool(int core, int max, int keepAlive, int queueLength, String threadGroupName,
            String threadGroupId) {
        this.blockingQueue = queueLength == -1 ? new LinkedBlockingQueue() : new LinkedBlockingQueue(queueLength);
        this.executor = new EnforcerThreadPoolExecutor(core, max, (long) keepAlive, TimeUnit.SECONDS,
                this.blockingQueue, new NativeThreadFactory(new ThreadGroup(threadGroupName), threadGroupId));
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }
}
