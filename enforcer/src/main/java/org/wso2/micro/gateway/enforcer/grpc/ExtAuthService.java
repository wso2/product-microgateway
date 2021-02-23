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

package org.wso2.micro.gateway.enforcer.grpc;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.envoyproxy.envoy.service.auth.v3.DeniedHttpResponse;
import io.envoyproxy.envoy.service.auth.v3.OkHttpResponse;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import io.grpc.stub.StreamObserver;
import org.json.JSONObject;
import org.wso2.micro.gateway.enforcer.api.ResponseObject;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.HttpConstants;
import org.wso2.micro.gateway.enforcer.server.RequestHandler;

/**
 * This is the gRPC server written to match with the envoy ext-authz filter proto file. Envoy proxy call this service.
 * This is the entry point to the filter chain process for a request.
 */
public class ExtAuthService extends AuthorizationGrpc.AuthorizationImplBase {

    private RequestHandler requestHandler = new RequestHandler();

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        ResponseObject responseObject = requestHandler.process(request, responseObserver);
        CheckResponse response1 = buildResponse(request, responseObject);
        responseObserver.onNext(response1);

        // When you are done, you must call onCompleted.
        responseObserver.onCompleted();
    }

    private CheckResponse buildResponse(CheckRequest request, ResponseObject responseObject) {
        DeniedHttpResponse.Builder responseBuilder = DeniedHttpResponse.newBuilder();
        HttpStatus status = HttpStatus.newBuilder().setCodeValue(responseObject.getStatusCode()).build();
        String traceKey = request.getAttributes().getRequest().getHttp().getId();
        if (responseObject.isDirectResponse()) {
            // To handle pre flight options request
            if (responseObject.getStatusCode() == HttpConstants.NO_CONTENT_STATUS_CODE) {
                responseObject.getHeaderMap().forEach((key, value) -> {
                            HeaderValueOption headerValueOption = HeaderValueOption.newBuilder()
                                    .setHeader(HeaderValue.newBuilder().setKey(key).setValue(value).build())
                                    .build();
                            responseBuilder.addHeaders(headerValueOption);
                        }
                );
                return CheckResponse.newBuilder()
                        .setStatus(Status.newBuilder().setCode(getCode(responseObject.getStatusCode())))
                        .setDeniedResponse(responseBuilder.setStatus(status).build())
                        .setDynamicMetadata(Struct.newBuilder().putFields("correlationID",
                                Value.newBuilder().setStringValue(responseObject.getCorrelationID()).build())
                                .build())
                        .build();
            }
            // Error handling
            JSONObject responseJson = new JSONObject();
            responseJson.put(APIConstants.MessageFormat.ERROR_CODE, responseObject.getErrorCode());
            responseJson.put(APIConstants.MessageFormat.ERROR_MESSAGE, responseObject.getErrorMessage());
            responseJson.put(APIConstants.MessageFormat.ERROR_DESCRIPTION, responseObject.getErrorDescription());
            HeaderValueOption headerValueOption = HeaderValueOption.newBuilder().setHeader(
                    HeaderValue.newBuilder().setKey(APIConstants.CONTENT_TYPE_HEADER)
                            .setValue(APIConstants.APPLICATION_JSON).build())
                    .setHeader(HeaderValue.newBuilder().setKey(APIConstants.API_TRACE_KEY).setValue(traceKey).build())
                    .build();
            responseBuilder.addHeaders(headerValueOption);

            return CheckResponse.newBuilder()
                    .setStatus(Status.newBuilder().setCode(getCode(responseObject.getStatusCode())))
                    .setDeniedResponse(responseBuilder.setBody(responseJson.toString()).setStatus(status).build())
                    .setDynamicMetadata(Struct.newBuilder().putFields("correlationID",
                            Value.newBuilder().setStringValue(responseObject.getCorrelationID()).build())
                            .build())
                    .build();
        } else {
            OkHttpResponse.Builder okResponseBuilder = OkHttpResponse.newBuilder();
            if (responseObject.getHeaderMap() != null) {
                responseObject.getHeaderMap().forEach((key, value) -> {
                            HeaderValueOption headerValueOption = HeaderValueOption.newBuilder()
                                    .setHeader(HeaderValue.newBuilder().setKey(key).setValue(value).build())
                                    .build();
                            okResponseBuilder.addHeaders(headerValueOption);
                        }
                );
            }
            HeaderValueOption headerValueOption = HeaderValueOption.newBuilder()
                    .setHeader(HeaderValue.newBuilder().setKey(APIConstants.API_TRACE_KEY).setValue(traceKey).build())
                    .build();
            responseBuilder.addHeaders(headerValueOption);
            return CheckResponse.newBuilder().setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
                    .setOkResponse(okResponseBuilder.build())
                    .setDynamicMetadata(Struct.newBuilder().putFields("correlationID",
                            Value.newBuilder().setStringValue(responseObject.getCorrelationID()).build())
                            .build())
                    .build();
        }
    }

    private int getCode(int statusCode) {
        switch (statusCode) {
            case 200:
                return Code.OK_VALUE;
            case 401:
                return Code.UNAUTHENTICATED_VALUE;
            case 403:
                return Code.PERMISSION_DENIED_VALUE;
            case 409:
                return Code.RESOURCE_EXHAUSTED_VALUE;
        }
        return Code.INTERNAL_VALUE;
    }
}
