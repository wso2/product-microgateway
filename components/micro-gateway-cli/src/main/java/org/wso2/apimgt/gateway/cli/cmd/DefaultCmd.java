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

/**
 * This class represents the "main" command required by the JCommander.
 */
public class DefaultCmd implements GatewayLauncherCmd {

    @Parameter(names = { "--help", "-h", "?" }, hidden = true, description = "for more information")
    private boolean helpFlag;

    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Override
    public void execute() {
        if (helpFlag) {
            printUsageInfo(GatewayCliCommands.HELP);
            return;
        }
        printUsageInfo(GatewayCliCommands.DEFAULT);
    }

    @Override
    public String getName() {
        return GatewayCliCommands.DEFAULT;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }
}
