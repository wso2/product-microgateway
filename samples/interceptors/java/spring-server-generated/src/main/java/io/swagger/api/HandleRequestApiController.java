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

package io.swagger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.model.Headers;
import io.swagger.model.RequestHandlerRequestBody;
import io.swagger.model.RequestHandlerResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Base64;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-04T06:10:22.979Z[GMT]")
@RestController
public class HandleRequestApiController implements HandleRequestApi {

    private static final Logger log = LoggerFactory.getLogger(HandleRequestApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public HandleRequestApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<RequestHandlerResponseBody> handleRequest(@Parameter(in = ParameterIn.DEFAULT, description = "Content of the request ", schema = @Schema()) @Valid @RequestBody RequestHandlerRequestBody body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            String base64EncodedReqBody = body.getRequestBody(); // base64EncodedReqBody will be null, if `request_body` is not added in `includes` in open API
            byte[] decodeBytes = Base64.getDecoder().decode(base64EncodedReqBody);
            JSONObject json = new JSONObject(new String(decodeBytes));
            String xml = XML.toString(json);

            Headers headersToAdd = new Headers();
            headersToAdd.put("x-user", "admin");

            Headers headersToReplace = new Headers();
            headersToReplace.put("content-type", "application/xml");

            String encodedXmlBody = Base64.getEncoder().encodeToString(xml.getBytes());
            return new ResponseEntity<RequestHandlerResponseBody>(
                    new RequestHandlerResponseBody()
                            .body(encodedXmlBody)
                            .headersToReplace(headersToReplace)
                            .headersToAdd(headersToAdd)
                    , HttpStatus.OK);
        }
        return new ResponseEntity<RequestHandlerResponseBody>(HttpStatus.NOT_IMPLEMENTED);
    }
}
