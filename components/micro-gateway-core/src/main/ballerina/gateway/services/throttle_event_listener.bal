// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

string jmsConnectionInitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID,
    JMS_CONNECTION_INITIAL_CONTEXT_FACTORY, "bmbInitialContextFactory");
string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
    "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'");
string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, "");
string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, "");

service jmsListener =
service {
    resource function onMessage(jms:TopicSubscriberCaller consumer, jms:Message message) {
        map<any>|error m = message.getMapMessageContent();
        if (m is map<any>) {
            log:printDebug("ThrottleMessage Received");
            //Throttling decisions made by TM going to throttleDataMap
            if (m.hasKey(THROTTLE_KEY)) {
                GlobalThrottleStreamDTO globalThrottleStreamDtoTM = {
                    throttleKey: <string>m[THROTTLE_KEY],
                    isThrottled: <boolean>m[IS_THROTTLED],
                    expiryTimeStamp: <int>m[EXPIRY_TIMESTAMP] };

                if (globalThrottleStreamDtoTM.isThrottled == true) {
                    putThrottleData(globalThrottleStreamDtoTM);
                }
                else {
                    removeThrottleData(globalThrottleStreamDtoTM.throttleKey);
                }
                //Blocking decisions going to a separate map
            }
            else if (m.hasKey(BLOCKING_CONDITION_KEY)){
                putBlockCondition(m);
            }
        }
        else {
            log:printError("Error occurred while reading message", err = m);
        }
    }
};

# `startSubscriberService` function create jms connection, jms session and jms topic subscriber.
# It binds the subscriber endpoint and jms listener
#
# + return - jms:TopicSubscriber for global throttling event publishing
public function startSubscriberService() returns jms:TopicSubscriber {
    // Initialize a JMS connectiontion with the provider.
    jms:Connection jmsConnection = new({
            initialContextFactory:jmsConnectionInitialContextFactory,
            providerUrl: jmsConnectionProviderUrl,
            username: jmsConnectionUsername,
            password: jmsConnectionPassword
        });
    // Initialize a JMS session on top of the created connection.
    jms:Session jmsSession = new(jmsConnection, {
            acknowledgementMode: "AUTO_ACKNOWLEDGE"
        });

    jms:TopicSubscriber subscriberEndpoint = new(jmsSession, topicPattern = "throttleData");
    _ = subscriberEndpoint.__attach(jmsListener, {});
    _ = subscriberEndpoint.__start();
    return subscriberEndpoint;
}

# `initiateThrottlingJmsListener` function initialize jmslistener subscriber service if `enabledGlobalTMEventPublishing`
# is enabled
#
# + return - boolean value of jmslistener started or not
public function initiateThrottlingJmsListener() returns boolean {
    enabledGlobalTMEventPublishing = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID,
        GLOBAL_TM_EVENT_PUBLISH_ENABLED, false);

    if (enabledGlobalTMEventPublishing) {
        jms:TopicSubscriber topicSubscriber = startSubscriberService();
        log:printInfo("subscriber service for global throttling is started");
        return true;
    }
    return false;
}

