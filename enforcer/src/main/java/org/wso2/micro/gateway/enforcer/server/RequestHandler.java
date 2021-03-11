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
package org.wso2.micro.gateway.enforcer.server;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.api.API;
import org.wso2.micro.gateway.enforcer.api.APIFactory;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.ResponseObject;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.AdapterConstants;

import java.util.Map;

/**
 * This class handles the request coming via the external auth gRPC service.
 */
public class RequestHandler {
    private static final Logger logger = LogManager.getLogger(RequestHandler.class);

    public ResponseObject process(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        API matchedAPI = APIFactory.getInstance().getMatchedAPI(request);

        if (matchedAPI == null) {
            ResponseObject responseObject = new ResponseObject();
            responseObject.setStatusCode(APIConstants.StatusCodes.NOTFOUND.getCode());
            responseObject.setErrorCode(APIConstants.StatusCodes.NOTFOUND.getValue());
            responseObject.setDirectResponse(true);
            responseObject.setErrorMessage(APIConstants.NOT_FOUND_MESSAGE);
            responseObject.setErrorDescription(APIConstants.NOT_FOUND_DESCRIPTION);
            return responseObject;
        } else if (logger.isDebugEnabled()) {
            APIConfig api = matchedAPI.getAPIConfig();
            logger.debug("API {}/{} found in the cache", api.getBasePath(), api.getVersion());
        }

        RequestContext requestContext = buildRequestContext(matchedAPI, request);
        return matchedAPI.process(requestContext);
    }

    private RequestContext buildRequestContext(API api, CheckRequest request) {
        String requestPath = request.getAttributes().getRequest().getHttp().getPath();
        String method = request.getAttributes().getRequest().getHttp().getMethod();
        Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
        String res = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_RES_PATH_PARAM);
        String prodCluster = request.getAttributes().getContextExtensionsMap()
                .get(AdapterConstants.PROD_CLUSTER_HEADER_KEY);
        String sandCluster = request.getAttributes().getContextExtensionsMap()
                .get(AdapterConstants.SAND_CLUSTER_HEADER_KEY);
        long requestTimeInMillis = request.getAttributes().getRequest().getTime().getSeconds() * 1000;

        ResourceConfig resourceConfig = APIFactory.getInstance().getMatchedResource(api, res, method);
        return new RequestContext.Builder(requestPath).matchedResourceConfig(resourceConfig).requestMethod(method)
                .matchedAPI(api).headers(headers).prodClusterHeader(prodCluster).sandClusterHeader(sandCluster)
                .pathTemplate(res).requestTimeStamp(requestTimeInMillis).build();
    }
}
