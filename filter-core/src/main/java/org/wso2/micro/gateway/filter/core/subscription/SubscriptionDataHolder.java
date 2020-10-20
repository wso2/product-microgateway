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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds tenant wise subscription data stores
 */
public class SubscriptionDataHolder {

    protected Map<String, SubscriptionDataStore> subscriptionStore =
            new ConcurrentHashMap<>();
    private static SubscriptionDataHolder instance = new SubscriptionDataHolder();

    public static SubscriptionDataHolder getInstance() {

        return instance;
    }

    public void registerTenantSubscriptionStore(String tenantDomain) {

        SubscriptionDataStore tenantStore = subscriptionStore.get(tenantDomain);
        if (tenantStore == null) {
            tenantStore = new SubscriptionDataStoreImpl(tenantDomain);
        }
        subscriptionStore.put(tenantDomain, tenantStore);
    }

    public void unregisterTenantSubscriptionStore(String tenantDomain) {

        subscriptionStore.remove(tenantDomain);
    }

    public SubscriptionDataStore getTenantSubscriptionStore(String tenantDomain) {

        return subscriptionStore.get(tenantDomain);
    }

}

