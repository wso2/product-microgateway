// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/java.jms;

string jmsConnectioninitialContextFactoryTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, 
    JMS_CONNECTION_INITIAL_CONTEXT_FACTORY, DEFAULT_JMS_CONNECTION_INITIAL_CONTEXT_FACTORY);
string jmsConnectionProviderUrlTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
    DEFAULT_JMS_CONNECTION_PROVIDER_URL);
string jmsConnectionPasswordTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, JMS_CONNECTION_PASSWORD, 
    DEFAULT_JMS_CONNECTION_PASSWORD);
string jmsConnectionUsernameTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, JMS_CONNECTION_USERNAME, 
    DEFAULT_JMS_CONNECTION_USERNAME);
string tokenRevocationJMSTopic = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, REALTIME_JMS_CONNECTION_TOPIC, 
    DEFAULT_REALTIME_JMS_CONNECTION_TOPIC);

service jmsTokenRevocation = service {
    resource function onMessage(jms:Message message) {
        printDebug(KEY_TOKEN_REVOCATION_JMS, "Token revoked jms Message Received");
        if (message is jms:MapMessage) {
            string? | error ttl = message.getString("ttl");
            string? | error revokedToken = message.getString("revokedToken");
            map<string> inputMap = {};
            if (ttl is string && revokedToken is string) {
                inputMap[revokedToken] = ttl;
                var status = addToRevokedTokenMap(inputMap);
                if (status is boolean) {
                    printDebug(KEY_TOKEN_REVOCATION_JMS, "Successfully added to revoked token map");
                } else {
                    printDebug(KEY_TOKEN_REVOCATION_JMS, "Error while ading revoked token to map");
                }
            } else {
                printDebug(KEY_TOKEN_REVOCATION_JMS, "No keys named revokedToken and ttl");
            }
        } else {
            printError(KEY_TOKEN_REVOCATION_JMS, "Error occurred while reading message");
        }
    }
};

# `startSubscriberService` function create jms connection, jms session and jms topic subscriber.
# It binds the subscriber endpoint and jms listener
#
# + return - jms:TopicSubscriber for token Revocation
public function startTokenRevocationSubscriberService() returns @tainted jms:MessageConsumer | error {
    // Initialize a JMS connection  with the provider.
    jms:Connection | error connection = trap jms:createConnection({
        initialContextFactory: jmsConnectioninitialContextFactoryTokenRevocation,
        providerUrl: jmsConnectionProviderUrlTokenRevocation,
        username: jmsConnectionUsernameTokenRevocation,
        password: jmsConnectionPasswordTokenRevocation

    });

    if (connection is error) {
        printError(KEY_TOKEN_REVOCATION_JMS, "Error while creating the jms connection.", connection);
        return connection;
    } else {
        jms:Session | error session = trap connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
        if (session is error) {
            printError(KEY_TOKEN_REVOCATION_JMS, "Error while creating the jms session.", session);
            return session;
        } else {
            jms:Destination dest = check session->createTopic(tokenRevocationJMSTopic);
            jms:MessageConsumer | error subscriberEndpoint = trap session->createConsumer(dest);
            if (subscriberEndpoint is error) {
                printError(KEY_TOKEN_REVOCATION_JMS, "Error while creating the jms subscriber.", subscriberEndpoint);
            } else {
                var attachResult = subscriberEndpoint.__attach(jmsTokenRevocation);
                if (attachResult is error) {
                    printError(KEY_TOKEN_REVOCATION_JMS, "Message consumer hasn't been attached to the service.", attachResult);
                    return attachResult;
                }
                var startResult = subscriberEndpoint.__start();
                if (startResult is error) {
                    printError(KEY_TOKEN_REVOCATION_JMS, "Starting the task is failed.", startResult);
                    return startResult;
                }
                printDebug(KEY_TOKEN_REVOCATION_JMS, "Successfully created jms connection");
            }

            return subscriberEndpoint;
        }
    }
}

# `initiateTokenRevocationJmsListener` function initialize jmslistener subscriber service if `enabledTokenRevocation`
# is enabled
#
# + return - boolean value of jmslistener started or not
public function initiateTokenRevocationJmsListener() returns boolean {
    boolean enabledRealtimeMessage = getConfigBooleanValue(REALTIME_MESSAGE_INSTANCE_ID, REALTIME_MESSAGE_ENABLED, 
        DEFAULT_TOKEN_REVOCATION_ENABLED);
    if (enabledRealtimeMessage) {
        jms:MessageConsumer | error topicTokenRevocationSubscriber = trap startTokenRevocationSubscriberService();
        if (topicTokenRevocationSubscriber is jms:MessageConsumer) {
            printInfo(KEY_TOKEN_REVOCATION_JMS, "subscriber service for token revocation is started");
            return true;
        } else {
            printError(KEY_TOKEN_REVOCATION_JMS, "Error while starting subscriber service for token revocation", topicTokenRevocationSubscriber);
            return false;
        }
    }
    return false;
}
