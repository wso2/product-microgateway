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

package org.wso2.micro.gateway.core.compression;

import org.ballerinalang.jvm.util.exceptions.BLangRuntimeException;
import org.wso2.micro.gateway.core.Constants;
import org.wso2.micro.gateway.core.utils.ErrorUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Extern function wso2.gateway:compress.
 *
 *
 */
public class Compress {

    /**
     * Compresses a given folder or file.
     *
     * @param dirPath directory path to be compressed
     * @param destDir destination path to place the compressed file
     * @throws IOException exception if an error occurrs when compressing
     */
    private static void compress(Path dirPath, Path destDir) throws IOException {
        compressFiles(dirPath, new FileOutputStream(destDir.toFile()));
    }

    /**
     * Add file inside the src directory to the ZipOutputStream.
     *
     * @param zos      ZipOutputStream
     * @param filePath file path of each file inside the driectory
     * @throws IOException exception if an error occurrs when compressing
     */
    private static void addEntry(ZipOutputStream zos, Path filePath, String fileStr) throws IOException {
        ZipEntry ze = new ZipEntry(fileStr);
        zos.putNextEntry(ze);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    /**
     * Compresses files.
     *
     * @param outputStream outputstream
     * @return outputstream of the compressed file
     * @throws IOException exception if an error occurrs when compressing
     */
    static OutputStream compressFiles(Path dir, OutputStream outputStream) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        if (Files.isRegularFile(dir)) {
            Path fileName = dir.getFileName();
            if (fileName != null) {
                addEntry(zos, dir, fileName.toString());
            } else {
                throw new BLangRuntimeException("Error occurred when compressing");
            }
        } else {
            Stream<Path> list = Files.walk(dir);
            list.forEach(p -> {
                StringJoiner joiner = new StringJoiner("/");
                for (Path path : dir.relativize(p)) {
                    joiner.add(path.toString());
                }
                if (Files.isRegularFile(p)) {
                    try {
                        addEntry(zos, p, joiner.toString());
                    } catch (IOException e) {
                        throw new BLangRuntimeException("Error occurred when compressing");
                    }
                }
            });
        }
        zos.close();
        return outputStream;
    }


    public static Object compress(String dirPath, String destDir) throws Exception {
        Path srcPath = Paths.get(dirPath);
        Path destPath = Paths.get(destDir);
        if (!srcPath.toFile().exists()) {

            throw ErrorUtils.getBallerinaError(Constants.FILE_NOT_FOUND_ERROR, "Path of the folder to be " +
                    "compressed is not available: " + srcPath);
        } else {
            try {
                compress(srcPath, destPath);
                return null;
            } catch (IOException | BLangRuntimeException e) {
                throw ErrorUtils.getBallerinaError("Error occurred when compressing", e);
            }
        }
    }
}
