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

package org.wso2.micro.gateway.filter.core.common;

import org.wso2.micro.gateway.filter.core.config.MGWConfiguration;
import org.wso2.micro.gateway.filter.core.keymgt.KeyManagerDataService;
import org.wso2.micro.gateway.filter.core.security.KeyValidator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reference holder instance to hold certain set of object instances used by the filter core.
 */
public class ReferenceHolder {
    private static final ReferenceHolder instance = new ReferenceHolder();

    private final Map<String, KeyValidator> keyValidationHandlerMap = new ConcurrentHashMap<>();
    private KeyManagerDataService keyManagerDataService;
    private MGWConfiguration mgwConfiguration;

    private ReferenceHolder() {
    }

    public static ReferenceHolder getInstance() {
        return instance;
    }

    public KeyValidator getKeyValidationHandler(String tenantDomain) {

        if (keyValidationHandlerMap.containsKey(tenantDomain)) {
            return keyValidationHandlerMap.get(tenantDomain);
        }
        KeyValidator defaultKeyValidationHandler = new KeyValidator();
        keyValidationHandlerMap.put(tenantDomain, defaultKeyValidationHandler);
        return defaultKeyValidationHandler;
    }

    public void setKeyManagerDataService(KeyManagerDataService keyManagerDataService) {
        this.keyManagerDataService = keyManagerDataService;
    }

    public KeyManagerDataService getKeyManagerDataService() {
        return this.keyManagerDataService;
    }

    public MGWConfiguration getMGWConfiguration() {
        return mgwConfiguration;
    }

    public void setMGWConfiguration(MGWConfiguration mgwConfiguration) {
        this.mgwConfiguration = mgwConfiguration;
    }
}
