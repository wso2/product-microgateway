/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org).
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

package org.example.tests;

import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.Filter;

import java.util.Map;

public class CustomFilter implements Filter {
    private Map<String, String> configProperties;

    @Override
    public void init(APIConfig apiConfig, Map<String, String> configProperties) {
        this.configProperties = configProperties;
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        String headerValue = configProperties.get("CustomProperty");
        requestContext.addOrModifyHeaders("Custom-header-1", headerValue);
        return true;
    }
}
