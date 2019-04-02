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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIZipException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);
    private static final String ADD_URL = "addURL";

    /**
     * A method to zip a folder with given path.
     * 
     * Note: this zip method does not preserve permissions of files. eg: "execute" permissions.
     * 
     * @param sourceDirPath src path to zip
     * @param zipFilePath created zip file path
     * @throws IOException error while creating the zip file
     */
    public static void zip(String sourceDirPath, String zipFilePath) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            throw new CLIZipException("Error while including file " + path + " to the zip", e);
                        }
                    });
        } catch (CLIZipException e) {
            throw new IOException("Error while creating a zip from " + sourceDirPath, e);
        }
    }

    /**
     * A method to unzip a zip file to given folder path.
     *
     * Note: this unzip method does not preserve permissions of files. eg: "execute" permissions.
     *
     * @param zipFilePath src path of the zip
     * @param unzipLocation the path zip should be extracted to
     * @param isAddToClasspath if the file is jar, whether add it to the CLI class path
     * @throws IOException error while unzipping the file
     */
    public static void unzip(final String zipFilePath, final String unzipLocation, boolean isAddToClasspath) throws
            IOException {

        if (!(Files.exists(Paths.get(unzipLocation)))) {
            Files.createDirectories(Paths.get(unzipLocation));
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(unzipLocation, entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                    //Explicitly set exection permission to files inside bin folders.
                    if(filePath.toString().contains(File.separator + GatewayCliConstants.GW_DIST_BIN + File.separator)){
                        filePath.toFile().setExecutable(true, false);
                    }
                    //If file is a jar add it to the class loader dynamically.
                    if(isAddToClasspath && entry.getName().endsWith(GatewayCliConstants.EXTENSION_JAR)) {
                        addJarToClasspath(new File(filePath.toString()));
                    }
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private static void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read = 0;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    /**
     * A method to add a given jar to the system class loader.
     *
     * @param jar Jar file to be added to the class loader
     */
    private static void addJarToClasspath(File jar) {
        // Get the ClassLoader class
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> clazz = cl.getClass();

            // Get the protected addURL method from the parent URLClassLoader class
            Method method = clazz.getSuperclass().getDeclaredMethod(ADD_URL, URL.class);

            // Run projected addURL method to add JAR to classpath
            method.setAccessible(true);
            method.invoke(cl, jar.toURI().toURL() );
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
                InvocationTargetException | MalformedURLException e) {
            logger.error("Error while adding jar : " + jar.getName() + " to the class path", e);
        }
    }
}
