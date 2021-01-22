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
package org.wso2.micro.gateway.enforcer.listener.events;

import java.util.Set;

/**
 * Deploy API in Gateway Event.
 */
public class DeployAPIInGatewayEvent extends Event {

    private String apiId;
    private Set<String> gatewayLabels;

    public DeployAPIInGatewayEvent(String eventId, long timestamp, String type, String tenanrDomain, String apiId,
                                   Set<String> gatewayLabels) {
        this.eventId = eventId;
        this.timeStamp = timestamp;
        this.type = type;
        this.tenantDomain = tenanrDomain;

        this.apiId = apiId;
        this.gatewayLabels = gatewayLabels;

    }

    public Set<String> getGatewayLabels() {

        return gatewayLabels;
    }

    public void setGatewayLabels(Set<String> gatewayLabels) {

        this.gatewayLabels = gatewayLabels;
    }

    public String getApiId() {

        return apiId;
    }

    public void setApiId(String apiId) {

        this.apiId = apiId;
    }

}
