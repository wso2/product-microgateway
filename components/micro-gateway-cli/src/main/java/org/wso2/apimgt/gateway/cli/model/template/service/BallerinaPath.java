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
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPIWrapper;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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
    private boolean generateApiFaultResponses = false;
    private boolean addMethodNotFoundService = false;
    private ArrayList<String> allowedOperations;
    private String strAllowedOperations;

    public BallerinaPath() {
        this.operations = new LinkedHashSet<>();
        this.allowedOperations = new ArrayList<>();
    }

    @Override
    public BallerinaPath buildContext(PathItem item, ExtendedAPIWrapper api) throws BallerinaServiceGenException,
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
            allowedOperations.add("GET");
        } else if (generateApiFaultResponses) {
            // If there is no GET method defined for the path, a dummy resource will be added to handle 405 responses
            try {
                operation = new BallerinaOperation().buildContextForNotAllowed(api);
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
            allowedOperations.add("PUT");
        } else if (generateApiFaultResponses) {
            // If there is no PUT method defined for the path, a dummy resource will be added to handle 405 responses
            try {
                operation = new BallerinaOperation().buildContextForNotAllowed(api);
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
            allowedOperations.add("POST");
        } else if (generateApiFaultResponses) {
            // If there is no POST method defined for the path, a dummy resource will be added to handle 405 responses
            try {
                operation = new BallerinaOperation().buildContextForNotAllowed(api);
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
            allowedOperations.add("DELETE");
        } else if (generateApiFaultResponses) {
            // If there is no DELETE method defined for the path, a dummy resource will be added to handle 405 responses
            try {
                operation = new BallerinaOperation().buildContextForNotAllowed(api);
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
            allowedOperations.add("OPTIONS");
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
            allowedOperations.add("HEAD");
        } else if (generateApiFaultResponses) {
            // If there is no HEAD method defined for the path, a dummy resource will be added to handle 405 responses
            try {
                operation = new BallerinaOperation().buildContextForNotAllowed(api);
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
                throw new CLICompileTimeException("Error while parsing the information under PATCH resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("patch", operation);
            operations.add(entry);
            allowedOperations.add("PATCH");
        } else if (generateApiFaultResponses) {
            // If there is no PATCH method defined for the path, a dummy resource will be added to handle 405 responses
            try {
                operation = new BallerinaOperation().buildContextForNotAllowed(api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under PATCH resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>("patch", operation);
            operations.add(entry);
        }

        // Used to set 'allow' header in 405 responses
        String strAllowedOperationsArray = Arrays.toString(allowedOperations.toArray());
        strAllowedOperations = strAllowedOperationsArray.substring(1, strAllowedOperationsArray.length() - 1);
        return this;
    }

    @Override
    public BallerinaPath buildContext(PathItem item) throws BallerinaServiceGenException, CLICompileTimeException {
        return buildContext(item, null);
    }

    public BallerinaPath buildContext(PathItem item, ExtendedAPIWrapper api, Boolean generateApiFaultResponses)
            throws BallerinaServiceGenException, CLICompileTimeException {
        this.generateApiFaultResponses = generateApiFaultResponses;
        return buildContext(item, api);
    }

    public BallerinaPath buildContextForNotFound(ExtendedAPIWrapper api) throws BallerinaServiceGenException,
            CLICompileTimeException {
        Map.Entry<String, BallerinaOperation> entry;
        BallerinaOperation operation;
        String[] supportedOperations = {"get", "post", "put", "delete", "patch", "head"};
        for (String supportedOp : supportedOperations) {
            try {
                operation = new BallerinaOperation().buildContextForNotFound(api);
            } catch (CLICompileTimeException e) {
                throw new CLICompileTimeException("Error while parsing the information under resource.\n\t-"
                        + e.getTerminalMsg(), e);
            }
            entry = new AbstractMap.SimpleEntry<>(supportedOp, operation);
            operations.add(entry);
        }
        return this;
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
     * if operation level servers are not present.
     *
     * @param operation {@link Operation} The current considered operation object
     * @param pathLevelServers list if server objects defined in the open API PAth level
     */
    private void setServersToOperationLevel(Operation operation, List<Server> pathLevelServers) {
        if (operation.getServers() == null && pathLevelServers != null) {
            operation.setServers(pathLevelServers);
        }
    }

    public boolean isGenerateApiFaultResponses() {
        return generateApiFaultResponses;
    }

    public void setGenerateApiFaultResponses(boolean generateApiFaultResponses) {
        this.generateApiFaultResponses = generateApiFaultResponses;
    }

    public boolean isAddMethodNotFoundService() {
        return addMethodNotFoundService;
    }

    public void setAddMethodNotFoundService(boolean addMethodNotFoundService) {
        this.addMethodNotFoundService = addMethodNotFoundService;
    }
}
