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

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.SessionTimeoutException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.exception.UndefinedEventTypeException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeThreadFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.DataEndpointConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.DataEndpointConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for DataEndpoint, and this is a main class that needs to be implemented
 * for supporting different transports to DataPublisher. This abstraction provides the additional
 * functionality to handle failover, asynchronous connection to the endpoint, etc.
 */

public abstract class DataEndpoint {

    private static final Logger log = LogManager.getLogger(DataEndpoint.class);

    private DataEndpointConnectionWorker connectionWorker;

    private GenericKeyedObjectPool transportPool;

    private int batchSize;

    private EventPublisherThreadPoolExecutor threadPoolExecutor;

    private DataEndpointFailureCallback dataEndpointFailureCallback;

    private ExecutorService connectionService;

    private int maxPoolSize;

    private List<Event> events;

    private State state;

    private Semaphore immediateDispatchSemaphore;

    /**
     * Endpoint state.
     */
    public enum State {
        ACTIVE, UNAVAILABLE, BUSY, INITIALIZING
    }

    public DataEndpoint() {
        this.batchSize = DataEndpointConstants.DEFAULT_DATA_AGENT_BATCH_SIZE;
        this.state = State.INITIALIZING;
        events = new ArrayList<>();
    }

    void collectAndSend(Event event) {
        events.add(event);
        if (events.size() >= batchSize) {
            threadPoolExecutor.submitJobAndSetState(new EventPublisher(events), this);
            events = new ArrayList<>();
        }
    }

    void flushEvents() {
        if (events.size() != 0) {
            threadPoolExecutor.submitJobAndSetState(new EventPublisher(events), this);
            events = new ArrayList<>();
        }
    }

    void syncSend(Event event) {
        List<Event> events = new ArrayList<>(1);
        events.add(event);
        EventPublisher eventPublisher = new EventPublisher(events);
        setStateBusy();
        acquireImmediateDispatchSemaphore();
        try {
            eventPublisher.run();
        } finally {
            releaseImmediateDispatchSemaphore();
        }
    }

    private void acquireImmediateDispatchSemaphore() {
        boolean acquired = false;
        do {
            try {
                immediateDispatchSemaphore.acquire();
                acquired = true;
            } catch (final InterruptedException e) {
                // Do nothing
            }
        } while (!acquired);
    }

    private void releaseImmediateDispatchSemaphore() {
        immediateDispatchSemaphore.release();
    }

    private void setStateBusy() {
        int permits = immediateDispatchSemaphore.availablePermits();
        if (permits <= 1) {
            setState(State.BUSY);
        }
    }

    void setState(State state) {
        if (!this.state.equals(state)) {
            this.state = state;
        }
    }

    void connect()
            throws TransportException,
            DataEndpointAuthenticationException, DataEndpointException {
        if (connectionWorker != null) {
            connectionService.submit(connectionWorker);
        } else {
            throw new DataEndpointException("Data Endpoint is not initialized");
        }
    }

    synchronized void syncConnect(String oldSessionId) throws DataEndpointException {
        if (oldSessionId == null || oldSessionId.equalsIgnoreCase(getDataEndpointConfiguration().getSessionId())) {
            if (connectionWorker != null) {
                connectionWorker.run();
            } else {
                throw new DataEndpointException("Data Endpoint is not initialized");
            }
        }
    }

    public void initialize(DataEndpointConfiguration dataEndpointConfiguration)
            throws DataEndpointException, DataEndpointAuthenticationException,
            TransportException {
        this.transportPool = dataEndpointConfiguration.getTransportPool();
        this.batchSize = dataEndpointConfiguration.getBatchSize();
        this.connectionWorker = new DataEndpointConnectionWorker();
        this.connectionWorker.initialize(this, dataEndpointConfiguration);
        this.threadPoolExecutor = new EventPublisherThreadPoolExecutor(dataEndpointConfiguration.getCorePoolSize(),
                dataEndpointConfiguration.getMaxPoolSize(), dataEndpointConfiguration.getKeepAliveTimeInPool(),
                dataEndpointConfiguration.getReceiverURL());
        this.connectionService = Executors.newSingleThreadExecutor(new DataBridgeThreadFactory(
                "ConnectionService-" +
                        dataEndpointConfiguration.getReceiverURL()));
        this.maxPoolSize = dataEndpointConfiguration.getMaxPoolSize();
        this.immediateDispatchSemaphore = new Semaphore(maxPoolSize);
        connect();
    }

    /**
     * Login to the endpoint and return the sessionId.
     *
     * @param client   The client which can be used to connect to the endpoint.
     * @param userName The username which is used to login,
     * @param password The password which is required for the login operation.
     * @return returns the sessionId
     * @throws DataEndpointAuthenticationException
     */
    protected abstract String login(Object client, String userName, String password)
            throws DataEndpointAuthenticationException;

    /**
     * Logout from the endpoint.
     *
     * @param client    The client that is used to logout operation.
     * @param sessionId The current session Id.
     * @throws DataEndpointAuthenticationException
     */
    protected abstract void logout(Object client, String sessionId)
            throws DataEndpointAuthenticationException;


    public State getState() {
        return state;
    }

    void activate() {
        this.setState(State.ACTIVE);
    }

    void deactivate() {
        this.setState(State.UNAVAILABLE);
    }

