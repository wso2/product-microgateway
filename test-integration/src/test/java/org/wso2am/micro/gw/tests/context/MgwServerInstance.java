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

package org.wso2am.micro.gw.tests.context;

import org.apache.commons.io.FileUtils;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.util.Properties;


public class MgwServerInstance implements MgwServer {


    //private DockerComposeContainer environment;
    public static DockerComposeContainer environment;



    public MgwServerInstance() throws IOException, MicroGWTestException {
        File targetClassesDir = new File(MgwServerInstance.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();
        final Properties properties = new Properties();
        properties.load(MgwServerInstance.class.getClassLoader().getResourceAsStream("project.properties"));

        environment = new DockerComposeContainer(new File(targetDir + File.separator + "micro-gwtmp" +  File.separator +
                "wso2am-micro-gw-" + properties.getProperty("version") +  File.separator +
                "docker-compose.yaml"))
                .withLocalCompose(true);


    }
    public MgwServerInstance(String confPath) throws IOException, MicroGWTestException {
        createTmpMgwSetup();
        File targetClassesDir = new File(MgwServerInstance.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String mgwServerPath = targetClassesDir.getParentFile().toString() + File.separator + "server-tmp";
        copyFile(confPath, mgwServerPath  +  File.separator + "resources"  +  File.separator +
                "conf" +  File.separator + "config.toml");

        environment = new DockerComposeContainer(new File(mgwServerPath+  File.separator +
                "docker-compose.yaml"))
                .withLocalCompose(true);


    }

    public void startMGW() {
        environment.start();
    }

    public void stopMGW() {
        environment.stop();
    }

    private static void copyFile(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }

    private static void copyDirectory(String sourceLocation, String destLocation) throws MicroGWTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyDirectory(source, destination);
        } catch (IOException e) {
            throw new MicroGWTestException("error while copying config file. ");
        }
    }

    public static void createTmpMgwSetup() throws IOException, MicroGWTestException {
        File targetClassesDir = new File(MgwServerInstance.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();
        final Properties properties = new Properties();
        properties.load(MgwServerInstance.class.getClassLoader().getResourceAsStream("project.properties"));

        copyDirectory(targetDir + File.separator + "micro-gwtmp" +  File.separator +
                "wso2am-micro-gw-" + properties.getProperty("version"), targetDir +
                File.separator + "server-tmp");
    }

}
