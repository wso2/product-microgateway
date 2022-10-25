/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests.testcases.standalone.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.SourceControlUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SourceWatcherTestCase {
    public static final String ZIP = File.separator + "zip";
    public static final String UPDATED = File.separator + "updated";
    public static final String PETSTORE1 = File.separator + "petstore1";

    protected String jwtToken;

    @BeforeClass(description = "Initialize the setup")
    void start() throws Exception{
        jwtToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "read:pets",
                false);
    }

    @Test(description = "Test if artifacts added from source version control are deployed")
    public void testAddedAPI() throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(
                Utils.getServiceURLHttps("/v2/pet/findByStatus?status=available") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test if zip artifacts updated from source version control are deployed",
            dependsOnMethods = {"testAddedAPI"})
    public void testUpdatingAPI() throws Exception{
        String projectDir = Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + UPDATED;
        SourceControlUtils.updateApiProjectInRepo(projectDir, false);
        Utils.delay(4000, "Interrupted while waiting for adapter to sync");

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);

        HttpResponse response1 = HttpsClientRequest.retryUntil404(
                Utils.getServiceURLHttps("/v2/pet/findByStatus?status=available") , headers);
        Assert.assertNotNull(response1);
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched");

        HttpResponse response2 = HttpsClientRequest.retryGetRequestUntilDeployed(
                Utils.getServiceURLHttps("/v2/store/inventory") , headers);
        Assert.assertNotNull(response2);
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test if artifacts deleted from source version control are undeployed",
            dependsOnMethods = {"testUpdatingAPI"})
    public void testDeletedAPI() throws Exception {
        SourceControlUtils.deleteApiProjectInRepo(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.DIRECTORY);
        Utils.delay(4000, "Interrupted while waiting for adapter to sync");

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);

        HttpResponse response = HttpsClientRequest.retryUntil404(
                Utils.getServiceURLHttps("/zip/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched");
    }

    @Test(description = "Test if zip artifacts added from source version control are deployed")
    public void testAddedZipAPI() throws Exception{
        String projectDir = Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + ZIP;
        Utils.zip(projectDir + PETSTORE1, PETSTORE1);
        SourceControlUtils.commitApiProjectToRepo(projectDir, true, "Add Zipped API Project");
        Utils.delay(4000, "Interrupted while waiting for adapter to sync");

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);

        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(
                Utils.getServiceURLHttps("/zip/store/inventory") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test if zip artifacts deleted from source version control are undeployed",
            dependsOnMethods = {"testAddedZipAPI"})
    public void testDeletedZipAPI() throws Exception {
        SourceControlUtils.deleteApiProjectInRepo(Utils.getTargetDirPath()
                + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + ZIP);
        Utils.delay(4000, "Interrupted while waiting for adapter to sync");

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);

        HttpResponse response = HttpsClientRequest.retryUntil404(
                Utils.getServiceURLHttps("/zip/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched");
    }
}
