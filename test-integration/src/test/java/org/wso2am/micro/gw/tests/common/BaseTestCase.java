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

package org.wso2am.micro.gw.tests.common;


import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.testcontainers.containers.DockerComposeContainer;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.common.model.ApplicationDTO;
import org.wso2am.micro.gw.tests.common.model.SubscribedApiDTO;
import org.wso2am.micro.gw.tests.context.MgwServerInstance;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;




public class BaseTestCase {

    protected MgwServerInstance microGWServer;


    public void startMGW() throws MicroGWTestException {
        //Utils.checkPortAvailability(TestConstant.ADAPTER_IMPORT_API_PORT);
        //Utils.checkPortAvailability(TestConstant.GATEWAY_LISTENER_HTTPS_PORT);
        microGWServer.startMGW();

    }

    public void stopMGW() {
        microGWServer.stopMGW();

    }

    //utill class
    public void deployAPI(String apiZipFilePath, String TrustStorePath) throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(getImportAPIServiceURLHttps(
                "/mgw/1.0/import/api") , headers, TrustStorePath);
        multipart.addFilePart("file", new File(apiZipFilePath));
        HttpResponse response = multipart.getResponse();

        // todo
        TimeUnit.SECONDS.sleep(5);
        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched", HttpStatus.SC_SUCCESS, response.getResponseCode());
    }



    public static String getImportAPIServiceURLHttps(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + TestConstant.ADAPTER_IMPORT_API_PORT), servicePath)
                .toString();
    }

    public static String getServiceURLHttps(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_PORT), servicePath)
                .toString();
    }

    protected String getMockServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + TestConstant.MOCK_SERVER_PORT), servicePath).toString();
    }

    protected String getJWT(API api, ApplicationDTO applicationDTO, String tier, String keyType, int validityPeriod)
            throws Exception {
        SubscribedApiDTO subscribedApiDTO = new SubscribedApiDTO();
        subscribedApiDTO.setContext(api.getContext() + "/" + api.getVersion());
        subscribedApiDTO.setName(api.getName());
        subscribedApiDTO.setVersion(api.getVersion());
        subscribedApiDTO.setPublisher("admin");

        subscribedApiDTO.setSubscriptionTier(tier);
        subscribedApiDTO.setSubscriberTenantDomain("carbon.super");

        JSONObject jwtTokenInfo = new JSONObject();
        jwtTokenInfo.put("subscribedAPIs", new JSONArray(Arrays.asList(subscribedApiDTO)));
        return TokenUtil.getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod);
    }

}
