// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/io;
import ballerina/lang.'array as arrays;
import ballerina/lang.'string as str;
import ballerina/stringutils;


service gatewayNotificationService = service {
    resource function onMessage(jms:Message message) {
        if (message is jms:MapMessage) {
            io:println("###### message recieved ");
            string? | error eventType = message.getString(NOTIFICATION_EVENT_TYPE);
            string? | error timestamp = message.getString(NOTIFICATION_EVENT_TIMESTAMP);
            string? | error event = message.getString(NOTIFICATION_EVENT);

            if (eventType is string && event is string) {
                io:println("###### eventtype : " + eventType);
                handleNotificationMessage(eventType, event);
            } else {
                printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error occurred while reading notification message.");
            }
        } else {
            printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error occurred while reading notification message.");
        }
    }
};

# `startGatewayNotificationSubscriberService` function create jms connection, jms session and jms topic subscriber.
# It binds the subscriber endpoint and jms listener
#
# + return - jms:TopicSubscriber for global throttling event publishing
public function startGatewayNotificationSubscriberService() returns @tainted jms:MessageConsumer | error {
    // Initialize a JMS connection  with the provider.
    jms:Connection | error connection = trap jms:createConnection({
        initialContextFactory: jmsConnectionInitialContextFactory,
        providerUrl: jmsConnectionProviderUrl,
        username: jmsConnectionUsername,
        password: jmsConnectionPassword

    });
    if (connection is error) {
        printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error while creating the jms connection for " +
        "gateway notifications.", connection);
        return connection;
    } else {
        jms:Session | error session = trap connection->createSession({acknowledgementMode: "AUTO_ACKNOWLEDGE"});
        if (session is error) {
            printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error while creating the jms session.", session);
            return session;
        } else {
            jms:Destination dest = check session->createTopic("notification");
            jms:MessageConsumer | error subscriberEndpoint = trap session->createConsumer(dest);
            if (subscriberEndpoint is error) {
                printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error while creating the jms subscriber for " +
                "gateway notification.", subscriberEndpoint);
            } else {
                var attachResult = subscriberEndpoint.__attach(gatewayNotificationService);
                if (attachResult is error) {
                    printError(KEY_NOTIFICATION_EVENT_LISTENER, "Message consumer for gateway notifications hasn't " +
                    "been attached to the service.", attachResult);
                    return attachResult;
                }
                var startResult = subscriberEndpoint.__start();
                if (startResult is error) {
                    printError(KEY_NOTIFICATION_EVENT_LISTENER, "Starting the task is failed.", startResult);
                    return startResult;
                }
                printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "Successfully created jms connection for gateway notifications");
            }

            return subscriberEndpoint;
        }
    }
}

function handleNotificationMessage(string eventType, string encodedEvent ) {
    var decodedEvent = arrays:fromBase64(encodedEvent);
    if (decodedEvent is  byte[]) {
        var decodedString = str:fromBytes(decodedEvent);
        if (decodedString is string) {
            printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "Decoded JMS notification : " + decodedString);
            io:StringReader sr = new(decodedString, encoding = "UTF-8");
            json jsonEvent = checkpanic sr.readJson();
            printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "Decoded JMS json value : " + jsonEvent.toJsonString());
            if (stringutils:equalsIgnoreCase(APPLICATION_CREATE_EVENT, eventType) ||
                stringutils:equalsIgnoreCase(APPLICATION_UPDATE_EVENT, eventType)) {
                Application app = convertApplicationEventToApplicationDTO(jsonEvent);
                printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "JMS application to create recieved : " + app.toString());
                pilotDataProvider.addApplication(<@untainted>app.tenantDomain, <@untainted>app);
            } else if (stringutils:equalsIgnoreCase(APPLICATION_DELETE_EVENT, eventType)) {
                Application app = convertApplicationEventToApplicationDTO(jsonEvent);
                printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "JMS application to delete recieved : " + app.toString());
                pilotDataProvider.removeApplication(<@untainted>app.tenantDomain, <@untainted>app);
            } else if (stringutils:equalsIgnoreCase(SUBSCRIPTIONS_CREATE_EVENT, eventType) ||
                       stringutils:equalsIgnoreCase(SUBSCRIPTIONS_UPDATE_EVENT, eventType)) {
                Subscription sub = convertSubscriptionEventToSubscriptionDTO(jsonEvent);
                printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "JMS subscription to create recieved : " + sub.toString());
                pilotDataProvider.addSubscription(<@untainted>sub.tenantDomain, <@untainted>sub);
            } else if (stringutils:equalsIgnoreCase(SUBSCRIPTIONS_DELETE_EVENT, eventType)) {
                Subscription sub = convertSubscriptionEventToSubscriptionDTO(jsonEvent);
                printDebug(KEY_NOTIFICATION_EVENT_LISTENER, "JMS subscription to delete recieved : " + sub.toString());
                pilotDataProvider.removeSubscription(<@untainted>sub.tenantDomain, <@untainted>sub);
            }
        } else {
            printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error occurred while decoding base 64 byte array to string", decodedString);
        }
    } else {
        printError(KEY_NOTIFICATION_EVENT_LISTENER, "Error occurred while decoding the recieved event.", decodedEvent);
    }
}
