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
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.api.ResponseObject;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.deniedresponse.DeniedResponse;

/**
 * generates JSON formatted denied responses.
 */
public class JsonDeniedResponse extends DeniedResponse {

    public JsonDeniedResponse(DeniedHttpResponse.Builder denyResponseBuilder) {
        super(denyResponseBuilder);
    }

    @Override
    public void setResponse(ResponseObject responseObject) {
        JSONObject responseJson = new JSONObject();
        responseJson.put(APIConstants.MessageFormat.ERROR_CODE, responseObject.getErrorCode());
        responseJson.put(APIConstants.MessageFormat.ERROR_MESSAGE, responseObject.getErrorMessage());
        responseJson.put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                responseObject.getErrorDescription());
        denyResponseBuilder.setBody(responseJson.toString());
        HeaderValueOption headerValueOption = HeaderValueOption.newBuilder().setHeader(HeaderValue.newBuilder()
                .setKey(APIConstants.CONTENT_TYPE_HEADER)
                .setValue(APIConstants.APPLICATION_JSON).build()).build();
        denyResponseBuilder.addHeaders(headerValueOption);
    }
}
