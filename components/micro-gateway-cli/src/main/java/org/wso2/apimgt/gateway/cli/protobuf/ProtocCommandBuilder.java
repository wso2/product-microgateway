/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.protobuf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class is used to build the protoc compiler command to generate descriptor.
 */
public class ProtocCommandBuilder {
    private static final String EXE_PATH_PLACEHOLDER = "{{exe_file_path}}";
    private static final String PROTO_PATH_PLACEHOLDER = "{{proto_file_path}}";
    private static final String PROTO_FOLDER_PLACEHOLDER = "{{proto_folder_path}}";
    private static final String DESC_PATH_PLACEHOLDER = "{{desc_file_path}}";
    private static final String COMMAND_PLACEHOLDER = "{{exe_file_path}} --proto_path={{proto_folder_path}} " +
            "{{proto_file_path}} --descriptor_set_out={{desc_file_path}}";
    public static final String PROTOC_PLUGIN_EXE_URL_SUFFIX = "https://repo1.maven.org/maven2/com/google/" +
            "protobuf/protoc/";
    private String protocVersion = "3.9.1";
    public static final String PROTOC_PLUGIN_EXE_PREFIX = ".exe";
    private String exePath;
    private String protoPath;
    private String protoFolderPath;
    private String descriptorSetOutPath;

    private static final Logger logger = LoggerFactory.getLogger(ProtocCommandBuilder.class);
    private static final PrintStream OUT = System.out;

    ProtocCommandBuilder(String protoPath, String protofolderPath, String descriptorSetOutPath) {
        this.exePath = getProtocExePath();
        this.protoPath = protoPath;
        this.descriptorSetOutPath = descriptorSetOutPath;
        this.protoFolderPath = protofolderPath;
    }

    /**
     * Build the command to generate descriptor by compiling the protobuf file using protoc.exe.
     *
     * @return the command to generate descriptor
     */
    public String build() {
        //before generation of the command, download the executable to the toolkit.
        try {
            downloadProtocexe();
        } catch (IOException e) {
            throw new CLIInternalException("Error while downloading the protoc executable : protoc-" +
                    OSDetector.getDetectedClassifier() + ".exe");
        }
        String finalCommand = COMMAND_PLACEHOLDER.replace(EXE_PATH_PLACEHOLDER, exePath)
                .replace(PROTO_PATH_PLACEHOLDER, protoPath)
                .replace(DESC_PATH_PLACEHOLDER, descriptorSetOutPath)
                .replace(PROTO_FOLDER_PLACEHOLDER, protoFolderPath);
        logger.debug("Protobuf compilation command : ", finalCommand);
        return finalCommand;
    }

    /**
     * Download the protoc executor.
     */
    private void downloadProtocexe() throws IOException {
        String protocFilename = getProtocFilename();
        String protocDirPath = CmdUtils.getProtocDirPath();
        File protocExeFile = new File(protocDirPath, protocFilename);
        String protocExePath = protocExeFile.getAbsolutePath(); // if file already exists will do nothing
        if (!protocExeFile.exists()) {
            logger.info("Downloading protoc executor file - " + protocFilename);
            String protocDownloadurl = PROTOC_PLUGIN_EXE_URL_SUFFIX + protocVersion + "/protoc-" + protocVersion
                    + "-" + OSDetector.getDetectedClassifier() + PROTOC_PLUGIN_EXE_PREFIX;
            File tempDownloadFile = new File(protocDirPath,
                    "protoc-" + OSDetector.getDetectedClassifier() + ".exe.download");
            try {
                downloadFile(new URL(protocDownloadurl), tempDownloadFile);
                Files.move(tempDownloadFile.toPath(), protocExeFile.toPath());
                //set application user permissions to 455
                grantPermission(protocExeFile);
            } catch (CLIInternalException e) {
                Files.deleteIfExists(Paths.get(protocExePath));
                throw e;
            }
            logger.debug("Download process is successfully completed. Executor file path - " + protocExeFile.getPath());
        } else {
            grantPermission(protocExeFile);
            logger.info("Continue with existing protoc executor file at " + protocExeFile.getPath());
        }
    }

    /**
     * Download file in the url to the destination file.
     *
     * @param url  file URL.
     * @param file destination file.
     */
    public static void downloadFile(URL url, File file) {
        try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(file)) {
            int length;
            byte[] buffer = new byte[1024]; // buffer for portion of data
            int count = 0;
            while ((length = in.read(buffer)) > -1) {
                fos.write(buffer, 0, length);

                // just to make it look we are doing something when file is downloading
                count++;
                if (count == 1000) {
                    OUT.print('.');
                    count = 0;
                }
            }
        } catch (IOException e) {
            String msg = "Error while downloading the file: " + file.getName() + ". " + e.getMessage();
            throw new CLIInternalException(msg);
        }
    }

    /**
     * Grant permission to the protoc executor file.
     *
     * @param file protoc executor file.
     */
    public static void grantPermission(File file) {
        boolean isExecutable = file.setExecutable(true);
        boolean isReadable = file.setReadable(true);
        boolean isWritable = file.setWritable(true);
        if (isExecutable && isReadable && isWritable) {
            logger.debug("Successfully granted permission for protoc exe file");
        } else {
            String msg = "Error while providing execute permission to protoc executor file: " + file.getName();
            throw new CLIInternalException(msg);
        }
    }

    private static String getProtocExePath() {
        return CmdUtils.getProtocDirPath() + File.separator + getProtocFilename();
    }

    private static String getProtocFilename() {
        return "protoc-" + OSDetector.getDetectedClassifier() + ".exe";
    }
}
