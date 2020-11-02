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
package org.wso2.micro.gateway.filter.core.api;

import org.wso2.micro.gateway.filter.core.Filter;
import org.wso2.micro.gateway.filter.core.api.config.APIConfig;

import java.util.List;

/**
 * Interface to hold different API types. This can REST, gRPC, graphql and etc.
 */
public interface API {

    List<Filter> getFilters();

    String init(Object apiDefinition);

    ResponseObject process(RequestContext requestContext);

    APIConfig getAPIConfig();

    default boolean executeFilterChain(RequestContext requestContext) {
        boolean proceed;
        for (Filter filter : getFilters()) {
            proceed = filter.handleRequest(requestContext);
            if (!proceed) {
                return false;
            }
        }
        return true;
    }
}
