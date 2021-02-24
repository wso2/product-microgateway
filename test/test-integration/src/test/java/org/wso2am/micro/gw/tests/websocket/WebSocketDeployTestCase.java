package org.wso2am.micro.gw.tests.websocket;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.ApiProjectGenerator;
import org.wso2am.micro.gw.tests.util.HttpResponse;
import org.wso2am.micro.gw.tests.util.HttpsPostMultipart;
import org.wso2am.micro.gw.tests.util.TestConstant;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebSocketDeployTestCase extends BaseTestCase {

    @BeforeClass(description = "initialise the setup")
    public void start() throws Exception {
        super.startMGW();
    }

    @AfterClass(description = "stop the setup")
    void stop() {
        super.stopMGW();
    }

    @Test(description = "Test to check websocket API deployment with apis/openApis/mockWebSocketApiProdSand.yaml")
    public void webSocketDeployTest() throws Exception{
        String apiZipFile = ApiProjectGenerator.createApictlProjZip(null,null,
                "apis/openApis/mockWebSocketAPIProdSand.yaml");
        // Set header
        Map<String, String> headers = new HashMap<String,String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic YWRtaW46YWRtaW4=");
        HttpsPostMultipart multipart = new HttpsPostMultipart(getImportAPIServiceURLHttps(
                TestConstant.ADAPTER_IMPORT_API_RESOURCE) , headers);
        multipart.addFilePart("file", new File(apiZipFile));
        HttpResponse response = multipart.getResponse();

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
