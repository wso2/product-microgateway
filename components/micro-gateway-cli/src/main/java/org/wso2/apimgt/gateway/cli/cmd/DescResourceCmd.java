/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.model.rest.ResourceRepresentation;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "desc resource", commandDescription = "describe the given resource in the microgateway")
public class DescResourceCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(DescResourceCmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = {"--project"}, hidden = true)
    private String projectName;

    @Override
    public void execute() {

        projectName = GatewayCmdUtils.buildProjectName(projectName);

        String resource_id = GatewayCmdUtils.getSingleArgument(mainArgs);
        ResourceRepresentation resource = OpenAPICodegenUtils.getResource(projectName, resource_id);
        if (resource != null) {
            printResourceDetails(resource);
        } else {
            outStream.println("No resource available for the ID : " + resource_id);
        }
    }

    private void printResourceDetails(ResourceRepresentation resource){
        outStream.println("Resource_id : " + resource.getId());
        outStream.println("API Name : " + resource.getApi());
        outStream.println("API version : " + resource.getId());
        outStream.println("Resource : " + resource.getName());
        outStream.println("Operation: " + resource.getMethod());

        outStream.println("\nEndpointConfiguration: ");
        outStream.println(RouteUtils.getResourceAsYaml(resource.getId()));
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
