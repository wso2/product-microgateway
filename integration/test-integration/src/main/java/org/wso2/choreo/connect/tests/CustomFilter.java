/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.Filter;

import java.util.Map;

public class CustomFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(CustomFilter.class);

    @Override
    public void init(APIConfig apiConfig) {
        log.info("Custom Filter is Initialized for the API. " + apiConfig.getName() + ":" + apiConfig.getVersion());
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        requestContext.addOrModifyHeaders("Custom-header-1", "Foo");
        if (requestContext.getPathParameters() != null) {
            for (Map.Entry<String, String> entry: requestContext.getPathParameters().entrySet()) {
                requestContext.addOrModifyHeaders(entry.getKey(), entry.getValue());
                log.info(entry.getKey() + ":" + entry.getValue() + " is added as a header.");
            }
        }
        if (requestContext.getQueryParameters() != null) {
            for (Map.Entry<String, String> entry: requestContext.getQueryParameters().entrySet()) {
                requestContext.addOrModifyHeaders(entry.getKey(), entry.getValue());
                log.info(entry.getKey() + ":" + entry.getValue() + " is added as a header.");
            }
        }
        if (requestContext.getHeaders().containsKey("custom-remove-header")) {
            requestContext.getRemoveHeaders().add("custom-remove-header");
            log.info("Custom-remove-header is added as a header.");
        }
        log.info("Custom-header-1 is added as a header.");
        return true;
    }
}
