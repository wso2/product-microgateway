/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.apimgt.gateway.cli.model.template.service;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps the {@link Operation} from swagger models to provide iterable child models.
 *
 */
public class BallerinaOperation implements BallerinaOpenAPIObject<BallerinaOperation, Operation> {

    public static final String X_THROTTLING_TIER = "x-throttling-tier";
    public static final String X_SCOPE = "x-scope";
    public static final String X_AUTH_TYPE = "x-auth-type";
    public static final String AUTH_TYPE_NONE = "None";
    private List<String> tags;
    private String summary;
    private String description;
    private String resourceTier;
    private ExternalDocumentation externalDocs;
    private String operationId;
    private List<BallerinaParameter> parameters;
    private List<String> methods;
    private String scope;
    private boolean isSecured = true;
    private boolean hasProdEpConfig = false;
    private boolean hasSandEpConfig = false;
    private MgwEndpointConfigDTO epConfig;
    private String requestInterceptor;
    private String responseInterceptor;
    private String apiRequestInterceptor;
    private String apiResponseInterceptor;

    // Not static since handlebars can't see static variables
    private final List<String> allMethods =
            Arrays.asList("HEAD", "OPTIONS", "PATCH", "DELETE", "POST", "PUT", "GET");

    @Override
    public BallerinaOperation buildContext(Operation operation, ExtendedAPI api) throws BallerinaServiceGenException {
        if (operation == null) {
            return getDefaultValue();
        }

        // OperationId with spaces will cause trouble in ballerina code.
        // Replacing it with '_' so that we can identify there was a ' ' when doing bal -> swagger
        this.operationId = getTrimmedOperationId(operation.getOperationId());
        this.tags = operation.getTags();
        this.summary = operation.getSummary();
        this.description = operation.getDescription();
        this.externalDocs = operation.getExternalDocs();
        this.parameters = new ArrayList<>();
        this.methods = null;
        Map<String, Object> extensions =  operation.getExtensions();
        if(extensions != null){
            Optional<Object> resourceTier = Optional.ofNullable(extensions.get(X_THROTTLING_TIER));
            resourceTier.ifPresent(value -> this.resourceTier = value.toString());
            Optional<Object> scopes = Optional.ofNullable(extensions.get(X_SCOPE));
            scopes.ifPresent(value -> this.scope = value.toString());
            Optional<Object> authType = Optional.ofNullable(extensions.get(X_AUTH_TYPE));
            authType.ifPresent(value -> {
                if (AUTH_TYPE_NONE.equals(value)) {
                    this.isSecured = false;
                }
            });
            //set resource level endpoint configuration
            setEpConfigDTO(operation);
            //set resource level request interceptors
            Optional<Object> requestInterceptor = Optional.ofNullable(extensions
                    .get(OpenAPIConstants.REQUEST_INTERCEPTOR));
            requestInterceptor.ifPresent(value -> this.requestInterceptor = value.toString());
            //set resource level response interceptors
            Optional<Object> responseInterceptor = Optional.ofNullable(extensions
                    .get(OpenAPIConstants.RESPONSE_INTERCEPTOR));
            responseInterceptor.ifPresent(value -> this.responseInterceptor = value.toString());
            //set dev-first resource level throttle policy
            Optional<Object> devFirstResourceTier = Optional.ofNullable(extensions.get(OpenAPIConstants.THROTTLING_TIER));
            devFirstResourceTier.ifPresent(value -> this.resourceTier = value.toString());
        }

        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                this.parameters.add(new BallerinaParameter().buildContext(parameter, api));
            }
        }

        return this;
    }

    @Override
    public BallerinaOperation buildContext(Operation operation) throws BallerinaServiceGenException {
        return buildContext(operation, null);
    }

    private String getTrimmedOperationId (String operationId) {
        if (operationId == null) {
            return null;
        }

        return operationId.replaceAll(" ", "_");
    }

    @Override
    public BallerinaOperation getDefaultValue() {
        return new BallerinaOperation();
    }

    public List<String> getTags() {
        return tags;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getOperationId() {
        return operationId;
    }

    public List<BallerinaParameter> getParameters() {
        return parameters;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public List<String> getMethods() {
        return methods;
    }

    public List<String> getAllMethods() {
        return allMethods;
    }

    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
    }

    public String getResourceTier() {
        return resourceTier;
    }

    public void setResourceTier(String resourceTier) {
        this.resourceTier = resourceTier;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public void setSecured(boolean secured) {
        isSecured = secured;
    }

    public MgwEndpointConfigDTO getEpConfigDTO() {
        return epConfig;
    }

    private void setEpConfigDTO(Operation operation) {
        this.epConfig = OpenAPICodegenUtils.getResourceEpConfigForCodegen(operation);
        if (epConfig != null) {
            if (epConfig.getProdEndpointList() != null) {
                hasProdEpConfig = true;
            }
            if (epConfig.getSandboxEndpointList() != null) {
                hasSandEpConfig = true;
            }
        }
    }

    public String getRequestInterceptor() {
        return requestInterceptor;
    }

    public void setRequestInterceptor(String requestInterceptor) {
        this.requestInterceptor = requestInterceptor;
    }

    public String getResponseInterceptor() {
        return responseInterceptor;
    }

    public void setResponseInterceptor(String responseInterceptor) {
        this.responseInterceptor = responseInterceptor;
    }

    public String getApiRequestInterceptor() {
        return apiRequestInterceptor;
    }

    public void setApiRequestInterceptor(String requestInterceptor) {
        //if user specify the same interceptor function in both api level and resource level ignore
        // api level interceptor
        if (this.requestInterceptor == null || !this.requestInterceptor.equals(requestInterceptor)) {
            this.apiRequestInterceptor = requestInterceptor;
        }
    }

    public String getApiResponseInterceptor() {
        return apiResponseInterceptor;
    }

    public void setApiResponseInterceptor(String responseInterceptor) {
        //if user specify the same interceptor function in both api level and resource level ignore
        // api level interceptor
        if (this.responseInterceptor == null || !this.responseInterceptor.equals(responseInterceptor)) {
            this.apiResponseInterceptor = responseInterceptor;
        }
    }
}
