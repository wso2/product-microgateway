/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint;

import org.wso2.carbon.databridge.commons.utils.DataBridgeThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Event Publisher Threadpool Executor.
 */
public class EventPublisherThreadPoolExecutor extends ThreadPoolExecutor {

    private final Semaphore semaphore;

    public EventPublisherThreadPoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTimeInPool,
                                            String receiverURL) {
        super(corePoolSize, maxPoolSize, keepAliveTimeInPool, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new DataBridgeThreadFactory(receiverURL));
        semaphore = new Semaphore(maxPoolSize);
    }

    @Override
    public void execute(final Runnable task) {
        boolean acquired = false;
        do {
            try {
                semaphore.acquire();
                acquired = true;
            } catch (final InterruptedException e) {
                // Do nothing
            }
        } while (!acquired);
        super.execute(task);
    }

    public void submitJobAndSetState(DataEndpoint.EventPublisher publisher, DataEndpoint dataEndpoint) {
        int permits = semaphore.availablePermits();
        if (permits <= 1) {
            dataEndpoint.setState(DataEndpoint.State.BUSY);
        }
        publisher.setPoolSemaphore(semaphore);
        submit(new Thread(publisher));
    }

}
