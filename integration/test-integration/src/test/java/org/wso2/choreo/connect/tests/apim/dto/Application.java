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
package org.wso2.choreo.connect.tests.apim.dto;

import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;

public class Application {
    private final String appName;
    private final String description;
    private final String throttleTier;
    private final ApplicationDTO.TokenTypeEnum tokenType;

    public Application(String appName, String throttleTier) {
        this.appName = appName;
        this.description = "An application";
        this.throttleTier = throttleTier;
        this.tokenType = ApplicationDTO.TokenTypeEnum.JWT;
    }

    public String getName() {
        return appName;
    }

    public String getDescription() {
        return description;
    }

    public String getThrottleTier() {
        return throttleTier;
    }

    public ApplicationDTO.TokenTypeEnum getTokenType() {
        return tokenType;
    }
}
