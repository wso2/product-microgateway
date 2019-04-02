/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apimgt.gateway.cli.hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.HashingException;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

/**
 * Utility class used for hashing the changes in the lib directory of micro gateway.
 *
 */
public class LibHashUtils {
    private static final Logger logger = LoggerFactory.getLogger(LibHashUtils.class);
    private static final String MD5 = "MD5";

    /**
     * Generate hashes for the library zip files inside CLI lib folder. If the hashes of the zip file is different.
     *
     * @return true if there are changes detected vs the previous check.
     * @throws HashingException error while change detection.
     */
    public static boolean detectChangesInLibraries() throws HashingException {
        boolean iseDetected = false;
        try {
            String hashFilePath = GatewayCmdUtils.getCLILibHashHolderFileLocation();
            Map<String, String> storedHashes;
            if (Files.exists(Paths.get(hashFilePath))) {
                storedHashes = GatewayCmdUtils.readFileToMap(hashFilePath);
            } else {
                // if file does not exist add default values to the map.
                storedHashes = new HashMap<>();
                storedHashes.put(GatewayCliConstants.CLI_PLATFORM, "");
                storedHashes.put(GatewayCliConstants.CLI_RUNTIME, "");
            }
            String libPath = GatewayCmdUtils.getCLILibPath();
            for (Map.Entry<String, String> entry : storedHashes.entrySet()) {
                String filePath = libPath + File.separator + entry.getKey() + GatewayCliConstants.EXTENSION_ZIP;
                try {
                    if (Files.exists(Paths.get(filePath))) {
                        String checkSum = getFileCheckSum(filePath);
                        if (!checkSum.equals(entry.getValue())) {
                            logger.debug("Checksum difference detected for key : " + entry.getKey());
                            storedHashes.put(entry.getKey(), checkSum);
                            iseDetected = true;
                        }
                    }
                } catch (NoSuchAlgorithmException | IOException e) {
                    logger.error("Error while calculating check sum for path : " + filePath, e);
                }
            }
            if (iseDetected) {
                GatewayCmdUtils.writeMapToFile(storedHashes, hashFilePath);
                return true;
            } else {
                logger.debug("No checksum difference detected in the lib directories");
                return false;
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new HashingException("Error while gateway library change detection", e);
        }
    }

    private static String getFileCheckSum(String filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance(MD5);
        md.update(Files.readAllBytes(Paths.get(filePath)));
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }
}
