/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.utils.grpc;

import org.ballerinalang.net.grpc.builder.BallerinaFileBuilder;
import org.ballerinalang.net.grpc.exception.BalGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.utils.grpc.GrpcGen.BalFileGenerationUtils;
import org.wso2.apimgt.gateway.cli.utils.grpc.GrpcGen.BalGenToolException;
import org.wso2.apimgt.gateway.cli.utils.grpc.GrpcGen.DescriptorsGenerator;
import org.wso2.apimgt.gateway.cli.utils.grpc.GrpcGen.OSDetector;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.wso2.apimgt.gateway.cli.utils.grpc.GrpcGen.BalFileGenerationUtils.*;
import static org.wso2.apimgt.gateway.cli.utils.grpc.GrpcGen.BalGenerationConstants.*;

public class GRPCUtils {

    private final String protoPath;

    public GRPCUtils(String protoPath) {
        this.protoPath = protoPath;
    }

    private static PrintStream outStream = System.out;
    private static final Logger LOG = LoggerFactory.getLogger(GRPCUtils.class);
    private String balOutPath = "";
    private String exePath;
    private String protocVersion = "3.4.0";

    public void execute() {
        if (protoPath == null || !protoPath.toLowerCase(Locale.ENGLISH).endsWith(PROTO_SUFFIX)) {
            String errorMessage = "Invalid proto file path. Please input valid proto file location.";
            outStream.println(errorMessage);
            throw new BalGenToolException(errorMessage);
        }

        if (!Files.isReadable(Paths.get(protoPath))) {
            String errorMessage = "Provided service proto file is not readable. Please input valid proto file " +
                    "location.";
            outStream.println(errorMessage);
            throw new BalGenToolException(errorMessage);
        }

        try {
            downloadProtocexe();
        } catch (BalGenToolException e) {
            LOG.error("Error while generating protoc executable. ", e);
            throw new BalGenToolException("Error while generating protoc executable. ", e);
        }

        File descFile = createTempDirectory();
        StringBuilder msg = new StringBuilder();
        outStream.println("Initializing the gateway proxy code generation.");

        byte[] root;
        List<byte[]> dependant;

        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            List<String> protoFiles = readProperties(classLoader);
            for (String file : protoFiles) {
                try {
                    exportResource(file, classLoader);
                } catch (Exception e) {
                    msg.append("Error extracting resource file ").append(file).append(NEW_LINE_CHARACTER);
                    outStream.println(msg.toString());
                    LOG.error("Error exacting resource file " + file, e);
                }
            }
            msg.append("Successfully generated initial files.").append(NEW_LINE_CHARACTER);
            root = BalFileGenerationUtils.getProtoByteArray(this.exePath, this.protoPath, descFile.getAbsolutePath());
            if (root.length == 0) {
                throw new BalGenerationException("Error occurred at generating proto descriptor.");
            }

            LOG.info("Successfully generated root descriptor.");
            dependant = DescriptorsGenerator.generateDependentDescriptor
                    (descFile.getAbsolutePath(), this.protoPath, new ArrayList<>(), exePath, classLoader);
            LOG.info("Successfully generated dependent descriptor.");
        } finally {
            //delete temporary meta files
            delete(new File(META_LOCATION));
            delete(new File(TEMP_GOOGLE_DIRECTORY));
            LOG.debug("Successfully deleted temporary files.");
        }
        try {
            BallerinaFileBuilder ballerinaFileBuilder;
            // By this user can generate stub at different location
            if (EMPTY_STRING.equals(balOutPath)) {
                ballerinaFileBuilder = new BallerinaFileBuilder(dependant);
            } else {
                ballerinaFileBuilder = new BallerinaFileBuilder(dependant, balOutPath);
            }
            ballerinaFileBuilder.setRootDescriptor(root);
            ballerinaFileBuilder.build();
        } catch (BalGenerationException e) {
            LOG.error("Error generating ballerina file.", e);
            msg.append("Error generating ballerina file.").append(NEW_LINE_CHARACTER);
            outStream.println(msg.toString());
        }
        msg.append("Successfully generated ballerina file.").append(NEW_LINE_CHARACTER);

