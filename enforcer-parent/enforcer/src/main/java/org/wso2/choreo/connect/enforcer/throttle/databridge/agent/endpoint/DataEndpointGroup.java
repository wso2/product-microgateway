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

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.utils.DataBridgeThreadFactory;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.DataEndpointAgent;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.AgentConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.DataEndpointConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.EventQueueFullException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.DataEndpointConstants;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.DataPublisherUtil;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.EndpointUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class holds the endpoints associated within a group. Also it has a queue
 * to hold the list of events that needs to be processed by the endpoints with
 * provided the load balancing, or failover configuration.
 */
public class DataEndpointGroup implements DataEndpointFailureCallback {
    private static final Logger log = LogManager.getLogger(DataEndpointGroup.class);

    private List<DataEndpoint> dataEndpoints;

    private HAType haType;

    private EventQueue eventQueue = null;

    private int reconnectionInterval;

    private final Integer startIndex = 0;

    private AtomicInteger currentDataPublisherIndex = new AtomicInteger(startIndex);

    private AtomicInteger maximumDataPublisherIndex = new AtomicInteger();

    private ScheduledExecutorService reconnectionService;

    private final String publishingStrategy;

    private boolean isShutdown = false;

    private SSLSocketFactory sslSocketFactory;

    /**
     * HA Type.
     */
    public enum HAType {
        FAILOVER, LOADBALANCE
    }

    public DataEndpointGroup(HAType haType, DataEndpointAgent agent) {
        this.dataEndpoints = new ArrayList<>();
        this.haType = haType;
        this.reconnectionService = Executors.newScheduledThreadPool(1,
                new DataBridgeThreadFactory("ReconnectionService"));
        this.reconnectionInterval = agent.getAgentConfiguration().getReconnectionInterval();
        this.publishingStrategy = agent.getAgentConfiguration().getPublishingStrategy();
        if (!publishingStrategy.equalsIgnoreCase(DataEndpointConstants.SYNC_STRATEGY)) {
            this.eventQueue = new EventQueue(agent.getAgentConfiguration().getQueueSize());
        }
        this.reconnectionService.scheduleAtFixedRate(new ReconnectionTask(), reconnectionInterval,
                reconnectionInterval, TimeUnit.SECONDS);
        currentDataPublisherIndex.set(startIndex);

        try {
            SSLContext ctx = EndpointUtils.createSSLContext(AgentConfiguration.getInstance().getTrustStore());
            this.sslSocketFactory = ctx.getSocketFactory();
        } catch (DataEndpointException e) {
            log.error("Error when initializing SSL socket factory");
        }
    }

    public void addDataEndpoint(DataEndpoint dataEndpoint) {
        dataEndpoints.add(dataEndpoint);
        dataEndpoint.registerDataEndpointFailureCallback(this);
        maximumDataPublisherIndex.incrementAndGet();
    }

    public void tryPublish(Event event) throws EventQueueFullException {
        if (eventQueue != null) {
            eventQueue.tryPut(event);
        } else if (!isShutdown) {
            trySyncPublish(event);
        }
    }

    public void publish(Event event) {
        if (eventQueue != null) {
            eventQueue.put(event);
        } else if (!isShutdown) {
            syncPublish(event);
        }
    }

