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
    public static final String event = "event";
    public static final String payloadData = "payloadData";
    public static final String appKey = "appKey";
    public static final String subscriptionKey = "subscriptionKey";
    public static final String appTier = "appTier";
    public static final String subscriptionTier = "subscriptionTier";
    public static final String tenMinAppPolicy = "10MinAppPolicy";
    public static final String tenMinSubPolicy = "10MinSubPolicy";
    public static final String unauthenticated = "Unauthenticated";
    public static final String throttleDataTopic = "throttleData";
    public static final String brokerUsername = "admin";
    public static final String brokerPassword = "admin";
    public static final String brokerHost = "localhost";
    public static final String brokerPort = "5672";
    public static final String throttleKey = "throttleKey";
    public static final String expiryTimeStamp = "expiryTimeStamp";
    public static final String isThrottled = "isThrottled";
}
