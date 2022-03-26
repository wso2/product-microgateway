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
package org.wso2.choreo.connect.enforcer.server;

import com.google.protobuf.ByteString;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.api.ResponseObject;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.Map;

/**
 * This class handles the request coming via the external auth gRPC service.
 */
public class HttpRequestHandler implements RequestHandler<CheckRequest, ResponseObject> {
    private static final Logger logger = LogManager.getLogger(RequestHandler.class);

    public ResponseObject process(CheckRequest request) {
        API matchedAPI = APIFactory.getInstance().getMatchedAPI(request);
        if (matchedAPI == null) {
            ResponseObject responseObject = new ResponseObject();
            responseObject.setStatusCode(APIConstants.StatusCodes.NOTFOUND.getCode());
            responseObject.setErrorCode(APIConstants.StatusCodes.NOTFOUND.getValue());
            responseObject.setDirectResponse(true);
            responseObject.setErrorMessage(APIConstants.NOT_FOUND_MESSAGE);
            responseObject.setErrorDescription(APIConstants.NOT_FOUND_DESCRIPTION);
            return responseObject;
        }
        APIConfig api = matchedAPI.getAPIConfig();
        logger.debug("API {}/{} found in the cache", api.getBasePath(), api.getVersion());

        // putting API details into ThreadContext for logging purposes
        ThreadContext.push(api.getName());
        ThreadContext.push(api.getOrganizationId());
        ThreadContext.push(api.getBasePath());

        RequestContext requestContext = buildRequestContext(matchedAPI, request);
        ResponseObject responseObject = matchedAPI.process(requestContext);

        // to clear the ThreadContext's stack used for logging
        ThreadContext.removeStack();
        return responseObject;
    }

    private RequestContext buildRequestContext(API api, CheckRequest request) {
        String requestPayload = null;
        String requestPath = request.getAttributes().getRequest().getHttp().getPath();
        String method = request.getAttributes().getRequest().getHttp().getMethod();
        Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
        String pathTemplate = request.getAttributes().getContextExtensionsMap().get(APIConstants.GW_RES_PATH_PARAM);
        String prodCluster = request.getAttributes().getContextExtensionsMap()
                .get(AdapterConstants.PROD_CLUSTER_HEADER_KEY);
        String sandCluster = request.getAttributes().getContextExtensionsMap()
                .get(AdapterConstants.SAND_CLUSTER_HEADER_KEY);
        long requestTimeInMillis = request.getAttributes().getRequest().getTime().getSeconds() * 1000 +
                request.getAttributes().getRequest().getTime().getNanos() / 1000000;
        String requestID =  request.getAttributes().getRequest().getHttp().
                getHeadersOrDefault(HttpConstants.X_REQUEST_ID_HEADER,
                request.getAttributes().getRequest().getHttp().getId());
        String address = "";
        if (request.getAttributes().getSource().hasAddress() &&
                request.getAttributes().getSource().getAddress().hasSocketAddress()) {
            address = request.getAttributes().getSource().getAddress().getSocketAddress().getAddress();
        }
        if (!request.getAttributes().getRequest().getHttp().getRawBody().isEmpty()) {
            ByteString byteString = request.getAttributes().getRequest().getHttp().getRawBody();
            if (byteString.isValidUtf8()) {
                requestPayload = byteString.toStringUtf8();
            }
        }
        if (!request.getAttributes().getRequest().getHttp().getBody().isEmpty()) {
            ByteString byteString = request.getAttributes().getRequest().getHttp().getBodyBytes();
            if (byteString.isValidUtf8()) {
                requestPayload = byteString.toStringUtf8();
            }
        }
        address = FilterUtils.getClientIp(headers, address);
        ResourceConfig resourceConfig = null;
        resourceConfig = APIFactory.getInstance().getMatchedResource(api, pathTemplate, method);
        return new RequestContext.Builder(requestPath).matchedResourceConfig(resourceConfig).requestMethod(method)
                .matchedAPI(api.getAPIConfig()).headers(headers).requestID(requestID).address(address)
                .prodClusterHeader(prodCluster).sandClusterHeader(sandCluster).requestTimeStamp(requestTimeInMillis)
                .pathTemplate(pathTemplate).requestPayload(requestPayload).build();
    }
}
