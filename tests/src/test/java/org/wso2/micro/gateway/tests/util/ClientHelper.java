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

package org.wso2.micro.gateway.tests.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class ClientHelper {
    /**
     * Full qualified class name of the andes initial context factory
     */
    public static final String ANDES_INITIAL_CONTEXT_FACTORY = "org.wso2.andes.jndi" +
            ".PropertiesFileInitialContextFactory";

    /**
     * Connection factory name used
     */
    public static final String CONNECTION_FACTORY = "ConnectionFactory";

    public static InitialContextBuilder getInitialContextBuilder(String username,
                                                                 String password,
                                                                 String brokerHost,
                                                                 String port) {
        return new InitialContextBuilder(username, password, brokerHost, port);
    }

    public static class InitialContextBuilder {

        public static final String CONNECTION_FACTORY_PREFIX = "connectionfactory.";
        private final Properties contextProperties;
        private final String username;
        private final String password;
        private final String brokerHost;
        private final String port;

        public InitialContextBuilder(String username, String password, String brokerHost, String port) {
            this.username = username;
            this.password = password;
            this.brokerHost = brokerHost;
            this.port = port;
            contextProperties = new Properties();
            contextProperties.put(Context.INITIAL_CONTEXT_FACTORY, ANDES_INITIAL_CONTEXT_FACTORY);
            String connectionString = getBrokerConnectionString();
            contextProperties.put(CONNECTION_FACTORY_PREFIX + CONNECTION_FACTORY, connectionString);
        }

        public InitialContextBuilder withTopic(String topicName) {
            contextProperties.put("topic." + topicName, topicName);
            return this;
        }

        public InitialContext build() throws NamingException {
            return new InitialContext(contextProperties);
        }

        private String getBrokerConnectionString() {
            return "amqp://" + username + ":" + password + "@clientID/carbon?brokerlist='tcp://"
                    + brokerHost + ":" + port + "'";
        }
    }
}
