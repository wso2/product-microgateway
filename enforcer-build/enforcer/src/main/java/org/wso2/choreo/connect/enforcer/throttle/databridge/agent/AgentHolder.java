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


import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.AgentConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;

/**
 * The holder the created agent and this is singleton class.
 */
public class AgentHolder {

    private static AgentHolder instance;
    private DataEndpointAgent agent;

    private AgentHolder() {
        agent = new DataEndpointAgent(AgentConfiguration.getInstance());
    }

    public static synchronized AgentHolder getInstance() {
        if (instance == null) {
            instance = new AgentHolder();
        }
        return instance;
    }

    public static synchronized void shutdown() throws DataEndpointException {
        if (instance != null) {
            instance.agent.shutDown();
            instance = null;
        }
    }

    /**
     * Returns the default agent,and the first element in the data.agent.config.yaml
     * is taken as default data publisher type.
     *
     * @return DataEndpointAgent for the default endpoint name.
     */
    public DataEndpointAgent getDataEndpointAgent() {
        return agent;
    }
}
