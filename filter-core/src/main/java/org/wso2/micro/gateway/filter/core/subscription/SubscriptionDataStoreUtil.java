/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.filter.core.subscription;

/**
 * Utility methods related to subscription data store functionalities.
 */
public class SubscriptionDataStoreUtil {

    public static final String DELEM_PERIOD = ".";

    public static String getAPICacheKey(String context, String version) {

        return context + DELEM_PERIOD + version;
    }

    public static String getSubscriptionCacheKey(int appId, int apiId) {

        return appId + DELEM_PERIOD + apiId;
    }

    public static String getPolicyCacheKey(String tierName, int tenantId) {

        return tierName + DELEM_PERIOD + tenantId;
    }

}

