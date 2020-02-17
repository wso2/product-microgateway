/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apimgt.gateway.cli.model.template.service;


import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;

import java.util.HashMap;

/**
 * Data holder for a MGW Interceptor.
 */
public class BallerinaInterceptor {
    private String org;
    private String module;
    private String version;
    private String name;
    private String id;
    private String invokeStatement;
    private String importStatement;
    private String fqn;
    private Type type;

    /**
     * Interceptor types.
     *
     */
    public enum Type {
        JAVA,
        LOCAL,
        CENTRAL
    }

    private static HashMap<String, String> pickedIdentifiers = new HashMap<>();

    public BallerinaInterceptor(String extension) throws BallerinaServiceGenException {
        version = null;

        // if extension describes a central ballerina module, extract details
        if (extension.contains(OpenAPIConstants.INTERCEPTOR_PATH_SEPARATOR) &&
                extension.contains(OpenAPIConstants.INTERCEPTOR_FUNC_SEPARATOR)) {
            String[] parts = extension.split(OpenAPIConstants.INTERCEPTOR_PATH_SEPARATOR);
            String[] funcParts = parts[parts.length - 1].split(OpenAPIConstants.INTERCEPTOR_FUNC_SEPARATOR);

            if (parts.length == 2) {
                // set module name when the version is not specified in the extension
                module = funcParts[0];
            } else if (parts.length == 3) {
                module = parts[1];
                version = funcParts[0];
            }

            name = funcParts[1];
            org = parts[0];
            fqn = org + OpenAPIConstants.INTERCEPTOR_PATH_SEPARATOR + module;
            id = pickModuleIdentifier(fqn);

            if (id == null) {
                throw new BallerinaServiceGenException("Couldn't pick an unique identifier for module " + fqn);
            }
            invokeStatement = id + OpenAPIConstants.INTERCEPTOR_FUNC_SEPARATOR + name;
            importStatement = fqn + ' ' + OpenAPIConstants.MODULE_IMPORT_STATEMENT_CONSTANT + ' ' + id;
            type = Type.CENTRAL;
        } else if (extension.startsWith(OpenAPIConstants.INTERCEPTOR_JAVA_PREFIX)) {
            name = extension;
            invokeStatement = name;
            type = Type.JAVA;
        } else {
            name = extension;
            invokeStatement = name;
            type = Type.LOCAL;
        }
    }

    /**
     * Pick a unique identifier for a given module.
     * This will put each picked identifier into a global map. Which will then be used
     * to identify the picked identifiers.
     *
     * @param module ballerina interceptor module with relevant organization
     *               <p>Pattern: {@code org/module}</p>
     * @return selected identifier for the module
     */
    private String pickModuleIdentifier(String module) {
        // import identifier is already set for this module. No need to set a new identifier
        if (pickedIdentifiers.containsKey(module)) {
            return pickedIdentifiers.get(module);
        }

        for (String id : OpenAPIConstants.MODULE_IDENTIFIER_LIST) {
            // if current identifier value is not there as a value of the picked identifier map,
            // select it as the identifier for this interceptor module.
            if (!pickedIdentifiers.containsValue(id)) {
                pickedIdentifiers.put(module, id);
                return id;
            }
        }

        return null;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInvokeStatement() {
        return invokeStatement;
    }

    public void setInvokeStatement(String invokeStatement) {
        this.invokeStatement = invokeStatement;
    }

    public String getFqn() {
        return fqn;
    }

    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getImportStatement() {
        return importStatement;
    }

    public void setImportStatement(String importStatement) {
        this.importStatement = importStatement;
    }
}
