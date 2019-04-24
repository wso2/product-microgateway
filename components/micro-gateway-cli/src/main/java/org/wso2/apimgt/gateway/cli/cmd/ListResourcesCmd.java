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
 */
package org.wso2.apimgt.gateway.cli.cmd;

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

@Parameters(commandNames = "list resources", commandDescription = "list routes of the microgateway")
public class ListResourcesCmd implements GatewayLauncherCmd{
    private static final Logger LOGGER = LoggerFactory.getLogger(ListResourcesCmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(names = {"--project"}, hidden = true)
    private String projectName;

    @Parameter(hidden = true)
    private List<String> mainArgs;

    @Override
    public void execute() {
        projectName = GatewayCmdUtils.buildProjectName(projectName);
        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectMgwDefinitionFilePath(projectName));

        if (mainArgs == null) {
            printResourceDetailsForSingleAPI(OpenAPICodegenUtils.getAllResources(projectName));
        } else {
            String apiId = GatewayCmdUtils.getSingleArgument(mainArgs);
            printResourceDetailsForSingleAPI(OpenAPICodegenUtils.listResourcesFromSwaggerForAPI(projectName, apiId));
        }
    }

    private void printResourceDetailsForSingleAPI(List<ResourceRepresentation> resources) {
        if (resources == null || resources.size() == 0) {
            outStream.println("\nNo resources available in the project");
        } else {
            String tableStructure = "%-33s%-60s%-10s%-30s%-10s\n";
            outStream.format(tableStructure, "resourceId", "resource", "method", "api", "version");

            for (ResourceRepresentation resource : resources) {
                outStream.format(tableStructure, resource.getId(), resource.getName(), resource.getMethod(),
                        resource.getApi(), resource.getVersion());
            }
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
