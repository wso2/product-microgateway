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

package org.wso2.micro.gateway.enforcer.models;

import java.util.Objects;

/**
 * Cache Key For Application KeyMapping Entries.
 */
public class ApplicationKeyMappingCacheKey {

    private String consumerKey;
    private String keyManager;

    public ApplicationKeyMappingCacheKey(String consumerKey, String keyManager) {
        this.consumerKey = consumerKey;
        this.keyManager = keyManager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationKeyMappingCacheKey that = (ApplicationKeyMappingCacheKey) o;
        return Objects.equals(consumerKey, that.consumerKey) &&
                Objects.equals(keyManager, that.keyManager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerKey, keyManager);
    }

    @Override
    public String toString() {

        return "ApplicationKeyMappingCacheKey{" +
                "consumerKey='" + consumerKey + '\'' +
                ", keyManager='" + keyManager + '\'' +
                '}';
    }
}

