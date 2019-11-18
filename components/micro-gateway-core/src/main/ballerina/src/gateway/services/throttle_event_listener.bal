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
import wso2/jms;

 string jmsConnectionInitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID,
     JMS_CONNECTION_INITIAL_CONTEXT_FACTORY, "bmbInitialContextFactory");
 string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
     "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'");
 string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, "");
 string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, "");

service messageServ = service {
    resource function onMessage(jms:Message message) {
        printDebug(KEY_THROTTLE_UTIL, "ThrottleMessage Received-----------------------" + message.toString());
        if (message is jms:MapMessage) {
            printDebug(KEY_THROTTLE_UTIL, "message is map message----------------------");
            string?|error throttleKey = message.getString(THROTTLE_KEY);
            boolean|error throttleEnable = message.getBoolean(IS_THROTTLED);
            int|error expiryTime = message.getLong(EXPIRY_TIMESTAMP);
            string?|error blockingKey = message.getString(BLOCKING_CONDITION_KEY);
            if (throttleKey is string) {
                printDebug(KEY_THROTTLE_UTIL, "enter to the throttle key" + throttleKey.toString() + "enable throttle "+  throttleEnable.toString());
                if (throttleEnable is boolean && expiryTime is int) {
                        GlobalThrottleStreamDTO globalThrottleStreamDtoTM = {
                        throttleKey: throttleKey,
                        isThrottled: throttleEnable,
                        expiryTimeStamp: expiryTime };
                        if (globalThrottleStreamDtoTM.isThrottled == true) {
                            putThrottleData(globalThrottleStreamDtoTM);
                        } else {
                            printDebug(KEY_THROTTLE_UTIL, "remove throttledata" );
                            removeThrottleData(globalThrottleStreamDtoTM.throttleKey);
                        }
                }  else {
                   log:printInfo("Throlling configs values are wrong.");
                }
            } else if (blockingKey is string) {
                   //putBlockCondition();
            }
        } else {
            log:printError("Error occurred while reading message");
        }
    }
};
 //service jmsListener =
 //service {
 //    resource function onMessage(jms:TopicSubscriberCaller consumer, jms:Message message) {
 //        map<any>|error m = message.getMapMessageContent();
 //        if (m is map<any>) {
 //            log:printDebug("ThrottleMessage Received");
 //            //Throttling decisions made by TM going to throttleDataMap
 //            if (m.hasKey(THROTTLE_KEY)) {
 //                GlobalThrottleStreamDTO globalThrottleStreamDtoTM = {
 //                    throttleKey: <string>m[THROTTLE_KEY],
 //                    isThrottled: <boolean>m[IS_THROTTLED],
 //                    expiryTimeStamp: <int>m[EXPIRY_TIMESTAMP] };
 //
 //                if (globalThrottleStreamDtoTM.isThrottled == true) {
 //                    putThrottleData(globalThrottleStreamDtoTM);
 //                } else {
 //                    removeThrottleData(globalThrottleStreamDtoTM.throttleKey);
 //                }
 //                //Blocking decisions going to a separate map
 //            } else if (m.hasKey(BLOCKING_CONDITION_KEY)){
 //                putBlockCondition(m);
 //            }
 //        } else {
 //            log:printError("Error occurred while reading message", err = m);
 //        }
 //    }
 //};

 # `startSubscriberService` function create jms connection, jms session and jms topic subscriber.
 # It binds the subscriber endpoint and jms listener
 #
 # + return - jms:TopicSubscriber for global throttling event publishing
 public function startSubscriberService() returns @tainted jms:MessageConsumer|error {
     // Initialize a JMS connectiontion with the provider.
     printDebug(KEY_THROTTLE_UTIL, "Start subscribe---------------logggggg");
     jms:Connection connection = check jms:createConnection({
                    initialContextFactory: jmsConnectionInitialContextFactory,
                    providerUrl: jmsConnectionProviderUrl,
                    username: jmsConnectionUsername,
                    password: jmsConnectionPassword

               });
     jms:Session session = check connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
      printDebug(KEY_THROTTLE_UTIL, "auto ack---------------logggggg");
     jms:Destination dest = check session->createTopic("throttleData");
     jms:MessageConsumer subscriberEndpoint = check session->createDurableSubscriber(dest, "sub-1");
     var attachResult = subscriberEndpoint.__attach(messageServ);
     printDebug(KEY_THROTTLE_UTIL, "attachResult---------------logggggg");
     if (attachResult is error) {
         log:printInfo("subscriber service for global throttling is started");
          printDebug(KEY_THROTTLE_UTIL, "subscriber service for global throttling is  notstarte");
     }
     var startResult = subscriberEndpoint.__start();
     if (startResult is error) {
         log:printInfo("Starting the task is failed.");
     }
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
         printDebug(KEY_THROTTLE_UTIL, "Enabled Global throttling------");
         jms:MessageConsumer|error topicSubscriber = trap startSubscriberService();
         if (topicSubscriber is jms:MessageConsumer) {
            log:printInfo("subscriber service for global throttling is started");
            return true;
         } else {
             log:printError("Error while starting subscriber service for global throttling");
             return false;
         }
     }
     return false;
 }

