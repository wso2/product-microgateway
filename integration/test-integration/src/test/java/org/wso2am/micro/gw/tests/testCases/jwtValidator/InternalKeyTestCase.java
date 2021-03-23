package org.wso2am.micro.gw.tests.testCases.jwtValidator;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.util.*;

import java.util.HashMap;
import java.util.Map;

public class InternalKeyTestCase extends BaseTestCase {
    protected String internalKey;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        internalKey = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, true);
    }

    @Test(description = "Test to check the InternalKey is working")
    public void invokeInternalKeyHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", internalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test to check the internal key auth validate invalid signature key")
    public void invokeInternalKeyHeaderInvalidTokenTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", TestConstant.INVALID_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
    }

    @Test(description = "Test to check the internal key auth validate expired token")
    public void invokeExpiredInternalKeyTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Internal-Key", TestConstant.EXPIRED_INTERNAL_KEY_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

}
