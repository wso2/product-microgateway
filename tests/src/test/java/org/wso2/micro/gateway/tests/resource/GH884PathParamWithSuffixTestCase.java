package org.wso2.micro.gateway.tests.resource;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * If an resource is templated in following format with an extension (suffix starting with '.'),
 * Gateway should consider it as a valid resource and handle the requests properly.
 * <p>
 *     Ex:
 *     {@code GET '/petstore/pet/{petId}.api'}
 * </p>
 */
public class GH884PathParamWithSuffixTestCase extends BaseTestCase {
    private String jwtTokenProd;

    @BeforeClass
    public void start() throws Exception {
        String project = "param_suffix";
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        super.init(project, new String[]{"resource/884_path_param_suffix.yaml"});
    }


    @Test(description = "Test Invoking an suffixed API resource.")
    public void testInvokingSuffixedResource() throws Exception {
        HttpResponse response = sendRequest("petstore/v2/pet/1.api");
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test invoking a suffixed resource without the suffix.")
    public void testInvokingSuffixedResourceWithoutSuffix() throws Exception {
        HttpResponse response = sendRequest("petstore/v2/pet/1");
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND, "Response code mismatched");
    }

    private HttpResponse sendRequest(String path) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);

        return HttpClientRequest.doGet(getServiceURLHttp(path), headers);
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
