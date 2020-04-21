package org.wso2.micro.gateway.tests.jwtTransformer;

import io.netty.handler.codec.http.HttpHeaderNames;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;

import java.util.HashMap;
import java.util.Map;

import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;


/**
 * This test class is used to test jwt custom claims mapping transformer.
 */
public class JwtTransformerTestCase extends BaseTestCase {
    private String jwtTokenProdWithScopes;

    @BeforeClass
    public void start() throws Exception {
        String project = "jwtTransformerProject";
        //Define application info
        Map<String, Object> claimMap = new HashMap<>();
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        JSONArray scopes = new JSONArray();
        scopes.add("admin");
        scopes.add("write");
        scopes.add("read");
        claimMap.put("scp", scopes);

        jwtTokenProdWithScopes = TokenUtil.getJwtWithCustomClaimsTransformer(application, new JSONObject(),
                TestConstant.KEY_TYPE_PRODUCTION, 3600, claimMap);
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[]{"jwtTransformer/jwt_transformer.yaml", "mgw-JwtValueTransformer.jar"},
                null, "confs/jwt-transformer-test-config.conf");
    }

    @Test(description = "Test the jwt claims mapping key and value transformation functionality")
    public void testJWtTransformer() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProdWithScopes);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v1/pet/3"),
                        headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_VALID_JWT_TRANSFORMER);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
