/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class JWTUtilsTest {

    WireMockServer wireMockServer = null;

    @Before
    public void startMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().port(8088)); // You can configure other options here
        wireMockServer.start();
    }

    @Test
    public void retrieveJWKSConfigurationTest() {
        WireMock.configureFor("http", "localhost", 8088);
        WireMock.stubFor(WireMock.get("/oauth2/jwks")
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":" +
                                "\"MDJlNjIxN2E1OGZlOGVmMGQxOTFlMzBmNmFjZjQ0Y2YwOGY0N2I0YzE4YzZjNjRhYmRmMmQ0ODd" +
                                "iNDhjMGEwMA_RS256\",\"alg\":\"RS256\",\"n\":\"kdgncoCrz655Lq8pTdX07eoVBjdZDCUE" +
                                "6ueBd0D1hpJ0_zE3x3Az6tlvzs98PsPuGzaQOMmuLa4qxNJ-OKxJmutDUlClpuvxuf-jyq4gCV5tEI" +
                                "ILWRMBjlBEpJfWm63-VKKU4nvBWNJ7KfhWjl8-DUdNSh2pCDLpUObmb9Kquqc1x4BgttjN4rx_P-3_" +
                                "v-1jETXzIP1L44yHtpQNv0khYf4j_aHjcEri9ykvpz1mtdacbrKK25N4V1HHRwDqZiJzOCCISXDuqB" +
                                "6wguY_v4n0l1XtrEs7iCyfRFwNSKNrLqr23tR1CscmLfbH6ZLg5CYJTD-1uPSx0HMOB4Wv51PbWw\"}" +
                                "]}")));

        try {
            JWKSet jwkSet = JWTUtils.retrieveJWKSConfiguration("http://localhost:8088/oauth2/jwks");
            Assert.assertNotNull(jwkSet);
            Assert.assertNotNull(jwkSet.getKeyByKeyId("MDJlNjIxN2E1OGZlOGVmMGQxOTFlMzBmNmFjZjQ0Y2YwOGY0N" +
                    "2I0YzE4YzZjNjRhYmRmMmQ0ODdiNDhjMGEwMA_RS256"));
            JWK jwk = jwkSet.getKeyByKeyId("MDJlNjIxN2E1OGZlOGVmMGQxOTFlMzBmNmFjZjQ0Y2YwOGY0N" +
                    "2I0YzE4YzZjNjRhYmRmMmQ0ODdiNDhjMGEwMA_RS256");
            Assert.assertEquals(jwk.getKeyType().getValue(), "RSA");
            Assert.assertEquals(jwk.toRSAKey().getPublicExponent().toString(), "AQAB");
            Assert.assertEquals(jwk.getAlgorithm().getName(), "RS256" );
            Assert.assertEquals(((RSAKey) jwk).getModulus().toString(), "kdgncoCrz655Lq8pTdX07eoVBjdZDCUE6ueBd0D1hpJ0_" +
                    "zE3x3Az6tlvzs98PsPuGzaQOMmuLa4qxNJ-OKxJmutDUlClpuvxuf-jyq4gCV5tEIILWRMBjlBEpJfWm63-VKKU4nvBWN" +
                    "J7KfhWjl8-DUdNSh2pCDLpUObmb9Kquqc1x4BgttjN4rx_P-3_v-1jETXzIP1L44yHtpQNv0khYf4j_aHjcEri9ykvpz1m" +
                    "tdacbrKK25N4V1HHRwDqZiJzOCCISXDuqB6wguY_v4n0l1XtrEs7iCyfRFwNSKNrLqr23tR1CscmLfbH6ZLg5CYJTD-" +
                    "1uPSx0HMOB4Wv51PbWw");
        } catch (IOException | RemoteKeySourceException e) {
            Assert.fail("JWKS Configuration could not be retrieved : " + e.getMessage());
        }
    }

    @After
    public void stopMockServer() {
        wireMockServer.stop();
    }
}
//kdgncoCrz655Lq8pTdX07eoVBjdZDCUE6ueBd0D1hpJ0_zE3x3Az6tlvzs98PsPuGzaQOMmuLa4qxNJ-OKxJmutDUlClpuvxuf-jyq4gCV5tEIILWRMBjlBEpJfWm63-VKKU4nvBWNJ7KfhWjl8-DUdNSh2pCDLpUObmb9Kquqc1x4BgttjN4rx_P-3_v-1jETXzIP1L44yHtpQNv0khYf4j_aHjcEri9ykvpz1mtdacbrKK25N4V1HHRwDqZiJzOCCISXDuqB6wguY_v4n0l1XtrEs7iCyfRFwNSKNrLqr23tR1CscmLfbH6ZLg5CYJTD-1uPSx0HMOB4Wv51PbWw

