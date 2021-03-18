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
import java.util.regex.Pattern;

/**
 * Apictl project generator class.
 *
 */
public class ApiProjectGenerator {

    private static final String definitions = "Definitions";
    private static final String instruct = "instruct";
    private static final String sequences = "Sequences";
    private static final String libs = "libs";
    private static final String interceptors = "Interceptors";
    private static final String image = "Image";
    private static final String docs = "Docs";
    private static final String endpointCertificates = "Endpoint-certificates";
    private static final String openAPIFile = "swagger.yaml";
    private static final String apiYamlFile = "api.yaml";

    /**
     * Create apictl project zip file.
     *
     * @param apiYamlPath openapi file path.
     *
     * @throws MicroGWTestException
     */
    public static String createApictlProjZip(String apiYamlPath, String swaggerPath) throws MicroGWTestException {
        return createApictlProjZip(apiYamlPath, swaggerPath, null);
    }

    /**
     * Create apictl project zip file.
     *
     * @param apiYamlPath  openapi file path.
     * @param certificatePath endpoint certificate file path.
     *
     * @throws MicroGWTestException if the apictl project creation fails.
     */
    public static String createApictlProjZip(String apiYamlPath, String swaggerPath, String certificatePath) throws MicroGWTestException {

        File targetClassesDir = new File(ApiProjectGenerator.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();

        String filename = apiYamlPath.split(Pattern.quote("."))[0];
        String apisZipPath = ApiProjectGenerator.class.getClassLoader()
                .getResource("apis").getPath() + File.separator + "apiProjects" + File.separator + filename;
        createDirectory(apisZipPath);
        createDirectory(apisZipPath + File.separator + definitions);
        createDirectory(apisZipPath + File.separator + instruct);
        createDirectory(apisZipPath + File.separator + sequences);
        createDirectory(apisZipPath + File.separator + libs);
        createDirectory(apisZipPath + File.separator + interceptors);
        createDirectory(apisZipPath + File.separator + image);
        createDirectory(apisZipPath + File.separator + docs);
        createDirectory(apisZipPath + File.separator + endpointCertificates);

        String testResourcesPath = targetDir + File.separator  + "test-classes" + File.separator;

        //api.yaml
        Utils.copyFile(testResourcesPath + apiYamlPath, apisZipPath + File.separator +
            apiYamlFile);
        
        //swagger.yaml
        Utils.copyFile(testResourcesPath + swaggerPath, apisZipPath + File.separator + definitions + File.separator +
            openAPIFile);

        if (certificatePath != null) {
            Utils.copyFile(testResourcesPath + certificatePath, 
                apisZipPath + File.separator + endpointCertificates + File.separator + "backend.crt");
        }
        ZipDir.createZipFile(apisZipPath);
        return apisZipPath + ".zip";
    }

    private static boolean createDirectory(String filePath) {
        File theDir = new File(filePath);
        return theDir.mkdirs();
    }
}
