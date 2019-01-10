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

import io.swagger.models.*;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIInfoDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.API_endpointDTO;

import java.util.*;

/**
 * Wrapper for {@link Swagger}.
 * <p>This class can be used to push additional context variables for handlebars</p>
 */
public class BallerinaService implements BallerinaSwaggerObject<BallerinaService, Swagger> {
    private String name;
    private APIInfoDTO api;
    private ContainerConfig containerConfig;
    private Config config;
    private List<API_endpointDTO> endpointConfig;
    private String srcPackage;
    private String modelPackage;
    private String qualifiedServiceName;
    private Info info = null;
    private ExternalDocs externalDocs = null;
    private Set<Map.Entry<String, String>> security = null;
    private List<Tag> tags = null;
    private Set<Map.Entry<String, BallerinaPath>> paths = null;

    /**
     * Build a {@link BallerinaService} object from a {@link Swagger} object.
     * All non iterable objects using handlebars library is converted into
     * supported iterable object types.
     *
     * @param swagger {@link Swagger} type object to be converted
     * @return Converted {@link BallerinaService} object
     * @throws BallerinaServiceGenException when OpenAPI to BallerinaService parsing failed
     */
    @Override
    public BallerinaService buildContext(Swagger swagger) throws BallerinaServiceGenException {
        this.info = swagger.getInfo();
        this.externalDocs = swagger.getExternalDocs();
        this.tags = swagger.getTags();
        this.containerConfig = GatewayCmdUtils.getContainerConfig();
        this.config = GatewayCmdUtils.getConfig();
        setPaths(swagger);
        return this;
    }

    @Override
    public BallerinaService buildContext(Swagger definition, APIInfoDTO api) throws BallerinaServiceGenException {
        this.name = CodegenUtils.trim(api.getName());
        this.api = api;
        this.qualifiedServiceName =
                CodegenUtils.trim(api.getName()) + "_" + replaceAllNonAlphaNumeric(api.getVersion());
        if (api instanceof APIDTO) {
            this.endpointConfig = ((APIDTO) api).getEndpoint();
        }
        return buildContext(definition);
    }

    @Override
    public BallerinaService getDefaultValue() {
        return null;
    }

    /**
     * Populate path models into iterable structure.
     * This method will also add an operationId to each operation,
     * if operationId not provided in swagger definition
     *
     * @param swagger {@code OpenAPI} definition object with schema definition
     * @throws BallerinaServiceGenException when context building fails
     */
    private void setPaths(Swagger swagger) throws BallerinaServiceGenException {
        if (swagger.getPaths() == null) {
            return;
        }

        this.paths = new LinkedHashSet<>();
        Map<String, Path> pathList = swagger.getPaths();
        for (Map.Entry<String, Path> path : pathList.entrySet()) {
            BallerinaPath balPath = new BallerinaPath().buildContext(path.getValue(), this.api);
            balPath.getOperations().forEach(operation -> {
                if (operation.getValue().getOperationId() == null) {
                    // set the ballerina function name as {http_method}{UUID} ex : get_2345_sdfd_4324_dfds
                    String operationId = operation.getKey() + "_" + UUID.randomUUID().toString().replaceAll("-", "_");
                    operation.getValue().setOperationId(operationId);
                }
            });
            paths.add(new AbstractMap.SimpleEntry<>(path.getKey(), balPath));
        }
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


    private String replaceAllNonAlphaNumeric(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "_");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public APIInfoDTO getApi() {
        return api;
    }

    public void setApi(APIDTO api) {
        this.api = api;
    }

    public List<API_endpointDTO> getEndpointConfig() {
        return endpointConfig;
    }

    public void setEndpointConfig(List<API_endpointDTO> endpointConfig) {
        this.endpointConfig = endpointConfig;
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
}
