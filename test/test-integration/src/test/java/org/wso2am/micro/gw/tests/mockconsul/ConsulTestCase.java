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

package org.wso2am.micro.gw.tests.mockconsul;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.util.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConsulTestCase extends BaseTestCase {
    public static int pollInterval = 5; //seconds
    public static final String mockConsulServerURL = "http://localhost:8500";
    public static final String routerConfigDumpURL = "http://localhost:9000/config_dump";
    //    public static final String consulContext = "/v1/health/service/";
    public static final String testCaseContext = "/tc/";


    @BeforeClass(description = "initialise a mock consul server")
    void start() throws Exception {
        File targetClassesDir = new File(ConsulTestCase.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String configPath = targetClassesDir.toString() + File.separator + "conf" + File.separator +
                "consul" + File.separator + "withhttp" + File.separator + "config.toml";
        super.startMGW(configPath, "consul");

        //mockConsulApis.yaml file should put to the resources/apis/openApis folder
        String apiZipfile = ApiProjectGenerator.createApictlProjZip("apis/openApis/mockConsulApis.yaml");
//        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(BaseTestCase.getImportAPIServiceURLHttps(
                TestConstant.ADAPTER_IMPORT_API_RESOURCE) , headers);
        multipart.addFilePart("file", new File(apiZipfile));
        HttpResponse response = multipart.getResponse();
        System.out.println(response.getResponseCode());


        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

//        TimeUnit.SECONDS.sleep(5);
    }

    @AfterClass(description = "stop the mock consul server")
    void stop() {
        super.stopMGW();
    }

    @Test(description = "Connect to consul server")
    public void consulLoadConfigToRouterTest() throws IOException, InterruptedException {
        String testCaseName = "1";
        //load the test case data to the consul mock server
        HttpResponse tcResp = HttpClientRequest.doGet(mockConsulServerURL + testCaseContext + testCaseName, new HashMap<>());
        Assert.assertTrue(tcResp.getData().toString().contains(testCaseName),"test case loaded");
        //wait till the adapter picks up the change and update the router
        TimeUnit.SECONDS.sleep(pollInterval*2L + 2);
        //get router's config
        HttpResponse response = HttpClientRequest.doGet(routerConfigDumpURL, new HashMap<>());

//        System.out.println(response.getData());
        String assertStr = "This data should be loaded according to syntax parse";
        Assert.assertTrue(response.getData().contains("3000"), assertStr);
        Assert.assertTrue(response.getData().contains("8080"), assertStr);
        Assert.assertTrue(response.getData().contains("4000"), assertStr);

        Assert.assertFalse(response.getData().contains("5001"), "Default host should be removed");
        Assert.assertFalse(response.getData().contains("6000"), "Only selected tags should be loaded to config");
        Assert.assertFalse(response.getData().contains("7000"), "Health check critical nodes should be removed");
        Assert.assertFalse(response.getData().contains("5000"), "Only nodes corresponding to selected service should be loaded");


    }

    @Test(description = "Change in consul config/Health check fail reflects in router")
    public void consulReflectChange() throws IOException, InterruptedException {
        String testCaseName = "2";
        //load the first test case state
        HttpClientRequest.doGet(mockConsulServerURL + testCaseContext + "1", new HashMap<>());
        //wait till the adapter picks up the change and update the router
        TimeUnit.SECONDS.sleep(pollInterval + 2);

        //load the current test case data to the consul mock server
        HttpClientRequest.doGet(mockConsulServerURL + testCaseContext + testCaseName, new HashMap<>());
        //wait till the adapter picks up the change and update the router
        TimeUnit.SECONDS.sleep(pollInterval + 2);
        //get router's config
        HttpResponse response = HttpClientRequest.doGet(routerConfigDumpURL, new HashMap<>());
        String assertStr = "This data should be loaded according to syntax parse";
        Assert.assertTrue(response.getData().contains("3000"), assertStr);
        Assert.assertTrue(response.getData().contains("8080"), assertStr);
        Assert.assertTrue(response.getData().contains("4500"), assertStr);
        Assert.assertTrue(response.getData().contains("7000"), assertStr);

        Assert.assertFalse(response.getData().contains("5001"), "Default host should be removed");
        Assert.assertFalse(response.getData().contains("6000"), "Only selected tags should be loaded to config");
        Assert.assertFalse(response.getData().contains("4000"), "Health check critical nodes should be removed");
        Assert.assertFalse(response.getData().contains("5000"), "Only nodes corresponding to selected service should be loaded");

    }

    @Test(description = "Consul server is not reachable")
    public void consulServerDown() throws IOException, InterruptedException {
        String testCaseName = "3";
        //load the first state
        HttpClientRequest.doGet(mockConsulServerURL + testCaseContext + "1", new HashMap<>());
        //wait till the adapter picks up the change and update the router
        TimeUnit.SECONDS.sleep(pollInterval + 2);
        //shutdown the mock consul server
        HttpClientRequest.doGet(mockConsulServerURL + testCaseContext + testCaseName, new HashMap<>());
        //get router's config
        HttpResponse response = HttpClientRequest.doGet(routerConfigDumpURL, new HashMap<>());
        TimeUnit.SECONDS.sleep(pollInterval * 2L + 2);

        //router config should be equal to the first state
//        System.out.println(response.getData());
        String assertStr = "This data should be loaded according to syntax parse";
        Assert.assertTrue(response.getData().contains("3000"), assertStr);
        Assert.assertTrue(response.getData().contains("8080"), assertStr);
        Assert.assertTrue(response.getData().contains("4000"), assertStr);

        Assert.assertFalse(response.getData().contains("5001"), "Default host should be removed");
        Assert.assertFalse(response.getData().contains("6000"), "Only selected tags should be loaded to config");
        Assert.assertFalse(response.getData().contains("7000"), "Health check critical nodes should be removed");
        Assert.assertFalse(response.getData().contains("5000"), "Only nodes corresponding to selected service should be loaded");

    }

}
