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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.config.APIKey;
import org.wso2.apimgt.gateway.cli.model.config.ApplicationSecurity;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Wraps the {@link Operation} from swagger models to provide iterable child models.
 */
public class BallerinaOperation implements BallerinaOpenAPIObject<BallerinaOperation, Operation> {

    public static final String X_THROTTLING_TIER = OpenAPIConstants.APIM_THROTTLING_TIER;
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
    private String scope;
    private boolean isSecured = true;
    //to identify if the isSecured flag is set from the operation
    private boolean isSecuredAssignedFromOperation = false;
    private MgwEndpointConfigDTO epConfig;
    private BallerinaInterceptor reqInterceptorContext;
    private BallerinaInterceptor resInterceptorContext;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean isJavaRequestInterceptor;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean isJavaResponseInterceptor;

    /**
     * b7a function name of operation level request interceptor.
     */
    private String requestInterceptor;
    /**
     * b7a function name of operation level response interceptor.
     */
    private String responseInterceptor;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private List<APIKey> apiKeys;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private List<String> authProviders;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean applicationSecurityOptional;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean hasProdEpConfig = false;
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean hasSandEpConfig = false;

    // Not static since handlebars can't see static variables
    private final List<String> allMethods =
            Arrays.asList("HEAD", "OPTIONS", "PATCH", "DELETE", "POST", "PUT", "GET");