        outStream.println(msg.toString());
    }

    /**
     * Download the protoc executor.
     */
    private void downloadProtocexe() {
        if (exePath == null) {
            exePath = "protoc-" + OSDetector.getDetectedClassifier() + ".exe";
            File exeFile = new File(exePath);
            exePath = exeFile.getAbsolutePath(); // if file already exists will do nothing
            if (!exeFile.isFile()) {
                outStream.println("Downloading proc executor ...");
                try {
                    boolean newFile = exeFile.createNewFile();
                    if (newFile) {
                        LOG.info("Successfully created new protoc exe file" + exePath);
                    }
                } catch (IOException e) {
                    throw new BalGenToolException("Exception occurred while creating new file for protoc exe. ", e);
                }
                String url = PROTOC_PLUGIN_EXE_URL_SUFFIX + protocVersion + "/protoc-" + protocVersion + "-" +
                        OSDetector.getDetectedClassifier() + PROTOC_PLUGIN_EXE_PREFIX;
                try {
                    saveFile(new URL(url), exePath);
                    File file = new File(exePath);
                    //set application user permissions to 455
                    grantPermission(file);
                } catch (IOException e) {
                    throw new BalGenToolException("Exception occurred while writing protoc executable to file. ", e);
                }
                outStream.println("Download successfully completed!");
            } else {
                grantPermission(exeFile);
                outStream.println("Continue with existing protoc executor.");
            }
        } else {
            outStream.println("Pre-Downloaded descriptor detected ...");
        }
    }

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/wrapper.proto"
     */
    private static void exportResource(String resourceName, ClassLoader classLoader) {
        try (InputStream initialStream = classLoader.getResourceAsStream(resourceName);
             OutputStream resStreamOut = new FileOutputStream(resourceName.replace("stdlib",
                     "protobuf"))) {
            if (initialStream == null) {
                throw new BalGenToolException("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = initialStream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (IOException e) {
            throw new BalGenToolException("Cannot find '" + resourceName + "' resource  at the jar.", e);
        }
    }

    /**
     * Create meta temp directory which needed for intermediate processing.
     *
     * @return Temporary Created meta file.
     */
    private File createTempDirectory() {
        File metadataHome = new File(META_LOCATION);
        if (!metadataHome.exists() && !metadataHome.mkdir()) {
            throw new IllegalStateException("Couldn't create dir: " + metadataHome);
        }

        File googleHome = new File(TEMP_GOOGLE_DIRECTORY);
        createTempDirectory(googleHome);

        File protobufHome = new File(googleHome, TEMP_PROTOBUF_DIRECTORY);
        createTempDirectory(protobufHome);

        File compilerHome = new File(protobufHome, TEMP_COMPILER_DIRECTORY);
        createTempDirectory(compilerHome);

        return new File(metadataHome, getProtoFileName() + "-descriptor.desc");
    }

    private void createTempDirectory(File dirName) {
        if (!dirName.exists() && !dirName.mkdir()) {
            throw new IllegalStateException("Couldn't create dir: " + dirName);
        }
    }

    private String getProtoFileName() {
        File file = new File(protoPath);
        return file.getName().replace(PROTO_SUFFIX, EMPTY_STRING);
    }

    private List<String> readProperties(ClassLoader classLoader) {
        String fileName;
        List<String> protoFilesList = new ArrayList<>();
        try (InputStream initialStream = classLoader.getResourceAsStream("standardProtos.properties");
             BufferedReader reader = new BufferedReader(new InputStreamReader(initialStream, StandardCharsets.UTF_8))) {
            while ((fileName = reader.readLine()) != null) {
                protoFilesList.add(fileName);
            }
        } catch (IOException e) {
            throw new BalGenToolException("Error in reading standardProtos.properties.", e);
        }
        return protoFilesList;
    }

    public void setBalOutPath(String balOutPath) {
        this.balOutPath = balOutPath;
    }

    public void setExePath(String exePath) {
        this.exePath = exePath;
    }

    public void setProtocVersion(String protocVersion) {
        this.protocVersion = protocVersion;
    }

    public static String readApi(String filePath) {
        String responseStr;
        try {
            responseStr = new String(Files.readAllBytes(Paths.get(filePath)), GatewayCliConstants.CHARSET_UTF8);
        } catch (IOException e) {
            LOG.error("Error while reading api definition.", e);
            throw new CLIInternalException("Error while reading api definition.");
        }
        return responseStr;
    }
}
