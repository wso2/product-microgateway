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

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.config.APIKey;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Wrapper for {@link OpenAPI}.
 * <p>This class can be used to push additional context variables for handlebars</p>
 */
public class BallerinaService implements BallerinaOpenAPIObject<BallerinaService, OpenAPI> {
    private String name;
    private ContainerConfig containerConfig;
    private Config config;
    private MgwEndpointConfigDTO endpointConfig;
    private String srcPackage;
    private String modelPackage;
    private String qualifiedServiceName;
    private Info info = null;
    private List<Tag> tags = null;
    private Set<Map.Entry<String, BallerinaPath>> paths = null;
    private String basepath;
    private HashMap<String, String> identifierMap = new HashMap<>();
    private ArrayList<String> importModules = new ArrayList<>();
    private HashMap<String, String> moduleVersionMap = new HashMap<>();
    //to recognize whether it is a devfirst approach
    private boolean isDevFirst = true;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private List<String> authProviders;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private List<APIKey> apiKeys;

    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    private ExtendedAPI api;

    /**
     * Build a {@link BallerinaService} object from a {@link OpenAPI} object.
     * All non iterable objects using handlebars library is converted into
     * supported iterable object types.
     *
     * @param openAPI {@link OpenAPI} type object to be converted
     * @return Converted {@link BallerinaService} object
     */
    @Override
    public BallerinaService buildContext(OpenAPI openAPI) {
        this.info = openAPI.getInfo();
        this.tags = openAPI.getTags();
        this.containerConfig = CmdUtils.getContainerConfig();
        this.config = CmdUtils.getConfig();
        return this;
    }

    @Override
    public BallerinaService buildContext(OpenAPI definition, ExtendedAPI api) throws BallerinaServiceGenException {
        this.name = CodegenUtils.trim(api.getName());
        this.api = api;
        this.qualifiedServiceName =
                CodegenUtils.trim(api.getName()) + "__" + replaceAllNonAlphaNumeric(api.getVersion());
        this.endpointConfig = api.getEndpointConfigRepresentation();
        this.setBasepath(api.getSpecificBasepath());
        this.authProviders = OpenAPICodegenUtils.getAuthProviders(api.getMgwApiSecurity());
        this.apiKeys = OpenAPICodegenUtils.generateAPIKeysFromSecurity(definition.getSecurity());
        setPaths(definition);
        // to extract the module names of ballerina modules to be imported from the Ballerina Central
        // if specified in api level interceptors
        extractImportModules(definition);
        return buildContext(definition);
    }

    @Override
    public BallerinaService getDefaultValue() {
        return null;
    }

    public BallerinaService srcPackage(String srcPackage) {
        if (srcPackage != null) {
            this.srcPackage = srcPackage.replaceFirst("\\.", "/");
        }
        return this;
    }

    public BallerinaService modelPackage(String modelPackage) {
        if (modelPackage != null) {
            this.modelPackage = modelPackage.replaceFirst("\\.", "/");
        }
        return this;
    }

