/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2am.micro.gw.tests.util;


import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * To compress a directory in ZIP format.
 *
 */
public class ZipDir extends SimpleFileVisitor<Path> {
    private static final Logger log = LoggerFactory.getLogger(ZipDir.class);

    private static ZipOutputStream zos;
    private final Path sourceDir;

    public ZipDir(Path sourceDir) {
        this.sourceDir = sourceDir;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        try {
            Path targetFile = sourceDir.relativize(file);
            zos.putNextEntry(new ZipEntry(targetFile.toString()));

            byte[] bytes = Files.readAllBytes(file);
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        } catch (IOException e) {
            log.error("Error occurred while adding files to the zip", e);
        }
        return FileVisitResult.CONTINUE;
    }

    public static void createZipFile(String dirPath) {
        Path sourceDir = Paths.get(dirPath);
        String zipFileName = dirPath.concat(".zip");

        try (FileOutputStream fileOutputStream = new FileOutputStream(zipFileName)) {
            zos = new ZipOutputStream(fileOutputStream);
            Files.walkFileTree(sourceDir, new ZipDir(sourceDir));
            zos.close();
        } catch (IOException e) {
            log.error("Error occurred while compressing directory into zip", e);
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }
}
