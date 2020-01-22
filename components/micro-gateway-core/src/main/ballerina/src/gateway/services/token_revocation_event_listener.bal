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

import wso2/jms;

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
        printDebug(KEY_TOKEN_REVOCATION_JMS, "token revoked jms Message Received");
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
    jms:Connection connection = check jms:createConnection({
        initialContextFactory: jmsConnectioninitialContextFactoryTokenRevocation,
        providerUrl: jmsConnectionProviderUrlTokenRevocation,
        username: jmsConnectionUsernameTokenRevocation,
        password: jmsConnectionPasswordTokenRevocation

    });

    jms:Session session = check connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
    jms:Destination dest = check session->createTopic(tokenRevocationJMSTopic);
    jms:MessageConsumer subscriberEndpoint = check session->createDurableSubscriber(dest, "sub-2");
    var attachResult = subscriberEndpoint.__attach(jmsTokenRevocation);
    if (attachResult is error) {
        printDebug(KEY_THROTTLE_UTIL, "Message consumer hasn't been attached to the service.");
    }
    var startResult = subscriberEndpoint.__start();
    if (startResult is error) {
        printDebug(KEY_THROTTLE_UTIL, "Starting the task is failed.");
    }
    return subscriberEndpoint;
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
            printError(KEY_TOKEN_REVOCATION_JMS, "Error while starting subscriber service for token revocation");
            return false;
        }
    }
    return false;
}