    /**
     * Send the list of events to the actual endpoint.
     *
     * @param client The client that can be used to send the events.
     * @param events List of events that needs to be sent.
     * @throws DataEndpointException
     * @throws SessionTimeoutException
     * @throws UndefinedEventTypeException
     */
    protected abstract void send(Object client, List<Event> events) throws
            DataEndpointException, SessionTimeoutException, UndefinedEventTypeException;

    protected DataEndpointConfiguration getDataEndpointConfiguration() {
        if (connectionWorker == null) {
            return null;
        }
        return this.connectionWorker.getDataEndpointConfiguration();
    }

    private Object getClient() throws DataEndpointException {
        try {
            return transportPool.borrowObject(getDataEndpointConfiguration().getPublisherKey());
        } catch (Exception e) {
            throw new DataEndpointException("Cannot borrow client for " +
                    getDataEndpointConfiguration().getPublisherKey(), e);
        }
    }

    private void returnClient(Object client) {
        try {
            transportPool.returnObject(getDataEndpointConfiguration().getPublisherKey(), client);
        } catch (Exception e) {
            log.warn("Error occurred while returning object to connection pool", e);
            discardClient(client);
        }
    }

    private void discardClient(Object client) {
        if (client != null) {
            try {
                transportPool.invalidateObject(getDataEndpointConfiguration().getPublisherKey(), client);
            } catch (Exception e) {
                log.error("Error while invalidating the client ", e);
            }
        }
    }

    void registerDataEndpointFailureCallback(DataEndpointFailureCallback callback) {
        dataEndpointFailureCallback = callback;
    }

    /**
     * Event Publisher worker thread to actually sends the events to the endpoint.
     */
    class EventPublisher implements Runnable {
        List<Event> events;
        private Semaphore semaphore;

        public EventPublisher(List<Event> events) {
            this.events = events;
        }

        @Override
        public void run() {
            String sessionId = getDataEndpointConfiguration().getSessionId();
            try {
                publish();
            } catch (SessionTimeoutException e) {
                try {
                    if (sessionId == null || sessionId.equalsIgnoreCase(getDataEndpointConfiguration().
                            getSessionId())) {
                        syncConnect(sessionId);
                    }
                    publish();
                } catch (UndefinedEventTypeException ex) {
                    log.error("Unable to process this event.", ex);
                    semaphoreRelease();
                } catch (Exception ex) {
                    log.error("Unexpected error occurred while sending the event. ", ex);
                    handleFailedEvents(this.events);
                }
            } catch (DataEndpointException e) {
                log.error("Unable to send events to the endpoint. ", e);
                handleFailedEvents(this.events);
            } catch (UndefinedEventTypeException e) {
                log.error("Unable to process this event.", e);
                semaphoreRelease();
            } catch (Exception ex) {
                log.error("Unexpected error occurred while sending the event. ", ex);
                handleFailedEvents(this.events);
            } catch (Throwable t) {
                //There can be situations where runtime exceptions/class not found exceptions occur,
                // This block help to catch those exceptions.
                //No need to retry send events. Deactivating the state would be enough.
                log.error("Unexpected error occurred while sending events. ", t);
                semaphoreRelease();
                deactivate();
            } finally {
                //If any processing error occurred the state will be changed to unavailable,
                // Hence the state switch should be happening only in busy state where the publishing was success.
                if (state.equals(State.BUSY)) {
                    activate();
                }
                if (log.isDebugEnabled()) {
                    log.debug("Current threads count is : " + threadPoolExecutor.getActiveCount() +
                            ", maxPoolSize is : " +
                            maxPoolSize + ", therefore state is now : " + getState() + " at time : " +
                            System.nanoTime());
                }
            }
        }

        public void setPoolSemaphore(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        private void publish() throws DataEndpointException, SessionTimeoutException, UndefinedEventTypeException {
            Object client = getClient();
            try {
                send(client, this.events);
                semaphoreRelease();
            } finally {
                returnClient(client);
            }
        }

        private void semaphoreRelease() {
            if (this.semaphore != null) {
                this.semaphore.release();
            }
        }
    }

    private void handleFailedEvents(List<Event> events) {
        deactivate();
        dataEndpointFailureCallback.tryResendEvents(events, this);
    }

    boolean isConnected() {
        return !state.equals(State.UNAVAILABLE);
    }

    public String toString() {
        if (getDataEndpointConfiguration() == null) {
            return "null";
        }
        return "( Receiver URL : " + getDataEndpointConfiguration().getReceiverURL() + ", Authentication URL : " +
                getDataEndpointConfiguration().getAuthURL() + ")";
    }

    /**
     * Graceful shutdown until publish all the events given to the endpoint.
     */
    public void shutdown() {
        log.info("Shutdown triggered for data publisher endpoint URL - " +
                getDataEndpointConfiguration().getReceiverURL());
        while (threadPoolExecutor.getActiveCount() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        connectionWorker.disconnect(getDataEndpointConfiguration());
        connectionService.shutdownNow();
        threadPoolExecutor.shutdownNow();
        try {
            connectionService.awaitTermination(10, TimeUnit.SECONDS);
            threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }
        log.info("Completed shutdown for data publisher endpoint URL - " +
                getDataEndpointConfiguration().getReceiverURL());
    }
}
