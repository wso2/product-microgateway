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

package org.wso2.choreo.connect.tests.util;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.JWTClaimsConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.choreo.connect.mockbackend.Constants;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.wso2.choreo.connect.tests.util.ApictlUtils.API_PROJECTS_PATH;

/**
 * Utility class for test integration common functions.
 */
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    /**
     * Wait for port to open until given timeout period.
     *
     * @param port    The port that needs to be checked
     * @param timeout The timeout waiting for the port to open
     * @param verbose if verbose is set to true,
     * @throws RuntimeException if the port is not opened within the timeout
     */
    public static void waitForPort(int port, long timeout, boolean verbose, String hostName)
            throws RuntimeException {
        long startTime = System.currentTimeMillis();
        boolean isPortOpen = false;
        while (!isPortOpen && (System.currentTimeMillis() - startTime) < timeout) {
            Socket socket = null;
            try {
                InetAddress address = InetAddress.getByName(hostName);
                socket = new Socket(address, port);
                isPortOpen = socket.isConnected();
                if (isPortOpen) {
                    if (verbose) {
                        log.info("Successfully connected to the server on port " + port);
                    }
                    return;
                }
            } catch (IOException e) {
                if (verbose) {
                    log.info("Waiting until server starts on port " + port);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            } finally {
                try {
                    if ((socket != null) && (socket.isConnected())) {
                        socket.close();
                    }
                } catch (IOException e) {
                    log.error("Cannot close the socket with is used to check the server status ", e);
                }
            }
        }
        throw new RuntimeException("Port " + port + " is not open");
    }

    /**
     * wait until port is closed within given timeout value in mills.
     *
     * @param port    - port number
     * @param timeout - mat time to wait
     */
    public static void waitForPortToClosed(int port, int timeout) {
        long time = System.currentTimeMillis() + timeout;
        boolean portOpen = Utils.isPortOpen(port);
        while (portOpen && System.currentTimeMillis() < time) {
            // wait until server shutdown is completed
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                //ignore
            }
            portOpen = Utils.isPortOpen(port);
        }
        if (portOpen) {
            throw new RuntimeException("Port not closed properly when stopping server");
        }
    }

    /**
     * Check whether given port is in use or not.
     *
     * @param port - port number
     * @throws CCTestException if port is already in use
     */
    public static void checkPortAvailability(int port) throws CCTestException {

        //check whether http port is already occupied
        if (isPortOpen(port)) {
            throw new CCTestException("Unable to start carbon server on port " +
                                                   (port) + " : Port already in use");
        }
    }

    /**
     * Check whether the provided port is open.
     *
     * @param port The port that needs to be checked
     * @return true if the <code>port</code> is open & false otherwise
     */
    public static boolean isPortOpen(int port) {
        Socket socket = null;
        boolean isPortOpen = false;
        try {
            InetAddress address = InetAddress.getLocalHost();
            socket = new Socket(address, port);
            isPortOpen = socket.isConnected();
            if (isPortOpen) {
                log.info("Successfully connected to the server on port " + port);
            }
        } catch (IOException e) {
            log.info("Port " + port + " is closed and available for use");
            isPortOpen = false;
        } finally {
            try {
                if ((socket != null) && (socket.isConnected())) {
                    socket.close();
                }
            } catch (IOException e) {
                log.error("Can not close the socket with is used to check the server status ", e);
            }
        }
        return isPortOpen;
    }

    /**
     * Unzip a zip file into a given location.
     *
     * @param sourceFilePath - zip file need to extract
     * @param extractedDir   - destination path given file to extract
     * @throws IOException
     */
    public static void extractFile(String sourceFilePath, String extractedDir) throws IOException {
        FileOutputStream fileoutputstream = null;

        String fileDestination = extractedDir + File.separator;
        byte[] buf = new byte[1024];
        ZipInputStream zipinputstream = null;
        ZipEntry zipentry;
        try {
            zipinputstream = new ZipInputStream(new FileInputStream(sourceFilePath));

            zipentry = zipinputstream.getNextEntry();

            while (zipentry != null) {
                //for each entry to be extracted
                String entryName = fileDestination + zipentry.getName();
                entryName = entryName.replace('/', File.separatorChar);
                entryName = entryName.replace('\\', File.separatorChar);
                int n;

                File newFile = new File(entryName);
                boolean fileCreated = false;
                if (zipentry.isDirectory()) {
                    if (!newFile.exists()) {
                        fileCreated = newFile.mkdirs();
                    }
                    zipentry = zipinputstream.getNextEntry();
                    continue;
                } else {
                    File resourceFile =
                            new File(entryName.substring(0, entryName.lastIndexOf(File.separator)));
                    if (!resourceFile.exists()) {
                        if (!resourceFile.mkdirs()) {
                            break;
                        }
                    }
                }

                fileoutputstream = new FileOutputStream(entryName);

                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
                    fileoutputstream.write(buf, 0, n);
                }

                fileoutputstream.close();
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();

            }
            zipinputstream.close();
        } catch (IOException e) {
            log.error("Error on archive extraction ", e);
            throw new IOException("Error on archive extraction ", e);

        } finally {
            if (fileoutputstream != null) {
                fileoutputstream.close();
            }
            if (zipinputstream != null) {
                zipinputstream.close();
            }
        }
    }

    public static void zip(final String dirPathToZip, final String folderName) throws IOException {
        Path zipFile = Files.createFile(Paths.get(dirPathToZip + ".zip"));
        Path sourceDirPath = Paths.get(dirPathToZip);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile),
                StandardCharsets.UTF_8); Stream<Path> paths = Files.walk(sourceDirPath)) {
            paths
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {

                        ZipEntry zipEntry = new ZipEntry(
                                // example: dir/petstore.zip/petstore/api.yaml
                                // to get the correct structure when decompressed.
                                folderName + File.separator + sourceDirPath.relativize(path));
                        try {
                            zipOutputStream.putNextEntry(zipEntry);
                            Files.copy(path, zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            log.error("Error while creating zip for {}", sourceDirPath);
                        }
                    });
        }
        deleteFolder(new File(dirPathToZip));
    }

    /**
     * Return the system property value of os.name. System.getProperty("os.name").
     *
     * @return Operating System name
     */
    public static String getOSName() {
        return System.getProperty("os.name");
    }


    /**
     * Delete a given folder and all it's content.
     *
     * @param folder The folder to delete.
     */
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }

        folder.delete();
    }

    /**
     * Encode a value to base64 format
     *
     * @param value The value to be encoded.
     */
    public static String encodeValueToBase64(String value) throws Exception {
        return Base64.getEncoder().encodeToString(value.getBytes("utf-8"));
    }

    /**
     * Encode a byte array to base64 format
     *
     * @param value The value to be encoded.
     */
    public static String encodeValueToBase64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    /**
     * Invoke an API
     *
     * @param token      The token to be sent with the request header.
     * @param requestUrl The url to which the request should be sent.
     */
    public static HttpResponse invokeApi(String token, String requestUrl) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        return HttpClientRequest.retryGetRequestUntilDeployed(
                Utils.getServiceURLHttps(requestUrl), headers);
    }

    /**
     * Assert the result of a response
     *
     * @param response     The response object.
     * @param responseData The data which is expected as the response
     * @param responseCode The response code which is expected
     */
    public static void assertResult(HttpResponse response, String responseData, int responseCode) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), responseData);
        Assert.assertEquals(responseCode, response.getResponseCode(), "Response code mismatched");
    }

    /**
     * Invoke with GET and return exacted response as an EchoResponse from /echo-full endpoint.
     *
     * @param basePath      Context of the API
     * @param resourcePath  Resource to be invoked
     * @param headers       Headers to include in the request
     * @param jwtToken      Access token to include in the authorization header
     * @return exacted response as an EchoResponse
     * @throws Exception if an error occurs when invoking the API or extracting the response
     */
    public static EchoResponse invokeEchoGet(String basePath, String resourcePath,
                                             Map<String, String> headers, String jwtToken) throws Exception {
        HttpResponse response = invokeGet(basePath, resourcePath, headers, jwtToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return new Gson().fromJson(response.getData(), EchoResponse.class);
    }

    /**
     * Invoke with POST and return exacted response as an EchoResponse from /echo-full endpoint.
     *
     * @param basePath      Context of the API
     * @param resourcePath  Resource to be invoked
     * @param payload       Payload for the POST request
     * @param headers       Headers to include in the request
     * @param jwtToken      Access token to include in the authorization header
     * @return exacted response as an EchoResponse
     * @throws Exception if an error occurs when invoking the API or extracting the response
     */
    public static EchoResponse invokeEchoPost(String basePath, String resourcePath, String payload,
                                              Map<String, String> headers, String jwtToken) throws Exception {
        HttpResponse response = invokePost(basePath, resourcePath, payload, headers, jwtToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return new Gson().fromJson(response.getData(), EchoResponse.class);
    }

    /**
     * Extract the HttpResponse payload received after calling the endpoint /echo-full
     * into an EchoResponse object and return.
     *
     * @param response a HttpResponse
     * @return extracted EchoResponse
     */
    public static EchoResponse extractToEchoResponse(HttpResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return new Gson().fromJson(response.getData(), EchoResponse.class);
    }

    public static EchoResponse extractToEchoResponse(org.apache.http.HttpResponse response) throws IOException {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
        return new Gson().fromJson(EntityUtils.toString(response.getEntity()), EchoResponse.class);
    }

    public static EchoResponse extractToEchoResponse(java.net.http.HttpResponse<String> response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.statusCode(), HttpStatus.SC_OK, "Response code mismatched");
        return new Gson().fromJson(response.body(), EchoResponse.class);
    }

    /**
     * Send a GET request with provided headers and an authorization bearer token.
     *
     * @param basePath      Context of the API
     * @param resourcePath  Resource to be invoked
     * @param headers       Headers to include in the request
     * @param jwtToken      Access token to include in the authorization header
     * @return HttpResponse
     * @throws Exception if an error occurs when invoking the API
     */
    public static HttpResponse invokeGet(String basePath, String resourcePath,
                                          Map<String, String> headers, String jwtToken) throws Exception {
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        return HttpsClientRequest.doGet(Utils.getServiceURLHttps(basePath + resourcePath), headers);
    }

    /**
     * Send a POST request with provided headers and an authorization bearer token.
     *
     * @param basePath      Context of the API
     * @param resourcePath  Resource to be invoked
     * @param payload       Payload for the POST request
     * @param headers       Headers to include in the request
     * @param jwtToken      Access token to include in the authorization header
     * @return HttpResponse
     * @throws Exception if an error occurs when invoking the API
     */
    public static HttpResponse invokePost(String basePath, String resourcePath, String payload,
                                           Map<String, String> headers, String jwtToken) throws Exception {
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        return HttpsClientRequest.doPost(Utils.getServiceURLHttps(basePath + resourcePath), payload, headers);
    }

    /**
     * Delay the program for a given time period
     *
     * @param delayTime The time in milliseconds for the program to be delayed.
     */
    public static void delay(int delayTime, String msgIfInterrupted) {
        try {
            Thread.sleep(delayTime);
        } catch (InterruptedException ex) {
            Assert.fail(msgIfInterrupted);
        }
    }

    /**
     * Delete a file
     *
     * @param filePath file location.
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }

    /**
     * Delete a file or directory without throwing exception
     *
     * @param filePath file location.
     */
    public static boolean deleteQuietly(String filePath) {
        File file = new File(filePath);
        return FileUtils.deleteQuietly(file);
    }

    /**
     * Delay the program for a given time period
     *
     * @param sourceLocation file location.
     * @param destLocation   copy destination.
     * @throws CCTestException
     */
    public static void copyFile(String sourceLocation, String destLocation) throws CCTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            throw new CCTestException("error while copying file. ", e);
        }
    }

    public static void copyFile2(String sourceLocation, String destLocation) throws CCTestException {
        try {
            Path sourcePath = Paths.get(sourceLocation);
            Path destPath = Paths.get(destLocation);
            Files.copy(sourcePath,destPath,StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new CCTestException("error while copying file. ", e);
        }
    }


    /**
     * Delay the program for a given time period
     *
     * @param sourceLocation folder location.
     * @param destLocation   copy destination.
     * @throws CCTestException
     */
    public static void copyDirectory(String sourceLocation, String destLocation) throws CCTestException {
        File source = new File(sourceLocation);
        File destination = new File(destLocation);
        try {
            FileUtils.copyDirectory(source, destination);
        } catch (IOException e) {
            throw new CCTestException("error while copying directory. ");
        }
    }

    /**
     * Retrieve  the value from JSON object bu using the key.
     *
     * @param httpResponse - Response that containing the JSON object in it response data.
     * @param key          - key of the JSON value the need to retrieve.
     * @return String - The value of provided key as a String
     * @throws CCTestException - Exception throws when resolving the JSON object in the HTTP response
     */
    protected String getValueFromJSON(HttpResponse httpResponse, String key) throws CCTestException {
        try {
            JSONObject jsonObject = new JSONObject(httpResponse.getData());
            return jsonObject.get(key).toString();
        } catch (JSONException e) {
            throw new CCTestException("Exception thrown when resolving the JSON object in the HTTP response ", e);
        }
    }

    /**
     * Creates a set of conditional groups with a list of conditions
     *
     * @param limit Throttle limit of the conditional group.
     * @return Created list of conditional group DTO
     */
    public static List<ConditionalGroupDTO> createConditionalGroups(ThrottleLimitDTO limit, String throttledIP,
                                                                    String throttledHeader, String throttledQueryParam,
                                                                    String throttledQueryParamValue,
                                                                    String throttledClaim) {
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();

        // create an IP condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> ipGrp = new ArrayList<>();
        IPConditionDTO ipConditionDTO = DtoFactory.createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum.IPSPECIFIC,
                throttledIP, null, null);
        ThrottleConditionDTO ipCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.IPCONDITION, false, null, ipConditionDTO,
                        null, null);
        ipGrp.add(ipCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "IP conditional group", ipGrp, limit));

        // create a header condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> headerGrp = new ArrayList<>();
        HeaderConditionDTO headerConditionDTO =
                DtoFactory.createHeaderConditionDTO(HttpHeaders.USER_AGENT.toLowerCase(Locale.ROOT), throttledHeader);
        ThrottleConditionDTO headerCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.HEADERCONDITION, false, headerConditionDTO,
                        null, null, null);
        headerGrp.add(headerCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Header conditional group", headerGrp, limit));

        // create a query parameter condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> queryGrp = new ArrayList<>();
        QueryParameterConditionDTO queryParameterConditionDTO =
                DtoFactory.createQueryParameterConditionDTO(throttledQueryParam, throttledQueryParamValue);
        ThrottleConditionDTO queryParameterCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.QUERYPARAMETERCONDITION, false, null, null,
                        null, queryParameterConditionDTO);
        queryGrp.add(queryParameterCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Query param conditional group", queryGrp, limit));

        // create a JWT claims condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> claimGrp = new ArrayList<>();
        String claimUrl = "http://wso2.org/claims/applicationname";
        JWTClaimsConditionDTO jwtClaimsConditionDTO =
                DtoFactory.createJWTClaimsConditionDTO(claimUrl, throttledClaim);
        ThrottleConditionDTO jwtClaimsCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.JWTCLAIMSCONDITION, false, null, null,
                        jwtClaimsConditionDTO, null);
        claimGrp.add(jwtClaimsCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "JWT Claim conditional group", claimGrp, limit));

        return conditionalGroups;
    }

    /**
     * Gives the GraphQL schema path used in the sample GraphQL project
     *
     * @return File path of the GraphQL schema file
     */
    public static String getGraphQLSchemaPath() {
        String samplesDirPath = Utils.getCCSamplesDirPath();
        return samplesDirPath + API_PROJECTS_PATH + "SampleGraphQLApi" + File.separator +
                "Definitions" + File.separator + "schema.graphql";
    }

    public static String getDockerMockGraphQLServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://mockBackend:" + TestConstant.MOCK_GRAPHQL_SERVER_PORT), servicePath).toString();
    }

    public static String convertYamlToJson(String yamlString) {
        Yaml yaml= new Yaml();
        Object obj = yaml.load(yamlString);
        return JSONValue.toJSONString(obj);
    }

    public static JSONObject changeHeadersToLowerCase(JSONObject headers) {
        JSONObject headersCaseInsensitive = new JSONObject();
        Iterator it = headers.keys();

        while (it.hasNext()) {
            String keyRaw = (String) it.next();
            String key = keyRaw.toLowerCase();
            headersCaseInsensitive.put(key, headers.get(keyRaw));
        }
        return headersCaseInsensitive;
    }

    public static String getTargetDirPath() {
        File targetClassesDir = new File(Utils.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        return targetClassesDir.getParentFile().toString();
    }

    /**
     * Retrieves the path for cc sample API projects.
     *
     * @return String - samples directory path
     */
    public static String getCCSamplesDirPath(){
        File targetClassesDir = new File(Utils.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        return targetClassesDir.getParentFile().getParentFile().getParentFile().getParentFile().toString()
                + File.separator + "samples";
    }

    /**
     * @param responseHeaders HTTP response headers list
     * @param requiredHeader  header name as a string
     * @return the required HTTP header (if not found, null will be returned)
     */
    public static String pickHeader(Map<String, String> responseHeaders, String requiredHeader) {
        if (requiredHeader == null) {
            return null;
        }
        for (String headerName : responseHeaders.keySet()) {
            if (requiredHeader.equalsIgnoreCase(headerName)) {
                return headerName;
            }
        }
        return null;
    }

    public static String getAdapterServiceURLHttps(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + TestConstant.ADAPTER_PORT), servicePath)
                .toString();
    }

    public static String getServiceURLHttps(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_PORT), servicePath)
                .toString();
    }

    public static String getServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://localhost:" + TestConstant.GATEWAY_LISTENER_HTTP_PORT), servicePath)
                .toString();
    }

    public static String getServiceURLWebSocket(String servicePath) throws URISyntaxException {
        return new URI("ws://localhost:" + TestConstant.GATEWAY_LISTENER_HTTP_PORT + "/" + servicePath).toString();
    }

    public static String getMockServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://localhost:" + TestConstant.MOCK_SERVER_PORT), servicePath).toString();
    }

    public static String getDockerMockServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://mockBackend:" + TestConstant.MOCK_SERVER_PORT), servicePath).toString();
    }

    public static String getDockerMockService2URLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://mockBackend:" + TestConstant.MOCK_SERVER2_PORT), servicePath).toString();
    }

    public static String getMockInterceptorManagerHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://localhost:" + Constants.INTERCEPTOR_STATUS_SERVER_PORT), servicePath).toString();
    }

    public static String getAPIMServiceURLHttps(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + TestConstant.APIM_SERVLET_TRP_HTTPS_PORT), servicePath)
                .toString();
    }

    public static String getAPIMServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://localhost:" + TestConstant.APIM_SERVLET_TRP_HTTP_PORT), servicePath).toString();
    }

    public static String getDockerMockServiceURLHttp2ClearText(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://mockBackend2:" + TestConstant.MOCK_BACKEND_HTTP2_CLEAR_TEXT_SERVER_PORT), servicePath).toString();
    }

    public static String getDockerMockServiceURLHttp2Secured(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://mockBackend:" + TestConstant.MOCK_BACKEND_HTTP2_SECURED_SERVER_PORT), servicePath).toString();
    }
}
