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

package org.wso2.choreo.connect.enforcer.deniedresponse.types;

import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.DeniedHttpResponse;
import org.wso2.choreo.connect.enforcer.api.ResponseObject;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.deniedresponse.DeniedResponse;
import org.wso2.choreo.connect.enforcer.util.SOAPUtils;

/**
 * generates SOAP 1.1 formatted denied responses.
 */
public class Soap11DeniedResponse extends DeniedResponse {
    public Soap11DeniedResponse(DeniedHttpResponse.Builder denyResponseBuilder) {
        super(denyResponseBuilder);
    }

    @Override
    public void setResponse(ResponseObject responseObject) {
        denyResponseBuilder.setBody(SOAPUtils.getSoapFaultMessage(APIConstants.SOAP11_PROTOCOL,
                responseObject.getErrorMessage(), responseObject.getErrorDescription(), responseObject.getErrorCode()));
        HeaderValueOption headerValueOption = HeaderValueOption.newBuilder().setHeader(HeaderValue.newBuilder()
                .setKey(APIConstants.CONTENT_TYPE_HEADER)
                .setValue(APIConstants.CONTENT_TYPE_TEXT_XML).build()).build();
        denyResponseBuilder.addHeaders(headerValueOption);
    }
}
