/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.common.model.SubscribedApiDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Base test class for CLI based tests
 */
public class BaseTestCase {
    protected ServerInstance microGWServer;
    protected CLIExecutor cliExecutor;
    protected MockHttpServer mockHttpServer;

    public void init(String label) throws Exception {
        microGWServer = ServerInstance.initMicroGwServer(TestConstant.GATEWAY_LISTENER_PORT);
        String cliHome = microGWServer.getServerHome();

        mockHttpServer = new MockHttpServer(9443);
        mockHttpServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label);

        String balPath = CLIExecutor.getInstance().getLabelBalx(label);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "default-test-config.conf").getPath();
        String[] args = { "--config", configPath };
        microGWServer.startMicroGwServer(balPath, args);
    }

    public void finalize() throws Exception {
        mockHttpServer.stopIt();
        microGWServer.stopServer(false);
        MockAPIPublisher.getInstance().clear();
    }

    protected String getJWT(API api, ApplicationDTO applicationDTO, String tier) throws Exception {
        return getJWT(api.getName(), "/" + api.getContext() + "/" + api.getVersion(), api.getVersion(), tier,
                applicationDTO.getName(), applicationDTO.getTier());
    }

    protected String getJWT(String apiName, String context, String version, String subTier, String appName,
            String appTier) throws Exception {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setId(10);
        applicationDTO.setName(appName);
        applicationDTO.setTier(appTier);

        SubscribedApiDTO subscribedApiDTO = new SubscribedApiDTO();
        subscribedApiDTO.setContext(context);
        subscribedApiDTO.setName(apiName);
        subscribedApiDTO.setVersion(version);
        subscribedApiDTO.setPublisher("admin");
        subscribedApiDTO.setSubscriptionTier(subTier);
        subscribedApiDTO.setSubscriberTenantDomain("carbon.super");

        JSONObject jwtTokenInfo = new JSONObject();
        jwtTokenInfo.put("aud", "http://org.wso2.apimgt/gateway");
        jwtTokenInfo.put("sub", "admin");
        jwtTokenInfo.put("application", new JSONObject(applicationDTO));
        jwtTokenInfo.put("scope", "am_application_scope default");
        jwtTokenInfo.put("iss", "https://localhost:8244/token");
        jwtTokenInfo.put("keytype", "PRODUCTION");
        jwtTokenInfo.put("subscribedAPIs", new JSONArray(Arrays.asList(subscribedApiDTO)));
        jwtTokenInfo.put("exp", System.currentTimeMillis() + 3600 * 1000);
        jwtTokenInfo.put("iat", System.currentTimeMillis());
        jwtTokenInfo.put("jti", UUID.randomUUID());

        String payload = jwtTokenInfo.toString();

        JSONObject head = new JSONObject();
        head.put("typ", "JWT");
        head.put("alg", "RS256");
        head.put("x5t", "UB_BQy2HFV3EMTgq64Q-1VitYbE");
        String header = head.toString();

        String base64UrlEncodedHeader = Base64.getUrlEncoder()
                .encodeToString(header.getBytes(Charset.defaultCharset()));
        String base64UrlEncodedBody = Base64.getUrlEncoder().encodeToString(payload.getBytes(Charset.defaultCharset()));

        Signature signature = Signature.getInstance("SHA256withRSA");
        String jksPath = getClass().getClassLoader().getResource("wso2carbon.jks").getPath();
        FileInputStream is = new FileInputStream(jksPath);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, "wso2carbon".toCharArray());
        String alias = "wso2carbon";
        Key key = keystore.getKey(alias, "wso2carbon".toCharArray());
        Key privateKey = null;
        if (key instanceof PrivateKey) {
            privateKey = key;
        }
        signature.initSign((PrivateKey) privateKey);
        String assertion = base64UrlEncodedHeader + "." + base64UrlEncodedBody;
        byte[] dataInBytes = assertion.getBytes(StandardCharsets.UTF_8);
        signature.update(dataInBytes);
        //sign the assertion and return the signature
        byte[] signedAssertion = signature.sign();
        String base64UrlEncodedAssertion = Base64.getUrlEncoder().encodeToString(signedAssertion);
        String jwt = base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.' + base64UrlEncodedAssertion;

        return jwt;
    }
}
