package org.wso2.choreo.connect.tests.testcases.standalone.endpoints;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

public class ProdSandDiffBasePathTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both endpoints provided")
    public void invokeProdSandEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/pet/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API level basepaths are different and " +
            "resource level basepaths are same")
    public void invokeProdSandResourceSameEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/pets/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/pets/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API level basepaths are different and " +
            "resource level basepaths are different")
    public void invokeProdSandResourceDiffEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/pet/findByTags"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/pet/findByTags"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API level basepaths are different and " +
            "resource level sandbox basepath is different")
    public void invokeProdSandResourceSandEndpoints() throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/v2/pets/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/v2/pets/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API level basepaths are different and " +
            "resource level production basepath is different")
    public void invokeProdSandResourceProdEndpoints() throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/v2/pets/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prodsand/v2/pets/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when only API level production basepath is given and " +
            "resource level production and sandbox basepaths are same")
    public void invokeProdResourceProdSandSameEndpoints() throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pets/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");

        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pets/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when only API level production basepath is given and " +
            "resource level production and sandbox basepaths are different")
    public void invokeProdResourceProdSandDiffEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pets/findByTags"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pets/findByTags"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when only API level production basepath is given and " +
            "only resource level production basepath is given")
    public void invokeProdResourceProdOnlyEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pet/findByTags"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pet/findByTags"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(
                sandResponse.getData().contains("Sandbox key offered to an API with no sandbox endpoint"));
    }

    @Test(description = "Invoke Production and Sandbox endpoint when only API level production basepath is given and " +
            "only resource level sandbox basepath is given")
    public void invokeProdResourceSandOnlyEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/v2/pets/findByTags"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/v2/pets/findByTags"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2);
    }
}
