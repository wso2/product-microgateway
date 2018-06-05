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

package org.wso2.apimgt.gateway.codegen.model;

import io.swagger.models.ExternalDocs;
import io.swagger.models.Info;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import org.apache.commons.lang3.StringUtils;
import org.wso2.apimgt.gateway.codegen.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.codegen.service.bean.EndpointConfig;
import org.wso2.apimgt.gateway.codegen.service.bean.ext.ExtendedAPI;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for {@link Swagger}.
 * <p>This class can be used to push additional context variables for handlebars</p>
 */
public class BallerinaService implements BallerinaSwaggerObject<BallerinaService, Swagger> {
    private String name;
    private ExtendedAPI api;
    private EndpointConfig endpointConfig;
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
        setPaths(swagger);
        return this;
    }

    @Override
    public BallerinaService buildContext(Swagger definition, ExtendedAPI api) throws BallerinaServiceGenException {
        this.name = trim(api.getName());
        this.api = api;
        this.qualifiedServiceName = trim(api.getName()) + "_" + replaceAllNonAlphaNumeric(api.getVersion());
        this.endpointConfig = api.getEndpointConfigRepresentation();
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
                    String pathName = path.getKey().substring(1); // need to drop '/' prefix from the key, ex:'/path'
                    String operationId = operation.getKey() + trim(StringUtils.capitalize(pathName));
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


    private String trim(String key) {
        if (key == null) {
            return null;
        }
        key = key.replaceAll(" ", "_");
        key = key.replaceAll("/", "_");
        key = key.replaceAll("\\{", "_");
        key = key.replaceAll("}", "_");
        return key;
    }

    private String replaceAllNonAlphaNumeric(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+","_");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EndpointConfig getEndpointConfig() {
        return endpointConfig;
    }

    public void setEndpointConfig(EndpointConfig endpointConfig) {
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
}
