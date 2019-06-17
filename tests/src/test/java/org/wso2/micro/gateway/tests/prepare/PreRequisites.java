package org.wso2.micro.gateway.tests.prepare;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.Assert;
import org.wso2.micro.gateway.tests.common.JMSPublisher;
import org.wso2.micro.gateway.tests.common.MockBackEndServer;
import org.wso2.micro.gateway.tests.context.Utils;

public class PreRequisites {
    private MockBackEndServer mockBackEndServer;
    @BeforeSuite
    private void initializeMessageBroker() throws Exception {
        JMSPublisher jmsPublisher = new JMSPublisher();
        jmsPublisher.startMessageBroker();
        System.out.println("JMS Message Broker");
    }

    @BeforeSuite
    public void startMockBackendServer() {
        int port = 2380;
        boolean isOpen = Utils.isPortOpen(port);
        Assert.assertFalse(isOpen, "Port: " + port + " already in use.");
        mockBackEndServer = new MockBackEndServer(port);
        mockBackEndServer.start();
    }

    @AfterSuite
    public void stop() throws Exception {
        //Stop all the mock servers
        mockBackEndServer.stopIt();

    }
}
