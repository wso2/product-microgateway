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
import org.ballerinalang.config.cipher.AESCipherTool;
import org.ballerinalang.config.cipher.AESCipherToolException;
import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.ContainerConfig;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CliLauncherException;
import org.wso2.apimgt.gateway.cli.model.rest.APICorsConfigurationDTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class GatewayCmdUtils {

    private static Config config;
    private static ContainerConfig containerConfig;
    private static CodeGenerationContext codeGenerationContext;

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config configFromFile) {
        config = configFromFile;
    }

    public static void setCodeGenerationContext(CodeGenerationContext codeGenerationContext) {
        GatewayCmdUtils.codeGenerationContext = codeGenerationContext;
    }

    public static CodeGenerationContext getCodeGenerationContext() {
        return codeGenerationContext;
    }

    /**
     * Read file as string
     *
     * @param path to the file
     * @param inResource whether file is in resources directory of jar or not
     * @return file content
     * @throws IOException if file read went wrong
     */
    public static String readFileAsString(String path, boolean inResource) throws IOException {
        InputStream is = null;
        if (inResource) {
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
        char c[] = s.toCharArray();
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
     * Store workspace location
     *
     * @param workspacePath project workspace location
     * @throws IOException if file write went wrong
     */
    public static void storeWorkspaceLocation(String workspacePath) throws IOException {
        String tempDirPath = getTempFolderLocation();
        createFolderIfNotExist(tempDirPath);

        String projectRootHolderFileLocation = getProjectRootHolderFileLocation();
        File pathFile = new File(projectRootHolderFileLocation);
        if (!pathFile.exists()) {
            pathFile.createNewFile();
        }
        //Write Content
        writeContent(workspacePath, pathFile);
    }

    /**
     * Retrieve stored workspace location
     *
     * @return workspace location
     * @throws IOException if file read went wrong
     */
    public static String getStoredWorkspaceLocation() throws IOException {
        String workspaceLocation = getProjectRootHolderFileLocation();
        if (new File(workspaceLocation).exists()) {
            return readFileAsString(workspaceLocation, false);
        } else {
            return null;
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
    public static String getFiltersFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_FILTERS;
    }

    /**
     * Get config folder location
     *
     * @return config folder location
     */
    public static String getConfigFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_CONF;
    }

    /**
     * Get workspace root holder file path
     *
     * @return workspace root holder file path
     */
    private static String getProjectRootHolderFileLocation() {
        return getTempFolderLocation() + File.separator + GatewayCliConstants.PROJECT_ROOT_HOLDER_FILE_NAME;
    }

    /**
     * Get temp folder location
     *
     * @return temp folder location
     */
    private static String getTempFolderLocation() {
        return getCLIHome() + File.separator + GatewayCliConstants.TEMP_DIR_NAME;
    }

    /**
     * Create the main structure of the project for all the labels
     *
     * @param root Root location to create the structure
     */
    public static void createMainProjectStructure(String root) {
        createFolderIfNotExist(root);

        String mainResourceDirPath = root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME;
        createFolderIfNotExist(mainResourceDirPath);

        String mainProjectDirPath = mainResourceDirPath + File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME;
        createFolderIfNotExist(mainProjectDirPath);
    }

    /**
     * Create a project structure for a particular label.
     *
     * @param root      project root location
     * @param labelName name of the label
     */
    public static void createLabelProjectStructure(String root, String labelName) {
        String mainResourceDir = root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME;
        String mainProjectDir = mainResourceDir + File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME;
        File labelDir = createFolderIfNotExist(mainProjectDir + File.separator + labelName);

        String labelSrcDirPath = labelDir + File.separator + GatewayCliConstants.PROJECTS_SRC_DIRECTORY_NAME;
        createFolderIfNotExist(labelSrcDirPath);

        String labelPolicySrcDirPath = labelSrcDirPath + File.separator + GatewayCliConstants.POLICY_DIR;
        createFolderIfNotExist(labelPolicySrcDirPath);

        String labelTargetDirPath = labelDir + File.separator + GatewayCliConstants.PROJECTS_TARGET_DIRECTORY_NAME;
        createFolderIfNotExist(labelTargetDirPath);

        String labelConfDirPath = labelDir + File.separator + GatewayCliConstants.CONF_DIRECTORY_NAME;
        createFolderIfNotExist(labelConfDirPath);
    }

    /**
     * Create a micro gateway distribution for the provided label
     *
     * @param projectRoot project root location
     * @param labelName   name of the label
     * @throws IOException erro while creating micro gateway distribution
     */
    public static void createLabelGWDistribution(String projectRoot, String labelName) throws IOException {
        createTargetGatewayDistStructure(projectRoot, labelName);

        String distPath = getTargetDistPath(projectRoot, labelName);
        String gwDistPath = getTargetGatewayDistPath(projectRoot, labelName);
        copyFolder(getCLIHome() + File.separator + GatewayCliConstants.CLI_LIB + File.separator
                + GatewayCliConstants.CLI_RUNTIME, gwDistPath + File.separator + GatewayCliConstants.GW_DIST_RUNTIME);
        copyTargetDistBinScripts(projectRoot, labelName);
        copyTargetDistBalx(projectRoot, labelName);

        //copy micro-gw.conf file to the distribution
        GatewayCmdUtils.copyFilesToSources(
                GatewayCmdUtils.getConfigFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_CONF_FILE,
                gwDistPath + File.separator + GatewayCliConstants.GW_DIST_CONF + File.separator
                        + GatewayCliConstants.GW_DIST_CONF_FILE);

        //creating an archive of the distribution
        ZipUtils.zip(distPath, getLabelTargetDirectoryPath(projectRoot, labelName) + File.separator + File.separator
                + GatewayCliConstants.GW_DIST_PREFIX + labelName + GatewayCliConstants.EXTENSION_ZIP);
    }

    /**
     * Creates the distribution structure for the label
     *
     * @param projectRoot project root location
     * @param labelName   name of the label
     */
    private static void createTargetGatewayDistStructure(String projectRoot, String labelName) {
        //path : {label}/target
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        createFolderIfNotExist(labelTargetPath);

        //path : {label}/target/distribution
        String distPath = getTargetDistPath(projectRoot, labelName);
        createFolderIfNotExist(distPath);

        //path : {label}/target/distribution/micro-gw-{label}
        String distMicroGWPath = getTargetGatewayDistPath(projectRoot, labelName);
        createFolderIfNotExist(distMicroGWPath);

        //path : {label}/target/distribution/micro-gw-{label}/bin
        String distBinPath = distMicroGWPath + File.separator + GatewayCliConstants.GW_DIST_BIN;
        createFolderIfNotExist(distBinPath);

        //path : {label}/target/distribution/micro-gw-{label}/conf
        String distConfPath = distMicroGWPath + File.separator + GatewayCliConstants.GW_DIST_CONF;
        createFolderIfNotExist(distConfPath);

        //path : {label}/target/distribution/micro-gw-{label}/logs
        String logsDirPath = distMicroGWPath + File.separator + GatewayCliConstants.PROJECTS_LOGS_DIRECTORY_NAME;
        createFolderIfNotExist(logsDirPath);

        //path : {label}/target/distribution/micro-gw-{label}/logs/access_logs
        createFileIfNotExist(logsDirPath, GatewayCliConstants.ACCESS_LOG_FILE);

        //path : {label}/target/distribution/micro-gw-{label}/exec
        String distExec = distMicroGWPath + File.separator + GatewayCliConstants.GW_DIST_EXEC;
        createFolderIfNotExist(distExec);
    }

    /**
     * Load the stored resource hash content from the CLI temp folder
     *
     * @return stored resource hash content from the CLI temp folder
     * @throws IOException error while loading stored resource hash content
     */
    public static String loadStoredResourceHashes() throws IOException {
        String resourceHashFileLocation = getResourceHashHolderFileLocation();
        String content = null;
        if (new File(resourceHashFileLocation).exists()) {
            content = GatewayCmdUtils.readFileAsString(resourceHashFileLocation, false);
        }
        return content;
    }

    /**
     * Saves the resource hash content to the CLI temp folder
     *
     * @param content resource hash content
     * @throws IOException error while saving resource hash content
     */
    public static void storeResourceHashesFileContent(String content) throws IOException {
        String tempDirPath = getTempFolderLocation();
        createFolderIfNotExist(tempDirPath);

        String resourceHashesFileLocation = getResourceHashHolderFileLocation();
        File pathFile = new File(resourceHashesFileLocation);
        if (!pathFile.exists()) {
            pathFile.createNewFile();
        }
        //Write Content
        writeContent(content, pathFile);
    }

    /**
     * Get resource hash holder file path
     *
     * @return resource hash holder file path
     */
    private static String getResourceHashHolderFileLocation() {
        return GatewayCmdUtils.getTempFolderLocation() + File.separator
                + GatewayCliConstants.RESOURCE_HASH_HOLDER_FILE_NAME;
    }

    /**
     * Get the distribution path for a given label
     *
     * @param projectRoot project root location
     * @param labelName   name of the label
     * @return distribution path for a given label
     */
    private static String getTargetDistPath(String projectRoot, String labelName) {
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        return labelTargetPath + File.separator + GatewayCliConstants.GW_TARGET_DIST;
    }

    /**
     * Get the gateway distribution path for a given label
     *
     * @param projectRoot project root location
     * @param labelName   name of the label
     * @return gateway distribution path for a given label
     */
    private static String getTargetGatewayDistPath(String projectRoot, String labelName) {
        String labelTargetPath = getTargetDistPath(projectRoot, labelName);
        return labelTargetPath + File.separator + GatewayCliConstants.GW_DIST_PREFIX + labelName;
    }

    /**
     * Copies shell scripts to the distribution location
     *
     * @param projectRoot project root location
     * @param labelName   name of the label
     * @throws IOException error while coping scripts
     */
    private static void copyTargetDistBinScripts(String projectRoot, String labelName) throws IOException {
        String linuxShContent = readFileAsString(GatewayCliConstants.GW_DIST_SH_PATH, true);
        linuxShContent = linuxShContent.replace(GatewayCliConstants.LABEL_PLACEHOLDER, labelName);
        String shTargetPath = getTargetGatewayDistPath(projectRoot, labelName);
        File pathFile = new File(shTargetPath + File.separator + GatewayCliConstants.GW_DIST_BIN + File.separator
                + GatewayCliConstants.GW_DIST_SH);
        try (FileWriter writer = new FileWriter(pathFile)) {
            writer.write(linuxShContent);
            pathFile.setExecutable(true);
        }
    }

    /**
     * Copies balx binaries to the distribution location
     *
     * @param projectRoot project root location
     * @param labelName   name of the label
     * @throws IOException error while coping balx files
     */
    private static void copyTargetDistBalx(String projectRoot, String labelName) throws IOException {
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        String gatewayDistExecPath =
                getTargetGatewayDistPath(projectRoot, labelName) + File.separator + GatewayCliConstants.GW_DIST_EXEC;
        File gatewayDistExecPathFile = new File(
                gatewayDistExecPath + File.separator + labelName + GatewayCliConstants.EXTENSION_BALX);
        File balxSourceFile = new File(
                labelTargetPath + File.separator + labelName + GatewayCliConstants.EXTENSION_BALX);
        if (balxSourceFile.exists()) {
            FileUtils.copyFile(balxSourceFile, gatewayDistExecPathFile);
        } else {
            System.err.println(labelName + ".balx could not be found in " + labelTargetPath);
        }
    }

    /**
     * Returns path to the conf folder in the project root
     *
     * @param root project root location
     * @return path to the conf folder in the project root
     */
    public static String getMainConfigDirPath(String root) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME + File.separator
                + GatewayCliConstants.CONF_DIRECTORY_NAME;
    }

    /**
     * Returns location of the main configuration file of given project root
     *
     * @return path configuration file
     */
    public static String getMainConfigLocation() {
        return getCLIHome() + File.separator + GatewayCliConstants.GW_DIST_RESOURCES + File.separator
                + GatewayCliConstants.GW_DIST_CONF + File.separator + GatewayCliConstants.MAIN_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the label conf folder in the project root
     *
     * @param root project root location
     * @return path to the label conf folder in the project root
     */
    public static String getLabelConfigDirPath(String root, String label) {
        return getLabelDirectoryPath(root, label) + File.separator + GatewayCliConstants.CONF_DIRECTORY_NAME;
    }

    /**
     * Returns location of the main configuration file of given project root
     *
     * @param root project root location
     * @return path label configuration file
     */
    public static String getLabelConfigLocation(String root, String labelName) {
        return getLabelConfigDirPath(root, labelName) + File.separator + GatewayCliConstants.LABEL_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the given label project in the project root path
     *
     * @param root      project root location
     * @param labelName name of the label
     * @return path to the given label project in the project root path
     */
    public static String getLabelDirectoryPath(String root, String labelName) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME + File.separator
                + GatewayCliConstants.PROJECTS_DIRECTORY_NAME + File.separator + labelName;
    }

    /**
     * Returns path to the /src of a given label project in the project root path
     *
     * @param root      project root location
     * @param labelName name of the label
     * @return path to the /src of a given label project in the project root path
     */
    public static String getLabelSrcDirectoryPath(String root, String labelName) {
        return getLabelDirectoryPath(root, labelName) + File.separator
                + GatewayCliConstants.PROJECTS_SRC_DIRECTORY_NAME;
    }

    /**
     * Returns path to the /target of a given label project in the project root path
     *
     * @param root      project root location
     * @param labelName name of the label
     * @return path to the /target of a given label project in the project root path
     */
    private static String getLabelTargetDirectoryPath(String root, String labelName) {
        return getLabelDirectoryPath(root, labelName) + File.separator
                + GatewayCliConstants.PROJECTS_TARGET_DIRECTORY_NAME;
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
                destinationFolder.mkdir();
            }

            //Get all files from source directory
            String files[] = sourceFolder.list();

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
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
            folder.mkdir();
        }
        return folder;
    }

    /**
     * Write content to a specified file
     *
     * @param content content to be written
     * @param file file object initialized with path
     * @throws IOException error while writing content to file
     */
    private static void writeContent(String content, File file) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Creates file if not exist
     *
     * @param path folder path
     * @param fileName name of the file
     */
    private static void createFileIfNotExist(String path, String fileName) {
        String filePath = path + File.separator + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                //TODO remove stracktrace and append this to a log
                e.printStackTrace();
            }
        }
    }

    /**
     * Create initial label configuration
     *
     * @param root  workspace location
     * @param label label name
     * @throws IOException if file create went wrong
     */
    public static void createLabelConfig(String root, String label) throws IOException {
        String mainConfig =
                getLabelConfigDirPath(root, label) + File.separator + GatewayCliConstants.LABEL_CONFIG_FILE_NAME;
        File file = new File(mainConfig);
        if (!file.exists()) {
            file.createNewFile();
            //Write Content
            String defaultConfig = readFileAsString(GatewayCliConstants.DEFAULT_LABEL_CONFIG_FILE_NAME, true);
            writeContent(defaultConfig, file);
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
        corsConfigurationDTO.setAccessControlAllowCredentials(true);
        corsConfigurationDTO.setAccessControlAllowOrigins(GatewayCliConstants.accessControlAllowOrigins);
        corsConfigurationDTO.setAccessControlAllowMethods(GatewayCliConstants.accessControlAllowMethods);
        corsConfigurationDTO.setAccessControlAllowHeaders(GatewayCliConstants.accessControlAllowHeaders);
        corsConfigurationDTO.setAccessControlAllowCredentials(GatewayCliConstants.accessControlAllowCredentials);
        return corsConfigurationDTO;
    }
}
