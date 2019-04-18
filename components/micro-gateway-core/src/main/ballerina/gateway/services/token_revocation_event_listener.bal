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


import ballerina/log;
import ballerina/io;
import ballerina/config;
import ballerina/jms;
import ballerina/http;

string jmsConnectioninitialContextFactoryTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID,
    REALTIME_JMS_CONNECTION_INITIAL_CONTEXT_FACTORY,
    "bmbInitialContextFactory");
string jmsConnectionProviderUrlTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, REALTIME_JMS_CONNECTION_PROVIDER_URL,
    "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'");
string jmsConnectionPasswordTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, REALTIME_JMS_CONNECTION_PASSWORD, "");
string jmsConnectionUsernameTokenRevocation = getConfigValue(REALTIME_MESSAGE_INSTANCE_ID, REALTIME_JMS_CONNECTION_USERNAME, "");

service jmsTokenRevocationListener =
service {
    resource function onMessage(jms:TopicSubscriberCaller consumer, jms:Message message) {
        printDebug(KEY_TOKEN_REVOCATION_JMS,"token revoked jms Message Received");
        map<any>|error mapMessage=message.getMapMessageContent();
        map<string> inputMap={};
        if (mapMessage is map<any>) {
            if (mapMessage.hasKey("revokedToken") && mapMessage.hasKey("ttl")) {
                string revokedToken = <string>mapMessage["revokedToken"];
                string ttl= <string>mapMessage["ttl"];
                inputMap[revokedToken]=ttl;
                var status= addToRevokedTokenMap(inputMap);
                if(status is boolean){
                    printDebug(KEY_TOKEN_REVOCATION_JMS, "Successfully added to revoked token map");
                }else{
                    printDebug(KEY_TOKEN_REVOCATION_JMS, "Error while ading revoked token to map");
                }
            }else{
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
public function startTokenRevocationSubscriberService() returns jms:TopicSubscriber {
    // Initialize a JMS connectiontion with the provider.
    jms:Connection jmsTokenRevocationConnection = new({
            initialContextFactory: jmsConnectioninitialContextFactoryTokenRevocation,
            providerUrl: jmsConnectionProviderUrlTokenRevocation,
            username: jmsConnectionUsernameTokenRevocation,
            password: jmsConnectionPasswordTokenRevocation
        });
    // Initialize a JMS session on top of the created connection.
    jms:Session jmsTokenRevocationSession = new(jmsTokenRevocationConnection, {
            acknowledgementMode: "AUTO_ACKNOWLEDGE"
        });

    jms:TopicSubscriber subscriberTokenRevocationEndpoint = new(jmsTokenRevocationSession, topicPattern = "jwtRevocation");
    _ = subscriberTokenRevocationEndpoint.__attach(jmsTokenRevocationListener, {});
    _ = subscriberTokenRevocationEndpoint.__start();
    return subscriberTokenRevocationEndpoint;
}

# `initiateTokenRevocationJmsListener` function initialize jmslistener subscriber service if `enabledTokenRevocation`
# is enabled
#
# + return - boolean value of jmslistener started or not
public function initiateTokenRevocationJmsListener() returns boolean {
    boolean enabledRealtimeMessage = getConfigBooleanValue(REALTIME_MESSAGE_INSTANCE_ID,
        REALTIME_MESSAGE_ENABLED, false);

    if (enabledRealtimeMessage) {
        jms:TopicSubscriber topicTokenRevocationSubscriber = startTokenRevocationSubscriberService();
        printInfo(KEY_TOKEN_REVOCATION_JMS , "subscriber service for token revocation is started");
        return true;
    }
    return false;
}
