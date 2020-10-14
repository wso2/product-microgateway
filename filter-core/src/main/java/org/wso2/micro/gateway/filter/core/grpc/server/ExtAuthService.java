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

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.envoyproxy.envoy.api.v2.core.HeaderValue;
import io.envoyproxy.envoy.api.v2.core.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v2.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v2.CheckRequest;
import io.envoyproxy.envoy.service.auth.v2.CheckResponse;
import io.envoyproxy.envoy.service.auth.v2.DeniedHttpResponse;
import io.envoyproxy.envoy.service.auth.v2.OkHttpResponse;
import io.envoyproxy.envoy.type.HttpStatus;
import io.grpc.stub.StreamObserver;
import org.json.JSONObject;
import org.wso2.micro.gateway.filter.core.api.ResponseObject;

/**
 * This is the gRPC server written to match with the envoy ext-authz filter proto file. Envoy proxy call this service.
 * This is the entry point to the filter chain process for a request.
 */
public class ExtAuthService extends AuthorizationGrpc.AuthorizationImplBase {

    private RequestHandler requestHandler = new RequestHandler();

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        ResponseObject responseObject = requestHandler.process(request, responseObserver);
        CheckResponse response1 = buildResponse(responseObject);
        responseObserver.onNext(response1);

        // When you are done, you must call onCompleted.
        responseObserver.onCompleted();
    }

    private CheckResponse buildResponse(ResponseObject responseObject) {
        if (responseObject.getStatusCode() != 200) {
            String errorCode = responseObject.getErrorCode();
            String errorDescription = responseObject.getErrorDescription();
            JSONObject responseJson = new JSONObject();
            responseJson.put("errorCode", errorCode);
            responseJson.put("errorDescription", errorDescription);

            HeaderValueOption headerValueOption = HeaderValueOption.newBuilder()
                    .setHeader(HeaderValue.newBuilder().setKey("Content-type").setValue("application/json").build())
                    .build();
            HttpStatus status = HttpStatus.newBuilder().setCodeValue(responseObject.getStatusCode()).build();
            return CheckResponse.newBuilder()
                    .setStatus(Status.newBuilder().setCode(getCode(responseObject.getStatusCode())).build())
                    .setDeniedResponse(DeniedHttpResponse.newBuilder().setBody(responseJson.toString())
                            .addHeaders(headerValueOption).setStatus(status).build()).build();
        } else {
            return CheckResponse.newBuilder().setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
                    .setOkResponse(OkHttpResponse.newBuilder().build()).build();
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
