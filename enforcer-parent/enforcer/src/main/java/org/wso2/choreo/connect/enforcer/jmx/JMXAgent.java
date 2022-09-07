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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX Client
 */
public class JMXAgent {
    private static final Logger logger = LoggerFactory.getLogger(JMXAgent.class);
    private static JMXConnectorServer jmxConnectorServer;
    private static String hostnameDefault = "";
    private static String rmiServerPort = "11111";
    private static String rmiRegistryPort = "9999";
    private static final String JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";

    public static void initJMXAgent() {
        try {

            String hostname = System.getProperty(JAVA_RMI_SERVER_HOSTNAME);
            if (hostname == null || hostname.isEmpty()) {
                hostname = hostnameDefault;
                System.setProperty(JAVA_RMI_SERVER_HOSTNAME, hostname);
            }

            String jmxURL = "service:jmx:rmi://" + hostname + ":" + rmiServerPort
                    + "/jndi/rmi://" + hostname + ":" + rmiRegistryPort + "/jmxrmi";
            JMXServiceURL jmxServiceURL = new JMXServiceURL(jmxURL);

            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null,
                    MBeanManagementFactory.getMBeanServer());
            jmxConnectorServer.start();
            logger.info("JMXServerManager JMX Service URL : " + jmxServiceURL.toString());
        } catch (Throwable throwable) {
            logger.error("Failed to start JMX Agent", throwable);
        }
    }
}
