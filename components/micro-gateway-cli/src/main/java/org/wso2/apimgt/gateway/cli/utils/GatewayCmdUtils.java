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
package org.wso2.apimgt.gateway.cli.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.cipher.AESCipherTool;
import org.wso2.apimgt.gateway.cli.cipher.AESCipherToolException;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.model.config.Etcd;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GatewayCmdUtils {

    private static final Logger logger = LoggerFactory.getLogger(GatewayCmdUtils.class);
    private static Config config;
    private static ContainerConfig containerConfig;
    private static CodeGenerationContext codeGenerationContext;
    private static Etcd etcd;
    private static final String algorithm = "AES";

    public static Etcd getEtcd() {
        return etcd;
    }

    public static void setEtcd(Etcd etcd) {
        GatewayCmdUtils.etcd = etcd;
    }

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config configFromFile) {
        config = configFromFile;
    }

    public static CodeGenerationContext getCodeGenerationContext() {
        return codeGenerationContext;
    }

    public static void setCodeGenerationContext(CodeGenerationContext codeGenerationContext) {
        GatewayCmdUtils.codeGenerationContext = codeGenerationContext;
    }

    /**
     * Read file as string
     *
     * @param path       to the file
     * @param inResource whether file is in resources directory of jar or not
     * @return file content
     * @throws IOException if file read went wrong
     */
    public static String readFileAsString(String path, boolean inResource) throws IOException {
        InputStream is;
        if (inResource) {
            path = getUnixPath(path);
            is = ClassLoader.getSystemResourceAsStream(path);
        } else {
            is = new FileInputStream(new File(path));
        }
        InputStreamReader inputStreamREader = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            inputStreamREader = new InputStreamReader(is, StandardCharsets.UTF_8);
            br = new BufferedReader(inputStreamREader);
            String content = br.readLine();
            if (content == null) {
                return sb.toString();
            }

            sb.append(content);

            while ((content = br.readLine()) != null) {
                sb.append('\n').append(content);
            }
        } finally {
            if (inputStreamREader != null) {
                try {
                    inputStreamREader.close();
                } catch (IOException ignore) {
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignore) {
                }
            }
        }
        return sb.toString();
    }

    /**
     * Create usage exception
     *
     * @param errorMsg error message
     * @return created usage exception
     */
    public static CliLauncherException createUsageException(String errorMsg) {
        CliLauncherException launcherException = new CliLauncherException();
        launcherException.addMessage("micro-gw: " + errorMsg);
        launcherException.addMessage("Run 'micro-gw help' for usage.");
        return launcherException;
    }

    /**
     * Convert first letter to lower case
     *
     * @param s string
     * @return first letter lower case string
     */
    public static String makeFirstLetterLowerCase(String s) {
        if (s == null) {
            return null;
        }
        char[] c = s.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

    /**
     * Encrypt given value with provided secret
     *
     * @param value  value to encrypt
     * @param secret encryption key
     * @return encrypted value
     */
    public static String encrypt(String value, String secret) {
        try {
            AESCipherTool cipherTool = new AESCipherTool(secret);
            return cipherTool.encrypt(value);
        } catch (AESCipherToolException e) {
            throw createUsageException("failed to encrypt client secret");
        }
    }

    /**
     * Decrypt given value with provided secret
     *
     * @param value  value to decrypt
     * @param secret decryption key
     * @return decrypted value
     */
    public static String decrypt(String value, String secret) {
        try {
            AESCipherTool cipherTool = new AESCipherTool(secret);
            return cipherTool.decrypt(value);
        } catch (AESCipherToolException e) {
            throw createUsageException("failed to decrypt client secret");
        }
    }

    /**
     * Returns current user dir
     *
     * @return current user dir
     */
    public static String getUserDir() {
        String currentDirProp = System.getProperty(GatewayCliConstants.SYS_PROP_CURRENT_DIR);
        if (currentDirProp != null) {
            return currentDirProp;
        } else {
            return System.getProperty(GatewayCliConstants.SYS_PROP_USER_DIR);
        }
    }

    /**
     * Get cli home location
     *
     * @return cli home location
     */
    public static String getCLIHome() {
        return System.getProperty(GatewayCliConstants.CLI_HOME);
    }

    /**
     * Get cli library location
     *
     * @return cli lib location
     */
    public static String getCLILibPath() {
        return getCLIHome() + File.separator + GatewayCliConstants.CLI_LIB;
    }

    /**
     * Get resources file directory path
     *
     * @return resources file directory path
     */
    public static String getResourceFolderLocation() {
        return System.getProperty(GatewayCliConstants.CLI_HOME) + File.separator
                + GatewayCliConstants.GW_DIST_RESOURCES;
    }

    /**
     * Get resources file directory path
     *
     * @return resources file directory path
     */
    public static String getLoggingPropertiesFileLocation() {
        return System.getProperty(GatewayCliConstants.CLI_HOME) + File.separator + GatewayCliConstants.CLI_CONF
                + File.separator + GatewayCliConstants.LOGGING_PROPERTIES_FILENAME;
    }

    /**
     * Get filters folder location
     *
     * @return filters folder location
     */
    public static String getDefinitionsLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_DEFINITIONS;
    }

    /**
     * Get filters folder location
     *
     * @return filters folder location
     */
    public static String getFiltersFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_FILTERS;
    }

    /**
     * Get policies folder location
     *
     * @return policies folder location
     */
    public static String getPoliciesFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_POLICIES;
    }

    /**
     * Get config folder location
     *
     * @return config folder location
     */
    private static String getConfigFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_CONF;
    }

    /**
     * Get temp folder location
     *
     * @param projectName name of the project
     * @return temp folder location
     */
    private static String getProjectTempFolderLocation(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.TEMP_DIR_NAME;
    }

    /**
     * Create a project structure for a particular project name.
     *
     * @param projectName name of the project
     */
    public static void createProjectStructure(String projectName) throws IOException {
        File projectDir = createFolderIfNotExist(getUserDir() + File.separator + projectName);

        String interceptorsPath = projectDir + File.separator + GatewayCliConstants.PROJECT_INTERCEPTORS_DIR;
        createFolderIfNotExist(interceptorsPath);

        String targetDirPath = projectDir + File.separator + GatewayCliConstants.PROJECT_TARGET_DIR;
        createFolderIfNotExist(targetDirPath);

        String confDirPath = projectDir + File.separator + GatewayCliConstants.PROJECT_CONF_DIR;
        createFolderIfNotExist(confDirPath);

        String definitionsPath = projectDir + File.separator + GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR;
        createFolderIfNotExist(definitionsPath);

        String genDirPath = projectDir + File.separator + GatewayCliConstants.PROJECT_GEN_DIR;
        createFolderIfNotExist(genDirPath);

        createFileIfNotExist(projectDir.getPath(), GatewayCliConstants.PROJECT_DEFINITION_FILE);
        createFileIfNotExist(projectDir.getPath(), GatewayCliConstants.PROJECT_POLICIES_FILE);

        String policyResPath = getDefinitionsLocation() + File.separator + GatewayCliConstants.GW_DIST_POLICIES_FILE;
        File policyResFile = new File(policyResPath);
        File policesFile = new File(projectDir + File.separator + GatewayCliConstants.PROJECT_POLICIES_FILE);

        if (Files.exists(policyResFile.toPath())) {
            FileUtils.copyFile(policyResFile, policesFile);
        } else {
            throw new CLIRuntimeException("Policy definition not found in CLI_HOME");
        }

        createGenDirectoryStructure(genDirPath);
    }

    /**
     * Save openAPI definition (developer first approach)
     *
     * @param projectName   project name
     * @param apiDefinition api Definition as String
     */
    public static void saveSwaggerDefinition(String projectName, String apiDefinition, String apiId) {
        if (apiDefinition.isEmpty()) {
            throw new CLIInternalException("No swagger definition is provided to generate API");
        }
        try {
            writeContent(apiDefinition, new File(getProjectGenSwaggerPath(projectName, apiId)));
        } catch (IOException e) {
            throw new CLIInternalException("Error while copying the swagger to the project directory");
        }
    }

    private static void saveSwaggerDefinitionForSingleAPI(String projectName, ExtendedAPI api) {
        String swaggerString = OpenAPICodegenUtils.generateSwaggerString(api);
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        GatewayCmdUtils.saveSwaggerDefinition(projectName, swaggerString, apiId);
    }

    /**
     * Save swagger definition for multiple APIs.
     *
     * @param projectName project name
     * @param apis        API object List
     */
    public static void saveSwaggerDefinitionForMultipleAPIs(String projectName, List<ExtendedAPI> apis) {
        for (ExtendedAPI api : apis) {
            saveSwaggerDefinitionForSingleAPI(projectName, api);
        }
    }

    /**
     * Create a micro gateway distribution for the provided project name
     *
     * @param projectName name of the project
     * @throws IOException error while creating micro gateway distribution
     */
    public static void createProjectGWDistribution(String projectName) throws IOException {
        createTargetGatewayDistStructure(projectName);

        String distPath = getTargetDistPath(projectName);
        String gwDistPath = getTargetGatewayDistPath(projectName);
        copyFolder(getCLIHome() + File.separator + GatewayCliConstants.CLI_LIB + File.separator
                + GatewayCliConstants.CLI_RUNTIME, gwDistPath + File.separator + GatewayCliConstants.GW_DIST_RUNTIME);
        copyTargetDistBinScripts(projectName);
        copyTargetDistBalx(projectName);

        //copy micro-gw.conf file to the distribution
        copyFilesToSources(
                GatewayCmdUtils.getConfigFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_CONF_FILE,
                gwDistPath + File.separator + GatewayCliConstants.GW_DIST_CONF + File.separator
                        + GatewayCliConstants.GW_DIST_CONF_FILE);

        String targetPath = getProjectTargetDirectoryPath(projectName);//target
        String zipFileName = GatewayCliConstants.GW_DIST_PREFIX + projectName + GatewayCliConstants.EXTENSION_ZIP;//micro-gw-projname.zip
        //creating an archive of the distribution
        ZipUtils.zip(distPath, targetPath + File.separator + zipFileName);

        // clean the target folder while keeping the distribution zip file
        cleanFolder(targetPath, ArrayUtils.add(GatewayCliConstants.PROJECTS_TARGET_DELETE_FILES,
                projectName + GatewayCliConstants.EXTENSION_BALX));
    }

    /**
     * Creates the distribution structure for the project name
     *
     * @param projectName name of the label
     */
    private static void createTargetGatewayDistStructure(String projectName) {
        //path : {projectName}/target
        String projectTargetPath = getProjectTargetDirectoryPath(projectName);
        createFolderIfNotExist(projectTargetPath);

        //path : {projectName}/target/distribution
        String distPath = getTargetDistPath(projectName);
        createFolderIfNotExist(distPath);

        //path : {projectName}/target/distribution/micro-gw-{projectName}
        String distMicroGWPath = getTargetGatewayDistPath(projectName);
        createFolderIfNotExist(distMicroGWPath);

        //path : {projectName}/target/distribution/micro-gw-{projectName}/bin
        String distBinPath = distMicroGWPath + File.separator + GatewayCliConstants.GW_DIST_BIN;
        createFolderIfNotExist(distBinPath);

        //path : {projectName}/target/distribution/micro-gw-{projectName}/conf
        String distConfPath = distMicroGWPath + File.separator + GatewayCliConstants.GW_DIST_CONF;
        createFolderIfNotExist(distConfPath);

        //path : {projectName}/target/distribution/micro-gw-{projectName}/logs
        String logsDirPath = distMicroGWPath + File.separator + GatewayCliConstants.PROJECT_LOGS_DIR;
        createFolderIfNotExist(logsDirPath);

        //path : {projectName}/target/distribution/micro-gw-{projectName}/logs/access_logs
        createFileIfNotExist(logsDirPath, GatewayCliConstants.ACCESS_LOG_FILE);

        //path : {projectName}/target/distribution/micro-gw-{projectName}/exec
        String distExec = distMicroGWPath + File.separator + GatewayCliConstants.GW_DIST_EXEC;
        createFolderIfNotExist(distExec);

        //path : {label}/target/distribution/micro-gw-{label}/api-usage-data
        String apiUsageDir = distMicroGWPath + File.separator + GatewayCliConstants.PROJECT_API_USAGE_DIR;
        createFolderIfNotExist(apiUsageDir);
    }

    /**
     * Load the stored resource hash content from the CLI temp folder
     *
     * @param projectName name of the project
     * @return stored resource hash content from the CLI temp folder
     * @throws IOException error while loading stored resource hash content
     */
    public static String loadStoredResourceHashes(String projectName) throws IOException {
        String resourceHashFileLocation = getResourceHashHolderFileLocation(projectName);
        String content = null;
        if (new File(resourceHashFileLocation).exists()) {
            content = GatewayCmdUtils.readFileAsString(resourceHashFileLocation, false);
        }
        return content;
    }

    /**
     * Saves the resource hash content to the CLI temp folder
     *
     * @param content     resource hash content
     * @param projectName name of the project
     * @throws IOException error while saving resource hash content
     */
    public static void storeResourceHashesFileContent(String content, String projectName) throws IOException {
        String tempDirPath = getProjectTempFolderLocation(projectName);
        createFolderIfNotExist(tempDirPath);

        String resourceHashesFileLocation = getResourceHashHolderFileLocation(projectName);
        File pathFile = new File(resourceHashesFileLocation);
        if (!pathFile.exists()) {
            boolean created = pathFile.createNewFile();
            if (created) {
                logger.trace("Hashed file: {} created. ", resourceHashesFileLocation);
            } else {
                logger.error("Failed to create hash file: {} ", resourceHashesFileLocation);
                throw new CLIInternalException("Error occurred while setting up the workspace structure");
            }
        }
        //Write Content
        writeContent(content, pathFile);
    }

    /**
     * Validate the list of main args and returns the first element.
     *
     * @param mainArgs List of main args provided to the command
     * @return first element
     */
    public static String getSingleArgument(List<String> mainArgs) {
        if (mainArgs.size() != 1) {
            throw new CLIRuntimeException("Only one argument accepted, "
                    + "but provided: " + String.join(",", mainArgs));
        } else {
            return mainArgs.get(0);
        }
    }

    /**
     * Get resource hash holder file path
     *
     * @param projectName name of the project
     * @return resource hash holder file path
     */
    private static String getResourceHashHolderFileLocation(String projectName) {
        return getProjectTempFolderLocation(projectName) + File.separator
                + GatewayCliConstants.RESOURCE_HASH_HOLDER_FILE_NAME;
    }


    /**
     * Get library zip files hash holder file path with in the CLI tool
     *
     * @return library zip hash holder file path of the CLI tool
     */
    public static String getCLILibHashHolderFileLocation() {
        return getCLIHome() + File.separator + GatewayCliConstants.TEMP_DIR_NAME + File.separator + GatewayCliConstants
                .LIB_HASH_HOLDER_FILE_NAME;
    }

    /**
     * Get the distribution path for a given project name
     *
     * @param projectName name of the project
     * @return distribution path for a given project name
     */
    private static String getTargetDistPath(String projectName) {
        return getProjectTargetDirectoryPath(projectName) + File.separator + GatewayCliConstants.GW_TARGET_DIST;
    }

    /**
     * Get the gateway distribution path for a given project name
     *
     * @param projectName name of the project
     * @return gateway distribution path for a given project name
     */
    private static String getTargetGatewayDistPath(String projectName) {
        return getTargetDistPath(projectName) + File.separator + GatewayCliConstants.GW_DIST_PREFIX + projectName;
    }

    /**
     * Copies shell scripts to the distribution location
     *
     * @param projectName name of the project
     * @throws IOException error while coping scripts
     */
    private static void copyTargetDistBinScripts(String projectName) throws IOException {
        String targetPath = getTargetGatewayDistPath(projectName);
        String binDir = targetPath + File.separator
                + GatewayCliConstants.GW_DIST_BIN + File.separator;

        String linuxShContent = readFileAsString(GatewayCliConstants.GW_DIST_SH_PATH, true);
        linuxShContent = linuxShContent.replace(GatewayCliConstants.LABEL_PLACEHOLDER, projectName);
        File shPathFile = new File(binDir + GatewayCliConstants.GW_DIST_SH);
        saveScript(linuxShContent, shPathFile);

        String winBatContent = readFileAsString(GatewayCliConstants.GW_DIST_BAT_PATH, true);
        winBatContent = winBatContent.replace(GatewayCliConstants.LABEL_PLACEHOLDER, projectName);
        File batPathFile = new File(binDir + GatewayCliConstants.GW_DIST_BAT);
        saveScript(winBatContent, batPathFile);
    }

    /**
     * Copies balx binaries to the distribution location
     *
     * @param projectName name of the project
     * @throws IOException error while coping balx files
     */
    private static void copyTargetDistBalx(String projectName) throws IOException {
        String projectGenTargetDirectoryPath = getProjectGenDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_TARGET_DIR;
        String gatewayDistExecPath =
                getTargetGatewayDistPath(projectName) + File.separator + GatewayCliConstants.GW_DIST_EXEC;
        File gatewayDistExecPathFile = new File(
                gatewayDistExecPath + File.separator + projectName + GatewayCliConstants.EXTENSION_BALX);
        File balxSourceFile = new File(
                projectGenTargetDirectoryPath + File.separator + projectName + GatewayCliConstants.EXTENSION_BALX);
        if (balxSourceFile.exists()) {
            FileUtils.copyFile(balxSourceFile, gatewayDistExecPathFile);
        } else {
            throw new CLIInternalException(projectName + ".balx could not be found in " + projectGenTargetDirectoryPath);
        }
    }

    /**
     * Returns location of the main configuration file of given project root
     *
     * @return path configuration file
     */
    public static String getMainConfigLocation() {
        return getCLIHome() + File.separator + GatewayCliConstants.GW_DIST_CONF + File.separator
                + GatewayCliConstants.MAIN_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the project conf folder
     *
     * @return path to the project conf folder
     */
    private static String getProjectConfigDirPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_CONF_DIR;
    }

    /**
     * Returns location of the deployment configuration file of given project
     *
     * @param projectName name of the project
     * @return path to deployment configuration file
     */
    public static String getDeploymentConfigLocation(String projectName) {
        return getProjectConfigDirPath(projectName) + File.separator + GatewayCliConstants.DEPLOYMENT_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the given project in the current working directory
     */
    public static String getProjectDirectoryPath(String projectName) {
        // TODO: do we need to change this?
        return new File(projectName).getAbsolutePath();
    }

    /**
     * Returns path to the /src of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.PROJECT_GEN_DIR;
    }

    /**
     * Returns path to the /gen/src of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenSrcDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.PROJECT_GEN_DIR + File.separator + GatewayCliConstants.GEN_SRC_DIR;
    }

    /**
     * Returns path to the /gen/api-definition of a given project in the current working directory
     *
     * @param projectName name of the project
     * @param apiId  md5 hash value of apiName:apiVersion
     *                    * @return path to the /gen/api-definition of a given project in the current working directory
     */
    public static String getProjectGenSwaggerPath(String projectName, String apiId) {
        return getProjectDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_GEN_DIR + File.separator +
                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR + File.separator + apiId
                + GatewayCliConstants.API_SWAGGER;
    }

    /**
     * Returns path to the /grpc_service/client of a given project in the current working directory
     *
     * @return path to the /grpc_service/client of a given project in the current working directory
     */
    public static String getProjectGrpcDirectoryPath() {
        return getUserDir() + File.separator
                + GatewayCliConstants.PROJECT_GRPC_SERVICE_DIR + File.separator +
                GatewayCliConstants.PROJECT_GRPC_CLIENT_DIR;
    }

    /**
     * Returns path to the /grpc_service of a given project in the current working directory
     *
     * @return path to the /grpc_service of a given project in the current working directory
     */
    public static String getProjectGrpcSoloDirectoryPath() {
        return getUserDir() + File.separator
                + GatewayCliConstants.PROJECT_GRPC_SERVICE_DIR;
    }

    /**
     * Returns path to the /target of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /target of a given project in the current working directory
     */
    private static String getProjectTargetDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.PROJECT_TARGET_DIR;
    }

    /**
     * Returns path to the /api-definitions of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /api-definitionsof a given project in the current working directory
     */
    public static String getProjectAPIDefinitionsDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_GEN_DIR + File.separator +
                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR;
    }

    /**
     * Returns the path to the routes configuration file (routes.yaml)
     *
     * @param projectName name of the project
     * @return path to the client-cert-metadata.yaml for a defined version of an API
     */
    public static String getProjectRoutesConfFilePath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_DEFINITION_FILE;
    }

    /**
     * This function recursively copy all the sub folder and files from source to destination file paths
     *
     * @param source      source location
     * @param destination destination location
     * @throws IOException error while copying folder to destination
     */
    public static void copyFolder(String source, String destination) throws IOException {
        File sourceFolder = new File(source);
        File destinationFolder = new File(destination);
        copyFolder(sourceFolder, destinationFolder);
    }

    /**
     * Copy files to resources directory
     *
     * @param sourcePath      source directory path
     * @param destinationPath destionation directory path
     * @throws IOException if file copy went wrong
     */
    public static void copyFilesToSources(String sourcePath, String destinationPath) throws IOException {
        Files.copy(Paths.get(sourcePath), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Cleans the given folder by deleting a set of specified files
     *
     * @param targetPath path of folder to clean
     * @param delete     files to delete
     * @throws IOException error while cleaning the folder
     */
    private static void cleanFolder(String targetPath, String... delete) throws IOException {
        File targetFolder = new File(targetPath);
        logger.debug("Cleaning the target folder: {}", targetPath);
        if (!targetFolder.isDirectory()) {
            logger.warn("Nothing to delete. Target folder {} is not a directory.", targetPath);
            return;
        }
        //Get all files from source directory
        String[] files = targetFolder.list();
        if (files == null) {
            logger.warn("Nothing to delete. Target folder {} is empty.", targetPath);
            return;
        }
        for (String file : files) {
            for (String deleteFile : delete) {
                if (!file.equals(deleteFile)) {
                    logger.trace("Keeping file: {}", file);
                    continue;
                }
                logger.trace("Deleting file: {}", file);
                File fileToDelete = new File(targetFolder, file);
                boolean success;
                if (fileToDelete.isDirectory()) {
                    FileUtils.deleteDirectory(fileToDelete);
                    success = !fileToDelete.exists();
                } else {
                    success = fileToDelete.delete();
                }
                if (success) {
                    logger.trace("Deleting file {} is successful.", file);
                } else {
                    logger.trace("Deleting file {} failed.", file);
                }
            }
        }
        logger.debug("Cleaning the target folder {} complete.", targetPath);
    }

    /**
     * This function recursively copy all the sub folder and files from sourceFolder to destinationFolder
     *
     * @param sourceFolder      source location
     * @param destinationFolder destination location
     * @throws IOException error while copying folder to destination
     */
    private static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
        //Check if sourceFolder is a directory or file
        //If sourceFolder is file; then copy the file directly to new location
        if (sourceFolder.isDirectory()) {
            //Verify if destinationFolder is already present; If not then create it
            if (!destinationFolder.exists()) {
                boolean created = destinationFolder.mkdir();
                if (created) {
                    logger.trace("Directory: {} created. ", destinationFolder.getAbsolutePath());
                } else {
                    logger.error("Failed to create directory: {} ", destinationFolder.getAbsolutePath());
                    throw new CLIInternalException("Error occurred while setting up the workspace structure");
                }
            }

            //Get all files from source directory
            String[] files = sourceFolder.list();

            if (files != null) {
                //Iterate over all files and copy them to destinationFolder one by one
                for (String file : files) {
                    File srcFile = new File(sourceFolder, file);
                    File destFile = new File(destinationFolder, file);

                    //Recursive function call
                    copyFolder(srcFile, destFile);
                }
            }
        } else {
            //Copy the file content from one place to another
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            //Make it writable, so that next time can clear the files without a permission issue
            boolean success = destinationFolder.setWritable(true);
            if (!success) {
                logger.debug("Setting write permission failed for {}", destinationFolder.getAbsolutePath());
            }
        }
    }

    /**
     * Creates a new folder if not exists
     *
     * @param path folder path
     * @return File object for the created folder
     */
    private static File createFolderIfNotExist(String path) {
        File folder = new File(path);
        if (!folder.exists() && !folder.isDirectory()) {
            boolean created = folder.mkdir();
            if (created) {
                logger.trace("Directory: {} created. ", path);
            } else {
                logger.error("Failed to create directory: {} ", path);
                throw new CLIInternalException("Error occurred while setting up the workspace structure");
            }
        }
        return folder;
    }

    /**
     * Write content to a specified file
     *
     * @param content content to be written
     * @param file    file object initialized with path
     * @throws IOException error while writing content to file
     */
    private static void writeContent(String content, File file) throws IOException {
        FileWriter writer = null;
        writer = new FileWriter(file);
        writer.write(content);
        writer.flush();
    }

    /**
     * Creates file if not exist
     *
     * @param path     folder path
     * @param fileName name of the file
     */
    private static void createFileIfNotExist(String path, String fileName) {
        String filePath = path + File.separator + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (created) {
                    logger.trace("File: {} created. ", path);
                } else {
                    logger.error("Failed to create file: {} ", path);
                    throw new CLIInternalException("Error occurred while setting up the workspace structure");
                }
            } catch (IOException e) {
                logger.error("Failed to create file: {} ", path, e);
                throw new CLIInternalException("Error occurred while setting up the workspace structure");
            }
        }
    }

    /**
     * Create initial deployment configuration file. If external file is provided using 'deploymentConfPath',
     * it will be taken as the deployment configuration. Otherwise, a default configuration will be copied.
     *
     * @param projectName        project name
     * @param deploymentConfPath path to deployment config
     * @throws IOException if file create went wrong
     */
    public static void createDeploymentConfig(String projectName, String deploymentConfPath)
            throws IOException {

        String depConfig = getProjectConfigDirPath(projectName) + File.separator
                + GatewayCliConstants.DEPLOYMENT_CONFIG_FILE_NAME;
        File file = new File(depConfig);

        if (deploymentConfPath == null) {
            if (!file.exists()) {
                String defaultConfig;
                boolean created = file.createNewFile();
                if (created) {
                    logger.debug("Deployment configuration file: {} created.", depConfig);
                } else {
                    throw new CLIInternalException("Failed to create the deployment configuration file: " + depConfig);
                }
                //Write Content
                defaultConfig = readFileAsString(GatewayCliConstants.DEFAULT_DEPLOYMENT_CONFIG_FILE_NAME, true);
                writeContent(defaultConfig, file);
            }
        } else {
            File inputDepConfFile = new File(deploymentConfPath);
            if (inputDepConfFile.exists()) {
                String inputConfigContent = readFileAsString(deploymentConfPath, false);
                //validate the provided file
                try {
                    TOMLConfigParser.parse(deploymentConfPath, ContainerConfig.class);
                } catch (ConfigParserException | IllegalStateException e) {
                    throw new CLIRuntimeException(
                            "Error while reading deployment configuration file: " + deploymentConfPath
                                    + ". The content is invalid.", e);
                }
                writeContent(inputConfigContent, file);
            } else {
                throw new CLIRuntimeException(
                        "Error while reading deployment configuration file. Probably the file path '" + deploymentConfPath
                                + "' is invalid.");
            }
        }
    }

    public static ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public static void setContainerConfig(ContainerConfig containerConfig) {
        GatewayCmdUtils.containerConfig = containerConfig;
    }

    public static void saveConfig(Config config, String configPath) {
        try {
            TOMLConfigParser.write(configPath, config);
        } catch (ConfigParserException e) {
            System.err.println("Error occurred while parsing configuration, when persisting.");
        }
    }

    public static APICorsConfigurationDTO getDefaultCorsConfig() {
        APICorsConfigurationDTO corsConfigurationDTO = new APICorsConfigurationDTO();
        corsConfigurationDTO.setCorsConfigurationEnabled(true);
        corsConfigurationDTO.setAccessControlAllowOrigins(GatewayCliConstants.accessControlAllowOrigins);
        corsConfigurationDTO.setAccessControlAllowMethods(GatewayCliConstants.accessControlAllowMethods);
        corsConfigurationDTO.setAccessControlAllowHeaders(GatewayCliConstants.accessControlAllowHeaders);
        corsConfigurationDTO.setAccessControlAllowCredentials(GatewayCliConstants.accessControlAllowCredentials);
        return corsConfigurationDTO;
    }

    /**
     * Replace backslashes `\` in windows path string with forward slashes `/`
     *
     * @param path Location of a resource (file or directory)
     * @return {String} File path with unix style file separator
     */
    public static String getUnixPath(String path) {
        return path.replace(File.separator, "/");
    }

    /**
     * Create script file in the given path with the given content
     *
     * @param content Content needs to be added to the script file
     * @param path    File object containing the path to save the script
     */
    private static void saveScript(String content, File path) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
            boolean success = path.setExecutable(true);
            if (success) {
                logger.trace("File: {} is set to executable. ", path.getAbsolutePath());
            } else {
                logger.error("Failed to set executable file: {} ", path.getAbsolutePath());
                throw new CLIInternalException("Error occurred while setting up the workspace structure");
            }
        }
    }

    /**
     * Delete project folder
     *
     * @param projectPath project path
     */
    public static void deleteProject(String projectPath) {
        File file = new File(projectPath);
        try {
            // Deleting the directory recursively.
            delete(file);
        } catch (IOException e) {
            // not throwing the error because deleting faild project is not
            // a critical task. This can be deleted manually if not used.
            logger.error("Failed to delete project : {} ", projectPath, e);
        }
    }

    private static void delete(File file) throws IOException {
        for (File childFile : file.listFiles()) {
            if (childFile.isDirectory()) {
                delete(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }
        if (!file.delete()) {
            throw new IOException();
        }
    }

    /**
     * Writes the map after serializing  to given path
     *
     * @param map resource hash content
     *            * @param filePath file path the map should be written to
     * @throws IOException error while saving resource hash content
     */
    public static void writeMapToFile(Map<String, String> map, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath); ObjectOutputStream obs = new
                ObjectOutputStream(fos)) {
            obs.writeObject(map);
        }
    }

    /**
     * Read the deserialize file content to map.
     *
     * @param filePath file path the map should be written to
     * @throws IOException error while saving resource hash content
     */
    public static Map<String, String> readFileToMap(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath); ObjectInputStream obi = new ObjectInputStream(fis)) {
            return (Map<String, String>) obi.readObject();
        }
    }

    /**
     * Prompts for a test input.
     *
     * @param outStream Print Stream
     * @param msg       message
     * @return user entered text
     */
    public static String promptForTextInput(PrintStream outStream, String msg) {
        outStream.println(msg);
        return System.console().readLine();
    }

    /**
     * Delete the subfolder containing metadata and swagger File
     *
     * @param projectName project name
     * @param apiId       API Id
     * @throws IOException If the directory does not exist
     */
    public static void deletePerAPIFolder(String projectName, String apiId) {
        try {
            delete(new File(GatewayCmdUtils.getProjectAPIFilesDirectoryPath(projectName) + File.separator +
                    apiId));
        } catch (IOException e) {
            throw new CLIInternalException("Delete folder is failed : " + apiId);
        }
    }

    /**
     * Read the project descriptor file at a given path.
     * <p>
     * This will only read the first line of the file. We are not interested
     * in the rest of the content in the file.
     * </p>
     *
     * @param filePath Path to project descriptor file
     * @return First line of the file at {@code filePath}
     * @throws IOException when failed to read file at {@code filePath}
     */
    private static String readGatewayProjectFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String projectPath = reader.readLine();

        return projectPath;
    }

    /**
     * Find the working project directory set in the {@link GatewayCliConstants#PROJECT_FILE_NAME}
     * of MGW_HOME.
     *
     * @return path to currently working project. {@code null} if unable to read project descriptor
     * or invalid project dir is detected
     */
    public static String findCurrentProject() {
        String projectFile = GatewayCmdUtils.getCLIHome() + File.separator + GatewayCliConstants.PROJECT_FILE_NAME;
        String fileContent = null;
        try {
            fileContent = readGatewayProjectFile(projectFile);
        } catch (IOException e) {
            // Ignore the exception, which will result in null as return value
        }

        if (fileContent != null && Files.isDirectory(Paths.get(fileContent))) {
            return fileContent.trim();
        } else {
            return null;
        }
    }

    /**
     * Build the correct current project directory. If {@code knownName} is a valid project name
     * it'll take the priority as the project name. If not {@link GatewayCliConstants#PROJECT_FILE_NAME}
     * file value will be taken as the project path. If both are no available error will be thrown.
     *
     * @param knownName known project name to prioritize
     * @return valid project name
     */
    public static String buildProjectName(String knownName) {
        String projectName = knownName;

        if (StringUtils.isEmpty(knownName)) {
            String workingProject = GatewayCmdUtils.findCurrentProject();

            if (StringUtils.isEmpty(workingProject)) {
                throw new CLIRuntimeException("Project name is not provided.");
            } else {
                projectName = workingProject;
                logger.debug("Working project was set from config: " + projectName);
            }
        }

        return projectName;
    }

    /**
     * Create directory structure for projects /gen directory.
     *
     * @param genDirPath path to project's /gen directory
     */
    private static void createGenDirectoryStructure(String genDirPath) {
        String genDefinitionsPath = genDirPath + File.separator + GatewayCliConstants.GEN_API_DEFINITIONS_DIR;
        createFolderIfNotExist(genDefinitionsPath);

        String genSrcPath = genDirPath + File.separator + GatewayCliConstants.GEN_SRC_DIR;
        createFolderIfNotExist(genSrcPath);

        String genPoliciesPath = genSrcPath + File.separator + GatewayCliConstants.GEN_POLICIES_DIR;
        createFolderIfNotExist(genPoliciesPath);
    }

    /**
     * Returns the path to the metadata for a defined version of an API
     * @param projectName name of the project
     * @param apiId md5 hash value of apiName:apiVersion
     * @return path to the metadata file of the API
     */
    public static String getAPIMetadataFilePath(String projectName, String apiId){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator + apiId + File.separator +
                GatewayCliConstants.API_METADATA_FILE;
    }

    /**
     * Returns path to the /API-Files of a given project in the current working directory
     * @param projectName name of the project
     * @return path to the /API-Files of a given project in the current working directory
     */
    public static String getProjectAPIFilesDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR;
    }

    public static void saveAPIMetadataFile(String projectName, String apiId, String apiMetadataYaml){
        if(!apiMetadataYaml.isEmpty()){
            try {
                writeContent(apiMetadataYaml, new File(GatewayCmdUtils.getAPIMetadataFilePath(projectName, apiId)));
            } catch (IOException e) {
                throw new CLIInternalException("Error while copying api-metaData to the project directory");
            }
        }
    }

    /**
     * Returns the path to the application-throttle-policies.yaml for for a defined version of an API
     * @param projectName name of the project
     * @return path to the application-throttle-policies.yaml for for a defined version of an API
     */
    public static String getProjectAppThrottlePoliciesFilePath(String projectName){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.APPLICATION_THROTTLE_POLICIES_FILE;
    }

    /**
     * Returns the path to the application-throttle-policies.yaml file for a defined version of an API
     * @param projectName name of the project
     * @return path to the application-throttle-policies.yaml file for a defined version of an API
     */
    public static String getProjectSubscriptionThrottlePoliciesFilePath(String projectName){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.SUBSCRIPTION_THROTTLE_POLICIES_FILE;
    }

    /**
     * Returns the path to the client-cert-metadata.yaml for a defined version of an API
     * @param projectName name of the project
     * @return path to the client-cert-metadata.yaml for a defined version of an API
     */
    public static String getProjectClientCertMetadataFilePath(String projectName){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.CLIENT_CERT_METADATA_FILE;
    }
    /**
     * Create API-Files Directory for a particular project
     * @param projectName name of the project
     * @param apiId md5 hash value for apiName:version
     */
    public static void createPerAPIFolderStructure(String projectName, String apiId){

        String apiFilesDirPath = getProjectAPIFilesDirectoryPath(projectName);
        String apiDirPath = apiFilesDirPath + File.separator + apiId;
        createFolderIfNotExist(apiDirPath);
    }

    /**
     * Returns the path to the swagger for a defined version of an API
     * @param projectName name of the project
     * @param apiId md5 hash value of apiName:apiVersion
     * @return path to the swagger for a defined version of an API
     */
    public static String getProjectSwaggerFilePath(String projectName, String apiId){
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator + apiId + File.separator +
                GatewayCliConstants.API_SWAGGER;
    }


}
