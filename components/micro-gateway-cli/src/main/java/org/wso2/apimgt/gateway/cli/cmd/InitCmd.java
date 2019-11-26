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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;
import org.wso2.apimgt.gateway.cli.utils.ToolkitLibExtractionUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This command is used to initialize a new microgateway project.
 * Command will create the basic directory structure for a microgateway project.
 */
@Parameters(commandNames = "init", commandDescription = "initialize a new project")
public class InitCmd implements LauncherCmd {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitCmd.class);
    private static final PrintStream OUT = System.out;

    @SuppressWarnings("unused")
    @Parameter(description = "project", required = true)
    private String projectName;

    @SuppressWarnings("unused")
    @Parameter(names = {"-f", "--force"}, description = "replace existing projects having same name", arity = 0)
    private boolean isForceful;

    @SuppressWarnings("unused")
    @Parameter(names = {"-d", "--deployment-config"}, description = "deployment-config file for docker/k8s")
    private String deploymentConfigPath;

    @SuppressWarnings("unused")
    @Parameter(names = {"--help", "-h", "?"}, description = "print command help", help = true)
    private boolean helpFlag;

    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @SuppressWarnings("unused")
    @Parameter(names = {"-a", "--api-definition"})
    private String apiDefinition;

    @SuppressWarnings("unused")
    @Parameter(names = "--headers")
    private String headers;

    @SuppressWarnings("unused")
    @Parameter(names = "--values")
    private String values;

    @SuppressWarnings("unused")
    @Parameter(names = {"-k", "--insecure"})
    private boolean insecure;

    @Override
    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("init");
            OUT.println(commandUsageInfo);
            return;
        }

        String workspace = CmdUtils.getUserDir();
        Path projectLocation = Paths.get(workspace + File.separator + projectName);
        boolean isDirectory = Files.isDirectory(projectLocation);

        if (isDirectory && !isForceful) {
            throw CmdUtils.createUsageException("Project name `" + projectName
                    + "` already exist. use -f or --force to forcefully update the project directory.");
        }

        // This is a valid force init
        if (isDirectory) {
            CmdUtils.deleteProject(projectName);
        }

        // Extract the zipped ballerina platform and runtime
        ToolkitLibExtractionUtils.extractPlatformAndRuntime();
        init(projectName, deploymentConfigPath, apiDefinition, headers, values, insecure);

        OUT.println("Project '" + projectName + "' is initialized successfully.");
        OUT.println("\n(Use \"" + CliConstants.MICRO_GW + ' ' + CliCommands.BUILD + ' ' + projectName
                + "\" after copying the api definitions)");
        OUT.println("(Use \"" + CliConstants.MICRO_GW + ' ' + CliCommands.IMPORT + ' ' + projectName
                + " [-l]|[-a -v]\" to import APIs from WSO2 API Manager)");
    }

    @Override
    public String getName() {
        return CliCommands.INIT;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }

    /**
     * Create project directory structure and initial deployment configuration.
     *
     * @param projectName          name of the project being initialized
     * @param deploymentConfigPath path to deployment config file (used in k8s scenarios)
     */
    private static void init(String projectName, String deploymentConfigPath, String apiDefinition, String headers,
                             String values, boolean insecure) {
        try {
            CmdUtils.createProjectStructure(projectName, apiDefinition, headers, values, insecure);
            CmdUtils.createDeploymentConfig(projectName, deploymentConfigPath);
        } catch (IOException e) {
            LOGGER.error("Error occurred while generating project configurations", e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        }
    }
}
