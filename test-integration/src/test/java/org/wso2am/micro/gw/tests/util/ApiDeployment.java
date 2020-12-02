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
import org.junit.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.wso2am.micro.gw.tests.common.BaseTestCase.getImportAPIServiceURLHttps;

/**
 * Api deployment class for the mgw.
 *
 */
public class ApiDeployment {

    /**
     * Get Mock backend server module root path.
     *
     * @param apiZipFilePath  path for the apictl project zip file
     *
     * @throws Exception
     */
    public static void deployAPI(String apiZipFilePath) throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(getImportAPIServiceURLHttps(
                TestConstant.ADAPTER_IMPORT_API_RESOURCE) , headers);
        multipart.addFilePart("file", new File(apiZipFilePath));
        HttpResponse response = multipart.getResponse();

        // todo (currently no way to test the availability of the api routes, so wait 5 seconds)
        TimeUnit.SECONDS.sleep(5);
        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched", HttpStatus.SC_SUCCESS, response.getResponseCode());
    }


    public void waitTillApisAvailable() {

    }
}
