/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.client;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.DataPublisherUtil;

/**
 * The abstract class that needs to be implemented when supporting a new non-secure transport
 * to mainly create, validate and terminate  the client to the endpoint.
 */
public abstract class AbstractClientPoolFactory extends BaseKeyedPoolableObjectFactory {

    @Override
    public Object makeObject(Object key)
            throws DataEndpointException, DataEndpointConfigurationException {
        Object[] urlParams = DataPublisherUtil.getProtocolHostPort(key.toString());
        return createClient(urlParams[0].toString(), urlParams[1].toString(),
                Integer.parseInt(urlParams[2].toString()));
    }

    /**
     * Make a connection to the receiver and return the client.
     *
     * @param protocol protocol that is used to connect to the endpoint
     * @param hostName hostname of the endpoint
     * @param port     port of the endpoint that is listening to
     * @return A valid client which has connected to the receiver and can be used
     * for rest of the operations regarding the endpoint.
     * @throws DataEndpointException Exception to be thrown when communicating with DataEndpoint.
     */
    public abstract Object createClient(String protocol, String hostName, int port)
            throws DataEndpointException;

    @Override
    public boolean validateObject(Object key, Object obj) {
        return validateClient(obj);
    }

    /**
     * Check the validity of the client whether it's in the position to make the
     * communication with endpoint.
     *
     * @param client Client object which needs to be validated.
     * @return Returns true/false based on the client is valid or invalid.
     */
    public abstract boolean validateClient(Object client);

    public void destroyObject(Object key, Object obj) {
        terminateClient(obj);
    }

    /**
     * Terminates the connection between the client and the endpoint.
     *
     * @param client The client which needs to be terminated.
     */
    public abstract void terminateClient(Object client);

}
