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
package org.wso2.apimgt.gateway.codegen.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * {@code GatewayLauncherCmd} represents a micro gateway cli launcher command.
 *
 */
public interface GatewayLauncherCmd {

    void execute();

    String getName();

    void setParentCmdParser(JCommander parentCmdParser);

    static String getCommandUsageInfo(String commandName) {
        if (commandName == null) {
            throw GatewayCmdUtils.createUsageException("invalid command");
        }

        String fileName = "cli-help/cli-" + commandName + ".help";
        try {
            return GatewayCmdUtils.readFileAsString(fileName, true);
        } catch (IOException e) {
            throw GatewayCmdUtils.createUsageException("usage info not available for command: " + commandName);
        }
    }
}
