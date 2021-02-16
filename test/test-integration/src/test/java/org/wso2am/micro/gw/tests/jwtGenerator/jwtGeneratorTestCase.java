package org.wso2am.micro.gw.tests.jwtGenerator;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.mockbackend.ResponseConstants;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.common.model.ApplicationDTO;
import org.wso2am.micro.gw.tests.mockbackend.MockBackendServer;
import org.wso2am.micro.gw.tests.util.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Jwt generator test cases.
 *
 */
public class jwtGeneratorTestCase extends BaseTestCase {
    private static String JWT_GENERATOR_ISSUER = "wso2.org/products/am";

    protected String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        super.startMGW();

        //deploy the api
        //api yaml file should put to the resources/apis/openApis folder
        String apiZipfile = ApiProjectGenerator.createApictlProjZip("/apis/openApis/mockApi.yaml");

        ApiDeployment.deployAPI(apiZipfile);

        //generate JWT token from APIM
        API api = new API();
        api.setName("PetStoreAPI");
        api.setContext("petstore/v1");
        api.setProdEndpoint(getMockServiceURLHttp("/echo/prod"));
        api.setVersion("1.0.0");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600,null);

    }

    @AfterClass(description = "stop the setup")
    void stop() {
        super.stopMGW();
    }

    @Test(description = "Test the availability of JWT Generator header")
    public void testResponseJWTGenerationHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttps("v2/jwtheader"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.VALID_JWT_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test JWT Generator token cache and the properties")
    public void testResponseJWTGenerationToken() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/jwttoken") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        JSONObject responseJSON = new JSONObject(response.getData());
        String tokenFull = responseJSON.get("token").toString();
        String strTokenBody = tokenFull.split("\\.")[1];
        String decodedTokenBody = new String(Base64.getUrlDecoder().decode(strTokenBody));
        JSONObject tokenBody = new JSONObject(decodedTokenBody);

        Assert.assertEquals(tokenBody.get("iss"), JWT_GENERATOR_ISSUER,
                "Issuer is  not set correctly in JWT generator");
        Assert.assertEquals(tokenBody.get("keytype"), TestConstant.KEY_TYPE_PRODUCTION,
                "Key type is not set correctly in JWT generator");
    }
}
