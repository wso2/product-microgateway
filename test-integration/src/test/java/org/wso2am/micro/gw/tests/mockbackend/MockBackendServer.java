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

package org.wso2am.micro.gw.tests.mockbackend;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testng.annotations.BeforeSuite;

import java.nio.file.Paths;

import static org.rnorth.visibleassertions.VisibleAssertions.pass;


public class MockBackendServer {


    @BeforeSuite(description = "create mock backend docker image")
    public void startMockBackendServer() {

        String pth = "/home/chashika/Documents/wso2/Envoy/product-microgateway/test/mock-backend-server/";
        ImageFromDockerfile image = new ImageFromDockerfile("wso2/mock-backend", false)
                .withFileFromPath(".", Paths.get(pth));
        verifyImage(image);

    }


    protected static void verifyImage(ImageFromDockerfile image) {
        GenericContainer container = new GenericContainer(image);

        try {
            container.start();

            pass("Should start from Dockerfile");
        } finally {
            container.stop();
        }
    }


}
