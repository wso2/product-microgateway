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

import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.DeniedHttpResponse;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import org.wso2.choreo.connect.enforcer.api.ResponseObject;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.deniedresponse.types.JsonDeniedResponse;
import org.wso2.choreo.connect.enforcer.deniedresponse.types.Soap11DeniedResponse;
import org.wso2.choreo.connect.enforcer.deniedresponse.types.Soap12DeniedResponse;

/**
 * DeniedResponsePreparer will prepare the DeniedHttpResponse.Builder accordingly to the appropriate response format.
 */
public class DeniedResponsePreparer {
    private DeniedResponse deniedResponse;

    private final DeniedHttpResponse.Builder denyResponseBuilder;

    private String responseType = "JSON";

    public DeniedResponsePreparer(DeniedHttpResponse.Builder denyResponseBuilder) {
        this.denyResponseBuilder = denyResponseBuilder;
    }
    private static final boolean soapErrorInXMLEnabled =
            ConfigHolder.getInstance().getConfig().getSoapErrorResponseConfigDto().isEnable();

    public DeniedResponse getDeniedResponse() {
        return deniedResponse;
    }

    /**
     * This function will set the error message according to the format that needs to be sent.
     * Currently, supported formats are JSON, SOAP1.1, SOAP1.2.
     *
     * @param request        CheckRequest object containing request details
     * @param responseObject ResponseObject containing the response details
     */
    public void setErrorMessage(CheckRequest request, ResponseObject responseObject) {
        findResponseType(request);
        switch (responseType) {
            case APIConstants.ErrorResponseTypes.SOAP11:
                deniedResponse = new Soap11DeniedResponse(denyResponseBuilder);
                deniedResponse.setResponse(responseObject);
                break;
            case APIConstants.ErrorResponseTypes.SOAP12:
                deniedResponse = new Soap12DeniedResponse(denyResponseBuilder);
                deniedResponse.setResponse(responseObject);
                break;
            default:
                deniedResponse = new JsonDeniedResponse(denyResponseBuilder);
                deniedResponse.setResponse(responseObject);
        }
    }

    public void addHeaders(HeaderValueOption headerValueOption) {
        denyResponseBuilder.addHeaders(headerValueOption);
    }

    public void addHeaders(HeaderValueOption.Builder headerValueOption) {
        denyResponseBuilder.addHeaders(headerValueOption);
    }

    public void setStatus(HttpStatus status) {
        denyResponseBuilder.setStatus(status);
    }

    public void setBody(String body) {
        denyResponseBuilder.setBody(body);
    }

    /**
     * Decides the response type.
     *
     * @param request CheckRequest object containing request details
     */
    private void findResponseType(CheckRequest request) {
        if (soapErrorInXMLEnabled) {
            String contentType = request.getAttributes().getRequest().getHttp().getHeadersMap()
                    .get(APIConstants.CONTENT_TYPE_HEADER.toLowerCase());
            if (APIConstants.CONTENT_TYPE_TEXT_XML.equals(contentType) && request.getAttributes().getRequest()
                    .getHttp().getHeadersMap().containsKey(APIConstants.SOAP_ACTION_HEADER_NAME)) {
                this.responseType = APIConstants.ErrorResponseTypes.SOAP11;
            } else if (APIConstants.CONTENT_TYPE_SOAP_XML.equals(contentType)) {
                this.responseType = APIConstants.ErrorResponseTypes.SOAP12;
            } else {
                this.responseType = APIConstants.ErrorResponseTypes.JSON;
            }
        } else {
            this.responseType = APIConstants.ErrorResponseTypes.JSON;
        }
    }

    public DeniedHttpResponse build() {
        return denyResponseBuilder.build();
    }
}
