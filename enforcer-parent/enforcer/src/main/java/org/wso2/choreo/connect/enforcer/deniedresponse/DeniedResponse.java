/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.deniedresponse;

import io.envoyproxy.envoy.service.auth.v3.DeniedHttpResponse;
import org.wso2.choreo.connect.enforcer.api.ResponseObject;

/**
 * Abstract class for generating a denied responses.
 */
public abstract class DeniedResponse {
    protected DeniedHttpResponse.Builder denyResponseBuilder;

    public DeniedResponse(DeniedHttpResponse.Builder denyResponseBuilder) {
        this.denyResponseBuilder = denyResponseBuilder;
    }

    /**
     * Sets the denied response to the deniedResponseBuilder.
     *
     * @param responseObject
     */
    public abstract void setResponse(ResponseObject responseObject);
}
