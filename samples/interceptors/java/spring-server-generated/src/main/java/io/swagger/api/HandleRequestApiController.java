package io.swagger.api;

import io.swagger.model.RequestHandlerRequestBody;
import io.swagger.model.RequestHandlerResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    public ResponseEntity<RequestHandlerResponseBody> handleRequest(@Parameter(in = ParameterIn.DEFAULT, description = "Content of the request
", schema=@Schema()) @Valid @RequestBody RequestHandlerRequestBody body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<RequestHandlerResponseBody>(objectMapper.readValue("{\n  \"directRespond\" : false,\n  \"headersToAdd\" : {\n    \"content-type\" : \"application/xml\",\n    \"content-length\" : \"40\"\n  },\n  \"interceptorContext\" : {\n    \"key1\" : \"value1\",\n    \"key2\" : \"value2\"\n  },\n  \"headersToRemove\" : [ \"key1\", \"key2\" ],\n  \"trailersToAdd\" : {\n    \"trailer1-key\" : \"value\"\n  },\n  \"dynamicEndpoint\" : {\n    \"endpointName\" : \"my-dynamic-endpoint\"\n  },\n  \"body\" : \"PGhlbGxvPndvcmxkPC9oZWxsbz4K\",\n  \"responseCode\" : 200\n}", RequestHandlerResponseBody.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<RequestHandlerResponseBody>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<RequestHandlerResponseBody>(HttpStatus.NOT_IMPLEMENTED);
    }

}
