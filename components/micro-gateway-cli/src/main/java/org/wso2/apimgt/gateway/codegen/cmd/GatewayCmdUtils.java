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
import org.wso2.apimgt.gateway.codegen.exception.CliLauncherException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class GatewayCmdUtils {

    private static Config config;

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

        String mainConfigDirPath = mainResourceDirPath + File.separator + GatewayCliConstants.MAIN_CONF_DIRECTORY_NAME;
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

        String labelTargetDirPath = labelDir + File.separator + GatewayCliConstants.PROJECTS_TARGET_DIRECTORY_NAME;
        createFolderIfNotExist(labelTargetDirPath);
    }

    /**
     * Creates the distribution structure for the label
     * 
     * @param projectRoot project root location
     * @param labelName name of the label
     * @return created distribution home path
     */
    public static String createTargetGatewayDistStructure(String projectRoot, String labelName) {
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        createFolderIfNotExist(labelTargetPath);
        
        String distPath = labelTargetPath + File. separator + GatewayCliConstants.GW_DIST_PREFIX + labelName;
        createFolderIfNotExist(distPath);

        String distBinPath = distPath + File. separator + GatewayCliConstants.GW_DIST_BIN;
        createFolderIfNotExist(distBinPath);

        String distConfPath = distPath + File. separator + GatewayCliConstants.GW_DIST_CONF;
        createFolderIfNotExist(distConfPath);

        String distExec = distPath + File. separator + GatewayCliConstants.GW_DIST_EXEC;
        createFolderIfNotExist(distExec);

        return distPath;
    }

    /**
     * Get the gateway distribution path for a given label
     * 
     * @param projectRoot project root location
     * @param labelName name of the label
     * @return gateway distribution path for a given label
     */
    public static String getTargetGatewayDistPath(String projectRoot, String labelName) {
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        return labelTargetPath + File. separator + GatewayCliConstants.GW_DIST_PREFIX + labelName;
    }

    /**
     * Copies shell scripts to the distribution location
     * 
     * @param projectRoot project root location
     * @param labelName name of the label
     * @throws IOException error while coping scripts 
     */
    public static void copyTargetDistBinScripts(String projectRoot, String labelName) throws IOException {
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
     * @return true if successfully copied
     * @throws IOException error while coping balx files
     */
    public static boolean copyTargetDistBalx(String projectRoot, String labelName) throws IOException {
        String labelTargetPath = getLabelTargetDirectoryPath(projectRoot, labelName);
        String gatewayDistExecPath =
                getTargetGatewayDistPath(projectRoot, labelName) + File.separator + GatewayCliConstants.GW_DIST_EXEC;
        File gatewayDistExecPathFile = new File(
                gatewayDistExecPath + File.separator + labelName + GatewayCliConstants.EXTENSION_BALX);
        File balxSourceFile = new File(
                labelTargetPath + File.separator + labelName + GatewayCliConstants.EXTENSION_BALX);
        if (balxSourceFile.exists()) {
            FileUtils.copyFile(balxSourceFile, gatewayDistExecPathFile);
            return true;
        } else {
            System.err.println(labelName + ".balx could not be found in " + labelTargetPath);
            return false;
        }
    }

    /**
     * Returns path to the conf folder in the project root
     * 
     * @param root project root location
     * @return path to the conf folder in the project root
     */
    public static String getMainConfigPath(String root) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME +
                                                File.separator + GatewayCliConstants.MAIN_CONF_DIRECTORY_NAME;
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
    public static String getLabelTargetDirectoryPath(String root, String labelName) {
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
        String mainConfig = getMainConfigPath(root) + File.separator + GatewayCliConstants.MAIN_CONFIG_FILE_NAME;
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
}
