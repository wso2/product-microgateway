/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import com.beust.jcommander.Parameters;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * This class represents the "version" command and it displays the current version of the micro-gw toolkit .
 */
@Parameters(commandNames = "version", commandDescription = "find the micro-gw toolkit version")
public class VersionCmd implements LauncherCmd {
    private static PrintStream outStream = System.err;

    @Override
    public void execute() {
        String fileLocation = CmdUtils.getResourceFolderLocation() + File.separator + CliConstants.VERSION_FILE;
        String version = getversion(fileLocation);
        outStream.println(version);
    }

    @Override
    public String getName() {
        return CliCommands.VERSION;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        // Nothing to implement
    }

    /**
     * Get micro-gw version from version.txt file.
     *
     * @param filePath  path of version.txt file
     */
    public static String getversion(String filePath) {
        String toolkitVersion;
        try {
            toolkitVersion = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            throw new CLIInternalException("Error occurred while finding the version.txt file : ", e);
        }
        return toolkitVersion;
    }
}
