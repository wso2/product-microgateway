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
package org.wso2.micro.gateway.enforcer.api;

import org.wso2.gateway.discovery.api.Api;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;

import java.util.List;

/**
 * Interface to hold different API types. This can REST, gRPC, graphql, websocket and etc.
 * @param <T> - Type of object accepted by the API to process.
 * @param <S> - Type of object returned by the API after processing.
 * e.g : RestAPI implements API <RequestContext, ResponseObject>
 */
public interface API <T, S>{

    List<Filter<T>> getFilters();

    String init(Api api);

    S process(T t);

    APIConfig getAPIConfig();

    default boolean executeFilterChain(T t) {
        boolean proceed;
        for (Filter<T> filter : getFilters()) {
            proceed = filter.handleRequest(t);
            if (!proceed) {
                return false;
            }
        }
        return true;
    }

}
