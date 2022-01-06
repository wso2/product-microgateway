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
package org.wso2.choreo.connect.enforcer.websocket;

/**
 * Constant field types for web socket related metadata values
 */
public class MetadataConstants {
    public static final String USERNAME = "username";
    public static final String APP_TIER = "applicationTier";
    public static final String TIER = "tier";
    public static final String API_TIER = "apiTier";
    public static final String CONTENT_AWARE_TIER_PRESENT = "isContentAwareTierPresent";
    public static final String API_KEY = "apiKey";
    public static final String KEY_TYPE = "keyType";
    public static final String CALLER_TOKEN = "callerToken";
    public static final String APP_ID = "applicationId";
    public static final String APP_UUID = "applicationUUID";
    public static final String APP_NAME = "applicationName";
    public static final String CONSUMER_KEY = "consumerKey";
    public static final String SUBSCRIBER = "subscriber";
    public static final String SPIKE_ARREST_LIMIT = "spikeArrestLimit";
    public static final String SUBSCRIBER_TENANT_DOMAIN = "subscriberTenantDomain";
    public static final String SPIKE_ARREST_UNIT = "spikeArrestUnit";
    public static final String STOP_ON_QUOTA = "stopOnQuotaReach";
    public static final String PRODUCT_NAME = "productName";
    public static final String PRODUCT_PROVIDER = "productProvider";
    public static final String API_PUBLISHER = "apiPublisher";
    public static final String API_TYPE_KEY = "apiType";
    public static final String REQUEST_ID = "requestId";
    public static final String MESSAGE_SIZE = "messageSize";
    public static final String GRPC_STREAM_ID = "streamId";
    public static final String DEFAULT_SUBSCRIBER_TENANT = "carbon.super";
}
