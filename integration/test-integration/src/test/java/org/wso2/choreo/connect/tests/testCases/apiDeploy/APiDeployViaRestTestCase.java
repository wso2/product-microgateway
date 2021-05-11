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

package org.wso2.choreo.connect.tests.testCases.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsPostMultipart;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Api deploy test cases.
 *
 */
public class APiDeployViaRestTestCase {

    @Test(description = "Test to check the api deployment is working")
    public void apiDeployTest() throws Exception {
        String apiZipfile = ApictlUtils.createProjectZip( "deploy_openAPI.yaml",
                "deploy_petstore", null);

        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(Utils.getAdapterServiceURLHttps(
                TestConstant.ADAPTER_APIS_RESOURCE) , headers);
        multipart.addFilePart("file", new File(apiZipfile));
        HttpResponse response = multipart.getResponse();

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(dependsOnMethods = "apiDeployTest", description = "Test undeploy API")
    public void apiDeleteTest() throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("apiName", "SwaggerPetstoreDeploy");
        queryParams.put("version", "1.0.5");

        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");

        HttpResponse response = HttpClientRequest.doDelete(Utils.getAdapterServiceURLHttps(
                TestConstant.ADAPTER_APIS_RESOURCE), queryParams, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
