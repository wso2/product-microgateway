/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.validation;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Multimap;
import io.swagger.parser.OpenAPIParser;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.wso2.micro.gateway.core.Constants;
import org.wso2.micro.gateway.core.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

/**
 * This class is for validating request/response payload against schema.
 */
public class Validate {
    public static final String REG_TIME_MODULE = "register.timeModule";
    private static final Logger logger = LogManager.getLogger(Validate.class);

    /**
     * Method to validate request message
     *
     * @param requestPath API request resource path
     * @param reqMethod   API request method
     * @param payload     Request payload
     * @param headers     Transport headers
     * @param queryParams Query parameters
     * @return Status of the validation
     */
    public static String validateRequest(String requestPath, String reqMethod, String payload,
                                         MapValue<String, String> headers, MapValue<String, ArrayValue> queryParams,
                                         String serviceName) {
        String swagger = CommonUtils.getOpenAPIMap().get(serviceName);
        OpenAPIParser parser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        OpenAPI openAPI = parser.readContents(swagger, null, parseOptions).getOpenAPI();
        convertHeadersToLowercase(openAPI);
        boolean timeModuleRegisterEnabled = Boolean.parseBoolean(System.getProperty(REG_TIME_MODULE, "false"));
        if (timeModuleRegisterEnabled) {
            Json.mapper().registerModule(new JavaTimeModule());
        }
        if (openAPI != null) {
            OpenApiInteractionValidator validator = getOpenAPIValidator(openAPI);
            Multimap<String, String> headersMap = SchemaValidationUtils.convertToMultimap(headers);
            Map<String, Collection<String>> queryParamsMap = SchemaValidationUtils
                    .convertMapofArrayValuesToMap(queryParams);
            OpenAPIRequest request = new OpenAPIRequest(requestPath, reqMethod, payload, headersMap, queryParamsMap,
                    swagger);
            ValidationReport validationReport = validator.validateRequest(request);
            if (validationReport.hasErrors()) {
                StringBuilder finalMessage = new StringBuilder();
                for (ValidationReport.Message message : validationReport.getMessages()) {
                    finalMessage.append(getErrorMessage(message)).append(", ");
                }
                // Remove the last comma and space, if present
                if (finalMessage.length() > 0) {
                    finalMessage.setLength(finalMessage.length() - 2);
                }
                return finalMessage.toString();
            }
        }
        return Constants.VALIDATED_STATUS;
    }

    /**
     * Method to validate response message
     *
     * @param resourcePath  Request resource path
     * @param reqMethod     Request method
     * @param responseCode  Response code
     * @param responseBody  Response payload
     * @param headers       Transport headers
     * @param serviceName   Service name
     * @return Status of the validation result
     */
    public static String validateResponse(String resourcePath, String reqMethod, String responseCode,
                                          String responseBody, MapValue<String, String> headers, String serviceName) {
        String swagger = CommonUtils.getOpenAPIMap().get(serviceName);
        OpenAPIParser parser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        OpenAPI openAPI = parser.readContents(swagger, null, parseOptions).getOpenAPI();
        convertHeadersToLowercase(openAPI);
        boolean timeModuleRegisterEnabled = Boolean.parseBoolean(System.getProperty(REG_TIME_MODULE, "false"));
        if (timeModuleRegisterEnabled) {
            Json.mapper().registerModule(new JavaTimeModule());
        }
        if (openAPI != null) {
            OpenApiInteractionValidator validator = getOpenAPIValidator(openAPI);
            Multimap<String, String> headersMap = SchemaValidationUtils.convertToMultimap(headers);
            OpenAPIResponse response = new OpenAPIResponse(resourcePath, reqMethod, responseCode, responseBody,
                    headersMap);
            ValidationReport validationReport = validator.validateResponse(response.getPath(), response.getMethod(),
                    response);
            if (validationReport.hasErrors()) {
                StringBuilder finalMessage = new StringBuilder();
                for (ValidationReport.Message message : validationReport.getMessages()) {
                    finalMessage.append(getErrorMessage(message)).append(", ");
                }
                // Remove the last comma and space, if present
                if (finalMessage.length() > 0) {
                    finalMessage.setLength(finalMessage.length() - 2);
                }
                return finalMessage.toString();
            }
        }
        return Constants.VALIDATED_STATUS;
    }

    /**
     * Method to generate OpenApiInteractionValidator when the openAPI is provided.
     *
     * @param openAPI openAPI
     * @return OpenApiInteractionValidator object for the provided swagger.
     */
    private static OpenApiInteractionValidator getOpenAPIValidator(OpenAPI openAPI) {
        return OpenApiInteractionValidator
                .createFor(openAPI)
                .withLevelResolver(
                        LevelResolver.create()
                                .withLevel("validation.schema.required", ValidationReport.Level.INFO)
                                .withLevel("validation.response.body.missing", ValidationReport.Level.INFO)
                                .withLevel("validation.schema.additionalProperties", ValidationReport.Level.IGNORE)
                                .build())
                .build();
    }

    /**
     * Method to iterate through openAPI paths and convert header parameter names to lowercase for each operation
     *
     * @param openAPI openAPI object
     */
    private static void convertHeadersToLowercase(OpenAPI openAPI) {
        // Iterate each path
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            // Iterate each operation
            PathItem pathItem = entry.getValue();
            if (pathItem != null) {
                List<Operation> operations = pathItem.readOperations();
                for (Operation operation : operations) {
                    if (operation.getParameters() != null) {
                        operation.setParameters(getLowercaseHeaderParameters(operation.getParameters()));
                    }
                }
            }
        }
    }

    /**
     * Method to read the parameter list and convert header parameter's name to lowercase
     *
     * @param parameters list of params
     * @return list of params with lower case header names
     */
    @NotNull
    private static List<Parameter> getLowercaseHeaderParameters(List<Parameter> parameters) {
        List<Parameter> headerParameters = parameters.stream()
                .filter(param -> param.getIn().equalsIgnoreCase("header"))
                .filter(param -> !param.getName().equalsIgnoreCase(Headers.CONTENT_TYPE))
                .collect(Collectors.toList());
        List<Parameter> modifiedHeaderParameters = headerParameters.stream()
                .map(Validate::replaceLowerCaseHeaderName).collect(Collectors.toList());
        List<Parameter> nonHeaderParameters = parameters.stream()
                .filter(param -> !(param instanceof HeaderParameter)).collect(Collectors.toList());
        nonHeaderParameters.addAll(modifiedHeaderParameters);
        return nonHeaderParameters;
    }

    /**
     * This method convert parameter name to lowercase.
     *
     * @param parameter param
     * @return parameter with lower case name
     */
    private static Parameter replaceLowerCaseHeaderName(Parameter parameter) {
        parameter.setName(parameter.getName().toLowerCase(Locale.ROOT));
        return parameter;
    }

    /**
     * Method to retrieve nested error messages from the validation report error messages
     *
     * @param message message of validation error report
     * @return error message with nested messages
     */
    private static String getErrorMessage(ValidationReport.Message message) {
        if (message.getNestedMessages().isEmpty()) {
            return message.getMessage();
        }
        StringBuilder combinedMessages = new StringBuilder();
        combinedMessages.append(message.getMessage());
        for (ValidationReport.Message nestedMessage : message.getNestedMessages()) {
            combinedMessages.append(", ").append(getErrorMessage(nestedMessage));
        }
        return combinedMessages.toString().trim();
    }
}
