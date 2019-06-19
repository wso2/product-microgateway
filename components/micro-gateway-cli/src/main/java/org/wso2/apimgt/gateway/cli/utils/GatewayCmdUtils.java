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
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class GatewayCmdUtils {

    private static final Logger logger = LoggerFactory.getLogger(GatewayCmdUtils.class);
    private static Config config;
    private static ContainerConfig containerConfig;
    private static CodeGenerationContext codeGenerationContext;
    private static boolean verboseLogsEnabled = setVerboseEnabled();
    private static final String openAPISpec2 = "2";

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
        launcherException.addMessage("Run 'micro-gw' for usage.");
        return launcherException;
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

        String extensionsPath = projectDir + File.separator + GatewayCliConstants.PROJECT_EXTENSIONS_DIR;
        createFolderIfNotExist(extensionsPath);

        String targetDirPath = projectDir + File.separator + GatewayCliConstants.PROJECT_TARGET_DIR;
        createFolderIfNotExist(targetDirPath);

        String targetGenDirPath = projectDir + File.separator + GatewayCliConstants.PROJECT_TARGET_DIR + File.separator
                + GatewayCliConstants.PROJECT_GEN_DIR;
        createFolderIfNotExist(targetGenDirPath);

        String confDirPath = projectDir + File.separator + GatewayCliConstants.PROJECT_CONF_DIR;
        createFolderIfNotExist(confDirPath);

        String definitionsPath = projectDir + File.separator + GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR;
        createFolderIfNotExist(definitionsPath);

        String projectServicesDirectory = projectDir + File.separator + GatewayCliConstants.PROJECT_SERVICES_DIR;
        String resourceServicesDirectory =
                getResourceFolderLocation() + File.separator + GatewayCliConstants.PROJECT_SERVICES_DIR;
        copyFolder(resourceServicesDirectory, projectServicesDirectory);

        createFileIfNotExist(projectDir.getPath(), GatewayCliConstants.PROJECT_POLICIES_FILE);

        String policyResPath = getDefinitionsLocation() + File.separator + GatewayCliConstants.GW_DIST_POLICIES_FILE;
        File policyResFile = new File(policyResPath);
        File policesFile = new File(projectDir + File.separator + GatewayCliConstants.PROJECT_POLICIES_FILE);

        String extensionResPath = getFiltersFolderLocation() + File.separator +
                GatewayCliConstants.GW_DIST_EXTENSION_FILTER;
        File extensionResFile = new File(extensionResPath);
        File extensionFile = new File(extensionsPath + File.separator +
                GatewayCliConstants.GW_DIST_EXTENSION_FILTER);

        String tokenRevocationResPath = getFiltersFolderLocation() + File.separator +
                GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION;
        File tokenRevocationResFile = new File(tokenRevocationResPath);
        File tokenRevocationFile = new File(extensionsPath + File.separator +
                GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);

        String startUpExtensionResourcePath = getFiltersFolderLocation() + File.separator +
                GatewayCliConstants.GW_DIST_START_UP_EXTENSION;
        File startUpExtensionResFile = new File(startUpExtensionResourcePath);
        File startUpExtensionFile = new File(extensionsPath + File.separator +
                GatewayCliConstants.GW_DIST_START_UP_EXTENSION);

        if (Files.exists(extensionResFile.toPath())) {
            FileUtils.copyFile(extensionResFile, extensionFile);
        } else {
            throw new CLIRuntimeException("Extension filter not found in CLI_HOME");
        }
        if (Files.exists(tokenRevocationResFile.toPath())) {
            FileUtils.copyFile(tokenRevocationResFile, tokenRevocationFile);
        } else {
            throw new CLIRuntimeException("Token revocation extension not Found in CLI_HOME");
        }
        if (Files.exists(startUpExtensionResFile.toPath())) {
            FileUtils.copyFile(startUpExtensionResFile, startUpExtensionFile);
        } else {
            throw new CLIRuntimeException("Start up extension not Found in CLI_HOME");
        }
        if (Files.exists(policyResFile.toPath())) {
            FileUtils.copyFile(policyResFile, policesFile);
        } else {
            throw new CLIRuntimeException("Policy definition not found in CLI_HOME");
        }

    }

    /**
     * Save openAPI definition (developer first approach)
     *
     * @param projectName   project name
     * @param apiDefinition api Definition as String
     */
    public static void saveSwaggerDefinition(String projectName, String apiDefinition, String apiId, String extension) {
        if (apiDefinition.isEmpty()) {
            throw new CLIInternalException("No swagger definition is provided to generate API");
        }
        try {
            Path genPath = Paths.get(GatewayCmdUtils.getProjectGenDirectoryPath(projectName));
            Path apiDefPath = Paths.get(GatewayCmdUtils.getProjectGenAPIDefinitionPath(projectName));
            if(Files.notExists(genPath)){
                Files.createDirectory(genPath);
                Files.createDirectory(apiDefPath);
            }
            writeContent(apiDefinition, new File(getProjectGenSwaggerPath(projectName, apiId, extension)));
        } catch (IOException e) {
            throw new CLIInternalException("Error while copying the swagger to the project directory");
        }
    }

    private static void saveSwaggerDefinitionForSingleAPI(String projectName, ExtendedAPI api) {
        String swaggerString = OpenAPICodegenUtils.generateSwaggerString(api);
        String apiId = HashUtils.generateAPIId(api.getName(), api.getVersion());
        String extension = openAPISpec2.equals(OpenAPICodegenUtils.findSwaggerVersion(api.getApiDefinition(), false))
                ? GatewayCliConstants.API_SWAGGER: GatewayCliConstants.API_OPENAPI_YAML;
        GatewayCmdUtils.saveSwaggerDefinition(projectName, swaggerString, apiId, extension);
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
            System.out.println("ID for API with name " + api.getName() +  " : " + HashUtils.generateAPIId(api.getName(), api.getVersion()));
        }
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
        return getUserDir() + File.separator + projectName;
    }

    /**
     * Returns path to the /gen of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.PROJECT_GEN_DIR;
    }

    /**
     * Returns path to the /target/gen of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectTargetGenDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_TARGET_DIR
                + File.separator + GatewayCliConstants.PROJECT_GEN_DIR;
    }

    /**
     * Returns path to the /gen/src of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenSrcDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_TARGET_DIR
                + File.separator + GatewayCliConstants.PROJECT_GEN_DIR + File.separator
                + GatewayCliConstants.GEN_SRC_DIR;
    }

    /**
     * Returns path to the /extensions of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /extensions of a given project in the current working directory
     */
    public static String getProjectExtensionsDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_EXTENSIONS_DIR;
    }

    /**
     * Returns path to the /interceptors of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectInterceptorsDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.PROJECT_INTERCEPTORS_DIR;
    }

    /**
     * Returns path to the /gen/src/interceptors of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /src of a given project in the current working directory
     */
    public static String getProjectGenSrcInterceptorsDirectoryPath(String projectName) {
        return getProjectDirectoryPath(projectName) + File.separator + GatewayCliConstants.PROJECT_TARGET_DIR
                + File.separator + GatewayCliConstants.PROJECT_GEN_DIR + File.separator
                + GatewayCliConstants.GEN_SRC_DIR + File.separator + GatewayCliConstants.PROJECT_INTERCEPTORS_DIR;
    }

    /**
     * Returns path to the /gen/api-definition of a given project in the current working directory
     *
     * @param projectName name of the project
     * @param apiId  md5 hash value of apiName:apiVersion
     * @param extensionType The file extension type. (ex : yaml or json)
     * @return path to the /gen/api-definition of a given project in the current working directory
     */
    public static String getProjectGenSwaggerPath(String projectName, String apiId, String extensionType) {
        return getProjectDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_GEN_DIR + File.separator +
                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR + File.separator + apiId
                + extensionType;
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
     * Returns path to the /gen/api-definition of a given project in the current working directory
     *
     * @param projectName name of the project
     * @return path to the /gen/api-definition of a given project in the current working directory
     */
    public static String getProjectGenAPIDefinitionPath(String projectName ) {
        return getProjectDirectoryPath(projectName) + File.separator +
                GatewayCliConstants.PROJECT_GEN_DIR + File.separator +
                GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR;
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
     * This function deletes the existing destination folder and recursively copy all the sub folder and files from
     * source to destination file paths.
     *
     * @param source      source location
     * @param destination destination location
     * @throws IOException error while copying folder to destination
     */
    public static void copyAndReplaceFolder(String source, String destination) throws IOException {
        File sourceFolder = new File(source);
        File destinationFolder = new File(destination);
        if (destinationFolder.exists()) {
            delete(destinationFolder);
        }
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
    public static void writeContent(String content, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
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
        File[] fileList = file.listFiles();
        if (fileList == null) {
            logger.debug("No files to delete in: {}", file.getAbsolutePath());
            return;
        }
        for (File childFile : fileList) {
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
     * Create directory structure for projects /gen directory.
     *
     * @param genDirPath path to project's /gen directory
     */
    public static void createGenDirectoryStructure(String genDirPath) throws IOException {
        Path genPath = Paths.get(genDirPath);
        FileUtils.deleteDirectory(new File(genDirPath));
        Files.createDirectory(genPath);
        String genSrcPath = genDirPath + File.separator + GatewayCliConstants.GEN_SRC_DIR;
        createFolderIfNotExist(genSrcPath);

        String genPoliciesPath = genSrcPath + File.separator + GatewayCliConstants.GEN_POLICIES_DIR;
        createFolderIfNotExist(genPoliciesPath);
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

    /**
     * Returns the path to the swagger for a defined version of an API
     * @param projectName name of the project
     * @param apiId md5 hash value of apiName:apiVersion
     * @return path to the swagger for a defined version of an API
     */
    public static String getProjectSwaggerFilePath(String projectName, String apiId) {
        return getProjectAPIFilesDirectoryPath(projectName) + File.separator + apiId + File.separator +
                GatewayCliConstants.API_SWAGGER;
    }

    /**
     * To print the message if verbose flag is set
     *
     * @param msg Message
     */
    public static void printVerbose(String msg) {
        if (verboseLogsEnabled) {
            System.out.println("micro-gw: [verbose] " + msg);
        }
    }

    /**
     * To read the system variable VERBOSE_ENABLED
     *
     * @return true if verbose flag is enabled
     */
    private static Boolean setVerboseEnabled() {
        String value = System.getProperty("VERBOSE_ENABLED");
        //bat file provides T and shell script provides true
        return value != null && (value.equals("T") || value.equalsIgnoreCase("true"));
    }
}
