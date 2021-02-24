package org.wso2am.micro.gw.tests.jwtGenerator;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.common.model.ApplicationDTO;
import org.wso2am.micro.gw.tests.util.*;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CustomJwtTransformerTestCase extends BaseTestCase {
    protected String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        String confPath = TestConstant.BASE_RESOURCE_DIR
                + File.separator + "jwtGenerator" + File.separator + "config.toml";
        super.startMGW(confPath, false, true);

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

    @Test(description = "Test custom jwt claim mapping")
    public void testDefaultJwtClaimMapping() throws Exception {
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
        Assert.assertEquals(tokenBody.get("CustomClaim: CUSTOM-CLAIM"), "admin",
                "The custom claim has not correctly mapped");
    }
}
