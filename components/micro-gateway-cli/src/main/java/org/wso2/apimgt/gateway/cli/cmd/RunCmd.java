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

import java.io.PrintStream;
import java.util.List;

/**
 * This class represents the "run" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "run", commandDescription = "micro gateway run information")
public class RunCmd implements GatewayLauncherCmd {
    private static PrintStream outStream = System.err;
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = { "-l", "--label" }, hidden = true)
    private String label;

    @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
    private boolean helpFlag;

    @Parameter(arity = 1)
    private List<String> argList;

    private JCommander parentCmdParser;

    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("run");
            outStream.println(commandUsageInfo);
            return;
        }

        if (StringUtils.isEmpty(label)) {
            outStream.println("Label can't be empty. You need to specify -l <label name>");
            return;
        }
    }

    @Override
    public String getName() {
        return GatewayCliCommands.RUN;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        this.parentCmdParser = parentCmdParser;
    }
}