    @Override
    public BallerinaOperation buildContext(Operation operation, ExtendedAPI api) throws BallerinaServiceGenException,
            CLICompileTimeException {
        if (operation == null) {
            return getDefaultValue();
        }

        // OperationId with spaces with special characters will cause errors in ballerina code.
        // Replacing it with uuid so that we can identify there was a ' ' when doing bal -> swagger
        operation.setOperationId(UUID.randomUUID().toString().replaceAll("-", ""));
        this.operationId = operation.getOperationId();
        this.tags = operation.getTags();
        this.summary = operation.getSummary();
        this.description = operation.getDescription();
        this.externalDocs = operation.getExternalDocs();
        this.parameters = new ArrayList<>();
        //to provide resource level security in dev-first approach
        ApplicationSecurity appSecurity = OpenAPICodegenUtils.populateApplicationSecurity(api.getName(),
                api.getVersion(), operation.getExtensions(), api.getMutualSSL());
        this.authProviders = OpenAPICodegenUtils.getMgwResourceSecurity(operation, appSecurity);
        this.apiKeys = OpenAPICodegenUtils.generateAPIKeysFromSecurity(operation.getSecurity(),
                this.authProviders.contains(OpenAPIConstants.APISecurity.apikey.name()));
        ApplicationSecurity apiAppSecurity = api.getApplicationSecurity();
        if (appSecurity != null && appSecurity.isOptional() != null) {
            // if app security is made optional at resource
            this.applicationSecurityOptional = appSecurity.isOptional();
        } else if (apiAppSecurity != null && apiAppSecurity.isOptional() != null) {
            // if app security made optional at API level
            this.applicationSecurityOptional = apiAppSecurity.isOptional();
        }
        //to set resource level scopes in dev-first approach
        this.scope = OpenAPICodegenUtils.getMgwResourceScope(operation);
        //set resource level endpoint configuration
        setEpConfigDTO(operation);
        Map<String, Object> exts = operation.getExtensions();

        if (exts != null) {
            // set interceptor details
            Object reqExt = exts.get(OpenAPIConstants.REQUEST_INTERCEPTOR);
            Object resExt = exts.get(OpenAPIConstants.RESPONSE_INTERCEPTOR);
            if (reqExt != null) {
                reqInterceptorContext = new BallerinaInterceptor(reqExt.toString());
                requestInterceptor = reqInterceptorContext.getInvokeStatement();
                isJavaRequestInterceptor = BallerinaInterceptor.Type.JAVA == reqInterceptorContext.getType();
            }
            if (resExt != null) {
                resInterceptorContext = new BallerinaInterceptor(resExt.toString());
                responseInterceptor = resInterceptorContext.getInvokeStatement();
                isJavaResponseInterceptor = BallerinaInterceptor.Type.JAVA == resInterceptorContext.getType();
            }


            Optional<Object> scopes = Optional.ofNullable(exts.get(X_SCOPE));
            scopes.ifPresent(value -> this.scope = "\"" + value.toString() + "\"");
            Optional<Object> authType = Optional.ofNullable(exts.get(X_AUTH_TYPE));
            authType.ifPresent(value -> {
                if (AUTH_TYPE_NONE.equals(value)) {
                    this.isSecured = false;
                }
            });
            Optional<Object> resourceTier = Optional.ofNullable(exts.get(X_THROTTLING_TIER));
            resourceTier.ifPresent(value -> this.resourceTier = value.toString());
            //set dev-first resource level throttle policy
            if (this.resourceTier == null) {
                Optional<Object> extResourceTier = Optional.ofNullable(exts.get(OpenAPIConstants.THROTTLING_TIER));
                extResourceTier.ifPresent(value -> this.resourceTier = value.toString());
            }
            if (api.getApiLevelPolicy() != null && this.resourceTier != null) {
                //if api level policy exists then we are neglecting the resource level policies
                String message = "[WARN] : Resource level policy: " + this.resourceTier
                        + " will be neglected due to the presence of API level policy: " + api.getApiLevelPolicy()
                        + " for the API : " + api.getName() + "\n";
                CmdUtils.appendMessagesToConsole(message);
                this.resourceTier = null;
            }
            Optional<Object> extDisableSecurity = Optional.ofNullable(exts.get(OpenAPIConstants.DISABLE_SECURITY));
            extDisableSecurity.ifPresent(value -> {
                try {
                    this.isSecured = !(Boolean) value;
                    this.isSecuredAssignedFromOperation = true;
                } catch (ClassCastException e) {
                    throw new CLIRuntimeException("The property '" + OpenAPIConstants.DISABLE_SECURITY +
                            "' should be a boolean value. But provided '" + value.toString() + "'.");
                }
            });
        }

        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                this.parameters.add(new BallerinaParameter().buildContext(parameter, api));
            }
        }

        return this;
    }

    @Override
    public BallerinaOperation buildContext(Operation operation) throws BallerinaServiceGenException,
            CLICompileTimeException {
        return buildContext(operation, null);
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
        if (this.scope == null) {
            this.scope = scope;
        }
    }

    public boolean isSecured() {
        return isSecured;
    }

    public void setSecured(boolean secured) {
        if (isSecuredAssignedFromOperation) {
            return;
        }
        isSecured = secured;
    }

    public MgwEndpointConfigDTO getEpConfigDTO() {
        return epConfig;
    }

    private void setEpConfigDTO(Operation operation) throws CLICompileTimeException {
        this.epConfig = OpenAPICodegenUtils.getResourceEpConfigForCodegen(operation);

        if (epConfig.getProdEndpointList() != null) {
            hasProdEpConfig = true;
        }
        if (epConfig.getSandboxEndpointList() != null) {
            hasSandEpConfig = true;
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

    public BallerinaInterceptor getReqInterceptorContext() {
        return reqInterceptorContext;
    }

    public void setReqInterceptorContext(BallerinaInterceptor reqInterceptorContext) {
        this.reqInterceptorContext = reqInterceptorContext;
    }

    public BallerinaInterceptor getResInterceptorContext() {
        return resInterceptorContext;
    }

    public void setResInterceptorContext(BallerinaInterceptor resInterceptorContext) {
        this.resInterceptorContext = resInterceptorContext;
    }

    void setSecuritySchemas(List<String> authProviders) {
        //update the Resource auth providers property only if there is no security scheme provided during instantiation
        if (this.authProviders.isEmpty()) {
            this.authProviders = new ArrayList<>(authProviders);
        }
        //set default auth providers after updating with api level auth providers
        if (!this.applicationSecurityOptional && this.authProviders.isEmpty()) {
            OpenAPICodegenUtils.addDefaultAuthProviders(this.authProviders);
        }
    }
}
