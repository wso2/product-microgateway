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

import io.swagger.models.ExternalDocs;
import io.swagger.models.Operation;
import io.swagger.models.parameters.Parameter;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIInfoDTO;

import java.util.*;

/**
 * Wraps the {@link Operation} from swagger models to provide iterable child models.
 *
 */
public class BallerinaOperation implements BallerinaSwaggerObject<BallerinaOperation, Operation> {

    public static final String X_THROTTLING_TIER = "x-throttling-tier";
    public static final String X_SCOPE = "x-scope";
    public static final String X_AUTH_TYPE = "x-auth-type";
    public static final String AUTH_TYPE_NONE = "None";
    private List<String> tags;
    private String summary;
    private String description;
    private String resourceTier;
    private ExternalDocs externalDocs;
    private String operationId;
    private List<BallerinaParameter> parameters;
    private List<String> methods;
    private String scope;
    private boolean isSecured = true;

    // Not static since handlebars can't see static variables
    private final List<String> allMethods =
            Arrays.asList("HEAD", "OPTIONS", "PATCH", "DELETE", "POST", "PUT", "GET");

    @Override
    public BallerinaOperation buildContext(Operation operation, APIInfoDTO api) throws BallerinaServiceGenException {
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
        Map<String, Object> extension =  operation.getVendorExtensions();
        Optional<Object> resourceTier = Optional.ofNullable(extension.get(X_THROTTLING_TIER));
        resourceTier.ifPresent(value -> this.resourceTier = value.toString());
        Optional<Object> scopes = Optional.ofNullable(extension.get(X_SCOPE));
        scopes.ifPresent(value -> this.scope = value.toString());
        Optional<Object> authType = Optional.ofNullable(extension.get(X_AUTH_TYPE));
        authType.ifPresent(value -> {
            if (AUTH_TYPE_NONE.equals(value)) {
                this.isSecured = false;
            }
        });

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

    public ExternalDocs getExternalDocs() {
        return externalDocs;
    }

    public void setExternalDocs(ExternalDocs externalDocs) {
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
}
