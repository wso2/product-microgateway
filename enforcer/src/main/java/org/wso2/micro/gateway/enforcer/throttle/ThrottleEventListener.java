/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.enforcer.throttle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * JMS event listener for throttle data.
 */
public class ThrottleEventListener implements MessageListener {
    private static final Log log = LogFactory.getLog(ThrottleEventListener.class);

    private ThrottleEventListener() {}

    public static void init() {
        String initialContextFactory = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
        String connectionFactoryNamePrefix = "connectionfactory.";
        String connectionFactoryName = "qpidConnectionfactory";
        String eventReceiverURL = "amqp://admin:admin@clientid/carbon?brokerlist='tcp://localhost:5672'";
        Runnable runnable = () -> {
            try {
                TopicConnection topicConnection;
                TopicSession topicSession;
                Properties properties = new Properties();
                properties.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
                properties.put(connectionFactoryNamePrefix + connectionFactoryName, eventReceiverURL);
                InitialContext context = new InitialContext(properties);
                TopicConnectionFactory connFactory = (TopicConnectionFactory) context.lookup(connectionFactoryName);
                topicConnection = connFactory.createTopicConnection();
                topicConnection.start();
                topicSession = topicConnection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
                Topic gatewayJmsTopic = topicSession.createTopic("throttleData");
                TopicSubscriber listener = topicSession.createSubscriber(gatewayJmsTopic);
                listener.setMessageListener(new ThrottleEventListener());
            } catch (NamingException | JMSException e) {
                log.error("Error while initiating jms connection...", e);
            }
        };
        Thread jmsThread = new Thread(runnable);
        jmsThread.start();
    }

    @Override
    public void onMessage(Message message) {
        if (message == null) {
            log.warn("Dropping the empty/null event received through jms receiver");
            return;
        } else if (!(message instanceof MapMessage)) {
            log.warn("Event dropped due to unsupported message type " + message.getClass());
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Event received in JMS Event Receiver - " + message);
        }

        try {
            Topic jmsDestination = (Topic) message.getJMSDestination();
            MapMessage mapMessage = (MapMessage) message;
            Map<String, Object> map = new HashMap<String, Object>();
            Enumeration enumeration = mapMessage.getMapNames();
            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                map.put(key, mapMessage.getObject(key));
            }

            if (APIConstants.TopicNames.TOPIC_THROTTLE_DATA.equalsIgnoreCase(jmsDestination.getTopicName())) {
                if (map.get(APIConstants.THROTTLE_KEY) != null) {
                    /*
                     * This message contains throttle data in map which contains Keys
                     * throttleKey - Key of particular throttling level
                     * isThrottled - Whether message has throttled or not
                     * expiryTimeStamp - When the throttling time window will expires
                     */

                    log.info("Event THROTTLE_KEY received in JMS Event Receiver - " + message);
//                    handleThrottleUpdateMessage(map);
                } else if (map.get(APIConstants.BLOCKING_CONDITION_KEY) != null) {
                    /*
                     * This message contains blocking condition data
                     * blockingCondition - Blocking condition type
                     * conditionValue - blocking condition value
                     * state - State whether blocking condition is enabled or not
                     */
                    log.info("Event BLOCKING_CONDITION_KEY received in JMS Event Receiver - " + message);
//                    handleBlockingMessage(map);
                } else if (map.get(APIConstants.POLICY_TEMPLATE_KEY) != null) {
                    /*
                     * This message contains key template data
                     * keyTemplateValue - Value of key template
                     * keyTemplateState - whether key template active or not
                     */
                    log.info("Event POLICY_TEMPLATE_KEY received in JMS Event Receiver - " + message);
//                    handleKeyTemplateMessage(map);
                }
            }
        } catch (JMSException e) {
            log.error("Error occurred when processing the received message ", e);
        }
    }
}
