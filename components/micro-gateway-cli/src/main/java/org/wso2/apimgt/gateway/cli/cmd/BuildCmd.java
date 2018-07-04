/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * This class represents the "build" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "build", commandDescription = "micro gateway build information")
public class BuildCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(BuildCmd.class);
    private static PrintStream outStream = System.err;
    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @SuppressWarnings("unused")
    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    private String projectName;

    @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
    private boolean helpFlag;

    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("build");
            outStream.println(commandUsageInfo);
            return;
        }

        try {
            projectName = GatewayCmdUtils.getProjectName(mainArgs);
            GatewayCmdUtils.createProjectGWDistribution(projectName);
            outStream.println("Build successful for the project - " + projectName);
        } catch (IOException e) {
            logger.error("Error occurred while creating micro gateway distribution for project {}.", projectName, e);
            throw new CLIInternalException("Error occurred while creating micro gateway distribution for project");
        }
    }

    @Override
    public String getName() {
        return GatewayCliCommands.BUILD;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }
}
