/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.apimgt.gateway.cli.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.io.FileUtils;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command used to set the current working project. This command will persist the path to current project
 * in a file in the MGW_HOME.
 */
@Parameters(commandNames = "set", commandDescription = "set working project")
public class SetProjectCmd implements GatewayLauncherCmd {
    private static final PrintStream OUT = System.out;

    @Parameter(required = true, hidden = true)
    private List<String> mainArgs;

    @Override
    public void execute() {
        String projectFile = GatewayCmdUtils.getCLIHome() + File.separator + GatewayCliConstants.PROJECT_FILE_NAME;
        String projectPath = GatewayCmdUtils.getSingleArgument(mainArgs);
        projectPath = new File(projectPath).getAbsolutePath();

        if (isValidProjectPath(projectPath)) {
            projectPath = projectPath.trim();

            try {
                // Write to file (creating file if doesn't exists and) replacing the current content
                FileUtils.writeStringToFile(new File(projectFile), projectPath.trim(), StandardCharsets.UTF_8, false);
            } catch (IOException e) {
                throw new CLIRuntimeException("Failed to save working project.");
            }
            OUT.println("Working project is set to: " + projectPath);
        } else {
            throw new CLIRuntimeException("Invalid project directory.");
        }
    }

    private boolean isValidProjectPath(String path) {
        // Currently only does isDirectory validation. Any other validations can be added later
        boolean isDir = Files.isDirectory(Paths.get(path));

        return isDir;
    }

    @Override
    public String getName() {
        return GatewayCliCommands.SET;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {

    }
}
