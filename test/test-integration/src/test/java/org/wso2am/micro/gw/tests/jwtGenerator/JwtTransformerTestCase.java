package org.wso2am.micro.gw.tests.jwtGenerator;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.util.HttpResponse;
import org.wso2am.micro.gw.tests.util.HttpsClientRequest;
import org.wso2am.micro.gw.tests.util.TestConstant;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JwtTransformerTestCase extends JwtGeneratorTestCase{

    @Test(description = "Test default jwt claim mapping")
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
        Assert.assertEquals(tokenBody.get("CUSTOM-CLAIM"), "admin",
                "The custom claim has not correctly mapped");
    }
}
