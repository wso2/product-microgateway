/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.micro.gateway.core.utils;

import org.wso2.micro.gateway.core.Constants;
import org.wso2.micro.gateway.core.validation.Validate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Set of common utility functionalities required when implementing the native java functions.
 */
public class CommonUtils {
    private static Map<String, String> openAPIMap = new HashMap<>();

    /**
     * Provide a map of all open APIs exposed by the gateway.
     *
     * @return Returns open API definitions of all the APIs exposed via gateway as a {@link Map}.
     */
    public static Map<String, String> getOpenAPIMap() {
        return openAPIMap;
    }

    /**
     * Provide open API definition as a json string for a particular API.
     * @param apiName Name of the API.
     * @param apiVersion Version of the API.
     *
     * @return Returns open API definition of the API as a json {@link String}.
     */
    public static String getOpenAPI(String apiName, String apiVersion) {
        String serviceName = getQualifiedServiceName(apiName, apiVersion);
        if (openAPIMap.containsKey(serviceName)) {
            return openAPIMap.get(serviceName);
        }
        return null;
    }

    /***
     * Extract resource artifacts from the jar file.
     * @param projectName project name.
     * @throws IOException
     */
    public static void extractResources(String projectName) throws IOException {
        String path = Constants.RESOURCE_LOCATION + projectName + Constants.FORWARD_SLASH;
        CodeSource src = Validate.class.getProtectionDomain().getCodeSource();
        StringBuffer stringBuffer;
        if (src != null) {
            URL jar = src.getLocation();
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            while (true) {
                ZipEntry e = zip.getNextEntry();
                if (e == null) {
                    break;
                }
                String name = e.getName();
                if (name.startsWith(path) && name.endsWith(Constants.JSON_EXTENSION)) {
                    String fileName = Paths.get(name).getFileName().toString();
                    String apiName = fileName.substring(0, fileName.lastIndexOf(Constants.UNDER_SCORE));
                    String apiVersion = fileName.substring(fileName.lastIndexOf(Constants.UNDER_SCORE) + 1,
                            fileName.lastIndexOf(Constants.DOT));
                    String serviceName = CommonUtils.getQualifiedServiceName(apiName, apiVersion);
                    InputStream in = Validate.class.getResourceAsStream(Constants.FORWARD_SLASH + name);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    stringBuffer = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line).append("\n");
                    }
                    openAPIMap.put(serviceName, stringBuffer.toString());
                }
            }
        }
    }

    private static String trim(String key) {
        if (key == null) {
            return null;
        }
        key = key.replaceAll("(\\.)|(-)|(\\{)|(})|(\\s)|(/)", "_");
        if (key.contains("*")) {
            key = key.replaceAll("\\*", UUID.randomUUID().toString().replaceAll("-", "_"));
        }
        return key;
    }

    private static String replaceAllNonAlphaNumeric(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "_");
    }

    public static String getQualifiedServiceName(String apiName, String apiVersion) {
        return trim(apiName) + "__" + replaceAllNonAlphaNumeric(apiVersion);

    }
}
