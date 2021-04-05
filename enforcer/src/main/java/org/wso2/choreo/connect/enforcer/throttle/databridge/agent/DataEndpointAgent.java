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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.client.AbstractClientPoolFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.client.AbstractSecureClientPoolFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.client.ClientPool;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.AgentConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.DataEndpoint;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinaryClientPoolFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinaryDataEndpoint;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinarySecureClientPoolFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;


import java.util.ArrayList;

/**
 * One agent is created for a specific data endpoint type,and this has the resources such as transport pool, etc
 * which are shared by all the data publishers created for the endpoint type.
 */

public class DataEndpointAgent {

    private ArrayList<DataPublisher> dataPublishers = new ArrayList<>();

    private GenericKeyedObjectPool transportPool;

    private GenericKeyedObjectPool securedTransportPool;

    private AgentConfiguration agentConfiguration;

    public DataEndpointAgent(AgentConfiguration agentConfiguration) {
        this.agentConfiguration = agentConfiguration;
        initialize();
    }

    private void initialize() {
        AbstractClientPoolFactory clientPoolFactory = new BinaryClientPoolFactory();
        AbstractSecureClientPoolFactory secureClientPoolFactory = new BinarySecureClientPoolFactory(
                agentConfiguration.getTrustStore());
        ClientPool clientPool = new ClientPool();
        this.transportPool = clientPool.getClientPool(
                clientPoolFactory,
                agentConfiguration.getMaxTransportPoolSize(),
                agentConfiguration.getMaxIdleConnections(),
                true,
                agentConfiguration.getEvictionTimePeriod(),
                agentConfiguration.getMinIdleTimeInPool());

        this.securedTransportPool = clientPool.getClientPool(
                secureClientPoolFactory,
                agentConfiguration.getSecureMaxTransportPoolSize(),
                agentConfiguration.getSecureMaxIdleConnections(),
                true,
                agentConfiguration.getSecureEvictionTimePeriod(),
                agentConfiguration.getSecureMinIdleTimeInPool());
    }

    public void addDataPublisher(DataPublisher dataPublisher) {
        dataPublishers.add(dataPublisher);
    }

    public AgentConfiguration getAgentConfiguration() {
        return agentConfiguration;
    }

    public GenericKeyedObjectPool getTransportPool() {
        return transportPool;
    }

    public GenericKeyedObjectPool getSecuredTransportPool() {
        return securedTransportPool;
    }

    public synchronized void shutDown(DataPublisher dataPublisher) {
        dataPublishers.remove(dataPublisher);
    }

    public DataEndpoint getNewDataEndpoint() {
        return new BinaryDataEndpoint();
    }

    public synchronized void shutDown() throws DataEndpointException {
        for (DataPublisher dataPublisher : dataPublishers) {
            dataPublisher.shutdown();
        }
        try {
            transportPool.close();
            securedTransportPool.close();
        } catch (Exception e) {
            throw new DataEndpointException("Error while closing the transport pool", e);
        }
    }
}
