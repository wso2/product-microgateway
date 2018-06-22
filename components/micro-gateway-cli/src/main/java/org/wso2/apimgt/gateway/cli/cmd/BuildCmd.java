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
import org.apache.commons.lang3.StringUtils;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * This class represents the "build" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "build", commandDescription = "micro gateway build information")
public class BuildCmd implements GatewayLauncherCmd {

    private static PrintStream outStream = System.err;
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = { "-n", "--project" }, hidden = true)
    private String projectName;

    @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
    private boolean helpFlag;

    @Parameter(arity = 1)
    private List<String> argList;

    private JCommander parentCmdParser;

    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("build");
            outStream.println(commandUsageInfo);
            return;
        }

        if (StringUtils.isEmpty(projectName)) {
            outStream.println("Label can't be empty. You need to specify -n <project name>");
            return;
        }

        try {
            String projectRoot = GatewayCmdUtils.getStoredWorkspaceLocation();
            GatewayCmdUtils.createLabelGWDistribution(projectRoot, projectName);
        } catch (IOException e) {
            outStream.println(
                    "Error while creating micro gateway distribution for project " + projectName + ". Reason: " + e
                            .getMessage());
            Runtime.getRuntime().exit(1);
        }
        Runtime.getRuntime().exit(0);
    }

    @Override
    public String getName() {
        return GatewayCliCommands.BUILD;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        this.parentCmdParser = parentCmdParser;
    }
}
