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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.DataEndpointConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.DataEndpoint;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.DataEndpointGroup;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.EventQueueFullException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util.DataPublisherUtil;



import java.util.ArrayList;
import java.util.Map;

/**
 * API Used to communicate with Data Receivers WSO2 BAM/CEP. It can be used to send events to
 * multiple DAS/CEP nodes with load balancing and failover logic.
 */
public class DataPublisher {

    private static final Logger log = LogManager.getLogger(DataPublisher.class);

    /**
     * List of group of endpoints where events needs to dispatched when
     * events are published using this API.
     */
    private ArrayList<DataEndpointGroup> endpointGroups = new ArrayList<>();

    /**
     * The Agent for which the data publisher belongs to.
     */
    private DataEndpointAgent dataEndpointAgent;

    private static final int FAILED_EVENT_LOG_INTERVAL = 10000;

    /**
     * The last failed event time kept, use to determine when to log an warning
     * message, without continuously doing so.
     */
    private long lastFailedEventTime;

    /**
     * The current failed event count. A normal long is used here, rather
     * than an AtomicLong, since this is not a critical stat.
     */
    private long failedEventCount;

    /**
     * Creates the DataPublisher instance for a specific user, and the it creates
     * connection asynchronously to receiver endpoint.
     *
     * @param receiverURLSet The receiving endpoint URL Set. This can be either load balancing URL set,
     *                       or Failover URL set.
     * @param authURLSet     The authenticating URL Set for the endpoints given in receiverURLSet parameter.
     *                       This should be in the same format as receiverURL set parameter. If null is passed
     *                       the authURLs will be offsetted by value of 100.
     * @param username       Authorized username at receiver.
     * @param password       The password of the username provided.
     * @throws DataEndpointException               Exception to be thrown when communicating with DataEndpoint.
     * @throws DataEndpointConfigurationException  Exception to be thrown When parsing the Data Endpoint
     *                                             configurations when initializing data publisher
     * @throws DataEndpointAuthenticationException Exception to be thrown when connecting the Data Endpoint
     * @throws TransportException                  Transport level exception
     */
    public DataPublisher(String receiverURLSet, String authURLSet, String username, String password)
            throws DataEndpointException, DataEndpointConfigurationException,
            DataEndpointAuthenticationException, TransportException {
        dataEndpointAgent = AgentHolder.getInstance().getDataEndpointAgent();
        if (authURLSet == null) {
            authURLSet = DataPublisherUtil.getDefaultAuthURLSet(receiverURLSet);
        }
        processEndpoints(dataEndpointAgent, receiverURLSet, authURLSet, username, password);
        dataEndpointAgent.addDataPublisher(this);
    }

