/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.throttle.databridge.publisher;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottlePublisherConfigDto;

/**
 * This class implemented to hold throttle data publishing agent pool. Reason for implement this is to
 * reduce unwanted object creation. This is using stack object pool as we may need to handle some scenarios
 * where unexpected load comes. In such cases we cannot have fixed size pool.
 */
public class ThrottleDataPublisherPool {

    private static final Logger log = LogManager.getLogger(ThrottleDataPublisherPool.class);

    private ObjectPool clientPool;

    private ThrottleDataPublisherPool() {
        //Using stack object pool to handle high concurrency scenarios without droping any messages.
        //Tuning this pool is mandatory according to use cases.
        //A finite number of "sleeping" or idle instances is enforced, but when the pool is empty, new instances
        // are created to support the new load. Hence this following data stricture places no limit on the number of "
        // active" instance created by the pool, but is quite useful for re-using Objects without introducing
        // artificial limits.
        //Proper tuning is mandatory for good performance according to system load.
        ThrottlePublisherConfigDto configuration = ConfigHolder.getInstance().getConfig().getThrottleConfig()
                .getThrottleAgent().getPublisher();
        clientPool = new StackObjectPool(new BasePoolableObjectFactory() {
            @Override
            public Object makeObject() throws Exception {
                if (log.isDebugEnabled()) {
                    log.debug("Initializing new ThrottleDataPublisher instance");
                }
                return new DataProcessAndPublishingAgent();
            }
        }, configuration.getMaxIdleDataPublishingAgents(), configuration.getInitIdleObjectDataPublishingAgents());
    }

    private static class ThrottleDataPublisherPoolHolder {
        private static final ThrottleDataPublisherPool INSTANCE = new ThrottleDataPublisherPool();

        private ThrottleDataPublisherPoolHolder() {
        }
    }

    public static ThrottleDataPublisherPool getInstance() {
        return ThrottleDataPublisherPoolHolder.INSTANCE;
    }

    public DataProcessAndPublishingAgent get() throws Exception {
        return (DataProcessAndPublishingAgent) clientPool.borrowObject();
    }

    public void release(DataProcessAndPublishingAgent client) throws Exception {
        //We must clean data references as it can caused to pass old data to global policy server.
        client.clearDataReference();
        clientPool.returnObject(client);
    }

    public void cleanup() {
        try {
            clientPool.close();
        } catch (Exception e) {
            log.warn("Error while cleaning up the object pool", e);
        }
    }
}
