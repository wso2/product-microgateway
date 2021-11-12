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
import io.swagger.model.ResponseHandlerRequestBody;
import io.swagger.model.ResponseHandlerResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-04T06:10:22.979Z[GMT]")
@RestController
public class HandleResponseApiController implements HandleResponseApi {

    private static final Logger log = LoggerFactory.getLogger(HandleResponseApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public HandleResponseApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<ResponseHandlerResponseBody> handleResponse(@Parameter(in = ParameterIn.DEFAULT, description = "Content of the request ", schema = @Schema()) @Valid @RequestBody ResponseHandlerRequestBody body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            ResponseHandlerResponseBody responseBody = new ResponseHandlerResponseBody();
            if (body.getResponseCode() == 200) {
                responseBody.setResponseCode(201);
            }
            return new ResponseEntity<ResponseHandlerResponseBody>(responseBody, HttpStatus.OK);
        }
        return new ResponseEntity<ResponseHandlerResponseBody>(HttpStatus.NOT_IMPLEMENTED);
    }
}
