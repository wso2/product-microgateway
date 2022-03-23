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

import java.io.File;

/**
 * Constants used in test cases.
 */
public class TestConstant {

    public static final String CHARSET_NAME = "UTF-8";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String HTTP_METHOD_HEAD = "HEAD";
    public static final String ADAPTER_APIS_RESOURCE = "/api/mgw/adapter/0.1/apis";

    public static final String KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String KEY_TYPE_SANDBOX = "SANDBOX";
    public static final int DEPLOYMENT_WAIT_TIME = 15000;

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

    public static final String EXPIRED_INTERNAL_KEY_TOKEN = "eyJraWQiOiJnYXRld2F5X2NlcnRpZmljYXRlX2FsaWFzIiwi" +
            "YWxnIjoiUlMyNTYifQ.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0Ojk0NDNc" +
            "L29hdXRoMlwvdG9rZW4iLCJrZXl0eXBlIjoiUFJPRFVDVElPTiIsInN1YnNjcmliZWRBUElzIjpbeyJzdWJzY3JpYmVyVGVuYW5" +
            "0RG9tYWluIjpudWxsLCJuYW1lIjoiUGl6emFTaGFja0FQSSIsImNvbnRleHQiOiJcL3Bpenphc2hhY2tcLzEuMC4wIiwicHVib" +
            "GlzaGVyIjoiYWRtaW4iLCJ2ZXJzaW9uIjoiMS4wLjAiLCJzdWJzY3JpcHRpb25UaWVyIjpudWxsfV0sImV4cCI6MTYxNTk1NTI1" +
            "OSwidG9rZW5fdHlwZSI6IkludGVybmFsS2V5IiwiaWF0IjoxNjE1ODk1MjU5LCJqdGkiOiI3YTQ2MmZhNy1hNGE2LTQ2Y2ItOTQ3" +
            "My0yZGQ5MTM1YzM1NmQifQ.iYNnaFrkZqw6JexLYDz8O68iueUoeDnCVpsCpKXabgOj1eLUaSrwDeW_Blg2fDkSO-NA_V9ESmtBXYg" +
            "UpE_gDNy8jRRyc-PMt30zphkiqBcyxU2vhEqRz9ne37yVGlq4hLWrlaRDZNIzj1PniLGA7Y_fLiuq_SvLoh6Zz4tm-p4eIeDunaDkK" +
            "lFn0V_NFOTKiTwue6WAJutP2gEy3Fd5__cxaxjZ6GUbntVmddMhCQk0-0HN5843CzpSVhW8OpAi9QOCKC6HIq8XnJHzyw06juQOKD" +
            "PCi2qmQ-LlCQlBQrA4ECIFBzA_IqihOwymTPJHuqxPhGkJx1WfQ1oSb03Lxw";

