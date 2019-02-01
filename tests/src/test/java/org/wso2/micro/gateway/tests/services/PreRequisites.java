package org.wso2.micro.gateway.tests.services;

import org.testng.annotations.BeforeSuite;
import org.wso2.micro.gateway.tests.common.JMSPublisher;

public class PreRequisites {
    @BeforeSuite
    private void initializeMessageBroker() throws Exception {
        JMSPublisher jmsPublisher = new JMSPublisher();
        jmsPublisher.startMessageBroker();
        System.out.println("JMS Message Broker");
    }
}
