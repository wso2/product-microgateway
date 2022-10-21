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
package org.wso2.choreo.connect.enforcer.models.admin;

import java.util.List;

/**
 * Model class for API Information
 */
public class APIInfo extends BasicAPIInfo {
    private String apiType = null;
    private String apiUUID = null;
    private String lcState = null;
    private List<SubscriptionInfo> subscriptions = null;

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getApiUUID() {
        return apiUUID;
    }

    public void setApiUUID(String apiUUID) {
        this.apiUUID = apiUUID;
    }

    public String getLcState() {
        return lcState;
    }

    public void setLcState(String lcState) {
        this.lcState = lcState;
    }

    public List<SubscriptionInfo> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionInfo> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
