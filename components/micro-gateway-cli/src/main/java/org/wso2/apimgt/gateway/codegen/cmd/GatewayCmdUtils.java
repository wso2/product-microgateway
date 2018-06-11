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
package org.wso2.apimgt.gateway.codegen.cmd;

import org.apache.commons.io.FileUtils;
import org.ballerinalang.config.cipher.AESCipherTool;
import org.ballerinalang.config.cipher.AESCipherToolException;
import org.wso2.apimgt.gateway.codegen.config.bean.Config;
import org.wso2.apimgt.gateway.codegen.config.bean.ContainerConfig;
import org.wso2.apimgt.gateway.codegen.exception.CliLauncherException;
import org.wso2.apimgt.gateway.codegen.utils.ZipUtils;

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

public class GatewayCmdUtils {

    private static Config config;
    private static ContainerConfig containerConfig;

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config configFromFile) {
        config = configFromFile;
    }

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

    public static CliLauncherException createUsageException(String errorMsg) {
        CliLauncherException launcherException = new CliLauncherException();
        launcherException.addMessage("micro-gw: " + errorMsg);
        launcherException.addMessage("Run 'micro-gw help' for usage.");
        return launcherException;
    }

    public static String makeFirstLetterLowerCase(String s) {
        if (s == null) {
            return null;
        }
        char c[] = s.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

    public static String encrypt(String value, String secret) {
        try {
            AESCipherTool cipherTool = new AESCipherTool(secret);
            return cipherTool.encrypt(value);
        } catch (AESCipherToolException e) {
            throw createUsageException("failed to encrypt client secret");
        }
    }

    public static String decrypt(String value, String secret) {
        try {
            AESCipherTool cipherTool = new AESCipherTool(secret);
            return cipherTool.decrypt(value);
        } catch (AESCipherToolException e) {
            throw createUsageException("failed to encrypt client secret");
        }
    }

    public static void storeProjectRootLocation(String projectRoot) throws IOException {
        String tempDirPath = getTempFolderLocation();
        createFolderIfNotExist(tempDirPath);

        String projectRootHolderFileLocation = getProjectRootHolderFileLocation();
        File pathFile = new File(projectRootHolderFileLocation);
        if (!pathFile.exists()) {
            pathFile.createNewFile();
        }
        FileWriter writer = null;
        //Write Content
        try {
            writer = new FileWriter(pathFile);
            writer.write(projectRoot);
        } finally {
            writer.close();
        }
    }

    public static String getStoredProjectRootLocation() throws IOException {
        String projectRootHolderFileLocation = getProjectRootHolderFileLocation();
        if (new File(projectRootHolderFileLocation).exists()) {
            return readFileAsString(projectRootHolderFileLocation, false);
        } else {
            return null;
        }
    }

    public static String getCLIHome() {
        return System.getenv(GatewayCliConstants.CLI_HOME);
    }

    public static String getResourceFolderLocation() {
        return System.getenv(GatewayCliConstants.CLI_HOME) + File.separator + GatewayCliConstants.GW_DIST_RESOURCES;
    }

    public static String getFiltersFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_FILTERS;
    }

    public static String getConfigFolderLocation() {
        return getResourceFolderLocation() + File.separator + GatewayCliConstants.GW_DIST_CONF;
    }

    private static String getProjectRootHolderFileLocation() {
        return getTempFolderLocation() + File.separator + GatewayCliConstants.PROJECT_ROOT_HOLDER_FILE_NAME;
    }

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

        String mainConfigDirPath = mainResourceDirPath + File.separator + GatewayCliConstants.CONF_DIRECTORY_NAME;
        createFolderIfNotExist(mainConfigDirPath);

        String mainProjectDirPath = mainResourceDirPath + File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME;
        createFolderIfNotExist(mainProjectDirPath);
    }

    /**
     * Create a project structure for a particular label.
     *
     * @param root project root location
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
     * @param labelName name of the label
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
        ZipUtils.zip(distPath, getLabelTargetDirectoryPath(projectRoot, labelName) + File.separator + File.separator
                + GatewayCliConstants.GW_DIST_PREFIX + labelName + GatewayCliConstants.EXTENSION_ZIP);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getConfigFolderLocation() + File.separator
                        + GatewayCliConstants.GW_DIST_CONF_FILE,
                GatewayCmdUtils.getLabelTargetDirectoryPath(projectRoot, labelName) + File.separator
                        + GatewayCliConstants.PROJECT_CONF_FILE);
    }

    /**
     * Creates the distribution structure for the label
     * 
     * @param projectRoot project root location
     * @param labelName name of the label
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
        String distBinPath = distMicroGWPath + File. separator + GatewayCliConstants.GW_DIST_BIN;
        createFolderIfNotExist(distBinPath);

        //path : {label}/target/distribution/micro-gw-{label}/conf
        String distConfPath = distMicroGWPath + File. separator + GatewayCliConstants.GW_DIST_CONF;
        createFolderIfNotExist(distConfPath);

        //path : {label}/target/distribution/micro-gw-{label}/exec
        String distExec = distMicroGWPath + File. separator + GatewayCliConstants.GW_DIST_EXEC;
        createFolderIfNotExist(distExec);
    }

    /**
     * Get the distribution path for a given label
     *
     * @param projectRoot project root location
     * @param labelName name of the label
     * @return distribution path for a given label
     */
    private static String getTargetDistPath(String projectRoot, String labelName) {
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        return labelTargetPath + File. separator + GatewayCliConstants.GW_TARGET_DIST;
    }


    /**
     * Get the gateway distribution path for a given label
     * 
     * @param projectRoot project root location
     * @param labelName name of the label
     * @return gateway distribution path for a given label
     */
    private static String getTargetGatewayDistPath(String projectRoot, String labelName) {
        String labelTargetPath = getTargetDistPath(projectRoot, labelName);
        return labelTargetPath + File. separator + GatewayCliConstants.GW_DIST_PREFIX + labelName;
    }

    /**
     * Copies shell scripts to the distribution location
     *
     * @param projectRoot project root location
     * @param labelName name of the label
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
     * @param labelName name of the label
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
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME +
                                                File.separator + GatewayCliConstants.CONF_DIRECTORY_NAME;
    }

    /**
     * Returns location of the main configuration file of given project root
     *
     * @param root project root location
     * @return path configuration file
     */
    public static String getMainConfigLocation(String root) {
        return getMainConfigDirPath(root) + File.separator
                + GatewayCliConstants.MAIN_CONFIG_FILE_NAME;
    }

    /**
     * Returns path to the label conf folder in the project root
     *
     * @param root project root location
     * @return path to the label conf folder in the project root
     */
    public static String getLabelConfigDirPath(String root, String label) {
        return getLabelDirectoryPath(root, label) +
                File.separator + GatewayCliConstants.CONF_DIRECTORY_NAME;
    }

    /**
     * Returns location of the main configuration file of given project root
     *
     * @param root project root location
     * @return path label configuration file
     */
    public static String getLabelConfigLocation(String root, String labelName) {
        return getLabelConfigDirPath(root, labelName) + File.separator
                + GatewayCliConstants.LABEL_CONFIG_FILE_NAME;
    }


    /**
     * Returns path to the given label project in the project root path
     *
     * @param root project root location
     * @param labelName name of the label
     * @return path to the given label project in the project root path
     */
    public static String getLabelDirectoryPath(String root, String labelName) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME +
                File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME
                + File.separator + labelName;
    }


    /**
     * Returns path to the /src of a given label project in the project root path
     *
     * @param root project root location
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
     * @param root project root location
     * @param labelName name of the label
     * @return path to the /target of a given label project in the project root path
     */
    private static String getLabelTargetDirectoryPath(String root, String labelName) {
        return getLabelDirectoryPath(root, labelName) + File.separator
                + GatewayCliConstants.PROJECTS_TARGET_DIRECTORY_NAME;
    }

    /**
     * Creates main config file resides in PROJECT_ROOT/conf
     *
     * @param root project root location
     * @throws IOException error while creating the main config file
     */
    public static void createMainConfig(String root) throws IOException {
        String mainConfig = getMainConfigLocation(root);
        File file = new File(mainConfig);
        if (!file.exists()) {
            file.createNewFile();
            //Write Content
            String defaultConfig = readFileAsString(GatewayCliConstants.DEFAULT_MAIN_CONFIG_FILE_NAME, true);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(defaultConfig);
            }
        }
    }

    /**
     * This function recursively copy all the sub folder and files from source to destination file paths
     *
     * @param source source location
     * @param destination destination location
     * @throws IOException error while copying folder to destination
     */
    public static void copyFolder(String source, String destination) throws IOException {
        File sourceFolder = new File(source);
        File destinationFolder = new File(destination);
        copyFolder(sourceFolder, destinationFolder);
    }

    public static void copyFilesToSources(String sourcePath, String destinationPath) throws IOException {
        Files.copy(Paths.get(sourcePath), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * This function recursively copy all the sub folder and files from sourceFolder to destinationFolder
     *
     * @param sourceFolder source location
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

    public static void createLabelConfig(String root, String label) throws IOException {
        String mainConfig = getLabelConfigDirPath(root, label) + File.separator + GatewayCliConstants.LABEL_CONFIG_FILE_NAME;
        File file = new File(mainConfig);
        if (!file.exists()) {
            file.createNewFile();
            FileWriter writer = null;
            //Write Content
            String defaultConfig = readFileAsString(GatewayCliConstants.DEFAULT_LABEL_CONFIG_FILE_NAME, true);
            try {
                writer = new FileWriter(file);
                writer.write(defaultConfig);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    public static ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public static void setContainerConfig(ContainerConfig containerConfig) {
        GatewayCmdUtils.containerConfig = containerConfig;
    }
}
