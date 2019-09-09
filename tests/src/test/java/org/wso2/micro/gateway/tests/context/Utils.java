/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.context;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
                    log.error("Can not close the socket with is used to check the server status ", e);
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
     * @throws MicroGWTestException if port is already in use
     */
    public static void checkPortAvailability(int port) throws MicroGWTestException {

        //check whether http port is already occupied
        if (isPortOpen(port)) {
            throw new MicroGWTestException("Unable to start carbon server on port " +
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

    /**
     * Return the system property value of os.name.
     * System.getProperty("os.name").
     *
     * @return Operating System name
     */
    public static String getOSName() {
        return System.getProperty("os.name");
    }

    /**
     * Copy a file.
     *
     * @param source The source file
     * @param target The target file
     * @throws MicroGWTestException if copying failed
     */
    public static void copyFile(Path source, Path target) throws MicroGWTestException {
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            throw new MicroGWTestException("Error copying file " + source + " to " + target, e);
        }
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
     * Invoke an API
     *
     * @param token      The token to be sent with the request header.
     * @param requestUrl The url to which the request should be sent.
     */
    public static HttpResponse invokeApi(String token, String requestUrl) throws Exception {
        Map<String, String> headers = new HashMap<>();
        //invoke api with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpClientRequest
                .doGet(requestUrl, headers);
        return response;
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
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    /**
     * Delay the program for a given time period
     *
     * @param delayTime The time in milliseconds for the program to be delayed.
     */
    public static void delay(int delayTime) {
        try {
            Thread.sleep(delayTime);
        } catch (InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }
    }
}
