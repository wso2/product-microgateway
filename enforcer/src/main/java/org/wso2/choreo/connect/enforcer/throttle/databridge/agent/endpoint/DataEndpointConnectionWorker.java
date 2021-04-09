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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.DataEndpointConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;

/**
 * DataEndpoint Connection worker class implementation.
 */
public class DataEndpointConnectionWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(DataEndpointConnectionWorker.class);

    private DataEndpointConfiguration dataEndpointConfiguration;

    private DataEndpoint dataEndpoint;

    @Override
    public void run() {
        if (isInitialized()) {
            try {
                connect();
                dataEndpoint.activate();
            } catch (DataEndpointAuthenticationException e) {
                log.error("Error while trying to connect to the endpoint. " + e.getErrorMessage(), e);
                dataEndpoint.deactivate();
            }
        } else {
            String errorMsg = "Data endpoint connection worker is not properly initialized ";
            if (dataEndpoint == null) {
                errorMsg += ", data Endpoint is not provided ";
            }
            if (dataEndpointConfiguration == null) {
                errorMsg += ", data Endpoint configuration is not provided";
            }
            errorMsg += ".";
            log.error(errorMsg);
        }
    }

    DataEndpointConfiguration getDataEndpointConfiguration() {
        return dataEndpointConfiguration;
    }

    /**
     * Initialize the data endpoint connection worker.
     * A connection worker can be instantiated only ONE time.
     *
     * @param dataEndpoint              DataEndpoint instance to handle the connection.
     * @param dataEndpointConfiguration DataEndpointConfiguration to handle the connection.
     * @throws DataEndpointException if data endpoint configuration is already set or the data endpoint is
     *                               already configured with connection worker
     */
    public void initialize(DataEndpoint dataEndpoint, DataEndpointConfiguration dataEndpointConfiguration)
            throws DataEndpointException {
        if (this.dataEndpointConfiguration == null) {
            this.dataEndpointConfiguration = dataEndpointConfiguration;
        } else {
            throw new DataEndpointException("Already data endpoint configuration is set: " +
                    this.dataEndpointConfiguration.toString() + " for the endpoint " +
                    dataEndpointConfiguration.toString());
        }

        if (this.dataEndpoint == null) {
            this.dataEndpoint = dataEndpoint;
        } else {
            throw new DataEndpointException("Already data endpoint is configured for the connection worker");
        }
    }

    private void connect() throws DataEndpointAuthenticationException {
        Object client = null;
        try {
            client = this.dataEndpointConfiguration.getSecuredTransportPool().
                    borrowObject(dataEndpointConfiguration.getAuthKey());
            String sessionId = this.dataEndpoint.
                    login(client, dataEndpointConfiguration.getUsername(),
                            dataEndpointConfiguration.getPassword());
            dataEndpointConfiguration.setSessionId(sessionId);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new DataEndpointAuthenticationException("Cannot borrow client for " +
                    dataEndpointConfiguration.getAuthURL(), e);
        } finally {
            try {
                this.dataEndpointConfiguration.getSecuredTransportPool().
                        returnObject(dataEndpointConfiguration.getAuthKey(), client);
            } catch (Exception e) {
                this.dataEndpointConfiguration.getSecuredTransportPool().
                        clear(dataEndpointConfiguration.getAuthKey());
            }
        }

    }

    public void disconnect(DataEndpointConfiguration dataPublisherConfiguration) {
        Object client = null;
        try {
            client = this.dataEndpointConfiguration.getSecuredTransportPool().
                    borrowObject(dataPublisherConfiguration.getAuthKey());
            this.dataEndpoint.logout(client, dataPublisherConfiguration.getSessionId());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot connect to the server at " + dataPublisherConfiguration.getAuthURL() +
                        ", for user: " + dataPublisherConfiguration.getUsername(), e);
            }
            log.warn("Cannot connect to the server at " + dataPublisherConfiguration.getAuthURL() +
                    ", for user: " + dataPublisherConfiguration.getUsername());
        } finally {
            try {
                this.dataEndpointConfiguration.getSecuredTransportPool().returnObject(
                        dataPublisherConfiguration.getAuthKey(), client);
            } catch (Exception e) {
                this.dataEndpointConfiguration.getSecuredTransportPool().clear(
                        dataPublisherConfiguration.getAuthKey());
            }
        }
    }

    private boolean isInitialized() {
        return dataEndpoint != null && dataEndpointConfiguration != null;
    }

}
