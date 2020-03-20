/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.micro.gateway.tests.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BasicGrpcTestCase extends BaseTestCase {
    private GrpcServer grpcServer;
    private String jwtTokenProd;
    private String jwtTokenProdWithValidScopes;
    private String basicAuthToken;

    @BeforeClass
    public void start() throws Exception {
        String project = "GrpcProject";
        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("grpcApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        Map<String, String> claimMap = new HashMap<>();
        claimMap.put("scope", "scope_1");
        jwtTokenProdWithValidScopes = TokenUtil
                .getJwtWithCustomClaims(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600,
                        claimMap);
        String originalInput = "generalUser1:password";
        basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[]{"../protobuf/mgwProto/basicProto.proto"});

        grpcServer = new GrpcServer();
        grpcServer.start();
    }

    @Test(description = "Test BasicAuth Grpc Passthrough with transport security")
    public void testBasicGrpcPassthrough() throws Exception {
        String response = testGrpcService("localhost:9595", "sample-request");
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "response received :sample-request");
    }

    @Test(description = "Test Grpc Test cases with JWT token : success")
    public void testGrpcWithJwtSuccessScenario() throws Exception {
        String token = "Bearer " + jwtTokenProd;
        String response = testGrpcServiceWithJwt("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "response received :sample-request");
    }

    @Test(description = "Test Grpc Test cases with JWT token : success")
    public void testGrpcWithBasicSuccessScenario() throws Exception {
        String token = "Basic " + basicAuthToken;
        //todo: rename the method to more generic one since this is used to do the basic Authentication
        String response = testGrpcServiceWithJwt("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "response received :sample-request");
    }

    @Test(description = "Test Grpc Test cases with JWT token: failure")
    public void testGrpcWithJwtFailureScenario() throws Exception {
        //todo: Analyze the ballerina side error when the value is an empty string
        String token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9" +
                "lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String response = testGrpcServiceWithJwt("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "16:Invalid Credentials");
    }

    @Test(description = "Test Grpc with Scopes using JWT token: failure")
    public void testGrpcWithJwtScopesFailureScenario() throws Exception {
        String token = "Bearer " + jwtTokenProd;
        String response = testGrpcServiceWithScopes("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "7:The access token does not allow you to access the requested resource");
    }

    @Test(description = "Test Grpc with Scopes using JWT token: success")
    public void testGrpcWithJwtScopesSuccessScenario() throws Exception {
        String token = "Bearer " + jwtTokenProdWithValidScopes;
        String response = testGrpcServiceWithScopes("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "response received :sample-request");
    }

    @Test(description = "Test Grpc with Scopes using JWT token: success")
    public void testServiceLevelThrottling() throws Exception {
        String token = "Bearer " + jwtTokenProdWithValidScopes;
        String response = testServiceLevelThrottling("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "8:Message throttled out");
    }

    @Test(description = "Test Grpc with Scopes using JWT token: success")
    public void testMethodLevelThrottling() throws Exception {
        String token = "Bearer " + jwtTokenProdWithValidScopes;
        String response = testMethodLevelThrottling("localhost:9590", "sample-request", token);
        Assert.assertNotNull(response);
        Assert.assertEquals(response, "8:Message throttled out");
    }

    @AfterClass
    public void stop() throws Exception {
        grpcServer.stop();
        //Stop all the mock servers
        super.finalize();
    }

    public String testGrpcService(String targetUrl, String requestText) throws InterruptedException {
        // Create a communication channel to the server, known as a Channel.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl).useTransportSecurity().build();
        try {
            GrpcBlockingClient client = new GrpcBlockingClient(channel);
            return client.testCall(requestText);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public String testGrpcServiceWithJwt(String targetUrl, String requestText, String token)
            throws InterruptedException {
        // Create a communication channel to the server, known as a Channel.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl).usePlaintext().build();
        try {
            JwtAuthBlockingClient client = new JwtAuthBlockingClient(channel, token);
            return client.testCall(requestText);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public String testGrpcServiceWithScopes(String targetUrl, String requestText, String token)
            throws InterruptedException {
        // Create a communication channel to the server, known as a Channel.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl).usePlaintext().build();
        try {
            JwtAuthBlockingClient client = new JwtAuthBlockingClient(channel, token);
            return client.testCallWithScopes(requestText);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public String testServiceLevelThrottling (String targetUrl, String requestText, String token) throws
            InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl).usePlaintext().build();
        try {
            JwtAuthBlockingClient client = new JwtAuthBlockingClient(channel, token);
            for (int i=0; i< 30; i++) {
                client.testCall(requestText);
            }
            return client.testCall(requestText);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public String testMethodLevelThrottling (String targetUrl, String requestText, String token) throws
            InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(targetUrl).usePlaintext().build();
        try {
            ThrottlingBlockingClient client = new ThrottlingBlockingClient(channel, token);
            for (int i=0; i<6; i++) {
                client.testCallMethodThrottling(requestText);
            }
            return client.testCallMethodThrottling(requestText);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

}