    public String getSrcPackage() {
        return srcPackage;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public Info getInfo() {
        return info;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public HashMap<String, String> getIdentifierMap() {
        return identifierMap;
    }

    /**
     * To add unique module identifier values for each interceptor as per the operation
     *
     * @param interceptorModule       Ballerina interceptor module with relevant organization
     * @param moduleIdentifierArray  The ballerina module identifier string Array
     */
    public void setIdentifierMap(String interceptorModule, ImmutableList<String> moduleIdentifierArray) {
        for (int iteratorCount = 0; iteratorCount < moduleIdentifierArray.size(); iteratorCount++) {
            while (!this.identifierMap.containsValue(moduleIdentifierArray.get(iteratorCount)) &&
                    !this.identifierMap.containsKey(interceptorModule) && interceptorModule != null) {
                this.identifierMap.put(interceptorModule, moduleIdentifierArray.get(iteratorCount));
            }
        }
    }

    /**
     * Returns the map which contains the interceptor module name with organization and the module version.
     *
     * @return  {@link HashMap} object
     */
    public HashMap<String, String> getModuleVersionMap() {
        return moduleVersionMap;
    }

    /**
     * Maps the interceptor's Module Name with the specific version when the interceptors are being looked-up from
     * the Ballerina Central.
     *
     * @param interceptorModuleName The interceptor module name with the organization
     * @param moduleVersion         The interceptor module version
     */
    public void setModuleVersionMap(String interceptorModuleName, String moduleVersion) {
        if ((!this.moduleVersionMap.containsKey(interceptorModuleName)) && (moduleVersion != null)) {
            this.moduleVersionMap.put(interceptorModuleName, moduleVersion);
        }
    }

    public ArrayList<String> getImportModules() {
        return importModules;
    }

    /**
     * To set the names of all the modules to be imported from the Ballerina Central
     *
     * @param moduleName     The name of the module which is stored in Ballerina Central
     */
    public void setImportModules(String moduleName) {
        if (!this.importModules.contains(moduleName) && (moduleName != null)) {
            this.importModules.add(moduleName);
        }
    }

    /**
     * To extract the ballerina module name from the openAPI definitions, if the api level interceptors are to be
     * accessed from the Ballerina Central
     *
     * @param openAPI       {@link OpenAPI} object
     */
    public void extractImportModules (OpenAPI openAPI) {
        // set ballerina modules to be imported for api level request interceptors
        Optional<Object> apiRequestInterceptor = Optional.ofNullable(openAPI.getExtensions()
                .get(OpenAPIConstants.REQUEST_INTERCEPTOR));
        if (apiRequestInterceptor.toString().contains(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)) {
           apiRequestInterceptor.ifPresent(value -> {
               // set the organization name with the module name
               String requestInterceptorModule = value.toString().split(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)[0]
                       + OpenAPIConstants.MODULE_STATEMENT_SEPARATOR
                       + OpenAPICodegenUtils.buildModuleStatement(value.toString());
               setIdentifierMap(requestInterceptorModule, OpenAPIConstants.MODULE_IDENTIFIER_LIST);
               String apiRequestInterceptorIdentifierValue = getIdentifierMap().get(requestInterceptorModule);
               String importStatement = requestInterceptorModule + OpenAPIConstants.WHITESPACE
                       + OpenAPIConstants.MODULE_IMPORT_STATEMENT_CONSTANT + OpenAPIConstants.WHITESPACE
                       + apiRequestInterceptorIdentifierValue;
               setImportModules(importStatement);
            });
        }
        // set ballerina modules to be imported for api level response interceptors
        Optional<Object> apiResponseInterceptor = Optional.ofNullable(openAPI.getExtensions()
                .get(OpenAPIConstants.RESPONSE_INTERCEPTOR));
        if (apiResponseInterceptor.toString().contains(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)) {
            apiResponseInterceptor.ifPresent(value -> {
                // set the organization name with the module name
                String responseInterceptorModule = value.toString().
                        split(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)[0]
                        + OpenAPIConstants.MODULE_STATEMENT_SEPARATOR
                        + OpenAPICodegenUtils.buildModuleStatement(value.toString());
                setIdentifierMap(responseInterceptorModule, OpenAPIConstants.MODULE_IDENTIFIER_LIST);
                String apiResponseInterceptorIdentifierValue = getIdentifierMap().get(responseInterceptorModule);
                String importStatement = responseInterceptorModule + OpenAPIConstants.WHITESPACE
                        + OpenAPIConstants.MODULE_IMPORT_STATEMENT_CONSTANT + OpenAPIConstants.WHITESPACE
                        + apiResponseInterceptorIdentifierValue;
                setImportModules(importStatement);
            });
        }
    }

    public Set<Map.Entry<String, BallerinaPath>> getPaths() {
        return paths;
    }

    /**
     * Populate path models into iterable structure.
     * This method will also add an operationId to each operation,
     * if operationId not provided in openAPI definition
     *
     * @param openAPI {@code OpenAPI} definition object with schema definition
     * @throws BallerinaServiceGenException when context building fails
     */
    private void setPaths(OpenAPI openAPI) throws BallerinaServiceGenException {
        if (openAPI.getPaths() == null || this.api == null) {
            return;
        }

        this.paths = new LinkedHashSet<>();
        Paths pathList = openAPI.getPaths();
        for (Map.Entry<String, PathItem> path : pathList.entrySet()) {
            BallerinaPath balPath = new BallerinaPath().buildContext(path.getValue(), this.api);
            balPath.getOperations().forEach(operation -> {
                // set the ballerina function name as {http_method}{UUID} ex : get_2345_sdfd_4324_dfds
                String operationId = operation.getKey() + "_" + UUID.randomUUID().toString().replaceAll("-", "_");
                operation.getValue().setOperationId(operationId);

                /*
                Set the import modules specified in the operation level request interceptors with identifiers when the
                interceptor is being looked-up from the Ballerina Central, and map the specific module with it's
                version if it has been specified in the OpenAPI definition.
                */
                if ((operation.getValue().getRequestInterceptorModule() != null) &&
                        (operation.getValue().getRequestInterceptor() != null)) {
                    if (operation.getValue().getRequestInterceptorModuleVersion() != null) {
                        // The version of the operation level request interceptor module
                        String requestInterceptorModuleVersion = operation.getValue()
                                .getRequestInterceptorModuleVersion();
                        // maps the operation level request interceptor module name with it's version
                        setModuleVersionMap(operation.getValue().getRequestInterceptorModule(),
                                requestInterceptorModuleVersion);
                    }
                    // maps the operation level request interceptor module name with an identifier from the list
                    setIdentifierMap(operation.getValue().getRequestInterceptorModule(),
                            OpenAPIConstants.MODULE_IDENTIFIER_LIST);
                    String requestInterceptorIdentifier = getIdentifierMap().get(operation.getValue()
                            .getRequestInterceptorModule());
                    String requestInterceptorFunctionCallStatement = requestInterceptorIdentifier
                            + OpenAPIConstants.INTERCEPTOR_STATEMENT_SEPARATOR
                            + operation.getValue().getRequestInterceptor();
                    // set the operation level request interceptor
                    operation.getValue().setRequestInterceptor(requestInterceptorFunctionCallStatement);
                    // set the import statement for the operation level request interceptor module
                    setImportModules(operation.getValue().getRequestInterceptorModule()
                            + OpenAPIConstants.WHITESPACE + OpenAPIConstants.MODULE_IMPORT_STATEMENT_CONSTANT
                            + OpenAPIConstants.WHITESPACE + requestInterceptorIdentifier);
                }

                /*
                Set the import modules specified in the operation level response interceptors with identifiers when the
                interceptor is being looked-up from the Ballerina Central, and map the specific module with it's
                version if it has been specified in the OpenAPI definition.
                */
                if ((operation.getValue().getResponseInterceptorModule() != null) &&
                        (operation.getValue().getResponseInterceptor() != null)) {
                    if (operation.getValue().getResponseInterceptorModuleVersion() != null) {
                        // The version of the operation level response interceptor module
                        String responseInterceptorModuleVersion = operation.getValue()
                                .getResponseInterceptorModuleVersion();
                        // maps the operation level response interceptor module name with it's version
                        setModuleVersionMap(operation.getValue().getResponseInterceptorModule(),
                                responseInterceptorModuleVersion);
                    }
                    // maps the operation level response interceptor module name with an identifier from the list
                    setIdentifierMap(operation.getValue().getResponseInterceptorModule(),
                            OpenAPIConstants.MODULE_IDENTIFIER_LIST);
                    String responseInterceptorIdentifier = getIdentifierMap().get(operation.getValue()
                            .getResponseInterceptorModule());
                    String responseInterceptorFunctionCallStatement = responseInterceptorIdentifier
                            + OpenAPIConstants.INTERCEPTOR_STATEMENT_SEPARATOR
                            + operation.getValue().getResponseInterceptor();
                    // set the operation level response interceptor
                    operation.getValue().setResponseInterceptor(responseInterceptorFunctionCallStatement);
                    // set the import statement for the operation level response interceptor module
                    setImportModules(operation.getValue().getResponseInterceptorModule()
                            + OpenAPIConstants.WHITESPACE + OpenAPIConstants.MODULE_IMPORT_STATEMENT_CONSTANT
                            + OpenAPIConstants.WHITESPACE + responseInterceptorIdentifier);
                }

                //to set auth providers property corresponding to the security schema in API-level
                operation.getValue().setSecuritySchemas(this.api.getMgwApiSecurity());

                //if it is the developer first approach
                if (isDevFirst) {

                    // for the purpose of adding API level request interceptors
                    Optional<Object> apiRequestInterceptor = Optional
                            .ofNullable(openAPI.getExtensions().get(OpenAPIConstants.REQUEST_INTERCEPTOR));
                    if (apiRequestInterceptor.toString().contains(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)) {
                        apiRequestInterceptor.ifPresent(value -> {
                        // set the organization name with the module name
                        String requestInterceptorModule = value.toString().
                                split(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)[0]
                                + OpenAPIConstants.MODULE_STATEMENT_SEPARATOR
                                + OpenAPICodegenUtils.buildModuleStatement(value.toString());
                        // The version of the api level request interceptor module
                        String requestInterceptorModuleVersion = OpenAPICodegenUtils.
                                buildModuleVersion(value.toString());
                            // maps the api level request interceptor module name with it's version if specified
                            if (requestInterceptorModuleVersion != null) {
                                setModuleVersionMap(requestInterceptorModule, requestInterceptorModuleVersion);
                            }
                        setIdentifierMap(requestInterceptorModule, OpenAPIConstants.MODULE_IDENTIFIER_LIST);
                        String apiRequestInterceptorIdentifierValue = getIdentifierMap().get(requestInterceptorModule);
                        String apiRequestInterceptorFunctionCallStatement = apiRequestInterceptorIdentifierValue +
                                OpenAPIConstants.INTERCEPTOR_STATEMENT_SEPARATOR +
                                value.toString().split(OpenAPIConstants.INTERCEPTOR_STATEMENT_SEPARATOR)[1];
                        operation.getValue().setApiRequestInterceptor(apiRequestInterceptorFunctionCallStatement);
                      });
                    } else {
                        apiRequestInterceptor.ifPresent(value -> operation.getValue()
                                .setApiRequestInterceptor(value.toString()));
                    }

                    // for the purpose of adding API level response interceptors
                    Optional<Object> apiResponseInterceptor = Optional
                            .ofNullable(openAPI.getExtensions().get(OpenAPIConstants.RESPONSE_INTERCEPTOR));
                    if (apiResponseInterceptor.toString().contains(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)) {
                       apiResponseInterceptor.ifPresent(value -> {
                       // set the organization name with the module name
                       String responseInterceptorModule = value.toString().
                               split(OpenAPIConstants.MODULE_STATEMENT_SEPARATOR)[0]
                               + OpenAPIConstants.MODULE_STATEMENT_SEPARATOR
                               + OpenAPICodegenUtils.buildModuleStatement(value.toString());
                       // The version of the api level response interceptor module
                       String responseInterceptorModuleVersion = OpenAPICodegenUtils.
                               buildModuleVersion(value.toString());
                           // maps the api level response interceptor module name with it's version if specified
                           if (responseInterceptorModuleVersion != null) {
                               setModuleVersionMap(responseInterceptorModule, responseInterceptorModuleVersion);
                           }
                       setIdentifierMap(responseInterceptorModule, OpenAPIConstants.MODULE_IDENTIFIER_LIST);
                       String apiResponseInterceptorIdentifierValue = getIdentifierMap().get(responseInterceptorModule);
                       String apiResponseInterceptorFunctionCallStatement = apiResponseInterceptorIdentifierValue +
                               OpenAPIConstants.INTERCEPTOR_STATEMENT_SEPARATOR +
                               value.toString().split(OpenAPIConstants.INTERCEPTOR_STATEMENT_SEPARATOR)[1];
                       operation.getValue().setApiResponseInterceptor(apiResponseInterceptorFunctionCallStatement);
                       });
                    } else {
                        apiResponseInterceptor.ifPresent(value -> operation.getValue()
                                .setApiResponseInterceptor(value.toString()));
                    }

                    //to add API-level throttling policy
                    Optional<Object> apiThrottlePolicy = Optional.ofNullable(openAPI.getExtensions()
                            .get(OpenAPIConstants.THROTTLING_TIER));
                    //api level throttle policy is added only if resource level resource tier is not available
                    if (operation.getValue().getResourceTier() == null) {
                        apiThrottlePolicy.ifPresent(value -> operation.getValue().setResourceTier(value.toString()));
                    }
                    //to add API-level security disable
                    Optional<Object> disableSecurity = Optional.ofNullable(openAPI.getExtensions()
                            .get(OpenAPIConstants.DISABLE_SECURITY));
                    disableSecurity.ifPresent(value -> {
                        try {
                            //Since we are considering based on 'x-wso2-disable-security', secured value should be the
                            // negation
                            boolean secured = !(Boolean) value;
                            operation.getValue().setSecured(secured);
                        } catch (ClassCastException e) {
                            throw new CLIRuntimeException("The property '" + OpenAPIConstants.DISABLE_SECURITY +
                                    "' should be a boolean value. But provided '" + value.toString() + "'.");
                        }
                    });
                    //to set scope property of API
                    operation.getValue().setScope(api.getMgwApiScope());
                }
            });
            paths.add(new AbstractMap.SimpleEntry<>(path.getKey(), balPath));
        }
    }

    private String replaceAllNonAlphaNumeric(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "_");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MgwEndpointConfigDTO getEndpointConfig() {
        return endpointConfig;
    }

    public void setEndpointConfig(MgwEndpointConfigDTO endpointConfig) {
        this.endpointConfig = endpointConfig;
    }

    public ExtendedAPI getApi() {
        return api;
    }

    public void setApi(ExtendedAPI api) {
        this.api = api;
    }

    public String getQualifiedServiceName() {
        return qualifiedServiceName;
    }

    public void setQualifiedServiceName(String qualifiedServiceName) {
        this.qualifiedServiceName = qualifiedServiceName;
    }

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getBasepath() {
        return basepath;
    }

    public void setBasepath(String basepath) {
        this.basepath = basepath;
    }

    public void setIsDevFirst(boolean value) {
        isDevFirst = value;
    }
}
