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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.packerina.cmd.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerator;
import org.wso2.apimgt.gateway.cli.codegen.ThrottlePolicyGenerator;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.config.DockerConfig;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;
import org.wso2.apimgt.gateway.cli.utils.ToolkitLibExtractionUtils;
import org.wso2.callhome.CallHomeExecutor;
import org.wso2.callhome.utils.CallHomeInfo;
import org.wso2.callhome.utils.MessageFormatter;
import org.wso2.callhome.utils.Util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class represents the "build" command and it holds arguments and flags specified by the user.
 */
@Parameters(commandNames = "build", commandDescription = "build a project")
public class BuildCmd implements LauncherCmd {
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

    @SuppressWarnings("unused")
    @Parameter(names = "--docker")
    private boolean isDocker;

    @SuppressWarnings("unused")
    @Parameter(names = "--docker-image")
    private String dockerImage;

    @SuppressWarnings("unused")
    @Parameter(names = "--docker-base-image")
    private String dockerBaseImage;

    private CountDownLatch latch = new CountDownLatch(1);
    public void execute() {

        if (helpFlag) {
            String commandUsageInfo = getCommandUsageInfo("build");
            outStream.println(commandUsageInfo);
            return;
        }

        runCallHomeSeparateThread();
        String projectName = this.projectName.replaceAll("[/\\\\]", "");
        File projectLocation = new File(CmdUtils.getProjectDirectoryPath(projectName));
        try {
            String projectCanonicalPath = projectLocation.getCanonicalPath();
            String projectAbsolutePath = projectLocation.getAbsolutePath();
            if (!projectLocation.exists()) {
                throw new CLIRuntimeException("Project " + projectName + " does not exist.");
            }
            //extract the ballerina platform and runtime
            ToolkitLibExtractionUtils.extractPlatformAndRuntime();

            String importedAPIDefLocation = CmdUtils.getProjectGenAPIDefinitionPath(projectName);
            String addedAPIDefLocation = CmdUtils.getProjectAPIFilesDirectoryPath(projectName);
            String grpcProtoLocation = CmdUtils.getGrpcDefinitionsDirPath(projectName);
            boolean isImportedAPIsAvailable = isOpenAPIsAvailable(importedAPIDefLocation);
            boolean isAddedAPIsAvailable = isOpenAPIsAvailable(addedAPIDefLocation);
            boolean isProtoFilesAvailable = isProtosAvailable(grpcProtoLocation);

            if (!isImportedAPIsAvailable && !isAddedAPIsAvailable && !isProtoFilesAvailable) {
                throw new CLIRuntimeException("Nothing to build. API definitions/ Grpc Service definitions does " +
                        "not exist.");
            }
            // Some times user might run the command from different directory other than the directory where the project
            // exists. In those cases we need to ask the users to run the command in directory where project
            // directory exists.
            if (!projectAbsolutePath.equalsIgnoreCase(projectCanonicalPath)) {
                throw new CLIRuntimeException(
                        "Current directory: '" + CmdUtils.getUserDir() + "' should have a project with name: '"
                                + projectName
                                + "'. Execute the build command from the directory where the project is initialized");
            }
            String toolkitConfigPath = CmdUtils.getMainConfigLocation();
            init(projectName, toolkitConfigPath, deploymentConfigPath);
            outStream.print("Generating sources...");

            // Create policies directory
            String genPoliciesPath =
                    CmdUtils.getProjectTargetModulePath(projectName) + File.separator + CliConstants.GEN_POLICIES_DIR;
            CmdUtils.createDirectory(genPoliciesPath, false);

            // Generate policy definitions
            ThrottlePolicyGenerator policyGenerator = new ThrottlePolicyGenerator();
            policyGenerator.generate(genPoliciesPath, projectName);

            // Created resources directory
            String resourcesPath =
                    CmdUtils.getProjectTargetModulePath(projectName) + File.separator + CliConstants.RESOURCES_DIR;
            CmdUtils.copyFolder(CmdUtils.getAPIDefinitionPath(projectName), resourcesPath);
            CmdUtils.copyFolder(CmdUtils.getProjectGenAPIDefinitionPath(projectName), resourcesPath);
            // If resources folder contains .yaml file, replace the .yaml with .json file
            replaceYAMLFilesToJson(resourcesPath);

            // Copy static source files
            CmdUtils.copyAndReplaceFolder(CmdUtils.getProjectInterceptorsPath(projectName),
                    CmdUtils.getProjectTargetInterceptorsPath(projectName));
            new CodeGenerator().generate(projectName, true);
            outStream.print(CmdUtils.format("[DONE]\n"));
            //wait until call home thread finishes the task.
            latch.await(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            throw new CLIInternalException("Error occurred while generating source code for the open API definitions.",
                    e);
        }
    }

    private String convertYamlToJson(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    private void replaceYAMLFilesToJson(String resPath) throws IOException {
        String fileContent;
        String val = null;
        FileInputStream fileInputStream = null;
        FileWriter writer = null;
        File dir = new File(resPath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            try {
                for (File child : directoryListing) {
                    String ch = child.toString();
                    if (ch.endsWith("yaml")) {
                        fileInputStream = new FileInputStream(child);
                        byte[] value = new byte[(int) child.length()];
                        fileInputStream.read(value);
                        fileInputStream.close();
                        fileContent = new String(value, StandardCharsets.UTF_8);
                        val = convertYamlToJson(fileContent);
                        writer = new FileWriter(resPath + "/" + Math.random() + ".json");
                        writer.write(val);
                        writer.close();
                        child.delete();
                    }
                }
            } finally {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    private boolean isOpenAPIsAvailable(String fileLocation) {
        File file = new File(fileLocation);
        FilenameFilter filter = (f, name) -> (name.endsWith(".yaml") || name.endsWith(".json"));
        String[] fileNames = file.list(filter);

        return fileNames != null && fileNames.length > 0;
    }

    private boolean isProtosAvailable(String fileLocation) {
        File file = new File(fileLocation);
        if (!file.exists()) {
            return false;
        }
        FilenameFilter protoFilter = (f, name) -> (name.endsWith(".proto"));
        String[] fileNames = file.list(protoFilter);
        if (fileNames != null && fileNames.length > 0) {
            return true;
        }
        //allow the users to have proto definitions inside a directory if required
        FileFilter dirFilter = (f) -> f.isDirectory();
        File[] subDirectories = file.listFiles(dirFilter);
        for (File dir : subDirectories) {
            return isProtosAvailable(dir.getAbsolutePath());
        }
        return false;
    }

    @Override
    public String getName() {
        return CliCommands.BUILD;
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
                CmdUtils.setConfig(config);
            } else {
                logger.error("Configuration: {} Not found.", configPath);
                throw new CLIInternalException("Error occurred while loading configurations.");
            }
            if (deploymentConfig != null) {
                Path deploymentConfigFile = Paths.get(deploymentConfig);
                if (Files.exists(deploymentConfigFile)) {
                    CmdUtils.createDeploymentConfig(projectName, deploymentConfig);
                }
            }
            String deploymentConfigPath = CmdUtils.getDeploymentConfigLocation(projectName);
            ContainerConfig containerConfig = TOMLConfigParser.parse(deploymentConfigPath, ContainerConfig.class);
            createDockerImageFromCLI(projectName, containerConfig);
            CmdUtils.setContainerConfig(containerConfig);

            CodeGenerationContext codeGenerationContext = new CodeGenerationContext();
            codeGenerationContext.setProjectName(projectName);
            CmdUtils.setCodeGenerationContext(codeGenerationContext);

            initTarget(projectName);
        } catch (ConfigParserException e) {
            logger.error("Error occurred while parsing the configurations {}", configPath, e);
            throw new CLIInternalException("Error occurred while loading configurations.");
        } catch (IOException e) {
            throw new CLIInternalException("Error occurred while reading the deployment configuration", e);
        }
    }

    private void createDockerImageFromCLI(String projectName, ContainerConfig containerConfig) {
        if (isDocker || !StringUtils.isEmpty(dockerImage) || !StringUtils.isEmpty(dockerBaseImage)) {
            PrintStream outStream = System.out;
            if (StringUtils.isEmpty(dockerImage)) {
                dockerImage = CmdUtils.promptForTextInput(outStream, "Enter docker image name: ["
                        + projectName + ":" + CliConstants.DEFAULT_VERSION + "]").trim();
            }
            if (StringUtils.isEmpty(dockerBaseImage)) {
                dockerBaseImage = CmdUtils.promptForTextInput(outStream,
                        "Enter docker baseImage [" + CliConstants.DEFAULT_DOCKER_BASE_IMAGE + "]: ").trim();
            }
            if (StringUtils.isBlank(dockerImage)) {
                dockerImage = projectName + ":" + CliConstants.DEFAULT_VERSION;
            }
            if (StringUtils.isBlank(dockerBaseImage)) {
                dockerBaseImage = CliConstants.DEFAULT_DOCKER_BASE_IMAGE;
            }

            String[] dockerNameAndTag = dockerImage.split(":");
            String dockerName = dockerNameAndTag[0];
            String dockerTag = dockerNameAndTag[1];

            DockerConfig dockerConfig = containerConfig.getDocker().getDockerConfig();
            dockerConfig.setEnable(true);
            dockerConfig.setName(dockerName);
            dockerConfig.setTag(dockerTag);
            dockerConfig.setBaseImage(dockerBaseImage);
        }
    }

    /**
     * Initializes the project build target directory.
     *
     * @param projectName Name of the micro gateway project.
     * @throws IOException Error occurred while creating target directory structure.
     */
    private void initTarget(String projectName) throws IOException {
        String projectDir = CmdUtils.getProjectDirectoryPath(projectName);
        String targetDirPath = projectDir + File.separator + CliConstants.PROJECT_TARGET_DIR;
        CmdUtils.createDirectory(targetDirPath, true);

        String targetGenDir = targetDirPath + File.separator + CliConstants.PROJECT_GEN_DIR;
        CmdUtils.createDirectory(targetGenDir, true);

        CmdUtils.createDirectory(CmdUtils.getProjectTargetGenGrpcSrcDirectory(projectName), true);
        CmdUtils.createDirectory(CmdUtils.getProjectTargetGenGrpcSrcOpenAPIsDirectory(projectName), true);
        CmdUtils.createDirectory(CmdUtils.getProjectTargetGenGrpcSrcDescDirectory(projectName), true);

        //Initializing the ballerina project.
        CommandUtil.initProject(Paths.get(targetGenDir));
        String projectModuleDir = CmdUtils.getProjectTargetModulePath(projectName);
        CmdUtils.createDirectory(projectModuleDir, true);
    }

    /**
     * Invoke call home.
     *
     */
    private void invokeCallHome() {
        try {
            String productHome = CmdUtils.getCLIHome();
            String trustStoreLocation = CmdUtils.getCacertsLocation();
            String trustStorePassword = CmdUtils.getCacertsPassword();

            CallHomeInfo callhomeinfo = Util.createCallHomeInfo(productHome, trustStoreLocation, trustStorePassword);
            CallHomeExecutor.execute(callhomeinfo);

            String callHomeResponse = CallHomeExecutor.getMessage();
            String formattedMessage = MessageFormatter.formatMessage(callHomeResponse, 180);
            CmdUtils.setCallHomeMessage(formattedMessage);
            latch.countDown();
        } catch (Exception e) {
            // All the exceptions during call home should be caught in order to continue the toolkit build process.
            logger.error("Error while initialising call home functionality", e);
            latch.countDown();
        }
    }


    /**
     * This method will do the call home functionality in a separate thread.
     *
     */
    private void runCallHomeSeparateThread() {
        Thread callHomeThread = new Thread(() -> {
            invokeCallHome();
        });
        callHomeThread.setName("callHomeThread");
        callHomeThread.start();
    }
}