    public static final String EXPIRED_API_KEY_TOKEN = "eyJ4NXQiOiJOVGRtWmpNNFpEazNOalkwWXpjNU1tWm1PRGd3TVRFM01XWXd" +
            "OREU1TVdSbFpEZzROemM0WkE9PSIsImtpZCI6ImdhdGV3YXlfY2VydGlmaWNhdGVfYWxpYXMiLCJ0eXAiOiJKV1QiLCJhbGciOiJSU" +
            "zI1NiJ9.eyJzdWIiOiJhZG1pbkBjYXJib24uc3VwZXIiLCJhcHBsaWNhdGlvbiI6eyJvd25lciI6ImFkbWluIiwidGllclF1b3RhVH" +
            "lwZSI6bnVsbCwidGllciI6IlVubGltaXRlZCIsIm5hbWUiOiJBUElLZXlUZXN0QXBwIiwiaWQiOjE0LCJ1dWlkIjoiNGZlZjM5NDIt" +
            "NjcyNy00NjA5LWFhMzctYzY2MWEwOTQ0NmFhIn0sImlzcyI6Imh0dHBzOlwvXC9hcGltOjk0NDNcL29hdXRoMlwvdG9rZW4iLCJ0aW" +
            "VySW5mbyI6eyJVbmxpbWl0ZWQiOnsidGllclF1b3RhVHlwZSI6InJlcXVlc3RDb3VudCIsImdyYXBoUUxNYXhDb21wbGV4aXR5Ijow" +
            "LCJncmFwaFFMTWF4RGVwdGgiOjAsInN0b3BPblF1b3RhUmVhY2giOnRydWUsInNwaWtlQXJyZXN0TGltaXQiOjAsInNwaWtlQXJyZX" +
            "N0VW5pdCI6bnVsbH19LCJrZXl0eXBlIjoiUFJPRFVDVElPTiIsInN1YnNjcmliZWRBUElzIjpbeyJzdWJzY3JpYmVyVGVuYW50RG9t" +
            "YWluIjoiY2FyYm9uLnN1cGVyIiwibmFtZSI6IkFQSUtleVRlc3RBUEkiLCJjb250ZXh0IjoiXC9hcGlLZXlcLzEuMC4wIiwicHVibG" +
            "lzaGVyIjoiYWRtaW4iLCJ2ZXJzaW9uIjoiMS4wLjAiLCJzdWJzY3JpcHRpb25UaWVyIjoiVW5saW1pdGVkIn1dLCJleHAiOjE2MzI3" +
            "NDA5MjgsInRva2VuX3R5cGUiOiJhcGlLZXkiLCJpYXQiOjE2MzI3NDA5MjcsImp0aSI6ImRlYTJjY2RhLTNjZTctNGYzOC04MDQ4LT" +
            "I1NzMwYjk0NWI3YiJ9.M_EpRWG8Bd1ekIUNz8mAtoeIvD-AMIoCZjAubNidFX-NXxFAkCmspLXvDwle9Do_tVMYZoEWjUAfHDqJWo8" +
            "TaLYjSc3goYvHoHAnv_0L0KQaveJYgv7nSQJ343bRVjDocjRZ0-iRY7CryoupoHPd1i0MGTqlWU04JUOadYhtwSUmw7PXnC-wXHGwb" +
            "UmONQ6_gbAAzEOEXD2K6yTfy16KeZvQTdv-zZJzb6ULi0kAuG4txJh38WMagHtu3fuhKm2KIDrflOZEKWmThU6HqB8aoxROABDlId1" +
            "VS_RbajyF8WfeZDv7W_tqNYbKbJVXxSJFFKfPeqL1A1N5po1dfLByPA==";

    public static final int GATEWAY_LISTENER_HTTPS_PORT = 9095;
    public static final int GATEWAY_LISTENER_HTTP_PORT = 9090;
    public static final int ADAPTER_PORT = 9843;
    public final static int MOCK_SERVER_PORT = 2383;
    public final static int MOCK_SERVER2_PORT = 2390;
    public final static int APIM_SERVLET_TRP_HTTPS_PORT = 9443;
    public final static int APIM_SERVLET_TRP_HTTP_PORT = 9763;
    public static final String MOCK_BACKEND_BASEPATH = "/v2";

    public static final int INVALID_CREDENTIALS_CODE = 900901;
    public static final String RESOURCE_FORBIDDEN_CODE = "900908";

    public static final String LINE = "\r\n";

    public static final String TEST_RESOURCES_PATH = File.separator + "test-classes";
    public static final String CONFIGS_DIR = File.separator + "configs";
    public static final String CONF_DIR = File.separator + "conf";
    public static final String DATABASE_DIR = File.separator + "database";
    public static final String TEST_DOCKER_COMPOSE_DIR = File.separator + "dockerCompose";
    public static final String DOCKER_COMPOSE_DIR = File.separator + "docker-compose";
    public static final String RESOURCES_DIR = File.separator + "resources";
    public static final String SECURITY_DIR = File.separator + "security";
    public static final String CC_TEMP_PATH = File.separator + "choreo-connect-temp";

    public static final String CONFIG_TOML_PATH = CONF_DIR + File.separator + "config.toml";
    public static final String DEPLYMNT_TOML_PATH = CONF_DIR + File.separator + "deployment.toml";
    public static final String DOCKER_COMPOSE_YAML_PATH = File.separator + "docker-compose.yaml";
    public static final String CA_CERTS_FILE = File.separator + "ca-certificates.crt";

