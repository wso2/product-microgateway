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
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists() && !tempDir.isDirectory()) {
            tempDir.mkdir();
        }

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
    
    public static void createMainProjectStructure(String root) {
        File rootDir = new File(root);
        if (!rootDir.exists() && !rootDir.isDirectory()) {
            rootDir.mkdir();
        }
        
        String mainResourceDirPath = root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME;
        File mainResourceDir = new File(mainResourceDirPath);
        if (!mainResourceDir.exists() && !mainResourceDir.isDirectory()) {
            mainResourceDir.mkdir();
        }

        String mainConfigDirPath = mainResourceDirPath + File.separator + GatewayCliConstants.MAIN_CONF_DIRECTORY_NAME;
        File mainConfigDir = new File(mainConfigDirPath);
        if (!mainConfigDir.exists() && !mainConfigDir.isDirectory()) {
            mainConfigDir.mkdir();
        }

        String mainProjectDirPath = mainResourceDir + File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME;
        File mainProjectDir = new File(mainProjectDirPath);
        if (!mainProjectDir.exists() && !mainProjectDir.isDirectory()) {
            mainProjectDir.mkdir();
        }
    }

    public static void createLabelProjectStructure(String root, String labelName) {
        String mainResourceDir = root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME;
        String mainProjectDir = mainResourceDir + File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME;

        String labelDirPath = mainProjectDir + File.separator + labelName;
        File labelDir = new File(labelDirPath);
        if (!labelDir.exists() && !labelDir.isDirectory()) {
            labelDir.mkdir();
        }

        String labelSrcDirPath = labelDir + File.separator + GatewayCliConstants.PROJECTS_SRC_DIRECTORY_NAME;
        File labelSrcDir = new File(labelSrcDirPath);
        if (!labelSrcDir.exists() && !labelSrcDir.isDirectory()) {
            labelSrcDir.mkdir();
        }

        String labelTargetDirPath = labelDir + File.separator + GatewayCliConstants.PROJECTS_TARGET_DIRECTORY_NAME;
        File labelTargetDir = new File(labelTargetDirPath);
        if (!labelTargetDir.exists() && !labelTargetDir.isDirectory()) {
            labelTargetDir.mkdir();
        }
    }

    public static String getMainConfigPath(String root) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME +
                                                File.separator + GatewayCliConstants.MAIN_CONF_DIRECTORY_NAME;
    }

    public static String getLabelSrcDirectoryPath(String root, String labelName) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME +
                File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME
                + File.separator + labelName + File.separator + GatewayCliConstants.PROJECTS_SRC_DIRECTORY_NAME;
     }

    public static String getLabelDirectoryPath(String root, String labelName) {
        return root + File.separator + GatewayCliConstants.MAIN_DIRECTORY_NAME +
                File.separator + GatewayCliConstants.PROJECTS_DIRECTORY_NAME
                + File.separator + labelName;
    }

    public static void createMainConfig(String root) throws IOException {
        String mainConfig = getMainConfigPath(root) + File.separator + GatewayCliConstants.MAIN_CONFIG_FILE_NAME;
        File file = new File(mainConfig);
        if (!file.exists()) {
            file.createNewFile();
            FileWriter writer = null;
            //Write Content
            String defaultConfig = readFileAsString(GatewayCliConstants.DEFAULT_MAIN_CONFIG_FILE_NAME, true);
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
}
