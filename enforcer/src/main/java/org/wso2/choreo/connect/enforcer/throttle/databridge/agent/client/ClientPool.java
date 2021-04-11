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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.client;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * This class is used hold the secure/non-secure connections for an Agent.
 */
public class ClientPool {
    private GenericKeyedObjectPool socketPool;
    private GenericKeyedObjectPool secureSocketPool;

    public GenericKeyedObjectPool getClientPool(AbstractClientPoolFactory factory,
                                                int maxActive,
                                                int maxIdle,
                                                boolean testOnBorrow,
                                                long timeBetweenEvictionRunsMillis,
                                                long minEvictableIdleTimeMillis) {
        if (socketPool == null) {
            synchronized (this) {
                if (socketPool == null) {
                    socketPool = new GenericKeyedObjectPool();
                    socketPool.setFactory(factory);
                    socketPool.setMaxActive(maxActive);
                    socketPool.setTestOnBorrow(testOnBorrow);
                    socketPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
                    socketPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
                    socketPool.setMaxIdle(maxIdle);
                    socketPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);
                }
            }
        }
        return socketPool;
    }

    public GenericKeyedObjectPool getClientPool(AbstractSecureClientPoolFactory factory,
                                                int maxActive,
                                                int maxIdle,
                                                boolean testOnBorrow,
                                                long timeBetweenEvictionRunsMillis,
                                                long minEvictableIdleTimeMillis) {
        if (secureSocketPool == null) {
            synchronized (this) {
                if (secureSocketPool == null) {
                    secureSocketPool = new GenericKeyedObjectPool();
                    secureSocketPool.setFactory(factory);
                    secureSocketPool.setMaxActive(maxActive);
                    secureSocketPool.setTestOnBorrow(testOnBorrow);
                    secureSocketPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
                    secureSocketPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
                    secureSocketPool.setMaxIdle(maxIdle);
                    secureSocketPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);
                }
            }
        }
        return secureSocketPool;
    }
}