    public static final String ROUTER_TRUSTSTORE_DIR = RESOURCES_DIR + File.separator + "router"
            + SECURITY_DIR + File.separator + "truststore";
    public static final String ENFORCER_TRUSTSTORE_DIR = RESOURCES_DIR + File.separator + "enforcer"
            + SECURITY_DIR + File.separator + "truststore";
    public static final String DOCKER_COMPOSE_CC_DIR = DOCKER_COMPOSE_DIR + File.separator + "choreo-connect";
    public static final String DROPINS_FOLDER_PATH = DOCKER_COMPOSE_DIR + RESOURCES_DIR
            + File.separator + "enforcer" + File.separator + "dropins";
    public static final String STARTUP_APIS_DIR = RESOURCES_DIR + File.separator
            + "adapter" + File.separator + "artifacts" + File.separator + "apis";

    public static final String HEALTH_ENDPOINT_RESPONSE = "{\"status\": \"healthy\"}";

    // apim related constants
    public static final String APIM_SERVICE_NAME_IN_DOCKER_COMPOSE = "apim";
    public static final String SUPER_TENANT_DOMAIN = "carbon.super";
    public static final String DEFAULT_TOKEN_VALIDITY_TIME = "36000";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String LOCAL_HOST_NAME = "localhost";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    //apim instances names
    public static final String AM_PRODUCT_GROUP_NAME = "APIM";
    public static final String AM_ALL_IN_ONE_INSTANCE = "all-in-one";

    public static final class THROTTLING {
        public static final String ADVANCED = "advanced";
        public static final String APPLICATION = "application";
        public static final String SUBSCRIPTION = "subscription";
    }

    public static final class APIM_INTERNAL_ROLE {
        public static final String SUBSCRIBER = "Internal/subscriber";
        public static final String PUBLISHER = "Internal/publisher";
        public static final String CREATOR = "Internal/creator";
        public static final String EVERYONE = "Internal/everyone";

        public APIM_INTERNAL_ROLE() {
        }
    }

    public static class GRANT_TYPE {
        public static final String PASSWORD = "password";
        public static final String CLIENT_CREDENTIAL = "client_credentials";
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String REFRESH_CODE = "refresh_token";
        public static final String SAML2 = "urn:ietf:params:oauth:grant-type:saml2-bearer";
        public static final String NTLM = "iwa:ntlm";
        public static final String IMPLICIT = "implicit";
        public static final String JWT = "urn:ietf:params:oauth:grant-type:jwt-bearer";

        public GRANT_TYPE() {
        }
    }

    public static class RESOURCE_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String TENK_PER_MIN = "10KPerMin";
        public static final String TWENTYK_PER_MIN = "20KPerMin";
        public static final String FIFTYK_PER_MIN = "50KPerMin";
        public static final int ULTIMATE_LIMIT = 20;
        public static final int PLUS_LIMIT = 5;
        public static final int BASIC_LIMIT = 1;

        public RESOURCE_TIER() {
        }
    }

    public static class API_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String GOLD = "Gold";
        public static final String SILVER = "Silver";
        public static final String BRONZE = "Bronze";
        public static final String ASYNC_UNLIMITED = "AsyncWHUnlimited";
        public static final int GOLD_LIMIT = 20;
        public static final int SILVER_LIMIT = 5;
        public static final int BRONZE_LIMIT = 1;

        public API_TIER() {
        }
    }

    public static final class APPLICATION_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String LARGE = "Large";
        public static final String MEDIUM = "Medium";
        public static final String SMALL = "Small";
        public static final String TEN_PER_MIN = "10PerMin";
        public static final int LARGE_LIMIT = 20;
        public static final int MEDIUM_LIMIT = 5;
        public static final int SMALL_LIMIT = 1;
        public static final String DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN = "50PerMin";

        public APPLICATION_TIER() {
        }
    }

    public static final class SUBSCRIPTION_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String GOLD = "Gold";
        public static final String SILVER = "Silver";
        public static final String BRONZE = "Bronze";
        public static final String UNAUTHENTICATED = "Unauthenticated";

        public SUBSCRIPTION_TIER() {
        }
    }

    public static final class SRARTUP_TEST {
        public static final String API_NAME = "ApiBeforeStartingCC";
        public static final String API_CONTEXT = "before_starting_CC";
        public static final String API_VERSION = "1.0.0";
        public static final String APP_NAME = "AppBeforeStartingCC";
    }
}
