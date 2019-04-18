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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.*;
import org.wso2.apimgt.gateway.cli.hashing.LibHashUtils;
import org.wso2.apimgt.gateway.cli.model.config.Etcd;
import org.wso2.apimgt.gateway.cli.utils.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class represents the "setup" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "setup", commandDescription = "setup information")
public class SetupCmd implements GatewayLauncherCmd {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetupCmd.class);
    private static PrintStream outStream = System.out;

    @SuppressWarnings("unused")
    @Parameter(hidden = true, required = true)
    private List<String> mainArgs;

    @SuppressWarnings("unused")
    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;

    @Parameter(names = {"-u", "--username"}, hidden = true)
    private String username;

    @Parameter(names = {"-p", "--password"}, hidden = true)
    private String password;

    @SuppressWarnings("unused")
    @Parameter(names = {"-l", "--label"}, hidden = true)
    private String label;

    @Parameter(names = {"-s", "--server-url"}, hidden = true)
    private String baseURL;

    @Parameter(names = {"-oa", "--openapi"}, hidden = true)
    private String openApi;

    @Parameter(names = {"-e", "--endpoint"}, hidden = true)
    private String endpoint;

    @Parameter(names = {"-ec", "--endpoint-config"}, hidden = true)
    private String endpointConfig;

    @Parameter(names = {"-t", "--truststore"}, hidden = true)
    private String trustStoreLocation;

    @Parameter(names = {"-w", "--truststore-pass"}, hidden = true)
    private String trustStorePassword;

    @Parameter(names = {"-c", "--config"}, hidden = true)
    private String toolkitConfigPath;

    @SuppressWarnings("unused")
    @Parameter(names = {"-d", "--deployment-config"}, hidden = true)
    private String deploymentConfigPath;

    @SuppressWarnings("unused")
    @Parameter(names = {"-a", "--api-name"}, hidden = true)
    private String apiName;

    @SuppressWarnings("unused")
    @Parameter(names = {"-v", "--version"}, hidden = true)
    private String version;

    @SuppressWarnings("unused")
    @Parameter(names = {"-f", "--force"}, hidden = true, arity = 0)
    private boolean isForcefully;

    @SuppressWarnings("unused")
    @Parameter(names = {"-k", "--insecure"}, hidden = true, arity = 0)
    private boolean isInsecure;

    @Parameter(names = {"-sec", "--security"}, hidden = true)
    private String security;

    @Parameter(names = {"-b", "--basepath"}, hidden = true)
    private String basepath;

    @Parameter(names = { "-etcd", "--enable-etcd" }, hidden = true, arity = 0)
    private boolean isEtcdEnabled;

    private String[] addCmdArgs;

    /**
     * This method is to support existing setup command
     * @param args commandLine arguments
     */
    void setArgsForAddCmd(String[] args) {
        String[] addCmdArgs = new String[args.length + 1];
        System.arraycopy(args, 1, addCmdArgs, 2, args.length - 1);
        addCmdArgs[0] = "add api";
        addCmdArgs[1] = "--project";
        this.addCmdArgs = addCmdArgs;
    }

    @Override
    public void execute() {
        String workspace = GatewayCmdUtils.getUserDir();
        String projectName = GatewayCmdUtils.getSingleArgument(mainArgs);
        if (projectName.contains(" ")) {
            throw GatewayCmdUtils.createUsageException("Only one argument accepted as the project name. but provided:" +
                    " " + projectName);
        }

        if (StringUtils.isEmpty(toolkitConfigPath)) {
            toolkitConfigPath = GatewayCmdUtils.getMainConfigLocation();
        }

        if (new File(workspace + File.separator + projectName).exists() && !isForcefully) {
            throw GatewayCmdUtils.createUsageException("Project name `" + projectName
                    + "` already exist. use -f or --force to forcefully update the project directory.");
        }
        if(new File(workspace + File.separator + projectName).exists()){
            GatewayCmdUtils.deleteProject(projectName);
        }
        // Extracts the zipped ballerina platform and runtime
        extractPlatformAndRuntime();
        init(projectName, deploymentConfigPath);

        //set etcd requirement
        Etcd etcd = new Etcd();
        etcd.setEtcdEnabled(isEtcdEnabled);
        //todo: needs to persist ?
        GatewayCmdUtils.setEtcd(etcd);
        LOGGER.debug("Etcd is enabled : " + isEtcdEnabled);

        if ((openApi != null) || (apiName != null) || (label != null)) {
            Main.main(addCmdArgs);
        }
        outStream.println("Setting up project " + projectName + " is successful.");
    }

    @Override
    public String getName() {
        return GatewayCliCommands.SETUP;
    }

    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
    }

    /**
     * Create project folder structure and initial deployment configuration
     * @param projectName projectName
     * @param deploymentConfigPath deploymentConfigPath
     */
    private static void init(String projectName, String deploymentConfigPath) {

        try {
            GatewayCmdUtils.createProjectStructure(projectName);
            GatewayCmdUtils.createDeploymentConfig(projectName, deploymentConfigPath);
        } catch (IOException e) {
            LOGGER.error("Error occurred while generating project configurations", e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        }
    }


    /**
     * Extracts the platform and runtime and copy related jars and balos to extracted runtime and platform.
     */
    private void extractPlatformAndRuntime() {
        try {
            String libPath = GatewayCmdUtils.getCLILibPath();
            String baloPath = GatewayCliConstants.CLI_GATEWAY + File.separator + GatewayCliConstants.CLI_BALO;
            String breLibPath = GatewayCliConstants.CLI_BRE + File.separator + GatewayCliConstants.CLI_LIB;
            String runtimeExtractedPath = libPath + File.separator + GatewayCliConstants.CLI_RUNTIME;
            String platformExtractedPath =
                    GatewayCmdUtils.getCLILibPath() + File.separator + GatewayCliConstants.CLI_PLATFORM;
            try {
                boolean isChangesDetected = LibHashUtils.detectChangesInLibraries();
                // Delete already extracted files if changes detected.
                if (isChangesDetected) {
                    Files.deleteIfExists(Paths.get(runtimeExtractedPath));
                    Files.deleteIfExists(Paths.get(platformExtractedPath));
                }
            } catch (HashingException e) {
                LOGGER.error("Error while detecting changes in gateway libraries", e);
            }
            if (!Files.exists(Paths.get(runtimeExtractedPath))) {
                ZipUtils.unzip(runtimeExtractedPath + GatewayCliConstants.EXTENSION_ZIP, runtimeExtractedPath, false);
                //copy balo to the runtime
                GatewayCmdUtils.copyFolder(libPath + File.separator + baloPath,
                        runtimeExtractedPath + File.separator + GatewayCliConstants.CLI_LIB + File.separator
                                + GatewayCliConstants.CLI_REPO);
                //copy gateway jars to runtime
                GatewayCmdUtils.copyFolder(libPath + File.separator + GatewayCliConstants.CLI_GATEWAY + File.separator
                        + GatewayCliConstants.CLI_RUNTIME, runtimeExtractedPath + File.separator + breLibPath);
            }

            if (!Files.exists(Paths.get(platformExtractedPath))) {
                ZipUtils.unzip(platformExtractedPath + GatewayCliConstants.EXTENSION_ZIP, platformExtractedPath, true);
                //copy balo to the platform
                GatewayCmdUtils.copyFolder(libPath + File.separator + baloPath,
                        platformExtractedPath + File.separator + GatewayCliConstants.CLI_LIB + File.separator
                                + GatewayCliConstants.CLI_REPO);
                //copy gateway jars to platform
                GatewayCmdUtils.copyFolder(libPath + File.separator + GatewayCliConstants.CLI_GATEWAY + File.separator
                        + GatewayCliConstants.CLI_PLATFORM, platformExtractedPath + File.separator + breLibPath);
            }

        } catch (IOException e) {
            String message = "Error while unzipping platform and runtime while project setup";
            LOGGER.error(message, e);
            throw new CLIInternalException(message);
        }
    }
}