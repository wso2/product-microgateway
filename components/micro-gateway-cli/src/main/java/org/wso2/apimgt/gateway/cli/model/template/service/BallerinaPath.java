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

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CLICompileTimeException;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wraps the {@link PathItem} from swagger models to provide an iterable object model
 * for operations.
 *
 */
public class BallerinaPath implements BallerinaOpenAPIObject<BallerinaPath, PathItem> {
    private Set<Map.Entry<String, BallerinaOperation>> operations;

    public BallerinaPath() {
        this.operations = new LinkedHashSet<>();
    }

    @Override
    public BallerinaPath buildContext(PathItem item, ExtendedAPI api) throws BallerinaServiceGenException,
            CLICompileTimeException {
        Map.Entry<String, BallerinaOperation> entry;
        BallerinaOperation operation;

        // Swagger PathItem object doesn't provide a iterable structure for operations
        // Therefore we have to manually check if each http verb exists
        if (item.getGet() != null) {
            setServersToOperationLevel(item.getGet(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getGet(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under GET resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("get", operation);
            operations.add(entry);
        }
        if (item.getPut() != null) {
            setServersToOperationLevel(item.getPut(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getPut(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under PUT resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("put", operation);
            operations.add(entry);
        }
        if (item.getPost() != null) {
            setServersToOperationLevel(item.getPost(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getPost(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under POST resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("post", operation);
            operations.add(entry);
        }
        if (item.getDelete() != null) {
            setServersToOperationLevel(item.getDelete(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getDelete(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under DELETE resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("delete", operation);
            operations.add(entry);
        }
        if (item.getOptions() != null) {
            setServersToOperationLevel(item.getOptions(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getOptions(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under OPTIONS resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("options", operation);
            operations.add(entry);
        }
        if (item.getHead() != null) {
            setServersToOperationLevel(item.getHead(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getHead(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under HEAD resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("head", operation);
            operations.add(entry);
        }
        if (item.getPatch() != null) {
            setServersToOperationLevel(item.getPatch(), item.getServers());
            try {
                operation = new BallerinaOperation().buildContext(item.getPatch(), api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under HEAD resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("patch", operation);
            operations.add(entry);
        }

        return this;
    }

    @Override
    public BallerinaPath buildContext(PathItem item) throws BallerinaServiceGenException, CLICompileTimeException {
        return buildContext(item, null);
    }

    @Override
    public BallerinaPath getDefaultValue() {
        return null;
    }

    public Set<Map.Entry<String, BallerinaOperation>> getOperations() {
        return operations;
    }


    /**
     * Method to set the Path level server opeartions to operation level servers,
     * if operation level servers are not present
     *
     * @param operation {@link Operation} The current considered operation object
     * @param pathLevelServers list if server objects defined in the open API PAth level
     */
    private void setServersToOperationLevel(Operation operation, List<Server> pathLevelServers) {
        if (operation.getServers() == null && pathLevelServers != null) {
            operation.setServers(pathLevelServers);
        }
    }
}
