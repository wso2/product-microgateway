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
import java.io.*;
import java.util.regex.Pattern;

/**
 * Apictl project generator class.
 *
 */
public class ApiProjectGenerator {

    /**
     * Create apictl project zip file.
     *
     * @param apiYamlName  name of the api yaml file.
     *
     * @throws MicroGWTestException
     */
    public static String createApictlProjZip(String apiYamlName) throws MicroGWTestException {

        File targetClassesDir = new File(ApiProjectGenerator.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();

        String filename = apiYamlName.split(Pattern.quote("."))[0];
        String apisZipPath = ApiProjectGenerator.class.getClassLoader()
                .getResource("apis").getPath() + File.separator + "apiProjects" + File.separator + filename;
        createDirectory(apisZipPath);
        createDirectory(apisZipPath + File.separator + "Meta-information");
        createDirectory(apisZipPath + File.separator + "instruct");
        createDirectory(apisZipPath + File.separator + "Sequences");
        createDirectory(apisZipPath + File.separator + "libs");
        createDirectory(apisZipPath + File.separator + "Interceptors");
        createDirectory(apisZipPath + File.separator + "Image");
        createDirectory(apisZipPath + File.separator + "Docs");

        String apiPath = targetDir + File.separator  + "test-classes" + File.separator + "apis" + File.separator +
                "openApis" + File.separator+ apiYamlName;
        Utils.copyFile(apiPath, apisZipPath + File.separator + "Meta-information" + File.separator +
                "swagger.yaml");

        ZipDir.createZipFile(apisZipPath);
        return apisZipPath + ".zip";
    }

    public static void createDirectory(String filePath) {
        File theDir = new File(filePath);
        theDir.mkdirs();
    }
}
