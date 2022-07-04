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

package org.wso2.choreo.connect.enforcer.models;

import org.wso2.choreo.connect.enforcer.common.CacheableEntity;

/**
 * Entity for keeping API related information.
 */
public class API implements CacheableEntity<String> {

    private boolean isDefaultVersion = false;
    private String apiUUID = null;
    private String lcState = null;

    public String getCacheKey() {

        return apiUUID;
    }

    public String getLcState() {
        return lcState;
    }

    public void setLcState(String lcState) {
        this.lcState = lcState;
    }

    // TODO: (VirajSalaka)
//    @Override
//    public String toString() {
//        return "API [apiId=" + apiId + ", provider=" + provider + ", name=" + name + ", version=" + version
//                + ", context=" + context + ", policy=" + policy + ", apiType=" + apiType + "]";
//    }

    public boolean isDefaultVersion() {
        return isDefaultVersion;
    }

    public void setDefaultVersion(boolean isDefaultVersion) {
        this.isDefaultVersion = isDefaultVersion;
    }

    public String getApiUUID() {
        return apiUUID;
    }

    public void setApiUUID(String apiUUID) {
        this.apiUUID = apiUUID;
    }
}