    /**
     * This validates the input that are passed in the DataPublisher creation,
     * and initiates the endpoints connection.
     *
     * @param dataEndpointAgent Agent of the DataPublisher.
     * @param receiverURLSet    The receiving endpoint URL Set. This can be either load balancing URL set,
     *                          or Failover URL set.
     * @param authURLSet        The authenticating URL Set for the endpoints given in receiverURLSet parameter.
     *                          This should be in the same format as receiverURL set parameter. If the authURLSet
     *                          is null, then default authURLSet will be generated from the receiverURL.
     * @param username          Authorized username at receiver.
     * @param password          The password of the username provided.
     * @throws DataEndpointConfigurationException  Exception to be thrown When parsing the Data Endpoint
     *                                             configurations when initializing data publisher
     * @throws DataEndpointException               Exception to be thrown when communicating with DataEndpoint.
     * @throws DataEndpointAuthenticationException Exception to be thrown when connecting the Data Endpoint
     * @throws TransportException                  Transport level exception
     */
    private void processEndpoints(DataEndpointAgent dataEndpointAgent,
                                  String receiverURLSet, String authURLSet, String username, String password)
            throws DataEndpointConfigurationException, DataEndpointException,
            DataEndpointAuthenticationException, TransportException {
        ArrayList<Object[]> receiverURLGroups = DataPublisherUtil.getEndpointGroups(receiverURLSet);
        ArrayList<Object[]> authURLGroups = DataPublisherUtil.getEndpointGroups(authURLSet);
        DataPublisherUtil.validateURLs(receiverURLGroups, authURLGroups);

        for (int i = 0; i < receiverURLGroups.size(); i++) {
            Object[] receiverGroup = (Object[]) receiverURLGroups.get(i);
            Object[] authGroup = (Object[]) authURLGroups.get(i);
            boolean failOver = (Boolean) receiverGroup[0];

            DataEndpointGroup endpointGroup;
            if (failOver) {
                endpointGroup = new DataEndpointGroup(DataEndpointGroup.HAType.FAILOVER, dataEndpointAgent);
            } else {
                endpointGroup = new DataEndpointGroup(DataEndpointGroup.HAType.LOADBALANCE,
                        dataEndpointAgent);
            }
            /*
             * Since the first element holds the failover/LB settings
             * we need to start iterating from 2nd element.
             */
            for (int j = 1; j < receiverGroup.length; j++) {
                DataEndpointConfiguration endpointConfiguration;
                String[] urlParams = DataPublisherUtil.getProtocolHostPort((String) receiverGroup[j]);

                if (urlParams[0].equalsIgnoreCase(DataEndpointConfiguration.Protocol.TCP.toString())) {
                    endpointConfiguration = new DataEndpointConfiguration((String) receiverGroup[j],
                            (String) authGroup[j], username, password, dataEndpointAgent.getTransportPool(),
                            dataEndpointAgent.getSecuredTransportPool(),
                            dataEndpointAgent.getAgentConfiguration().getBatchSize(),
                            dataEndpointAgent.getAgentConfiguration().getCorePoolSize(),
                            dataEndpointAgent.getAgentConfiguration().getMaxPoolSize(),
                            dataEndpointAgent.getAgentConfiguration().getKeepAliveTimeInPool());
                } else {
                    endpointConfiguration = new DataEndpointConfiguration((String) receiverGroup[j],
                            (String) authGroup[j], username, password, dataEndpointAgent.getSecuredTransportPool(),
                            dataEndpointAgent.getSecuredTransportPool(),
                            dataEndpointAgent.getAgentConfiguration().getBatchSize(),
                            dataEndpointAgent.getAgentConfiguration().getCorePoolSize(),
                            dataEndpointAgent.getAgentConfiguration().getMaxPoolSize(),
                            dataEndpointAgent.getAgentConfiguration().getKeepAliveTimeInPool());
                }
                DataEndpoint dataEndpoint = dataEndpointAgent.getNewDataEndpoint();
                dataEndpoint.initialize(endpointConfiguration);
                endpointGroup.addDataEndpoint(dataEndpoint);
            }
            endpointGroups.add(endpointGroup);
        }
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param event The Event that needs to sent for the receiver groups
     */
    public void publish(Event event) {
        for (DataEndpointGroup endpointGroup : endpointGroups) {
            endpointGroup.publish(event);
        }
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     */
    public void publish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray) {
        publish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     */
    public void publish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray, Map<String, String> arbitraryDataMap) {
        publish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray, arbitraryDataMap));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeStamp            Timestamp of the event.
     */
    public void publish(String streamId, long timeStamp, Object[] metaDataArray,
                        Object[] correlationDataArray, Object[] payloadDataArray) {
        publish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray, payloadDataArray));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeStamp            Timestamp of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     */
    public void publish(String streamId, long timeStamp, Object[] metaDataArray,
                        Object[] correlationDataArray, Object[] payloadDataArray,
                        Map<String, String> arbitraryDataMap) {
        publish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray,
                payloadDataArray, arbitraryDataMap));
    }

    private void onEventQueueFull(DataEndpointGroup endpointGroup, Event event) {
        this.failedEventCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastFailedEventTime > FAILED_EVENT_LOG_INTERVAL) {
            log.warn("Event queue is full, unable to process the event for endpoint group "
                    + endpointGroup.toString() + ", " + this.failedEventCount + " events dropped so far.");
            this.lastFailedEventTime = currentTime;
        }
        if (log.isDebugEnabled()) {
            log.debug("Dropped Event: " + event.toString() + " for the endpoint group " +
                    endpointGroup.toString());
        }
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will simply drop the event.
     *
     * @param event The event which needs to be published to the receiver groups
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(Event event) {
        boolean sent = true;
        for (DataEndpointGroup endpointGroup : endpointGroups) {
            try {
                endpointGroup.tryPublish(event);
                sent = true;
            } catch (EventQueueFullException e) {
                this.onEventQueueFull(endpointGroup, event);
                sent = false;
            }
        }
        return sent;
    }

    /**
     * Graceful shutdown of all the operations of the data publisher.
     * It will flush all the events to the relevant endpoint, and closes all the
     * resources and thread pools used for its operation. Once the shutdown operation
     * is called you can't publish events using the data publisher.
     *
     * @throws DataEndpointException Exception to be thrown when communicating with DataEndpoint.
     */
    public void shutdown() throws DataEndpointException {
        for (DataEndpointGroup dataEndpointGroup : endpointGroups) {
            dataEndpointGroup.shutdown();
        }
        dataEndpointAgent.shutDown(this);
    }

}
