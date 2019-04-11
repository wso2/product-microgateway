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
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

@Parameters(commandNames = "function", commandDescription = "custom function manilpulation")
public class FunctionCmd implements GatewayLauncherCmd {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionCmd.class);
    private static PrintStream outStream = System.out;

    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = {"-A", "--add"}, hidden = true)
    private Boolean isAdd = false;

    @Parameter(names = {"-U", "--update"}, hidden = true)
    private Boolean isUpdate = false;

    @Parameter(names = {"-f", "--file"}, hidden = true)
    private String fileLocation;

    @Parameter(names = {"-a", "--api"}, hidden = true)
    private String apiID;

    @Parameter(names = {"-r", "--resource"}, hidden = true)
    private String resourceID;

    @Parameter(names = {"-i", "--in"}, hidden = true)
    private String inFunction;

    @Parameter(names = {"-o", "--out"}, hidden = true)
    private String outFunction;

    @Override
    public void execute() {

        String projectName = GatewayCmdUtils.getProjectName(mainArgs);
        File projectLocation = new File(GatewayCmdUtils.getProjectDirectoryPath(projectName)
                + File.separator +"src");

        if (!projectLocation.exists()) {
            throw new CLIRuntimeException("Project " + projectName + " does not exist.");
        }
        //copy .bal file to the project directory
        try {
            File source = new File(fileLocation);
            FileUtils.copyFileToDirectory(source, projectLocation);
            outStream.println("file copied");
        } catch (IOException e) {
            LOGGER.error("error occured while copying file:" + e);
        }

        if (!isAdd) {
            return;
        }

        if (inFunction != null) {
            if (apiID != null) {
                //api level inFunction
                RouteUtils.addFunction(inFunction, RouteUtils.IN, apiID,
                        GatewayCmdUtils.getProjectRoutesConfFilePath(projectName), projectName);
            } else if (resourceID != null) {
                //add resource level inFunction
            } else {
                //global level in function
                RouteUtils.AddGlobalFunction(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName),
                        inFunction, "in");
            }
        } else if (outFunction != null) {
            //api level outFunction
            if (apiID != null) {
                RouteUtils.addFunction(outFunction, RouteUtils.OUT, apiID,
                        GatewayCmdUtils.getProjectRoutesConfFilePath(projectName), projectName);
            } else if (resourceID != null) {
                //add resource level outFunction
            } else {
                //global level outFunction
                RouteUtils.AddGlobalFunction(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName),
                        outFunction, "out");
            }
        }

    }

    @Override
    public String getName() {
        return GatewayCliCommands.FUNCTION;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }
}
