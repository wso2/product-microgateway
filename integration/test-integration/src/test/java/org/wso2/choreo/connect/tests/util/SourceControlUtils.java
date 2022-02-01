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

package org.wso2.choreo.connect.tests.util;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.common.net.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.Callable;

public class SourceControlUtils {

    private static final Logger log = LoggerFactory.getLogger(SourceControlUtils.class);

    public static final String GIT_URL = "http://localhost";
    public static final String GIT_API_URL = "http://localhost/api/v4";
    public static final String GIT_HEALTH_URL = "http://localhost/-/liveness";
    public static final String ARTIFACTS_DIR = File.separator + "git-artifacts";
    public static final String GIT_USERNAME = "root";
    public static final String GIT_PASSWORD = "svcAdmin";
    public static final String GIT_PROJECT_NAME = "testProject";
    public static final String GIT_PROJECT_PATH = "testProject";
    public static final String GIT_PROJECT_BRANCH = "main";
    public static final String ADD_FILE = "Add";
    public static final String UPDATE_FILE = "Update";
    public static final String DELETE_FILE = "Delete";
    public static final String ZIP_EXT = ".zip";

    public static final String DIRECTORY = File.separator + "directory";
    public static final String ZIP = File.separator + "zip";
    public static final String UPDATE = File.separator + "update";

    private static String accessToken = "";

