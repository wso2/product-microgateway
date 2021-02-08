/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

/**
 * JMS publisher related constants
 */
public class JMSPublisherConstants {
    public static final String EVENT = "event";
    public static final String PAYLOAD_DATA = "payloadData";
    public static final String APP_KEY = "appKey";
    public static final String SUBSCRIPTION_KEY = "subscriptionKey";
    public static final String APP_TIER = "appTier";
    public static final String SUBSCRIPTION_TIER = "subscriptionTier";
    public static final String TEN_MIN_APP_POLICY = "10MinAppPolicy";
    public static final String TEN_MIN_SUB_POLICY = "10MinSubPolicy";
    public static final String UNAUTHENTICATED = "Unauthenticated";
    public static final String THROTTLE_DATA_TOPIC = "throttleData";
    public static final String BROKER_USERNAME = "admin";
    public static final String BROKER_PASSWORD = "admin";
    public static final String BROKER_HOST = "localhost";
    public static final String BROKER_PORT = "5672";
    public static final String THROTTLE_KEY = "throttleKey";
    public static final String EXPIRYTIMESTAMP = "expiryTimeStamp";
    public static final String IS_THROTTLED = "isThrottled";
}
