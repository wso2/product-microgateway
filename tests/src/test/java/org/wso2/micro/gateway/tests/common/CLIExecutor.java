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
package org.wso2.micro.gateway.tests.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.micro.gateway.tests.context.Constants;
import org.wso2.micro.gateway.tests.context.MicroGWTestException;
import org.wso2.micro.gateway.tests.context.ServerLogReader;
import org.wso2.micro.gateway.tests.context.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Execute APIM CLI functions.
 */
public class CLIExecutor {
    private static final Logger log = LoggerFactory.getLogger(CLIExecutor.class);
    private String homeDirectory;
    private String cliHome;
    private static CLIExecutor instance;

    /**
     * Generate Microgateway project using import Cmd.
     *
     * @param label   label
     * @param project project name
     * @throws MicroGWTestException
     */
    public void generate(String label, String project) throws MicroGWTestException {
        createBackgroundEnv();
        //This config file is relevant to the publisher API
        String config = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("confs" +
                "/default-cli-test-config.toml")).getPath()).getAbsolutePath();
        String mgwCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_BIN + File.separator + "micro-gw";
        runInitCmd(mgwCommand, project);
        //todo: check using prompt as well
        String[] importCmdArray = {"-l", label, "-u", "admin", "-p", "admin", "-s", "https://localhost:9443",
                "--truststore", "lib/platform/bre/security/ballerinaTruststore.p12",
                "--truststore-pass", "ballerina"};
        runImportCmd(mgwCommand, project, importCmdArray);
        copyCustomizedPolicyFileFromResources(project);
        runBuildCmd(mgwCommand, project);
    }

    /**
     * Run the process.
     *
     * @param cmdArray      array containing all the commandline arguments
     * @param homeDirectory home directory for the process
     * @param errorMessage  error message needs to be printed if any error is occurred
     * @throws MicroGWTestException
     */
    private void runProcess(String[] cmdArray, String homeDirectory, String errorMessage) throws MicroGWTestException {
        try {
            Process process = Runtime.getRuntime().exec(cmdArray, new String[] {"MICROGW_HOME=" + cliHome, "JAVA_HOME="
                    + System.getenv("JAVA_HOME")}, new File(homeDirectory));
            new ServerLogReader("errorStream", process.getErrorStream()).start();
            new ServerLogReader("inputStream", process.getInputStream()).start();
            boolean isCompleted = process.waitFor(2, TimeUnit.MINUTES);
            if (!isCompleted) {
                throw new RuntimeException(errorMessage);
            }
            int processExitCode = process.exitValue();
            if (processExitCode != 0) {
                throw new RuntimeException(errorMessage);
            }
        } catch (IOException | InterruptedException e) {
            throw new MicroGWTestException(errorMessage, e);
        }
    }

    /**
     * Generate the project using developer first approach (Using OpenAPI definitions).
     *
     * @param project          project name
     * @param openAPIFileNames relative paths of openAPI definitions stored in resources directory.
     * @throws MicroGWTestException
     */
    public void generateFromDefinition(String project, String[] openAPIFileNames)
            throws MicroGWTestException {

        createBackgroundEnv();
        String mgwCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_BIN + File.separator + "micro-gw";
        runInitCmd(mgwCommand, project);
        copyOpenAPIDefinitionsToProject(project, openAPIFileNames);
        copyCustomizedPolicyFileFromResources(project);
        runBuildCmd(mgwCommand, project);
    }

    private void createBackgroundEnv() throws MicroGWTestException {
        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = null;
        try {
            path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject");
        } catch (IOException e) {
            throw new MicroGWTestException("The directory " + baseDir + " doesnot exist.", e);
        }
        log.info("CLI Project Home: " + path.toString());
        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);
        System.setProperty("user.dir", path.toString());
        homeDirectory = path.toString();
    }

    /**
     * Initialize the project.
     *
     * @param mgwCommand the path of microgateway executable
     * @param project    project name
     * @throws MicroGWTestException
     */
    private void runInitCmd(String mgwCommand, String project) throws MicroGWTestException {
        String[] initCmdArray = generateBasicCmdArgsBasedOnOS(mgwCommand, "init", project);
        String initErrorMsg = "Error occurred during initializing the project.";
        runProcess(initCmdArray, homeDirectory, initErrorMsg);
    }

    /**
     * Import apis to the project from API Manager publisher.
     *
     * @param mgwCommand the path of microgateway executable
     * @param project    project name
     * @param cmdArgs    commandline arguments that needs to be provided by the user.
     * @throws MicroGWTestException
     */
    private void runImportCmd(String mgwCommand, String project, String[] cmdArgs) throws MicroGWTestException {
        String[] importCmdArgs = generateBasicCmdArgsBasedOnOS(mgwCommand, "import", project);
        importCmdArgs = ArrayUtils.addAll(importCmdArgs, cmdArgs);
        String importErrorMsg = "Error occurred during importing the api.";
        runProcess(importCmdArgs, homeDirectory, importErrorMsg);
    }

    private String[] generateBasicCmdArgsBasedOnOS(String mgwCommand, String mainCommand, String project) {
        if (Utils.getOSName().toLowerCase().contains("windows")) {
            return new String[]{"cmd.exe", "/c", mgwCommand.trim() + ".bat", mainCommand, project};
        }
        return new String[]{"bash", mgwCommand, mainCommand, project};
    }

    /**
     * Build the project.
     *
     * @param mgwCommand the path of microgateway executable
     * @param project    project name
     * @throws MicroGWTestException
     */
    private void runBuildCmd(String mgwCommand, String project) throws MicroGWTestException {
        String[] buildCmdArray = generateBasicCmdArgsBasedOnOS(mgwCommand, "build", project);
        String buildErrorMsg = "Error occurred when building the project.";
        runProcess(buildCmdArray, homeDirectory, buildErrorMsg);
    }

    /**
     * Copy the customized policies definition for testing purposes.
     *
     * @param project project name
     * @throws MicroGWTestException
     */
    private void copyCustomizedPolicyFileFromResources(String project) throws MicroGWTestException {
        String projectDir = homeDirectory + File.separator + project + File.separator;
        File policyYamlResource = new File(getClass().getClassLoader().getResource("policies.yaml").getPath());
        File policyYamlFile = new File(projectDir + "/policies.yaml");
        try {
            FileUtils.copyFile(policyYamlResource, policyYamlFile);
        } catch (IOException e) {
            throw new MicroGWTestException("Error while copying policies.yaml.", e);
        }
    }

    /**
     * Copy the openAPI definitions' relative paths (compared to resources directory) to project directory.
     *
     * @param project          project name
     * @param openAPIFileNames array of openAPI definitions' relative paths stored in the resources directory
     * @throws MicroGWTestException
     */
    private void copyOpenAPIDefinitionsToProject(String project, String[] openAPIFileNames)
            throws MicroGWTestException {
        for (String openAPIFileName : openAPIFileNames) {
            File swaggerSrcPath = new File(
                    getClass().getClassLoader().getResource(Constants.OPEN_APIS + "/" +
                            openAPIFileName).getPath());
            File desPath;
            if (openAPIFileName.contains(".bal")) {
                desPath = new File(homeDirectory + File.separator + project + File.separator +
                        GatewayCliConstants.PROJECT_INTERCEPTORS_DIR + File.separator +
                        openAPIFileName.substring(openAPIFileName.lastIndexOf("/") + 1));
            } else {
                desPath = new File(
                        homeDirectory + File.separator + project + File.separator +
                                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR + File.separator + openAPIFileName
                                .substring(openAPIFileName.lastIndexOf("/") + 1));
            }
            try {
                FileUtils.copyFile(swaggerSrcPath, desPath);
            } catch (IOException e) {
                throw new MicroGWTestException("Error while copying the file from " + swaggerSrcPath +
                        " to " + desPath + ".", e);
            }
        }
    }

    private CLIExecutor() {
        cliHome = System.getProperty(Constants.SYSTEM_PROP_TOOLKIT);
    }

    public static CLIExecutor getInstance() {
        if (instance == null) {
            instance = new CLIExecutor();
        }
        return instance;
    }

    /**
     * Get the absolute path for the compiled ballerina project executable (balx file).
     *
     * @param project project name
     * @return the absolute path of the project's executable
     */
    public String getLabelBalx(String project) {
        return homeDirectory + File.separator + project + File.separator + "target" + File.separator + project +
                ".balx";
    }
}