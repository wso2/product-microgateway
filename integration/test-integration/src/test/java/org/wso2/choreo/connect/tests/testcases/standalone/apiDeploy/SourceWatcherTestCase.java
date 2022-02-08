/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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
import org.wso2.choreo.connect.tests.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SourceWatcherTestCase {

    protected String jwtToken;

    @BeforeClass(description = "Initialize the setup")
    void start() throws Exception{
        jwtToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "read:pets",
                false);
    }

    @Test(description = "Test if artifacts added from source version control are deployed")
    public void AddAPIProjectTest() throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/findByStatus?status=available") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    void commitDeletedFiles() throws Exception {
        List<String> filePaths = new ArrayList<>();
        File artifactsDir = new File(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.DIRECTORY);
        SourceControlUtils.getFiles(artifactsDir, filePaths);
        Map<String, String> fileActions = new HashMap<>();

        for (String filePath : filePaths){
            fileActions.put(filePath, SourceControlUtils.DELETE_FILE);
        }

        SourceControlUtils.commitFiles(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.DIRECTORY, "Delete artifacts", fileActions);
        TimeUnit.SECONDS.sleep(4);
    }

    @Test(description = "Test if artifacts deleted from source version control are undeployed", dependsOnMethods = {"AddAPIProjectTest"})
    public void DeleteAPIProjectTest() throws Exception{
        commitDeletedFiles();
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/findByStatus?status=available") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched");
    }

    void commitZipArtifact() throws Exception {
        List<String> filePaths = new ArrayList<>();
        File artifactsDir = new File(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.ZIP);
        SourceControlUtils.getFiles(artifactsDir, filePaths);
        Map<String, String> fileActions = new HashMap<>();

        for (String filePath : filePaths){
            fileActions.put(filePath, SourceControlUtils.ADD_FILE);
        }

        SourceControlUtils.commitFiles(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.ZIP, "Add zip artifacts", fileActions);
        TimeUnit.SECONDS.sleep(4);
    }

    @Test(description = "Test if zip artifacts added from source version control are deployed", dependsOnMethods = {"DeleteAPIProjectTest"})
    public void AddAPIZipProjectTest() throws Exception{
        commitZipArtifact();
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/store/inventory") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    void commitUpdatedArtifact() throws Exception {
        List<String> filePaths = new ArrayList<>();
        File artifactsDir = new File(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.UPDATE);
        SourceControlUtils.getFiles(artifactsDir, filePaths);
        Map<String, String> fileActions = new HashMap<>();

        for (String filePath : filePaths){
            fileActions.put(filePath, SourceControlUtils.UPDATE_FILE);
        }

        SourceControlUtils.commitFiles(Utils.getTargetDirPath() + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.UPDATE, "Update zip artifacts", fileActions);
        TimeUnit.SECONDS.sleep(4);
    }

    @Test(description = "Test if zip artifacts updated from source version control are deployed", dependsOnMethods = {"AddAPIZipProjectTest"})
    public void UpdateAPIZipProjectTest() throws Exception{
        commitUpdatedArtifact();
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/store/inventory") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched");
    }
}
