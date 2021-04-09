/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util;

/**
 * This class holds the constants required to fetch the properties from the ThrottleEvent.
 */
public class ThrottleEventConstants {

    private ThrottleEventConstants() {
    }

    public static final String MESSAGE_ID = "messageID";
    public static final String APP_KEY = "appKey";
    public static final String APP_TIER = "appTier";
    public static final String API_KEY = "apiKey";
    public static final String API_TIER = "apiTier";
    public static final String SUBSCRIPTION_KEY = "subscriptionKey";
    public static final String SUBSCRIPTION_TIER = "subscriptionTier";
    public static final String RESOURCE_KEY = "resourceKey";
    public static final String RESOURCE_TIER = "resourceTier";
    public static final String USER_ID = "userId";
    public static final String API_CONTEXT = "apiContext";
    public static final String API_VERSION = "apiVersion";
    public static final String APP_TENANT = "appTenant";
    public static final String API_TENANT = "apiTenant";
    public static final String APP_ID = "appId";
    public static final String API_NAME = "apiName";
    public static final String PROPERTIES = "properties";
}
