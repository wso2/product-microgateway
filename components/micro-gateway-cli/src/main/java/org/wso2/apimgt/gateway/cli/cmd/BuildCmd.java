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
import org.wso2.apimgt.gateway.cli.exception.*;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.RouteUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the "build" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "build", commandDescription = "micro gateway build information")
public class BuildCmd implements GatewayLauncherCmd {
    private static final Logger logger = LoggerFactory.getLogger(BuildCmd.class);
    private static PrintStream outStream = System.out;

    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @SuppressWarnings("unused")
    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @Parameter(names = {"--compiled"}, hidden = true, arity = 0)
    private boolean isCompiled;

    @Parameter(names = {"--help", "-h", "?"}, hidden = true, description = "for more information")
    private boolean helpFlag;


    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("build");
            outStream.println(commandUsageInfo);
            return;
        }

        String projectName = GatewayCmdUtils.getSingleArgument(mainArgs);
        projectName = projectName.replaceAll("[\\/\\\\]", "");
        File projectLocation = new File(GatewayCmdUtils.getProjectDirectoryPath(projectName));

        RouteUtils.setRoutesConfigPath(GatewayCmdUtils.getProjectRoutesConfFilePath(projectName));

        if (!projectLocation.exists()) {
            throw new CLIRuntimeException("Project " + projectName + " does not exist.");
        }

        //first phase of the build command; generation of ballerina code
        if(!isCompiled){
            try{
                String toolkitConfigPath = GatewayCmdUtils.getMainConfigLocation();
                init(projectName, toolkitConfigPath);
                CodeGenerator codeGenerator = new CodeGenerator();
                ThrottlePolicyGenerator policyGenerator = new ThrottlePolicyGenerator();
                boolean changesDetected;

                policyGenerator.generate(GatewayCmdUtils.getProjectSrcDirectoryPath(projectName) + File.separator
                        + GatewayCliConstants.POLICY_DIR, projectName);
                codeGenerator.generate(projectName, true, true);
                //Initializing the ballerina project and creating .bal folder.
                InitHandler.initialize(Paths.get(GatewayCmdUtils.getProjectDirectoryPath(projectName)), null,
                        new ArrayList<>(), null);

//todo:
//                try {
//                    changesDetected = HashUtils.detectChanges(apis, subscriptionPolicies,
//                            applicationPolicies, projectName);
//                } catch (HashingException e) {
//                    logger.error("Error while checking for changes of resources. Skipping no-change detection..", e);
//                    throw new CLIInternalException(
//                            "Error while checking for changes of resources. Skipping no-change detection..");
//                }
            } catch (IOException e) {
                throw new CLIInternalException("Error occured while generating ballerina code for the swagger file.");
            }
        }
        //second phase of the build command; ballerina code compilation
        else{
            try {
                GatewayCmdUtils.createProjectGWDistribution(projectName);
                outStream.println("Build successful for the project - " + projectName);
            } catch (IOException e) {
                logger.error("Error occurred while creating the micro gateway distribution for the project {}.", projectName, e);
                throw new CLIInternalException("Error occurred while creating the micro gateway distribution for the project");
            }
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
    private void init(String projectName, String configPath) {
        try {

            Path configurationFile = Paths.get(configPath);
            if (Files.exists(configurationFile)) {
                Config config = TOMLConfigParser.parse(configPath, Config.class);
                GatewayCmdUtils.setConfig(config);
            } else {
                logger.error("Configuration: {} Not found.", configPath);
                throw new CLIInternalException("Error occurred while loading configurations.");
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
        }
    }
}
