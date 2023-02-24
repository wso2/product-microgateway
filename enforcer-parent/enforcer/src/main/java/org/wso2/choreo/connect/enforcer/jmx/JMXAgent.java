/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.choreo.connect.enforcer.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX Connector Agent
 */
public class JMXAgent {
    private static final Logger logger = LogManager.getLogger(JMXAgent.class);
    private static JMXConnectorServer jmxConnectorServer;
    private static final String DEFAULT_RMI_SERVER_PORT = "11111";
    private static final String DEFAULT_RMI_REGISTRY_PORT = "9999";
    private static final String JAVA_JMX_RMI_SERVICE_PORT = "com.sun.management.jmxremote.port";
    private static final String JAVA_JMX_RMI_REGISTRY_PORT = "com.sun.management.jmxremote.rmi.port";

    public static void initJMXAgent() {
        if (JMXUtils.isJMXMetricsEnabled()) {
            try {
                String hostname = InetAddress.getLocalHost().getHostAddress();
                String rmiServerPort = System.getProperty(JAVA_JMX_RMI_SERVICE_PORT, DEFAULT_RMI_SERVER_PORT);
                String rmiRegistryPort = System.getProperty(JAVA_JMX_RMI_REGISTRY_PORT, DEFAULT_RMI_REGISTRY_PORT);

                LocateRegistry.createRegistry(Integer.parseInt(rmiRegistryPort));

                String jmxURL = String.format("service:jmx:rmi://%s:%s/jndi/rmi://%s:%s/jmxrmi", hostname,
                        rmiServerPort,
                        hostname, rmiRegistryPort);
                JMXServiceURL jmxServiceURL = new JMXServiceURL(jmxURL);

                jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null,
                        MBeanManagementFactory.getMBeanServer());
                jmxConnectorServer.start();
                logger.info("JMXAgent JMX Service URL : " + jmxServiceURL.toString());
            } catch (Throwable throwable) {
                logger.error("Failed to start JMX Agent", ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6805),
                        throwable);
            }
        }
    }
}
