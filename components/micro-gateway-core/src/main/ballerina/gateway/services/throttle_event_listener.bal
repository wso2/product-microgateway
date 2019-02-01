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

string jmsConnectioninitialContextFactory = getConfigValue(THROTTLE_CONF_INSTANCE_ID,
    JMS_CONNECTION_INITIAL_CONTEXT_FACTORY,
    "bmbInitialContextFactory");
string jmsConnectionProviderUrl = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PROVIDER_URL,
    "amqp://admin:admin@carbon/carbon?brokerlist='tcp://localhost:5672'");
string jmsConnectionPassword = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_PASSWORD, "");
string jmsConnectionUsername = getConfigValue(THROTTLE_CONF_INSTANCE_ID, JMS_CONNECTION_USERNAME, "");

// Initialize a JMS connection with the provider.
jms:Connection jmsConnection = new({
        initialContextFactory: jmsConnectioninitialContextFactory,
        providerUrl: jmsConnectionProviderUrl,
        username: jmsConnectionUsername,
        password: jmsConnectionPassword
    });

// Initialize a JMS session on top of the created connection.
jms:Session jmsSession = new(jmsConnection, {
        acknowledgementMode: "AUTO_ACKNOWLEDGE"
    });

// Initialize a Topic subscriber using the created session.
endpoint jms:TopicSubscriber subscriberEndpoint {
    session: jmsSession,
    topicPattern: "throttleData"
};

// Bind the created consumer(subscriber endpoint) to the listener service.
service<jms:Consumer> jmsListener bind subscriberEndpoint {
    onMessage(endpoint subscriber, jms:Message message) {
        match (message.getMapMessageContent()) {
            map m => {
                log:printDebug("ThrottleMessage Received");
                //Throttling decisions made by TM going to throttleDataMap
                if (m.hasKey(THROTTLE_KEY)) {
                    GlobalThrottleStreamDTO globalThrottleStreamDtoTM;
                    globalThrottleStreamDtoTM.throttleKey = <string>m[THROTTLE_KEY];
                    globalThrottleStreamDtoTM.isThrottled = check <boolean>m[IS_THROTTLED];
                    globalThrottleStreamDtoTM.expiryTimeStamp = check <int>m[EXPIRY_TIMESTAMP];

                    if (globalThrottleStreamDtoTM.isThrottled == true) {
                        putThrottleData(globalThrottleStreamDtoTM);
                    } else {
                        removeThrottleData(globalThrottleStreamDtoTM.throttleKey);
                    }
                    //Blocking decisions going to a separate map
                } else if (m.hasKey(BLOCKING_CONDITION_KEY)){
                    putBlockCondition(m);
                }
            }
            error e => log:printError("Error occurred while reading message", err = e);
        }
    }
}


