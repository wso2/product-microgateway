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
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.config.APIKey;
import org.wso2.apimgt.gateway.cli.model.config.ApplicationSecurity;
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
 * <p>
 *     Parsing {@link OpenAPI} object model from the mustache/handlebars templates
 *     makes the template logic complex. These Ballerina... classes helps the process
 *     by wrapping all required attributes into a easily parsable object model.
 * </p>
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
    private String projectName;
    private Info info = null;
    private List<Tag> tags = null;
    private Set<Map.Entry<String, BallerinaPath>> paths = null;
    private String basepath;
    private ArrayList<String> importModules = new ArrayList<>();
    private HashMap<String, String> libVersions = new HashMap<>();
    private boolean isGrpc;
    //to identify if there is any endpoint with backend security (to insert "import ballerina/auth")
    private boolean hasEpSecurity = false;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private List<String> authProviders;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private List<APIKey> apiKeys;
    //check whether mutual ssl is enabled
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean isMutualSSL = false;
    //if mutual ssl is enabled, verifying client is mandatory or optional
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private String mutualSSLClientVerification;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean applicationSecurityOptional;

    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    private ExtendedAPI api;

    /**
     * API level request interceptor name.
     * This should be a name of a b7a function.
     */
    private String requestInterceptor;
    /**
     * API level response interceptor name.
     * This should be a name of a b7a function.
     */
    private String responseInterceptor;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean isJavaRequestInterceptor;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private boolean isJavaResponseInterceptor;

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
        this.isGrpc = api.isGrpc();
        this.setProjectName(CodeGenerator.projectName);
        this.setBasepath(api.getSpecificBasepath());
        ApplicationSecurity appSecurity = api.getApplicationSecurity();
        this.authProviders = OpenAPICodegenUtils.getAuthProviders(api.getMgwApiSecurity(), appSecurity);
        this.apiKeys = OpenAPICodegenUtils.generateAPIKeysFromSecurity(definition.getSecurity(),
                this.authProviders.contains(OpenAPIConstants.APISecurity.apikey.name()));
        if (api.getMutualSSL() != null) {
            this.isMutualSSL = true;
            this.mutualSSLClientVerification = api.getMutualSSL();
        }
        if (appSecurity != null && appSecurity.isOptional() != null) {
            this.applicationSecurityOptional = appSecurity.isOptional();
        }
        addAPILevelPolicy(definition);
        setHasEpSecurity(endpointConfig);
        setPaths(definition);
        //set default auth providers for api level
        if (!this.applicationSecurityOptional && this.authProviders.isEmpty()) {
            OpenAPICodegenUtils.addDefaultAuthProviders(this.authProviders);
        }
        resolveInterceptors(definition.getExtensions());
        setResponseCache(definition.getExtensions());
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

    /**
     * Returns the map which contains the interceptor module name with organization and the module version.
     *
     * @return  {@link HashMap} object
     */
    public HashMap<String, String> getLibVersions() {
        return libVersions;
    }

    public Set<Map.Entry<String, BallerinaPath>> getPaths() {
        return paths;
    }

    /**
     * Populate path models into iterable structure.
     * Method is generally used populate any missing data in {@link BallerinaOperation} after
     * building operation context. This happens when operation context requires
     * additional data from its parents to populate its own context details.
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
            BallerinaPath balPath = null;
            try {
                balPath = new BallerinaPath().buildContext(path.getValue(), this.api);
            } catch (CLICompileTimeException e) {
                throw new CLIRuntimeException("Error while parsing information under path:" + path.getKey() +
                        "in the API \"" + api.getName() + ":" + api.getVersion() + "\".\n\t-" + e.getTerminalMsg(), e);
            }
            balPath.getOperations().forEach(op -> {
                BallerinaOperation operation = op.getValue();
                // set the ballerina function name as {http_method}{UUID} ex : get_2345_sdfd_4324_dfds
                String operationId = op.getKey() + UUID.randomUUID().toString().replaceAll("-", "");
                operation.setOperationId(operationId);

                //set hasEpSecurity to identify if there are operations with backend security config
                setHasEpSecurity(operation.getEpConfigDTO());

                // set import and function call statement for operation level interceptors
                updateOperationInterceptors(operation);

                //to set auth providers property corresponding to the security schema in API-level
                operation.setSecuritySchemas(this.authProviders);

                // if it is the developer first approach
                //to add API-level security disable
                Optional<Object> disableSecurity = Optional.ofNullable(openAPI.getExtensions()
                        .get(OpenAPIConstants.DISABLE_SECURITY));
                disableSecurity.ifPresent(value -> {
                    try {
                        //Since we are considering based on 'x-wso2-disable-security', secured value should be the
                        // negation
                        boolean secured = !(Boolean) value;
                        operation.setSecured(secured);
                    } catch (ClassCastException e) {
                        throw new CLIRuntimeException("The property '" + OpenAPIConstants.DISABLE_SECURITY +
                                "' should be a boolean value. But provided '" + value.toString() + "'.");
                    }
                });
                //to set scope property of API
                operation.setScope(api.getMgwApiScope());

            });
            paths.add(new AbstractMap.SimpleEntry<>(path.getKey(), balPath));
        }
    }

    /**
     * Add new import statement to the import statement list if it is already not there
     * in the list.
     *
     * @param importStmt The name of the module which is stored in Ballerina Central
     */
    private void addImport(String importStmt) {
        if (!this.importModules.contains(importStmt) && (importStmt != null)) {
            this.importModules.add(importStmt);
        }
    }

    private void addAPILevelPolicy(OpenAPI openAPI) {
        // if it is the developer first approach to add API-level throttling policy
        Optional<Object> apiThrottlePolicy = Optional.ofNullable(openAPI.getExtensions()
                .get(OpenAPIConstants.THROTTLING_TIER));
        if (!apiThrottlePolicy.isPresent()) {
            apiThrottlePolicy = Optional.ofNullable(openAPI.getExtensions()
                    .get(OpenAPIConstants.APIM_THROTTLING_TIER));
        }
        apiThrottlePolicy.ifPresent(value -> this.api.setApiLevelPolicy(value.toString()));
    }

    /**
     * Extracts the ballerina module names of interceptors provided in OpenAPI definition.
     * Import statements will also be assigned with an alias for easy reference. Final format
     * of a import statement will look like below.
     * <p>
     *     Ex:
     *     {@code import foo/bar as colombo}
     * </p>
     *
     * @param exts OpenAPI Extensions map
     * @throws BallerinaServiceGenException when fails to generate module identifier
     */
    private void resolveInterceptors(Map<String, Object> exts) throws BallerinaServiceGenException {
        Object reqExt = exts.get(OpenAPIConstants.REQUEST_INTERCEPTOR);
        Object resExt = exts.get(OpenAPIConstants.RESPONSE_INTERCEPTOR);

        if (reqExt != null) {
            BallerinaInterceptor reqInterceptor = new BallerinaInterceptor(reqExt.toString());

            // Add new library version and import statement if interceptor is coming from central
            if (BallerinaInterceptor.Type.CENTRAL == reqInterceptor.getType()) {
                // Set library version only if specific version is provided
                if (reqInterceptor.getVersion() != null) {
                    addLibVersion(reqInterceptor.getFqn(), reqInterceptor.getVersion());
                }

                addImport(reqInterceptor.getImportStatement());
            }
            this.isJavaRequestInterceptor = BallerinaInterceptor.Type.JAVA == reqInterceptor.getType();
            this.requestInterceptor = reqInterceptor.getInvokeStatement();
        }

        if (resExt != null) {
            BallerinaInterceptor resInterceptor = new BallerinaInterceptor(resExt.toString());

            if (BallerinaInterceptor.Type.CENTRAL == resInterceptor.getType()) {
                if (resInterceptor.getVersion() != null) {
                    addLibVersion(resInterceptor.getFqn(), resInterceptor.getVersion());
                }

                addImport(resInterceptor.getImportStatement());
            }
            isJavaResponseInterceptor = BallerinaInterceptor.Type.JAVA == resInterceptor.getType();
            this.responseInterceptor = resInterceptor.getInvokeStatement();
        }
    }

    private void updateOperationInterceptors(BallerinaOperation operation) {
        BallerinaInterceptor reqInterceptor = operation.getReqInterceptorContext();
        BallerinaInterceptor resInterceptor = operation.getResInterceptorContext();

        if (reqInterceptor != null) {
            if (BallerinaInterceptor.Type.CENTRAL == reqInterceptor.getType()) {
                if (reqInterceptor.getVersion() != null) {
                    addLibVersion(reqInterceptor.getFqn(), reqInterceptor.getVersion());
                }
                addImport(reqInterceptor.getImportStatement());
            }
        }

        if (resInterceptor != null) {
            if (BallerinaInterceptor.Type.CENTRAL == resInterceptor.getType()) {
                if (resInterceptor.getVersion() != null) {
                    addLibVersion(resInterceptor.getFqn(), resInterceptor.getVersion());
                }
                addImport(resInterceptor.getImportStatement());
            }
        }
    }

    /**
     * Add new item to map containing the ballerina library to version mapping.
     * This map will be used to populate ballerina.toml file which will define
     * specific b7a version for each ballerina module.
     *
     * @param libName    The interceptor module name with the organization
     * @param libVersion The interceptor module version
     */
    private void addLibVersion(String libName, String libVersion) {
        if ((!this.libVersions.containsKey(libName)) && (libVersion != null)) {
            this.libVersions.put(libName, libVersion);
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setHasEpSecurity(MgwEndpointConfigDTO endpointConfig) {
        if (hasEpSecurity || endpointConfig == null) {
            return;
        }
        if ((endpointConfig.getProdEndpointList() != null &&
                endpointConfig.getProdEndpointList().getSecurityConfig() != null) ||
                (endpointConfig.getSandboxEndpointList() != null &&
                        endpointConfig.getSandboxEndpointList().getSecurityConfig() != null)) {
            hasEpSecurity = true;
        }
    }

    public void setResponseCache(Map<String, Object> exts) {
        if (exts != null) {
            Object responseCacheObject = exts.get(OpenAPIConstants.RESPONSE_CACHE);
            if (responseCacheObject != null) {
                // This logic is written to maintain backward compatibility with APIM 2.x.
                // We can not use mapping object dto.
                Map cacheObjectMap = (Map) responseCacheObject;
                boolean enabled = (boolean) cacheObjectMap.get(OpenAPIConstants.ENABLED);
                if (enabled) {
                    api.setResponseCaching(OpenAPIConstants.CACHE_ENABLED);
                    if (cacheObjectMap.containsKey(OpenAPIConstants.CACHE_TIMEOUT)) {
                        int cacheTimeout = (int) cacheObjectMap.get(OpenAPIConstants.CACHE_TIMEOUT);
                        api.setCacheTimeout(cacheTimeout * 1000); //set the value in milli seconds.
                    }
                } else {
                    api.setResponseCaching(OpenAPIConstants.DISABLED);
                }
            }
        }
    }
}
