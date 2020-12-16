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

import org.wso2am.micro.gw.tests.context.MicroGWTestException;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Apictl project generator class.
 *
 */
public class ApiProjectGenerator {

    /**
     * Create apictl project zip file.
     *
     * @param apiYamlPath openapi file path.
     *
     * @throws MicroGWTestException
     */
    public static String createApictlProjZip(String apiYamlPath) throws MicroGWTestException {
        return createApictlProjZip(apiYamlPath, null);
    }

    /**
     * Create apictl project zip file.
     *
     * @param apiYamlPath  openapi file path.
     * @param certificatePath endpoint certificate file path.
     *
     * @throws MicroGWTestException if the apictl project creation fails.
     */
    public static String createApictlProjZip(String apiYamlPath, String certificatePath) throws MicroGWTestException {

        File targetClassesDir = new File(ApiProjectGenerator.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();

        String filename = apiYamlPath.split(Pattern.quote("."))[0];
        String apisZipPath = Objects.requireNonNull(ApiProjectGenerator.class.getClassLoader()
                .getResource("apis")).getPath() + File.separator + "apiProjects" + File.separator + filename;
        createDirectory(apisZipPath);
        createDirectory(apisZipPath + File.separator + "Meta-information");
        createDirectory(apisZipPath + File.separator + "instruct");
        createDirectory(apisZipPath + File.separator + "Sequences");
        createDirectory(apisZipPath + File.separator + "libs");
        createDirectory(apisZipPath + File.separator + "Interceptors");
        createDirectory(apisZipPath + File.separator + "Image");
        createDirectory(apisZipPath + File.separator + "Docs");
        createDirectory(apisZipPath + File.separator + "Endpoint-Certificates");
        //TODO: (VirajSalaka) refactor the code.
        String apiPath = targetDir + File.separator  + "test-classes" + File.separator + apiYamlPath;

        Utils.copyFile(apiPath, apisZipPath + File.separator + "Meta-information" + File.separator +
                "swagger.yaml");

        if (certificatePath != null) {
            String certPath = targetDir + File.separator  + "test-classes" + File.separator + certificatePath;
            Utils.copyFile(certPath, apisZipPath + File.separator + "Endpoint-Certificates" +
                    File.separator + "backend.crt");
        }
        ZipDir.createZipFile(apisZipPath);
        return apisZipPath + ".zip";
    }

    private static boolean createDirectory(String filePath) {
        File theDir = new File(filePath);
        return theDir.mkdirs();
    }
}
