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
package org.wso2am.micro.gw.tests.websocket;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.ApiProjectGenerator;
import org.wso2am.micro.gw.tests.util.HttpResponse;
import org.wso2am.micro.gw.tests.util.HttpsPostMultipart;
import org.wso2am.micro.gw.tests.util.TestConstant;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebSocketDeployTestCase extends BaseTestCase {

    @BeforeClass(description = "initialise the setup")
    public void start() throws Exception {
        super.startMGW();
    }

    @AfterClass(description = "stop the setup")
    void stop() {
        super.stopMGW();
    }

    @Test(description = "Test to check websocket API deployment with apis/openApis/mockWebSocketApiProdSand.yaml")
    public void webSocketDeployTest() throws Exception{
        String apiZipFile = ApiProjectGenerator.createApictlProjZip("apis/openApis/mockWebSocketAPIProdSand.yaml",null,
                null);
        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(getImportAPIServiceURLHttps(
                TestConstant.ADAPTER_IMPORT_API_RESOURCE) , headers);
        multipart.addFilePart("file", new File(apiZipFile));
        HttpResponse response = multipart.getResponse();

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
