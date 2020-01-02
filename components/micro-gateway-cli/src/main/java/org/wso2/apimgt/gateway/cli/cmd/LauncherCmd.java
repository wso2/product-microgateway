/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;

import java.io.IOException;
import java.io.PrintStream;

/**
 * {@code LauncherCmd} represents a micro gateway cli launcher command.
 */
public interface LauncherCmd {

    void execute();

    String getName();

    void setParentCmdParser(JCommander parentCmdParser);

    default String getCommandUsageInfo(String commandName) {
        if (commandName == null) {
            commandName = CliCommands.DEFAULT;
        }

        String fileName = "cli-help/cli-" + commandName + ".help";
        try {
            return CmdUtils.readFileAsString(fileName, true);
        } catch (IOException e) {
            throw CmdUtils.createUsageException("Usage info not available for command: " + commandName);
        }
    }

    default void printUsageInfo(String commandName) {
        PrintStream err = System.err;
        String usageInfo = getCommandUsageInfo(commandName);
        err.println(usageInfo);
    }
}
