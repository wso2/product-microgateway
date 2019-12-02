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

string jmsConnectionInitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID,
     JMS_CONNECTION_INITIAL_CONTEXT_FACTORY, "bmbInitialContextFactory");
string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
     "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'");
string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, "");
string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, "");

service messageServ = service {
    resource function onMessage(jms:Message message) {
        printDebug(KEY_THROTTLE_UTIL, "ThrottleMessage received.");
        if (message is jms:MapMessage) {
            string?|error throttleKey = message.getString(THROTTLE_KEY);
            boolean|error throttleEnable = message.getBoolean(IS_THROTTLED);
            int|error expiryTime = message.getLong(EXPIRY_TIMESTAMP);
            string?|error blockingKey = message.getString(BLOCKING_CONDITION_KEY);
            if (throttleKey is string) {
                printDebug(KEY_THROTTLE_UTIL, "Throttle Key : " + throttleKey.toString() + " Throttle status : " +
                throttleEnable.toString());
                if (throttleEnable is boolean && expiryTime is int) {
                    GlobalThrottleStreamDTO globalThrottleStreamDtoTM = {
                    throttleKey: throttleKey,
                    isThrottled: throttleEnable,
                    expiryTimeStamp: expiryTime };
                    if (globalThrottleStreamDtoTM.isThrottled == true) {
                        printDebug(KEY_THROTTLE_UTIL, "Adding to throttledata map.");
                        putThrottleData(globalThrottleStreamDtoTM);
                    } else {
                        printDebug(KEY_THROTTLE_UTIL, "Romoving from throttledata map.");
                        removeThrottleData(globalThrottleStreamDtoTM.throttleKey);
                    }
                }  else {
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
 public function startSubscriberService() returns @tainted jms:MessageConsumer|error {
     // Initialize a JMS connection  with the provider.
     jms:Connection connection = check jms:createConnection({
                    initialContextFactory: jmsConnectionInitialContextFactory,
                    providerUrl: jmsConnectionProviderUrl,
                    username: jmsConnectionUsername,
                    password: jmsConnectionPassword

               });
     jms:Session session = check connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
     jms:Destination dest = check session->createTopic("throttleData");
     jms:MessageConsumer subscriberEndpoint = check session->createDurableSubscriber(dest, "sub-1");
     var attachResult = subscriberEndpoint.__attach(messageServ);
     if (attachResult is error) {
          printDebug(KEY_THROTTLE_UTIL, "Message consumer hasn't been attached to the service.");
     }
     var startResult = subscriberEndpoint.__start();
     if (startResult is error) {
         printDebug(KEY_THROTTLE_UTIL, "Starting the task is failed.");
     }
     return subscriberEndpoint;
 }


 # `initiateThrottlingJmsListener` function initialize jmslistener subscriber service if `enabledGlobalTMEventPublishing`
 # is true
 #
 # + return - boolean value of jmslistener started or not
 public function initiateThrottlingJmsListener() returns boolean {
     enabledGlobalTMEventPublishing = getConfigBooleanValue(THROTTLE_CONF_INSTANCE_ID,
         GLOBAL_TM_EVENT_PUBLISH_ENABLED, false);
     if (!enabledGlobalTMEventPublishing) {
        return false;
     } else {
         jms:MessageConsumer|error topicSubscriber = trap startSubscriberService();
         if (topicSubscriber is jms:MessageConsumer) {
            printDebug(KEY_THROTTLE_UTIL, "subscriber service for global throttling is started.");
            return true;
         } else {
             printDebug(KEY_THROTTLE_UTIL, "Error while starting subscriber service for global throttling");
             return false;
         }
     }
 }