    private void trySyncPublish(Event event) {
        try {
            DataEndpoint endpoint = getDataEndpoint(false);
            if (endpoint != null) {
                endpoint.syncSend(event);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("DataEndpoint not available, dropping event : " + event);
                }
            }
        } catch (Throwable t) {
            log.error("Unexpected error: " + t.getMessage(), t);
        }
    }

    private void syncPublish(Event event) {
        try {
            DataEndpoint endpoint = getDataEndpoint(true);
            if (endpoint != null) {
                endpoint.syncSend(event);
            } else {
                log.error("Dropping event as DataPublisher is shutting down.");
                if (log.isDebugEnabled()) {
                    log.debug("Data publisher is shutting down, dropping event : " + event);
                }
            }
        } catch (Throwable t) {
            log.error("Unexpected error: " + t.getMessage(), t);
        }
    }

    /**
     * Event Queue Class.
     */
    class EventQueue {
        private RingBuffer<WrappedEventFactory.WrappedEvent> ringBuffer = null;
        private Disruptor<WrappedEventFactory.WrappedEvent> eventQueueDisruptor = null;
        private ExecutorService eventQueuePool = null;

        EventQueue(int queueSize) {
            eventQueuePool = Executors.newCachedThreadPool(
                    new DataBridgeThreadFactory("EventQueue"));
            eventQueueDisruptor = new Disruptor<>(new WrappedEventFactory(), queueSize, eventQueuePool,
                    ProducerType.MULTI, new BlockingWaitStrategy());
            eventQueueDisruptor.handleEventsWith(new EventQueueWorker());
            this.ringBuffer = eventQueueDisruptor.start();
        }

        private void tryPut(Event event) throws EventQueueFullException {

            long sequence;
            try {
                sequence = this.ringBuffer.tryNext(1);
                WrappedEventFactory.WrappedEvent bufferedEvent = this.ringBuffer.get(sequence);
                bufferedEvent.setEvent(event);
                this.ringBuffer.publish(sequence);
            } catch (InsufficientCapacityException e) {
                throw new EventQueueFullException("Cannot send events because the event queue is full", e);
            }
        }

        //Endless wait if at-least once endpoint is available.
        private void put(Event event) {
            do {
                try {
                    long sequence = this.ringBuffer.tryNext(1);
                    WrappedEventFactory.WrappedEvent bufferedEvent = this.ringBuffer.get(sequence);
                    bufferedEvent.setEvent(event);
                    this.ringBuffer.publish(sequence);
                    return;
                } catch (InsufficientCapacityException ex) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException ignored) {
                    }
                }
            } while (isActiveDataEndpointExists());
        }

        private void shutdown() {
            eventQueuePool.shutdown();
            eventQueueDisruptor.shutdown();
        }
    }

    /**
     * Event Queue Worker.
     */
    class EventQueueWorker implements EventHandler<WrappedEventFactory.WrappedEvent> {

        boolean isLastEventDropped = false;

        @Override
        public void onEvent(WrappedEventFactory.WrappedEvent wrappedEvent, long sequence, boolean endOfBatch) {
            DataEndpoint endpoint = getDataEndpoint(true);
            Event event = wrappedEvent.getEvent();
            if (endpoint != null) {
                isLastEventDropped = false;
                endpoint.collectAndSend(event);
                if (endOfBatch) {
                    flushAllDataEndpoints();
                }
            } else {
                if (!isLastEventDropped) {
                    log.error("Dropping all events as DataPublisher is shutting down.");
                }
                if (log.isDebugEnabled()) {
                    log.debug("Data publisher is shutting down, dropping event : " + event);
                }
                isLastEventDropped = true;
            }
        }
    }

    private void flushAllDataEndpoints() {
        for (DataEndpoint dataEndpoint : dataEndpoints) {
            if (dataEndpoint.getState().equals(DataEndpoint.State.ACTIVE)) {
                dataEndpoint.flushEvents();
            }
        }
    }

    private DataEndpoint getDataEndpoint(boolean isBusyWait) {
        return getDataEndpoint(isBusyWait, null);
    }

    /**
     * Find the next event processable endpoint to the
     * data endpoint based on load balancing and failover logic, and wait
     * indefinitely until at least one data endpoint becomes available based
     * on busywait parameter.
     *
     * @param isBusyWait waitUntil atleast one endpoint becomes available
     * @return DataEndpoint which can accept and send the events.
     */
    private DataEndpoint getDataEndpoint(boolean isBusyWait, DataEndpoint failedEP) {
        int startIndex;
        if (haType.equals(HAType.LOADBALANCE)) {
            startIndex = getDataPublisherIndex();
        } else {
            startIndex = this.startIndex;
        }
        int index = startIndex;

        while (true) {
            DataEndpoint dataEndpoint = dataEndpoints.get(index);
            if (dataEndpoint.getState().equals(DataEndpoint.State.ACTIVE) && dataEndpoint != failedEP) {
                return dataEndpoint;
            } else if (haType.equals(HAType.FAILOVER) && (dataEndpoint.getState().equals(DataEndpoint.State.BUSY) ||
                    dataEndpoint.getState().equals(DataEndpoint.State.INITIALIZING))) {
                /**
                 * Wait for some time until the failover endpoint finish publishing
                 */
                busyWait(1);
            } else {
                index++;
                if (index > maximumDataPublisherIndex.get() - 1) {
                    index = this.startIndex;
                }
                if (index == startIndex) {
                    if (isBusyWait) {
                        if (!reconnectionService.isShutdown()) {

                            /**
                             * Have fully iterated the data publisher list,
                             * and busy wait until data publisher
                             * becomes available
                             */
                            busyWait(1);
                        } else {
                            if (!isActiveDataEndpointExists()) {
                                return null;
                            } else {
                                busyWait(1);
                            }
                        }
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    private void busyWait(long timeInMilliSec) {
        try {
            Thread.sleep(timeInMilliSec);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean isActiveDataEndpointExists() {
        int index = startIndex;
        while (index < maximumDataPublisherIndex.get()) {
            DataEndpoint dataEndpoint = dataEndpoints.get(index);
            if (dataEndpoint.getState() != DataEndpoint.State.UNAVAILABLE) {
                if (log.isDebugEnabled()) {
                    log.debug("Available endpoint : " + dataEndpoint + " existing in state - " +
                            dataEndpoint.getState());
                }
                return true;
            }
            index++;
        }
        return false;
    }

    private synchronized int getDataPublisherIndex() {
        int index = currentDataPublisherIndex.getAndIncrement();
        if (index == maximumDataPublisherIndex.get() - 1) {
            currentDataPublisherIndex.set(startIndex);
        }
        return index;
    }

    public void tryResendEvents(List<Event> events, DataEndpoint dataEndpoint) {
        List<Event> unsuccessfulEvents = trySendActiveEndpoints(events, dataEndpoint);
        for (Event event : unsuccessfulEvents) {
            try {
                if (eventQueue != null) {
                    eventQueue.tryPut(event);
                } else {
                    trySyncPublish(event);
                }
            } catch (EventQueueFullException e) {
                log.error("Unable to put the event :" + event, e);
            }
        }
    }

    private List<Event> trySendActiveEndpoints(List<Event> events, DataEndpoint failedEP) {
        ArrayList<Event> unsuccessfulEvents = new ArrayList<>();
        for (Event event : events) {
            DataEndpoint endpoint = getDataEndpoint(false, failedEP);
            if (endpoint != null) {
                endpoint.collectAndSend(event);
            } else {
                unsuccessfulEvents.add(event);
            }
        }
        flushAllDataEndpoints();
        return unsuccessfulEvents;
    }

    private class ReconnectionTask implements Runnable {
        String failedDataEndpoints = "";
        public void run() {
            boolean isOneReceiverConnected = false;
            for (int i = startIndex; i < maximumDataPublisherIndex.get(); i++) {
                DataEndpoint dataEndpoint = dataEndpoints.get(i);
                if (!dataEndpoint.isConnected()) {
                    try {
                        dataEndpoint.connect();
                    } catch (Exception ex) {
                        dataEndpoint.deactivate();
                    }
                } else {
                    try {
                        String[] urlElements = DataPublisherUtil.getProtocolHostPort(
                                dataEndpoint.getDataEndpointConfiguration().getReceiverURL());
                        if (!isServerExists(dataEndpoint, urlElements[0], urlElements[1],
                                Integer.parseInt(urlElements[2]))) {
                            dataEndpoint.deactivate();
                        }
                    } catch (DataEndpointConfigurationException exception) {
                        log.warn("Data Endpoint with receiver URL:" +
                                dataEndpoint.getDataEndpointConfiguration().getReceiverURL()
                                + " could not be deactivated", exception);
                    }
                }
                if (dataEndpoint.isConnected()) {
                    isOneReceiverConnected = true;
                } else {
                    failedDataEndpoints = (dataEndpoint.getDataEndpointConfiguration() != null
                            ? dataEndpoint.getDataEndpointConfiguration().getReceiverURL() : "Null") + ",";
                }
            }
            if (!isOneReceiverConnected) {
                log.warn("Receiver is not reachable at reconnection for the endpoints: " + failedDataEndpoints +
                        ", will try to reconnect every " + reconnectionInterval + " sec");
            }
        }

        private boolean isServerExists(DataEndpoint dataEndpoint, String protocol, String ip, int port) {
            try {
                if (protocol.equals(DataEndpointConfiguration.Protocol.TCP.toString())) {
                    Socket socket = new Socket(ip, port);
                    socket.close();
                } else {
                    // this block is executed when connection is SSL
                    SSLSocket socket = null;
                    try {
                        socket = (SSLSocket) sslSocketFactory.createSocket(ip, port);
                        OutputStream outputStream = socket.getOutputStream();
                        String sessionId = dataEndpoint.getDataEndpointConfiguration().getSessionId();
                        ByteBuffer buf = ByteBuffer.allocate(sessionId.length());
                        outputStream.write(buf.array());
                        outputStream.flush();
                    } finally {
                        try {
                            if ((socket != null) && (socket.isConnected())) {
                                socket.close();
                            }
                        } catch (IOException e) {
                            log.error("Can not close the SSL socket which is used to check the server status ",
                                    e);
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    public String toString() {
        StringBuilder group = new StringBuilder();
        group.append("[ ");
        for (int i = 0; i < dataEndpoints.size(); i++) {
            DataEndpoint endpoint = dataEndpoints.get(i);
            group.append(endpoint.toString());
            if (i == dataEndpoints.size() - 1) {
                group.append(" ]");
                return group.toString();
            } else {
                if (haType == HAType.FAILOVER) {
                    group.append(DataEndpointConstants.FAILOVER_URL_GROUP_SEPARATOR);
                } else {
                    group.append(DataEndpointConstants.LB_URL_GROUP_SEPARATOR);
                }
            }
        }
        return group.toString();
    }

    public void shutdown() {
        reconnectionService.shutdownNow();
        if (eventQueue != null) {
            eventQueue.shutdown();
        }
        isShutdown = true;
        for (DataEndpoint dataEndpoint : dataEndpoints) {
            dataEndpoint.shutdown();
        }
    }
}
