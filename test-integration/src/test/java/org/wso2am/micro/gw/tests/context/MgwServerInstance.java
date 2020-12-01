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

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.testcontainers.containers.DockerComposeContainer;
import org.wso2am.micro.gw.tests.mockbackend.MockBackendServer;
import org.wso2am.micro.gw.tests.util.FileUtil;
import org.wso2am.micro.gw.tests.util.HttpClientRequest;
import org.wso2am.micro.gw.tests.util.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.wso2am.micro.gw.tests.common.BaseTestCase.getMockServiceURLHttp;


public class MgwServerInstance implements MgwServer {


    private DockerComposeContainer environment;



    public MgwServerInstance() throws IOException, MicroGWTestException {
        createTmpMgwSetup();
        File targetClassesDir = new File(MgwServerInstance.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String mgwServerPath = targetClassesDir.getParentFile().toString() + File.separator + "server-tmp";

        String dockerCompsePath = mgwServerPath+  File.separator + "docker-compose.yaml";
        MockBackendServer.addMockBackendServiceToDockerCompose(dockerCompsePath);
        environment = new DockerComposeContainer(new File(dockerCompsePath))
                .withLocalCompose(true);


    }
    public MgwServerInstance(String confPath) throws IOException, MicroGWTestException {
        createTmpMgwSetup();
        File targetClassesDir = new File(MgwServerInstance.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String mgwServerPath = targetClassesDir.getParentFile().toString() + File.separator + "server-tmp";
        FileUtil.copyFile(confPath, mgwServerPath  +  File.separator + "resources"  +  File.separator +
                "conf" +  File.separator + "config.toml");

        String dockerCompsePath = mgwServerPath+  File.separator + "docker-compose.yaml";
        MockBackendServer.addMockBackendServiceToDockerCompose(dockerCompsePath);
        environment = new DockerComposeContainer(new File(dockerCompsePath))
                .withLocalCompose(true);


    }

    public void startMGW() throws IOException {
        environment.start();
        waitTillBackendIsAvailable();

    }

    public void stopMGW() {
        environment.stop();
    }


    public static void createTmpMgwSetup() throws IOException, MicroGWTestException {
        File targetClassesDir = new File(MgwServerInstance.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();
        final Properties properties = new Properties();
        properties.load(MgwServerInstance.class.getClassLoader().getResourceAsStream("project.properties"));

        FileUtil.copyDirectory(targetDir + File.separator + "micro-gwtmp" +  File.separator +
                "wso2am-micro-gw-" + properties.getProperty("version"), targetDir +
                File.separator + "server-tmp");
    }

    public static void mySleep (int val) {
        try {
            TimeUnit.SECONDS.sleep(val);
        } catch (InterruptedException e) {
            //log.error("Thread interrupted");
        }
    }

    public static void waitTillBackendIsAvailable() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        HttpResponse response;

        int tries = 0;
        while (true){
            response= HttpClientRequest.doGet(getMockServiceURLHttp(
                    "/v2/pet/3") , headers);
            tries += 1;
            if(response != null) {
                if(response.getResponseCode() == HttpStatus.SC_OK || tries > 50) {
                    break;
                }
            }
            mySleep (5);
        }
    }

}
