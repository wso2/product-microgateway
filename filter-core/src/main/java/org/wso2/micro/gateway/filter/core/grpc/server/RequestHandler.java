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
package org.wso2.micro.gateway.filter.core.grpc.server;

import io.envoyproxy.envoy.service.auth.v2.CheckRequest;
import io.envoyproxy.envoy.service.auth.v2.CheckResponse;
import io.grpc.stub.StreamObserver;
import org.wso2.micro.gateway.filter.core.api.API;
import org.wso2.micro.gateway.filter.core.api.APIFactory;
import org.wso2.micro.gateway.filter.core.api.RequestContext;
import org.wso2.micro.gateway.filter.core.api.ResponseObject;
import org.wso2.micro.gateway.filter.core.api.config.ResourceConfig;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;

import java.util.Map;

/**
 * This class handles the request coming via the external auth gRPC service.
 */
public class RequestHandler {

    public ResponseObject process(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        String requestPath = request.getAttributes().getRequest().getHttp().getPath();
        String basePath = request.getAttributes().getContextExtensionsMap().get(APIConstants.BASE_PATH_PARAM);

        API matchedAPI = APIFactory.getInstance().getMatchedAPI(basePath, requestPath);
        RequestContext requestContext = buildRequestContext(matchedAPI, request);
        return matchedAPI.process(requestContext);

    }

    private RequestContext buildRequestContext(API api, CheckRequest request) {
        String requestPath = request.getAttributes().getRequest().getHttp().getPath();
        String method = request.getAttributes().getRequest().getHttp().getMethod();
        String matchedResource = request.getAttributes().getContextExtensionsMap().get(
                APIConstants.RESOURCE_PATH_PARAMETER);
        Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
        ResourceConfig resourceConfig = APIFactory.getInstance().getMatchedResource(api, matchedResource, method);
        return new RequestContext.Builder(requestPath).matchedResourceConfig(resourceConfig).requestMethod(method)
                .matchedAPI(api).headers(headers).build();
    }
}
