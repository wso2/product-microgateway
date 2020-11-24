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

package org.wso2am.micro.gw.tests.prepare;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.wso2am.micro.gw.tests.IntegrationTest;
import org.wso2am.micro.gw.tests.common.MockBackEndServer;
import org.wso2am.micro.gw.tests.context.Utils;


import java.io.File;

@Category(IntegrationTest.class)
public class PreRequisites {
    private MockBackEndServer mockBackEndServer;


    @Before
    public void startMockBackendServer() {
        int port = 2380;
        boolean isOpen = Utils.isPortOpen(port);
        Assert.assertFalse("Port: " + port + " already in use.", isOpen);
        mockBackEndServer = new MockBackEndServer(port);
        mockBackEndServer.start();

        System.out.println("mock server started");
    }

    @After
    public void stop() throws Exception {
        //Stop all the mock servers
        mockBackEndServer.stopIt();
    }

    @Before
    public void setTrustStore() {
        String trustStorePath = new File(
                getClass().getClassLoader().getResource("keystore/cacerts")
                        .getPath()).getAbsolutePath();
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        //System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        //System.setProperty("javax.net.ssl.trustStorePassword", "ballerina");
    }
}
