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

import java.io.File;

/**
 * Constants used in test cases.
 */
public class TestConstant {

    public static final String CHARSET_NAME = "UTF-8";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String HTTP_METHOD_HEAD = "HEAD";
    public static final String ADAPTER_IMPORT_API_RESOURCE = "/api/mgw/adapter/0.1/apis";

    public static final String KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String KEY_TYPE_SANDBOX = "SANDBOX";

    public static final String INVALID_JWT_TOKEN = "eyJ4NXQiOiJNell4TW1Ga09HWXdNV0kwWldObU5EY3hOR1l3WW1NNFpUQTNN" +
            "V0kyTkRBelpHUXpOR00wWkdSbE5qSmtPREZrWkRSaU9URmtNV0ZoTXpVMlpHVmxOZyIsImtpZCI6Ik16WXhNbUZrT0dZd01XST" +
            "BaV05tTkRjeE5HWXdZbU00WlRBM01XSTJOREF6WkdRek5HTTBaR1JsTmpKa09ERmtaRFJpT1RGa01XRmhNelUyWkdWbE5nX1JTM" +
            "jU2IiwiYWxnIjoiUlMyNTYifQ==.eyJhdWQiOiJBT2syNFF6WndRXzYyb2QyNDdXQnVtd0VFZndhIiwic3ViIjoiYWRtaW5AY2F" +
            "yYm9uLnN1cGVyIiwibmJmIjoxNTk2MDA5NTU2LCJhenAiOiJBT2syNFF6WndRXzYyb2QyNDdXQnVtd0VFZndhIiwic2NvcGUiOiJ" +
            "hbV9hcHBsaWNhdGlvbl9zY29wZSBkZWZhdWx0IiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9vYXV0aDIvdG9rZW4iLCJr" +
            "ZXl0eXBlIjoiUFJPRFVDVElPTiIsImV4cCI6MTYyNzU0NTU1NiwiaWF0IjoxNTk2MDA5NTU2LCJqdGkiOiIyN2ZkMWY4Ny01ZTI1" +
            "LTQ1NjktYTJkYi04MDA3MTFlZTJjZWMifQ==.otDREOsUUmXuSbIVII7FR59HAWqtXh6WWCSX6NDylVIFfED3GbLkopo6rwCh2EX6" +
            "yiP-vGTqX8sB9Zfn784cIfD3jz2hCZqOqNzSUrzamZrWui4hlYC6qt4YviMbR9LNtxxu7uQD7QMbpZQiJ5owslaASWQvFTJgBmss5" +
            "t7cnurrfkatj5AkzVdKOTGxcZZPX8WrV_Mo2-rLbYMslgb2jCptgvi29VMPo9GlAFecoMsSwywL8sMyf7AJ3y4XW5Uzq7vDGxojD" +
            "am7jI5W8uLVVolZPDstqqZYzxpPJ2hBFC_OZgWG3LqhUgsYNReDKKeWUIEieK7QPgjetOZ5Geb1mA==sdsds";
    public static final String EXPIRED_JWT_TOKEN = "eyJ4NXQiOiJNell4TW1Ga09HWXdNV0kwWldObU5EY3hOR1l3WW1NNFpUQTNNV0" +
            "kyTkRBelpHUXpOR00wWkdSbE5qSmtPREZrWkRSaU9URmtNV0ZoTXpVMlpHVmxOZyIsImtpZCI6Ik16WXhNbUZrT0dZd01XSTBaV05" +
            "tTkRjeE5HWXdZbU00WlRBM01XSTJOREF6WkdRek5HTTBaR1JsTmpKa09ERmtaRFJpT1RGa01XRmhNelUyWkdWbE5nX1JTMjU2Iiwi" +
            "YWxnIjoiUlMyNTYifQ.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhdXQiOiJBUFBMSUNBVElPTiIsImF1ZCI6IkJwM01oZUY" +
            "0NWxpS096Q2RuVEZVREI0THVjZ2EiLCJuYmYiOjE2MDYwNDM3NDEsImF6cCI6IkJwM01oZUY0NWxpS096Q2RuVEZVREI0THVjZ2EiL" +
            "CJzY29wZSI6ImFtX2FwcGxpY2F0aW9uX3Njb3BlIGRlZmF1bHQiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0Ojk0NDNcL29hdX" +
            "RoMlwvdG9rZW4iLCJleHAiOjE2MDYwNDczNDEsImlhdCI6MTYwNjA0Mzc0MSwianRpIjoiY2NhZWExNjAtMWNlOS00Y2VmLWI4YWQtM" +
            "zJmY2M3NWE4ODk5In0.vy4REXHFnqVSeWxka4f8EPRIocFtq6WI_ayMLCI7P0vAhB1rBenqvgSmE_H_FxRxE1h_tLFm5m1dtyXPvskf" +
            "mb2p0n88p_aqm3jQizxTo-Hd5CfHDMqr1ylovSjp81UI5aoniF9-aFND2TD4Povz8wUeZIQopMPyPKCSXPeW1b7leD1ROqhhvqWBm9-" +
            "-CGdjRlPII2dMR3SYkuoGMQyoCOP2j2pAiP01Q8VseGV9CXZBciDHjCPv-pnP_oTAWCLjqCzFw-fG3Z3C_euEfN2KhMu520UfGuBKz2" +
            "KcdFXFwDUzpLfyAsg8qHDGiMcM88sUc10cvMYQqRYw66SF3EqYWQ";

    public static final int GATEWAY_LISTENER_HTTPS_PORT = 9095;
    public static final int ADAPTER_IMPORT_API_PORT = 9843;
    public final static int MOCK_SERVER_PORT = 2383;


    public static final String BASE_RESOURCE_DIR = "src" + File.separator + "test" + File.separator + "resources";

    public static final int INVALID_CREDENTIALS_CODE = 900901;

    public static final String LINE = "\r\n";
    public static final String MOCK_BACKEND_DOCKER_IMAGE = "wso2/mg-mock-backend";

    public static final int MOCK_WEB_SOCKET_PORT = 2395;
    public static final int SECURE_MOCK_WEB_SOCKET_PORT = 2396;
    public static final String MOCK_WEBSOCKET_HELLO = "Hello";
    public static final int MOCK_WEBSOCKET_CONNECT_TIMEOUT = 5000;
    public static final int MOCK_WEBSOCKET_ECHO_CHECK_INTERVAL = 1000;
}
