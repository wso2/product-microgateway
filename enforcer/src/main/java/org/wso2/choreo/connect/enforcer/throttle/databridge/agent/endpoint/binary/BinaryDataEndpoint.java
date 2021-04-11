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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary;

import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.SessionTimeoutException;
import org.wso2.carbon.databridge.commons.exception.UndefinedEventTypeException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.DataEndpoint;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;

import java.net.Socket;
import java.util.List;

import static org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinaryEventSender.processResponse;
import static org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinaryEventSender.sendBinaryLoginMessage;
import static org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinaryEventSender.sendBinaryLogoutMessage;
import static org.wso2.choreo.connect.enforcer.throttle.databridge.agent.endpoint.binary.BinaryEventSender.sendBinaryPublishMessage;

/**
 * This class is Binary transport implementation for the Data Endpoint.
 */
public class BinaryDataEndpoint extends DataEndpoint {

    @Override
    protected String login(Object client, String userName, String password) throws DataEndpointAuthenticationException {
        Socket socket = (Socket) client;
        try {
            sendBinaryLoginMessage(socket, userName, password);
            return processResponse(socket);
        } catch (Exception e) {
            if (e instanceof DataEndpointAuthenticationException) {
                throw (DataEndpointAuthenticationException) e;
            } else {
                throw new DataEndpointAuthenticationException("Error while trying to login to data receiver :"
                        + socket.getRemoteSocketAddress().toString(), e);
            }
        }
    }

    @Override
    protected void logout(Object client, String sessionId) throws DataEndpointAuthenticationException {
        Socket socket = (Socket) client;
        try {
            sendBinaryLogoutMessage(socket, sessionId);
            processResponse(socket);
        } catch (Exception e) {
            if (e instanceof DataEndpointAuthenticationException) {
                throw (DataEndpointAuthenticationException) e;
            } else {
                throw new DataEndpointAuthenticationException("Error while trying to logout to data receiver :"
                        + socket.getRemoteSocketAddress().toString(), e);
            }
        }
    }

    @Override
    protected void send(Object client, List<Event> events) throws DataEndpointException,
            SessionTimeoutException, UndefinedEventTypeException {
        Socket socket = (Socket) client;
        String sessionId = getDataEndpointConfiguration().getSessionId();
        try {
            sendBinaryPublishMessage(socket, events, sessionId);
            processResponse(socket);
        } catch (Exception e) {
            if (e instanceof DataEndpointException) {
                throw (DataEndpointException) e;
            } else if (e instanceof UndefinedEventTypeException) {
                throw new UndefinedEventTypeException("Undefined Event Type Exception ", e);
            } else if (e instanceof SessionTimeoutException) {
                throw new SessionTimeoutException("Binary Session Expired Exception ", e);
            } else {
                throw new DataEndpointException("Error while trying to publish events to data receiver :"
                        + socket.getRemoteSocketAddress().toString(), e);
            }
        }
    }
}
