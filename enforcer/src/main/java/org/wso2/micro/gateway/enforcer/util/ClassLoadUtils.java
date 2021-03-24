/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.enforcer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ClassLoadUtils includes the Utility methods required for class loading operation during runtime.
 */
public class ClassLoadUtils {

    private static final Logger logger = LogManager.getLogger(ClassLoadUtils.class);
    public static final String DROPINS_DIRECTORY = "/home/wso2/lib/dropins/";
    public static final String JAR_EXTENSION = ".jar";
    public static final String CLASS_EXTENSION = ".class";

    public static Class<?> loadClass(String providedClassName) {
        List<String> jarFilesList = getJarFilesList();

        for (String s : jarFilesList) {
            try {
                String pathToJar = DROPINS_DIRECTORY + s;
                logger.debug("Navigating the JAR file: " + pathToJar);
                JarFile jarFile = new JarFile(pathToJar);
                Enumeration<JarEntry> e = jarFile.entries();
                URLClassLoader cl = getURLClassLoaderForJar(pathToJar);

                while (e.hasMoreElements()) {
                    JarEntry jarEntry = e.nextElement();
                    if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(CLASS_EXTENSION)) {
                        continue;
                    }
                    String className = formatClassNameFromJarEntry(jarEntry);
                    if (providedClassName.equals(className)) {
                        return cl.loadClass(className);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Error while loading class", e);
            }
        }
        return null;
    }

    public static String formatClassNameFromJarEntry(JarEntry jarEntry) {
        // -6 because of .class
        String className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);
        return className.replace('/', '.');
    }

    public static URLClassLoader getURLClassLoaderForJar(String pathToJar) throws MalformedURLException {
        URL[] urls = {new URL("jar:file:" + pathToJar + "!/")};
        return URLClassLoader.newInstance(urls);
    }

    public static List<String> getJarFilesList() {
        List<String> jarFilesList = new ArrayList<>();
        File[] files = new File(DROPINS_DIRECTORY).listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null.
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.endsWith(JAR_EXTENSION)) {
                        jarFilesList.add(file.getName());
                    }
                }
            }
        }
        return jarFilesList;
    }
}
