package org.wso2.micro.gateway.tests.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;

import java.util.concurrent.TimeUnit;

public class BasicGrpcTestCase extends BaseTestCase {
    private GrpcServer grpcServer;
    @BeforeClass
    public void start() throws Exception {
        String project = "OpenApiThrottlingProject";
        String configPath = "confs/http2-test.conf";
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[]{"../protobuf/mgwProto/basicProto.proto"},
                new String[]{"--b7a.log.level=DEBUG"}, configPath);

        grpcServer = new GrpcServer();
        grpcServer.start();
    }

    @Test(description = "Test Basic Grpc Passthrough")
    public void testBasicGrpcPassthrough() throws Exception {
        String response = testGrpcService("localhost:9590", "sample-request");
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "response received :sample-request");
    }

    @AfterClass
    public void stop() throws Exception {
        grpcServer.stop();
        //Stop all the mock servers
        super.finalize();
    }

    public String testGrpcService(String targetUrl, String requestText) throws InterruptedException {
        // Create a communication channel to the server, known as a Channel.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl).usePlaintext().build();
        try {
            GrpcBlockingClient client = new GrpcBlockingClient(channel);
            String responseText = client.testCall(requestText);
            return responseText;
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

}
