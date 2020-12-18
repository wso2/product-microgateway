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

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Api deployment class for the mgw.
 *
 */
public class ApiDeployment {

    private static final Logger log = LoggerFactory.getLogger(ApiDeployment.class);

    /**
     * Deploy api to adapter via adapter REST APIs.
     *
     * @param apiZipFilePath  path for the apictl project zip file
     *
     * @throws Exception
     */
    public static void deployAPI(String apiZipFilePath) throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(BaseTestCase.getImportAPIServiceURLHttps(
                TestConstant.ADAPTER_IMPORT_API_RESOURCE) , headers);
        multipart.addFilePart("file", new File(apiZipFilePath));
        HttpResponse response = multipart.getResponse();

        if (response.getResponseCode() != HttpStatus.SC_SUCCESS) {
            log.error("Api deployment is failed");
        }

        TimeUnit.SECONDS.sleep(5);
        //TODO: call this method method after implementing default listener in the router
        //waitTillRoutesAreAvailable();
    }

    //TODO: Need to call this method at the end of deployAPI() method after implementing default listener in the router
    /**
     * wait till api routes are available.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void waitTillRoutesAreAvailable() throws IOException, InterruptedException {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + "test-token");
        HttpResponse response;

        int tries = 0;
        while (true){
            response = HttpsClientRequest.doGet(BaseTestCase.getServiceURLHttps(
                    "/v2/pet/2") , headers);

            tries += 1;
            if(response != null) {
                //TODO: Need to correctly validate the response cde missmatch
                if(response.getResponseCode() == HttpStatus.SC_FORBIDDEN || tries > 10) {
                    break;
                }
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }
}