    /**
     * Get an access token for invoking the Gitlab REST API
     *
     * @return              Access token required to invoke the Gitlab REST API
     * @throws IOException  If an error occurs while sending the POST request
     */
    public static void generateAccessToken() throws IOException {
        String postBody = "grant_type=password&username=" + GIT_USERNAME + "&password=" + GIT_PASSWORD;
        Map<String, String> headers = new HashMap<String, String>(0);
        HttpResponse response = HttpClientRequest.doPost(GIT_URL + "/oauth/token", postBody, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        JSONObject tokenDataObject = new JSONObject(response.getData());
        accessToken = tokenDataObject.getString("access_token");
    }

    /**
     * Test the status of the Gitlab REST API with the accessToken - To be used before invoking the Gitlab REST API
     *
     * @throws IOException  If an error occurs while sending the GET request
     */
    public static void testGitStatus() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        HttpResponse response = HttpClientRequest.doGet(GIT_API_URL + "/projects", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        log.info("REST API invoked successfully");
    }

    /**
     * Create a new Gitlab project owned by the authenticated user
     *
     * @throws IOException  If an error occurs while sending the POST request
     */
    public static void createProject() throws IOException {
        String postBody = "name=" + GIT_PROJECT_NAME + "&path=" + GIT_PROJECT_PATH;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        HttpResponse response = HttpClientRequest.doPost(GIT_API_URL + "/projects", postBody, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_CREATED);
        log.info("New project created at " + GIT_URL + "/" + GIT_USERNAME + "/" + GIT_PROJECT_PATH + ".git");
    }

    /**
     * Commits files to the given Gitlab repository
     *
     * @param artifactsDirectoryPath    Path of the artifacts directory
     * @param commitMessage         Commit Message
     * @throws Exception
     */
    public static void commitFiles(String artifactsDirectoryPath, String commitMessage, Map<String, String> fileActions) throws Exception{
        JSONObject payload = new JSONObject();

        JSONArray actions = new JSONArray();
        File artifactsDir = new File(artifactsDirectoryPath);
        readArtifactsDirectory(artifactsDir, artifactsDirectoryPath, actions, fileActions);

        payload.put("branch", GIT_PROJECT_BRANCH);
        payload.put("commit_message", commitMessage);
        payload.put("actions", actions);
        String payloadString = payload.toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.put("Content-Type", "application/json");

        HttpResponse response = HttpClientRequest.doPost(GIT_API_URL + "/projects/" + GIT_USERNAME + "%2F" + GIT_PROJECT_PATH + "/repository/commits", payloadString, headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_CREATED);

        log.info("Artifacts committed to Git repository successfully");
    }

    /**
     * Reads the given directory
     *
     * @param directory                Path of the directory
     * @param baseDirectory            Path of the base directory (artifacts directory)
     * @param actions                  JSON Array of actions for committing the files to the repository
     * @throws FileNotFoundException   If an error occurs when reading the directory
     */
    public static void readArtifactsDirectory(File directory, String baseDirectory, JSONArray actions, Map<String, String> fileActions) throws Exception {
        List<String> filePaths = new ArrayList<>();
        getFiles(directory, filePaths);
        for (String filePath : filePaths){
            // Convert the filePath to a relative path
            String relativePath = filePath.replace(baseDirectory, ".");
            if (fileActions.containsKey(filePath)){
                String fileAction = fileActions.get(filePath);
                if (fileAction.equals(ADD_FILE)){
                    File addedFile = new File(filePath);
                    JSONObject action = addFile(addedFile, relativePath);
                    actions.put(action);
                } else if (fileAction.equals(UPDATE_FILE)){
                    File updatedFile = new File(filePath);
                    JSONObject action = updateFile(updatedFile, relativePath);
                    actions.put(action);
                } else if (fileAction.equals(DELETE_FILE)){
                    JSONObject action = deleteFile(relativePath);
                    actions.put(action);
                }
            }
        }
    }

    /**
     * Reads the given directory and adds the file paths to the given list
     *
     * @param directory               Path of the directory
     * @param filesList               List of file paths
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

    /**
     * Reads the content of the given file
     *
     * @param file
     * @return                          The content of the file
     * @throws FileNotFoundException    If any error occurs when reading the file
     */
    public static String readFile(File file) throws FileNotFoundException {
        String line = "";
        Scanner sc = new Scanner(file);
        StringBuffer buffer = new StringBuffer();
        while(sc.hasNextLine()){
            line = sc.nextLine();
            buffer.append(line + "\n");
        }
        return buffer.toString();
    }

    /**
     * Reads the given zip file in base64 format
     *
     * @param file
     * @return              The content of the file in base64 format
     * @throws IOException  If any error occurs when reading the file
     */
    public static String readZip(File file) throws IOException {
        String encodedBase64 = "";
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fileInputStreamReader.read(bytes);
        encodedBase64 = new String(Base64.encodeBase64(bytes));
        return encodedBase64;
    }

    /**
     * Creates a JSON object for adding a file to the repository
     *
     * @param file
     * @param filePath
     * @return
     * @throws IOException  If any error occurs when reading the file
     */
    public static JSONObject addFile(File file, String filePath) throws IOException {
        JSONObject action = new JSONObject();
        action.put("action", "create");
        action.put("file_path", filePath);
        String content = "";
        if (file.getName().endsWith(ZIP_EXT)){
            content = readZip(file);
            action.put("encoding", "base64");
        } else {
            content = readFile(file);
        }
        action.put("content", content);
        return action;
    }

    /**
     * Creates a JSON object for updating a file in the repository
     *
     * @param file
     * @param filePath
     * @return
     * @throws IOException  If any error occurs when reading the file
     */
    public static JSONObject updateFile(File file, String filePath) throws IOException {
        JSONObject action = new JSONObject();
        action.put("action", "update");
        action.put("file_path", filePath);
        String content = "";
        if (file.getName().endsWith(ZIP_EXT)){
            content = readZip(file);
            action.put("encoding", "base64");
        } else {
            content = readFile(file);
        }
        action.put("content", content);
        return action;
    }

    /**
     * Creates a JSON object for deleting a file from the repository
     *
     * @param filePath
     * @return
     */
    public static JSONObject deleteFile(String filePath){
        JSONObject action = new JSONObject();
        action.put("action", "delete");
        action.put("file_path", filePath);
        return action;
    }
}
