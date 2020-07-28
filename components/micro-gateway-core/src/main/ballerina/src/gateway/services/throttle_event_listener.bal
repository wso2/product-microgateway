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

import ballerina/io;
import ballerina/stringutils;
import ballerina/java.jms;

string jmsConnectionInitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_INITIAL_CONTEXT_FACTORY,
DEFAULT_JMS_CONNECTION_INITIAL_CONTEXT_FACTORY);
string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
DEFAULT_JMS_CONNECTION_PROVIDER_URL);
string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, DEFAULT_JMS_CONNECTION_PASSWORD);
string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, DEFAULT_JMS_CONNECTION_USERNAME);

map<string> keyTemplateMap = {};
map<string> blockConditionsMap = {};
table<IPRangeDTO> IpBlockConditionsMap = table {
    {},
    []

};

service messageServ = service {
    resource function onMessage(jms:Message message) {
        if (message is jms:MapMessage) {
            string? | error keyTemplateValue = message.getString(KEY_TEMPLATE_VALUE);
            string? | error throttleKey = message.getString(THROTTLE_KEY);
            string? | error evaluatedConditions = message.getString(EVALUATED_CONDITIONS);
            int remainingQuota = 0;
            string? | error blockingKey = message.getString(BLOCKING_CONDITION_KEY);
            if (keyTemplateValue is string) {
                handleKeyTemplateMessage(message, keyTemplateValue);
            } else if (throttleKey is string) {
                boolean | error throttleEnable = message.getBoolean(IS_THROTTLED);
                int | error expiryTime = message.getLong(EXPIRY_TIMESTAMP);
                printDebug(KEY_THROTTLE_EVENT_LISTENER, "policy Key : " + throttleKey.toString() + " Throttle status : " +
                throttleEnable.toString());
                if (throttleEnable is boolean && expiryTime is int) {
                    APICondition | error condition = extractAPIorResourceKey(throttleKey);
                    GlobalThrottleStreamDTO globalThrottleStreamDtoTM = {
                        policyKey: throttleKey,
                        resetTimestamp: expiryTime,
                        remainingQuota: remainingQuota,
                        isThrottled: throttleEnable
                    };

                    if (globalThrottleStreamDtoTM.isThrottled == true) {
                        printDebug(KEY_THROTTLE_EVENT_LISTENER, "Adding to throttledata map.");
                        putThrottleData(globalThrottleStreamDtoTM, throttleKey);

                        if (condition is APICondition && evaluatedConditions is string) {
                            string resourceKey = condition.resourceKey;
                            string conditionKey = condition.name;
                            ConditionDto[] conditions = extractConditionDto(evaluatedConditions);
                            putThrottledConditions(conditions, resourceKey, conditionKey);
                        }
                    } else {
                        printDebug(KEY_THROTTLE_EVENT_LISTENER, "Romoving from throttledata map.");
                        removeThrottleData(throttleKey);
                        if (condition is APICondition) {
                            string resourceKey = condition.resourceKey;
                            string conditionKey = condition.name;
                            removeThrottledConditions(resourceKey, conditionKey);
                        }
                    }
                } else {
                    printDebug(KEY_THROTTLE_EVENT_LISTENER, "Throlling configs values are wrong.");
                }
            } else if (blockingKey is string) {
                printDebug(KEY_THROTTLE_EVENT_LISTENER, "Blocking condition retrieved : " + blockingKey);
                handleBlockConditionMessage(message);
            }
        } else {
            printDebug(KEY_THROTTLE_EVENT_LISTENER, "Error occurred while reading throttle message.");
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
        printError(KEY_THROTTLE_EVENT_LISTENER, "Error while creating the jms connection.", connection);
        return connection;
    } else {
        jms:Session | error session = trap connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
        if (session is error) {
            printError(KEY_THROTTLE_EVENT_LISTENER, "Error while creating the jms session.", session);
            return session;
        } else {
            jms:Destination dest = check session->createTopic("throttleData");
            jms:MessageConsumer | error subscriberEndpoint = trap session->createConsumer(dest);
            if (subscriberEndpoint is error) {
                printError(KEY_THROTTLE_EVENT_LISTENER, "Error while creating the jms subscriber.", subscriberEndpoint);
            } else {
                var attachResult = subscriberEndpoint.__attach(messageServ);
                if (attachResult is error) {
                    printError(KEY_THROTTLE_EVENT_LISTENER, "Message consumer hasn't been attached to the service.", attachResult);
                    return attachResult;
                }
                var startResult = subscriberEndpoint.__start();
                if (startResult is error) {
                    printError(KEY_THROTTLE_EVENT_LISTENER, "Starting the task is failed.", startResult);
                    return startResult;
                }
                printDebug(KEY_THROTTLE_EVENT_LISTENER, "Successfully created jms connection");
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
            printDebug(KEY_THROTTLE_EVENT_LISTENER, "subscriber service for global throttling is started.");
            return true;
        } else {
            printError(KEY_THROTTLE_EVENT_LISTENER, "Error while starting subscriber service for global throttling");
            return false;
        }
    }
}

function handleKeyTemplateMessage(jms:MapMessage message, string keyTemplateValue) {
    printDebug(KEY_THROTTLE_EVENT_LISTENER, "Key template value : " + keyTemplateValue.toString());
    string? | error keyTemplateState = message.getString(KEY_TEMPLATE_STATE);
    if (keyTemplateState is string) {
        printDebug(KEY_THROTTLE_EVENT_LISTENER, "Key template state : " + keyTemplateState.toString());
        if (stringutils:equalsIgnoreCase(KEY_TEMPLATE_ADD, keyTemplateState)) {
            keyTemplateMap[keyTemplateValue] = <@untainted>keyTemplateValue;
            printDebug(KEY_THROTTLE_EVENT_LISTENER, "Key template key : " + keyTemplateValue.toString() + " added to the map");
        } else {
            string removedValue = keyTemplateMap.remove(keyTemplateValue);
            printDebug(KEY_THROTTLE_EVENT_LISTENER, "Key template key : " + keyTemplateValue.toString() + " with value : " +
            removedValue + " removed from the map");
        }
    }
}

function handleBlockConditionMessage(jms:MapMessage m) {
    string? | error condition = m.getString(BLOCKING_CONDITION_KEY);
    string? | error conditionValue = m.getString(BLOCKING_CONDITION_VALUE);
    string? | error conditionState = m.getString(BLOCKING_CONDITION_STATE);
    if (condition is string && conditionValue is string) {
        printDebug(KEY_THROTTLE_EVENT_LISTENER, "Block condition retrived with type : " + condition + " and value : " + conditionValue);
        if (conditionState is string && conditionState == TRUE) {
            blockConditionExist = true;
            if (stringutils:equalsIgnoreCase(condition, BLOCKING_CONDITION_IP) ||
            stringutils:equalsIgnoreCase(condition, BLOCKING_CONDITION_IP_RANGE)) {
                io:StringReader sr = new (conditionValue);
                json ip = checkpanic sr.readJson();
                if (ip is map<json>) {
                    printDebug(KEY_THROTTLE_EVENT_LISTENER, "IP Blocking condition json : " + ip.toJsonString());
                    int? | error conditionId = m.getInt(BLOCKING_CONDITION_ID);
                    string? | error conditionTenant = m.getString(BLOCKING_CONDITION_TENANAT_DOMAIN);
                    if (conditionId is int && conditionTenant is string) {
                        ip[BLOCKING_CONDITION_TYPE] = condition;
                        ip[BLOCKING_CONDITION_ID] = conditionId;
                        ip[BLOCKING_CONDITION_TENANAT_DOMAIN] = conditionTenant;
                    }
                    addIpDataToBlockConditionTable(ip);
                    printDebug(KEY_THROTTLE_EVENT_LISTENER, "Block condition added to the IP block condition map.");
                } else {
                    printDebug(KEY_THROTTLE_EVENT_LISTENER, "IP blocking condition could not be added to the map : " +
                    ip.toJsonString());
                }
            } else {
                blockConditionsMap[conditionValue] = <@untainted>conditionValue;
                printDebug(KEY_THROTTLE_EVENT_LISTENER, "Block condition added to the map.");
                blockConditionExist = true;
            }

        } else {
            if (stringutils:equalsIgnoreCase(condition, BLOCKING_CONDITION_IP) ||
            stringutils:equalsIgnoreCase(condition, BLOCKING_CONDITION_IP_RANGE)) {
                int? | error conditionId = m.getInt(BLOCKING_CONDITION_ID);
                if (conditionId is int) {
                    removeIpDataFromBlockConditionTable(conditionId);
                } else {
                    printError(KEY_THROTTLE_EVENT_LISTENER, "Error while removing the IP blocking condition with id : ", conditionId);
                }
            } else {
                _ = blockConditionsMap.remove(conditionValue);
                printDebug(KEY_THROTTLE_EVENT_LISTENER, "Block condition removed from the map.");
            }
            if (blockConditionsMap.keys().length() == 0 && !IpBlockConditionsMap.hasNext()) {
                blockConditionExist = false;
            }

        }
        printDebug(KEY_THROTTLE_EVENT_LISTENER, "Blocking condition map : " + blockConditionsMap.toJsonString());
        printDebug(KEY_THROTTLE_EVENT_LISTENER, "Blocking IP condition map : " + IpBlockConditionsMap.toString());
    }
}
