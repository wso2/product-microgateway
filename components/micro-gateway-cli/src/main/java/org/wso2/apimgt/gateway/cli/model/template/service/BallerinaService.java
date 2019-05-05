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
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.model.config.BasicAuth;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.model.config.Etcd;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * Wrapper for {@link OpenAPI}.
 * <p>This class can be used to push additional context variables for handlebars</p>
 */
public class BallerinaService implements BallerinaOpenAPIObject<BallerinaService, OpenAPI> {
    private String name;
    private ExtendedAPI api;
    private ContainerConfig containerConfig;
    private Config config;
    private MgwEndpointConfigDTO endpointConfig;
    private String srcPackage;
    private String modelPackage;
    private String qualifiedServiceName;
    private Info info = null;
    private ExternalDocumentation externalDocs = null;
    private Set<Map.Entry<String, String>> security = null;
    private List<Tag> tags = null;
    private Set<Map.Entry<String, BallerinaPath>> paths = null;
    private Etcd etcd;
    private String basepath;
    //to recognize whether it is a devfirst approach
    private boolean isDevFirst = true;

    /**
     * Build a {@link BallerinaService} object from a {@link OpenAPI} object.
     * All non iterable objects using handlebars library is converted into
     * supported iterable object types.
     *
     * @param openAPI {@link OpenAPI} type object to be converted
     * @return Converted {@link BallerinaService} object
     * @throws BallerinaServiceGenException when OpenAPI to BallerinaService parsing failed
     */
    @Override
    public BallerinaService buildContext(OpenAPI openAPI) throws BallerinaServiceGenException {
        this.info = openAPI.getInfo();
        this.externalDocs = openAPI.getExternalDocs();
        this.tags = openAPI.getTags();
        this.containerConfig = GatewayCmdUtils.getContainerConfig();
        //todo: fix this properly
        setSecuritySchemas(api.getMgwApiSecurity());
        this.config = GatewayCmdUtils.getConfig();
        this.etcd = GatewayCmdUtils.getEtcd();
        setPaths(openAPI);
        return this;
    }

    @Override
    public BallerinaService buildContext(OpenAPI definition, ExtendedAPI api) throws BallerinaServiceGenException {
        this.name = CodegenUtils.trim(api.getName());
        this.api = api;
        this.qualifiedServiceName =
                CodegenUtils.trim(api.getName()) + "_" + replaceAllNonAlphaNumeric(api.getVersion());
        this.endpointConfig = api.getEndpointConfigRepresentation();
        this.setBasepath(api.getSpecificBasepath());

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

    public Set<Map.Entry<String, String>> getSecurity() {
        return security;
    }

    public List<Tag> getTags() {
        return tags;
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
        //todo: remove comment
        if (openAPI.getPaths() == null) {
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
                //if it is the developer first approach
                if (isDevFirst) {
                    //to add API level request interceptor
                    Optional<Object> apiRequestInterceptor = Optional.ofNullable(openAPI.getExtensions()
                            .get("x-mgw-request-interceptor"));
                    apiRequestInterceptor.ifPresent(value -> operation.getValue()
                            .setApiRequestInterceptor(value.toString()));
                    //to add API level response interceptor
                    Optional<Object> apiResponseInterceptor = Optional.ofNullable(openAPI.getExtensions()
                            .get("x-mgw-response-interceptor"));
                    apiResponseInterceptor.ifPresent(value -> operation.getValue()
                            .setApiResponseInterceptor(value.toString()));
                    //to add API-level throttling policy
                    Optional<Object> apiThrottlePolicy = Optional.ofNullable(openAPI.getExtensions()
                            .get("x-mgw-throttling-tier"));
                    apiThrottlePolicy.ifPresent(value -> {
                        //api level throttle policy is added only if resource level resource tier is not available
                        if (operation.getValue().getResourceTier() == null) {
                            operation.getValue().setResourceTier(value.toString());
                        }
                    });
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

    private void setSecuritySchemas(String schemas) {
        Config config = GatewayCmdUtils.getConfig();
        BasicAuth basicAuth = new BasicAuth();
        boolean basic = false;
        boolean oauth2 = false;
        String[] schemasArray = schemas.trim().split("\\s*,\\s*");
        for (String s : schemasArray) {
            if (s.equalsIgnoreCase("basic")) {
                basic = true;
            } else if (s.equalsIgnoreCase("oauth2")) {
                oauth2 = true;
            }
        }
        if (basic && oauth2) {
            basicAuth.setOptional(true);
            basicAuth.setRequired(false);
        } else if (basic) {
            basicAuth.setRequired(true);
            basicAuth.setOptional(false);
        } else if (oauth2) {
            basicAuth.setOptional(false);
            basicAuth.setRequired(false);
        }
        config.setBasicAuth(basicAuth);
    }

    public void setIsDevFirst(boolean value){
        isDevFirst = value;
    }
}
