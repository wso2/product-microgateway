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
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;
import org.wso2.apimgt.gateway.cli.utils.SwaggerUtils;

import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "list", commandDescription = "list api/resources of the microgateway")
public class ListCmd implements GatewayLauncherCmd{

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = {"-ai", "--api-id"}, hidden = true)
    private String apiId;

    private boolean isListAPIsCmd;
    private String projectName;

    private static final Logger LOGGER = LoggerFactory.getLogger(AddCmd.class);
    private static PrintStream OUT_STREAM = System.out;
    @Override
    public void execute() {
        String[] typeAndProjectName = GatewayCmdUtils.getProjectNameAndType(mainArgs);
        if(typeAndProjectName[0].equals("apis")){
            isListAPIsCmd = true;
            projectName = typeAndProjectName[1];
        }
        else if(typeAndProjectName[0].equals("resources")){
            isListAPIsCmd = false;
            projectName = typeAndProjectName[1];
        }
        else{
            throw new CLIRuntimeException("Argument cannot be identified : " + typeAndProjectName[0]);
        }

        if(isListAPIsCmd){
            printAPIDetailsInTable(RouteUtils.listApis(projectName));
        }
        else{
            if(apiId.isEmpty()){
                throw new RuntimeException("API Id is not provided by the user");
            }
            printResourceDetails(SwaggerUtils
                    .listResourcesFromSwagger(GatewayCmdUtils.getProjectSwaggerFilePath(projectName, apiId)));
        }

    }

    private void printAPIDetailsInTable(List<String[]> rows){
        //todo: introduce constants
        if(rows == null || rows.size() == 0){
            OUT_STREAM.println("\nNo APIs in the project");
        }
        else{
            String tableStructure = "%-33s%-20s%-10s%-80s\n";
            OUT_STREAM.format(tableStructure, "apiId", "title", "version", "basepath");

            for(String[] row : rows){
                OUT_STREAM.format(tableStructure, row[0], row[1], row[2], row[3]);
            }
        }
    }

    private void printResourceDetails(List<String[]> rows){
        if(rows == null || rows.size() == 0){
            OUT_STREAM.println("\nNo resources available in the project");
        }
        else{
            String tableStructure = "%-33s%-60s%-10s\n";
            OUT_STREAM.format(tableStructure, "resourceId", "resource", "method");

            for(String[] row: rows){
                OUT_STREAM.format(tableStructure, row[0], row[1], row[2]);
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
