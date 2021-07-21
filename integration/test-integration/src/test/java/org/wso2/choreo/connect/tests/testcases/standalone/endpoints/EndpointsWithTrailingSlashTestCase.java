package org.wso2.choreo.connect.tests.testcases.standalone.endpoints;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EndpointsWithTrailingSlashTestCase {
    private static final String encodedCredentials = "Basic YWRtaW46YWRtaW4=";

    @BeforeClass
    public void createApiProject() throws IOException, CCTestException {
        ApictlUtils.createProject( "trailing_slash_in_endpoint_openAPI.yaml", "ep_petstore", null, null);
    }

    @Test
    public void deployAPI() throws CCTestException {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("ep_petstore", "test");
    }

    @Test
    public void invokeAPI() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), encodedCredentials);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/testkey") ,"scope=read:pets",  headers);
        String token = response.getData();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/new/pet/10") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
