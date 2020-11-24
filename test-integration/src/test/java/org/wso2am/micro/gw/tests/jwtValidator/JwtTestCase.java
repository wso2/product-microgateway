package org.wso2am.micro.gw.tests.dummy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.DockerComposeContainer;
import org.wso2am.micro.gw.tests.IntegrationTest;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.util.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.wso2am.micro.gw.tests.common.BaseTestCase.getImportAPIServiceURLHttps;


@Category(IntegrationTest.class)
public class DummyTestCase extends BaseTestCase {


    @Before
    public void start() throws Exception {
        super.startSetup();

        String apiZipfile = "/home/chashika/Desktop/petstore.zip";
        String certificatesTrustStorePath = "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/security/cacerts";
        super.deployAPI(apiZipfile,certificatesTrustStorePath);
    }

    @Test
    @DisplayName("Test to check the JWT auth working")
    public void invokeJWTHeaderSuccessTest() throws Exception{

        String certificatesTrustStorePath = "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/security/cacerts";
        //System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.VALID_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/1") , headers, certificatesTrustStorePath);

        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched",response.getResponseCode(), HttpStatus.SC_OK);
    }

    @Test
    @DisplayName("Test to check the JWT auth validate token")
    public void invokeJWTHeaderInvalidTokenTest() throws Exception{

        String certificatesTrustStorePath = "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/security/cacerts";

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.INVALID_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/1") , headers, certificatesTrustStorePath);

        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched",response.getResponseCode(),
                TestConstant.INVALID_CREDENTIALS_CODE);
    }

    @Test
    @DisplayName("Test to check the JWT auth validate token")
    public void invokeJWTHeaderExpiredTokenTest() throws Exception{

        String certificatesTrustStorePath = "/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/security/cacerts";

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.EXPIRED_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/1") , headers, certificatesTrustStorePath);

        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched",response.getResponseCode(),
                TestConstant.INVALID_CREDENTIALS_CODE);
    }



}
