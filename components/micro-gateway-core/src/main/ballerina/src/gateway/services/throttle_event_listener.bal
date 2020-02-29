// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

string jmsConnectionInitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_INITIAL_CONTEXT_FACTORY,
    DEFAULT_JMS_CONNECTION_INITIAL_CONTEXT_FACTORY);
string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
    DEFAULT_JMS_CONNECTION_PROVIDER_URL);
string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, DEFAULT_JMS_CONNECTION_PASSWORD);
string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, DEFAULT_JMS_CONNECTION_USERNAME);

service messageServ = service {
    resource function onMessage(jms:Message message) {
        if (message is jms:MapMessage) {
            string? | error throttleKey = message.getString(THROTTLE_KEY);
            int | error expiryTime = message.getLong(EXPIRY_TIMESTAMP);
            boolean | error throttleEnable = message.getBoolean(IS_THROTTLED);
            int remainingQuota = 0;
            string? | error blockingKey = message.getString(BLOCKING_CONDITION_KEY);
            if (throttleKey is string) {
                printDebug(KEY_THROTTLE_UTIL, "policy Key : " + throttleKey.toString() + " Throttle status : " +
                throttleEnable.toString());
                if (throttleEnable is boolean && expiryTime is int) {
                    GlobalThrottleStreamDTO globalThrottleStreamDtoTM = {
                        policyKey: throttleKey,
                        resetTimestamp: expiryTime,
                        remainingQuota: remainingQuota,
                        isThrottled: throttleEnable
                    };
                    if (globalThrottleStreamDtoTM.isThrottled == true) {
                        printDebug(KEY_THROTTLE_UTIL, "Adding to throttledata map.");
                        putThrottleData(globalThrottleStreamDtoTM, throttleKey);
                    } else {
                        printDebug(KEY_THROTTLE_UTIL, "Romoving from throttledata map.");
                        removeThrottleData(throttleKey);
                    }
                } else {
                    printDebug(KEY_THROTTLE_UTIL, "Throlling configs values are wrong.");
                }
            } else if (blockingKey is string) {
                printDebug(KEY_THROTTLE_UTIL, "Romoving from the throttledata map.");
                putBlockCondition(message);
            }
        } else {
            printDebug(KEY_THROTTLE_UTIL, "Error occurred while reading throttle message.");
        }
    }
};

# `startSubscriberService` function create jms connection, jms session and jms topic subscriber.
# It binds the subscriber endpoint and jms listener
#
# + return - jms:TopicSubscriber for global throttling event publishing
public function startSubscriberService() returns @tainted jms:MessageConsumer | error {
    // Initialize a JMS connection  with the provider.
    jms:Connection | error connection = trap jms:createConnection({
        initialContextFactory: jmsConnectionInitialContextFactory,
        providerUrl: jmsConnectionProviderUrl,
        username: jmsConnectionUsername,
        password: jmsConnectionPassword

    });
    if (connection is error) {
        printError(KEY_THROTTLE_UTIL, "Error while creating the jms connection.", connection);
        return connection;
    } else {
        jms:Session | error session = trap connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
        if (session is error) {
            printError(KEY_THROTTLE_UTIL, "Error while creating the jms session.", session);
            return session;
        } else {
            jms:Destination dest = check session->createTopic("throttleData");
            jms:MessageConsumer | error subscriberEndpoint = trap session->createDurableSubscriber(dest, "sub-1");
            if (subscriberEndpoint is error) {
                printError(KEY_THROTTLE_UTIL, "Error while creating the jms subscriber.", subscriberEndpoint);
            } else {
                var attachResult = subscriberEndpoint.__attach(messageServ);
                if (attachResult is error) {
                    printError(KEY_THROTTLE_UTIL, "Message consumer hasn't been attached to the service.", attachResult);
                    return attachResult;
                }
                var startResult = subscriberEndpoint.__start();
                if (startResult is error) {
                    printError(KEY_THROTTLE_UTIL, "Starting the task is failed.", startResult);
                    return startResult;
                }
                printDebug(KEY_THROTTLE_UTIL, "Successfully created jms connection");
            }

            return subscriberEndpoint;
        }
    }
}


# `initiateThrottlingJmsListener` function initialize jmslistener subscriber service if `enabledGlobalTMEventPublishing`
# is true
#
# + return - boolean value of jmslistener started or not
public function initiateThrottlingJmsListener() returns boolean {
    enabledGlobalTMEventPublishing = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID, GLOBAL_TM_EVENT_PUBLISH_ENABLED,
        DEFAULT_GLOBAL_TM_EVENT_PUBLISH_ENABLED);
    if (!enabledGlobalTMEventPublishing) {
        return false;
    } else {
        jms:MessageConsumer | error topicSubscriber = trap startSubscriberService();
        if (topicSubscriber is jms:MessageConsumer) {
            printDebug(KEY_THROTTLE_UTIL, "subscriber service for global throttling is started.");
            return true;
        } else {
            printError(KEY_THROTTLE_UTIL, "Error while starting subscriber service for global throttling");
            return false;
        }
    }
}
