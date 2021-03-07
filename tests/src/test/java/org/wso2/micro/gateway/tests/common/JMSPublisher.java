/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.micro.gateway.tests.common;

import com.google.gson.JsonObject;
import io.ballerina.messaging.broker.EmbeddedBroker;
import org.wso2.micro.gateway.tests.util.ClientHelper;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Date;

/**
 * JMS publisher to publish throttled data to throttleData
 */
public class JMSPublisher {
    public static EmbeddedBroker broker = new EmbeddedBroker();

    public void startMessageBroker() throws Exception {
        broker.start();
    }

    public void getJson(JsonObject jsonObject) throws JMSException, NamingException {
        String appKey = jsonObject.getAsJsonObject(JMSPublisherConstants.EVENT)
                .getAsJsonObject(JMSPublisherConstants.PAYLOAD_DATA)
                .get(JMSPublisherConstants.APP_KEY).getAsString();
        String subscriptionKey = jsonObject.getAsJsonObject(JMSPublisherConstants.EVENT)
                .getAsJsonObject(JMSPublisherConstants.PAYLOAD_DATA)
                .get(JMSPublisherConstants.SUBSCRIPTION_KEY).getAsString();

        String appTier = jsonObject.getAsJsonObject(JMSPublisherConstants.EVENT)
                .getAsJsonObject(JMSPublisherConstants.PAYLOAD_DATA).get(JMSPublisherConstants.APP_TIER)
                .getAsString();
        String subscriptionTier = jsonObject.getAsJsonObject(JMSPublisherConstants.EVENT)
                .getAsJsonObject(JMSPublisherConstants.PAYLOAD_DATA).get(JMSPublisherConstants.SUBSCRIPTION_TIER)
                .getAsString();

        if (appTier.equals(JMSPublisherConstants.TEN_MIN_APP_POLICY)) {
            publishMessage(appKey);
        } else if (subscriptionTier.equals(JMSPublisherConstants.TEN_MIN_SUB_POLICY) ||
                subscriptionTier.equals(JMSPublisherConstants.UNAUTHENTICATED)) {
            publishMessage(subscriptionKey);
        }
    }

    public void publishMessage(String msg) throws NamingException, JMSException {
        InitialContext initialContext = ClientHelper.getInitialContextBuilder(JMSPublisherConstants.BROKER_USERNAME,
                JMSPublisherConstants.BROKER_PASSWORD, JMSPublisherConstants.BROKER_HOST,
                JMSPublisherConstants.BROKER_PORT)
                .withTopic(JMSPublisherConstants.THROTTLE_DATA_TOPIC)
                .build();
        ConnectionFactory connectionFactory
                = (ConnectionFactory) initialContext.lookup(ClientHelper.CONNECTION_FACTORY);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = (Topic) initialContext.lookup(JMSPublisherConstants.THROTTLE_DATA_TOPIC);
        MessageProducer producer = session.createProducer(topic);

        MapMessage mapMessage = session.createMapMessage();
        mapMessage.setString(JMSPublisherConstants.THROTTLE_KEY, msg);
        Date date = new Date();
        long time = date.getTime() + 1000;
        mapMessage.setLong(JMSPublisherConstants.EXPIRYTIMESTAMP, time);
        mapMessage.setBoolean(JMSPublisherConstants.IS_THROTTLED, true);
        producer.send(mapMessage);

        connection.close();
    }
}
