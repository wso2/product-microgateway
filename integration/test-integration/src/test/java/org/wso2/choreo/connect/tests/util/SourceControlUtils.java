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

package org.wso2.choreo.connect.tests.util;

import com.azure.core.http.ContentType;
import com.google.common.net.HttpHeaders;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceControlUtils {

    private static final Logger log = LoggerFactory.getLogger(SourceControlUtils.class);

    public static final String GIT_URL = "http://localhost:8285";
    public static final String GIT_API_URL = "http://localhost:8285/api/v1";
    public static final String GIT_HEALTH_URL = "http://localhost:8285/api/v1/topics/search?q=git";
    public static final String ARTIFACTS_DIR = File.separator + "git-artifacts";
    public static final String GIT_USERNAME = "username";
    public static final String GIT_PROJECT_NAME = "testProject";
    public static final String GIT_PROJECT_PATH = "testProject";
    public static final String GIT_PROJECT_BRANCH = "main";
    public static final String DIRECTORY = File.separator + "directory";

    private static final String accessToken = "556018e1140708b97d5f3d189055a37e89b9ba82";

    // filesAddedAndSHA contains the last commit sha of each file and used when updating and deleting files.
    private static final Map<String, String> filesAddedAndSHA = new HashMap<>();

    /**
     * Test the status of the Git service REST API with the accessToken.
     *
     * @throws IOException  If an error occurs while sending the GET request
     */
    public static void testGitStatus() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON);
        HttpResponse response = HttpClientRequest.doGet(GIT_API_URL + "/user/repos", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        log.info("REST API invoked successfully");
    }

    /**
     * Create a new repository owned by the authenticated user.
     *
     * @throws IOException  If an error occurs while sending the POST request
     */
    public static void createRepo() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);
        String postBody = "{\n" +
                "  \"auto_init\": true,\n" +
                "  \"name\": \"" + GIT_PROJECT_NAME + "\",\n" +
                "  \"private\": true\n" +
                "}";
        HttpResponse response = HttpClientRequest.doPost(GIT_API_URL + "/user/repos", postBody, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_CREATED);
        log.info("New project created at " + GIT_URL + "/" + GIT_USERNAME + "/" + GIT_PROJECT_PATH + ".git");
    }

    /**
     * Commit the initial set of files to the repository.
     *
     * @param artifactsDirPath  Directory that contains the project
     * @param isZip             Whether the project is a zip
     * @param commitMessage     Commit Message
     * @throws Exception if an error occurs while reading files or sending the POST request
     */
    public static void commitApiProjectToRepo(String artifactsDirPath, boolean isZip, String commitMessage) throws Exception {
        List<String> filePaths = new ArrayList<>();
        File artifactsDir = new File(artifactsDirPath);
        SourceControlUtils.getFiles(artifactsDir, filePaths);

        for (String filePath : filePaths) {
            String fileContentBase64;
            if (isZip) {
                byte[] bytes = FileUtils.readFileToByteArray(new File(filePath));
                fileContentBase64 = Utils.encodeValueToBase64(bytes);
            } else {
                String fileContent = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
                fileContentBase64 = Utils.encodeValueToBase64(fileContent);
            }
            commitFileWithPOST(filePath.replace(artifactsDirPath, ""), fileContentBase64,
                    commitMessage);
        }
    }

    /**
     * Update the already committed API project in repository.
     *
     * @param artifactsDirPath  Directory that contains the project
     * @param isZip             Whether the project is a zip
     * @throws Exception if an error occurs while sending the PUT request
     */
    public static void updateApiProjectInRepo(String artifactsDirPath, boolean isZip) throws Exception {
        List<String> filePaths = new ArrayList<>();
        File artifactsDir = new File(artifactsDirPath);
        SourceControlUtils.getFiles(artifactsDir, filePaths);

        for (String filePath : filePaths) {
            String fileContentBase64;
            if (isZip) {
                byte[] bytes = FileUtils.readFileToByteArray(new File(filePath));
                fileContentBase64 = Utils.encodeValueToBase64(bytes);
            } else {
                String fileContent = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
                fileContentBase64 = Utils.encodeValueToBase64(fileContent);
            }
            commitFileWithPUT(filePath.replace(artifactsDirPath, ""), fileContentBase64,
                    "Update artifacts");
        }
    }

    /**
     * Delete the already committed API project from repository.
     *
     * @param artifactsDirPath   Directory that contains the project
     * @throws Exception if an error occurs while sending the DELETE request
     */
    public static void deleteApiProjectInRepo(String artifactsDirPath) throws Exception {
        List<String> filePaths = new ArrayList<>();
        File artifactsDir = new File(artifactsDirPath);
        SourceControlUtils.getFiles(artifactsDir, filePaths);

        for (String filePath : filePaths){
            String pathInRepo = filePath.replace(artifactsDirPath, "");
            String sha = filesAddedAndSHA.get(pathInRepo);
            commitFilesWithDELETE(pathInRepo, sha, "Delete artifacts");
        }
    }

    /**
     * Commits a new file to the given repository.
     *
     * @param filePathInRepo     Relative path of the artifacts to set in path parameter
     * @param fileContentBase64  Absolute path of the artifacts directory to read from
     * @param commitMessage      Commit Message
     * @throws IOException       If an error occurs while reading the directory or committing the files
     */
    static void commitFileWithPOST(String filePathInRepo, String fileContentBase64, String commitMessage) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("branch", GIT_PROJECT_BRANCH);
        payload.put("message", commitMessage);
        payload.put("content", fileContentBase64);
        String payloadString = payload.toString();

        HttpResponse response = HttpClientRequest.doPost(GIT_API_URL + "/repos/" + GIT_USERNAME + "/"
                + GIT_PROJECT_NAME + "/contents" + filePathInRepo, payloadString, getHeaders());
        JSONObject commitResponse = new JSONObject(response.getData());
        JSONObject content = commitResponse.getJSONObject("content");
        String sha = content.getString("sha");
        filesAddedAndSHA.put(filePathInRepo, sha);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_CREATED);
        log.info("{} : Artifact {} added to Git repository successfully", commitMessage, filePathInRepo);
    }

    /**
     * Commits an updated file to the given repository.
     *
     * @param filePathInRepo     Relative path of the artifacts to set in path parameter
     * @param fileContentBase64  Absolute path of the artifacts directory to read from
     * @param commitMessage      Commit Message
     * @throws IOException       If an error occurs while reading the directory or committing the files
     */
    static void commitFileWithPUT(String filePathInRepo, String fileContentBase64,
                                  String commitMessage) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("branch", GIT_PROJECT_BRANCH);
        payload.put("message", commitMessage);
        payload.put("sha", filesAddedAndSHA.get(filePathInRepo));
        payload.put("content", fileContentBase64);
        String payloadString = payload.toString();

        HttpResponse response = HttpClientRequest.doPut(GIT_API_URL + "/repos/" + GIT_USERNAME + "/"
                + GIT_PROJECT_NAME + "/contents" + filePathInRepo, payloadString, getHeaders());
        JSONObject commitResponse = new JSONObject(response.getData());
        JSONObject content = commitResponse.getJSONObject("content");
        String sha = content.getString("sha");
        filesAddedAndSHA.put(filePathInRepo, sha);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        log.info("{} : Artifact {} updated in Git repository successfully", commitMessage, filePathInRepo);
    }

    /**
     * Commits a deleted file to the given repository.
     *
     * @param filePathInRepo     Relative path of the artifacts to set in path parameter
     * @param sha                Sha of the last commit of the file
     * @param commitMessage      Commit Message
     * @throws IOException       If an error occurs while reading the directory or committing the files
     */
    static void commitFilesWithDELETE(String filePathInRepo, String sha, String commitMessage) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("branch", GIT_PROJECT_BRANCH);
        payload.put("message", commitMessage);
        payload.put("sha", sha);
        String payloadString = payload.toString();

        HttpResponse response = HttpClientRequest.doDelete(GIT_API_URL + "/repos/" + GIT_USERNAME + "/"
                + GIT_PROJECT_NAME + "/contents" + filePathInRepo, payloadString, getHeaders());
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        log.info("{} : Artifact {} deleted from repository successfully", commitMessage, filePathInRepo);
    }

    static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Reads the given directory and adds the file paths to the given list
     *
     * @param directory     Path of the directory
     * @param filesList     List of file paths
     */
    public static void getFiles(File directory, List<String> filesList){
        File[] files = directory.listFiles();
        for (File file : files){
            if (file.isDirectory()){
                getFiles(file, filesList);
            } else {
                String filePath = file.getPath();
                filesList.add(filePath);
            }
        }
    }
}
