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
import org.ballerinalang.packerina.init.InitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.cli.codegen.ThrottlePolicyGenerator;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.ToolkitLibExtractionUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This class represents the "build" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "build", commandDescription = "build a project")
public class BuildCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(BuildCmd.class);
    private static PrintStream outStream = System.out;

    @SuppressWarnings("unused")
    @Parameter(description = "project", required = true)
    private String projectName;

    @SuppressWarnings("unused")
    @Parameter(names = {"-d", "--deployment-config"}, description = "deployment-config file for docker/k8s")
    private String deploymentConfigPath;

    @SuppressWarnings("unused")
    @Parameter(names = {"--help", "-h", "?"}, description = "print command help", help = true)
    private boolean helpFlag;

    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("build");
            outStream.println(commandUsageInfo);
            //to avoid the command running for a second time
            System.exit(1);
        }

        String projectName = this.projectName.replaceAll("[/\\\\]", "");
        File projectLocation = new File(GatewayCmdUtils.getProjectDirectoryPath(projectName));

        if (!projectLocation.exists()) {
            throw new CLIRuntimeException("Project " + projectName + " does not exist.");
        }
        //extract the ballerina platform and runtime
        ToolkitLibExtractionUtils.extractPlatformAndRuntime();

        File importedAPIDefLocation = new File(GatewayCmdUtils.getProjectAPIDefinitionsDirectoryPath(projectName));
        File addedAPIDefLocation = new File(GatewayCmdUtils.getProjectAPIFilesDirectoryPath(projectName));

        if (importedAPIDefLocation.list() != null && importedAPIDefLocation.list().length == 0
                && addedAPIDefLocation.list() != null && addedAPIDefLocation.list().length == 0) {
            throw new CLIRuntimeException("Nothing to build. API definitions does not exist.");
        }

        try {
            String toolkitConfigPath = GatewayCmdUtils.getMainConfigLocation();
            init(projectName, toolkitConfigPath, deploymentConfigPath);

            CodeGenerator codeGenerator = new CodeGenerator();
            ThrottlePolicyGenerator policyGenerator = new ThrottlePolicyGenerator();
            GatewayCmdUtils
                    .createGenDirectoryStructure(GatewayCmdUtils.getProjectTargetGenDirectoryPath(projectName));
            policyGenerator.generate(GatewayCmdUtils.getProjectGenSrcDirectoryPath(projectName) + File.separator
                    + GatewayCliConstants.POLICY_DIR, projectName);
            GatewayCmdUtils.copyAndReplaceFolder(GatewayCmdUtils.getProjectInterceptorsDirectoryPath(projectName),
                    GatewayCmdUtils.getProjectGenSrcInterceptorsDirectoryPath(projectName));
            codeGenerator.generate(projectName, true);

            //Initializing the ballerina project and creating .bal folder.
            InitHandler.initialize(Paths.get(GatewayCmdUtils.getProjectTargetGenDirectoryPath(projectName)), null,
                    new ArrayList<>(), null);
        } catch (IOException e) {
            throw new CLIInternalException(
                    "Error occurred while generating source code for the open API definitions.", e);
        }
    }

    @Override
    public String getName() {
        return GatewayCliCommands.BUILD;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }

    //todo: implement this method properly
    private void init(String projectName, String configPath, String deploymentConfig) {
        try {

            Path configurationFile = Paths.get(configPath);
            if (Files.exists(configurationFile)) {
                Config config = TOMLConfigParser.parse(configPath, Config.class);
                GatewayCmdUtils.setConfig(config);
            } else {
                logger.error("Configuration: {} Not found.", configPath);
                throw new CLIInternalException("Error occurred while loading configurations.");
            }
            if (deploymentConfig != null) {
                Path deploymentConfigFile = Paths.get(deploymentConfig);
                if (Files.exists(deploymentConfigFile)) {
                    GatewayCmdUtils.createDeploymentConfig(projectName, deploymentConfig);
                }
            }
            String deploymentConfigPath = GatewayCmdUtils.getDeploymentConfigLocation(projectName);
            ContainerConfig containerConfig = TOMLConfigParser.parse(deploymentConfigPath, ContainerConfig.class);
            GatewayCmdUtils.setContainerConfig(containerConfig);

            CodeGenerationContext codeGenerationContext = new CodeGenerationContext();
            codeGenerationContext.setProjectName(projectName);
            GatewayCmdUtils.setCodeGenerationContext(codeGenerationContext);
        } catch (ConfigParserException e) {
            logger.error("Error occurred while parsing the configurations {}", configPath, e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        } catch (IOException e) {
            throw new CLIInternalException("Error occured while reading the deployment configuration", e);
        }
    }
}
