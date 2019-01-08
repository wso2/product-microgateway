package org.wso2.micro.gateway.tests.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.micro.gateway.tests.common.*;
import org.wso2.micro.gateway.tests.common.HTTP2Server.MockHttp2Server;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.HTTP2Client.Http2ClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;
import java.io.File;
import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTPS_PORT;
import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTP_PORT;

public class HTTP2TestCase extends BaseTestCase {

    protected final static int MOCK_HTTP2_SERVER_PORT = 8443;
    protected final static int MOCK_HTTP2_SECURE_SERVER_PORT = 8080;
    private static final Log log = LogFactory.getLog(HTTP2TestCase.class);
    protected MockHttp2Server mockHttp2Server;
    protected MockHttp2Server mockHttp2SecureServer;
    protected Http2ClientRequest http2ClientRequest;
    private String jwtTokenProd;

    @BeforeClass
    private void setup() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        APIDTO api = new APIDTO();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        System.out.println(api.getEndpoint());

//api.setEndpoint("http://localhost:8443");

        //api.setEndpoint("http://localhost:8443"); //   https://localhost:9443/echo/http2 (micro-gw server)


        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);
        //set security schemas
        String security = "oauth2";

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        //Register a production token with key validation info
        KeyValidationInfo info = new KeyValidationInfo();
        info.setApi(api);
        info.setApplication(application);
        info.setAuthorized(true);
        info.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info.setSubscriptionTier("Unlimited");

        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();

        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project, security);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "http2-test.conf").getPath();
        String[] args = {"--config", configPath};
        microGWServer.startMicroGwServer(balPath, args);

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
    }


    @Test(description = "Test API invocation with an HTTP/2.0 request via insecure connection")
    public void testHTTP2ForInsecureConnection() throws Exception {

        //http2 server is started with ssl disabled
        boolean isOpen = Utils.isPortOpen(MOCK_HTTP2_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_HTTP2_SERVER_PORT + " already in use.");
        mockHttp2Server = new MockHttp2Server(MOCK_HTTP2_SERVER_PORT, false);
        mockHttp2Server.start();

        //http2 client is initialized with ssl disabled
        http2ClientRequest = new Http2ClientRequest(false, GATEWAY_LISTENER_HTTP_PORT, jwtTokenProd);
        http2ClientRequest.start();


    }

    @Test(description = "Test API invocation with an HTTP/2.0 request via secure connection")
    public void testHTTP2ForSecureConnection() throws Exception {

        //http2 server is started with ssl enabled
        boolean isOpen = Utils.isPortOpen(MOCK_HTTP2_SECURE_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_HTTP2_SECURE_SERVER_PORT + " already in use.");
        mockHttp2SecureServer = new MockHttp2Server(MOCK_HTTP2_SECURE_SERVER_PORT, true);
        mockHttp2SecureServer.start();

        //http2 client is initialized with ssl enabled
        http2ClientRequest = new Http2ClientRequest(true, GATEWAY_LISTENER_HTTPS_PORT, jwtTokenProd);
        http2ClientRequest.start();

    }


    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();

    }
}