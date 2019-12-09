/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.micro.gateway.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extern function wso2.gateway:unzip.
 */
public class UnzipUtility {
    private static final int BUFFER_SIZE = 4096;

    /**
     * Unzip folders and files seperately
     *
     * @param zipFilePath    file path of the zip file
     * @param destDirectory  file path of the destination directory
     * @throws IOException exception if an error occurs when compressing
     */
    public void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        boolean isDestCreated = destDir.mkdir();
        if (!isDestCreated) {
            throw new IOException("Error occurred when creating folder");
        }
        try (FileInputStream f = new FileInputStream(zipFilePath); ZipInputStream zipIn = new ZipInputStream(f)) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                    if (entry.getName().equals("bin/ballerina")) {
                        File file = new File(filePath);
                        file.setExecutable(true, false);
                    }
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    boolean wasDirSuccessful = dir.mkdir();
                    if (!wasDirSuccessful) {
                        throw new IOException("Error occurred when creating folder");
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    /**
     * extract files in the ZipInputStream.
     *
     * @param zipIn      ZipInputStream
     * @param filePath   file path of each file inside the directory
     * @throws IOException exception if an error occurs when compressing
     */
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
}
