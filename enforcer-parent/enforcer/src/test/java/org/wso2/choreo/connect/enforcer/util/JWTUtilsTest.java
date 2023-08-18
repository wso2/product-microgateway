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

        String azureJWKSPayload = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\"," +
                "\"kid\":\"2ZQpJ3UpbjAYXYGaXEJl8lV0TOI\",\"x5t\":\"2ZQpJ3UpbjAYXYGaXEJl8lV0TOI\"," +
                "\"n\":\"wEMMJtj9yMQd8QS6Vnm538K5GN1Pr_I31_LUl9-OCYu-9_DrDvPGjViQK9kOiCjBfyqoAL-pBecn9-XXaS-C" +
                "4xZTn1ZRw--GELabuo0u-U6r3TKj42xFDEP-_R5RpOGshoC95lrKiU5teuhn4fBM3XfR2GB0dVMcpzN3h4-0OMvBK__Z" +
                "r9tkQCU_KzXTbNCjyA7ybtbr83NF9k3KjpTyOyY2S-qvFbY-AoqMhL9Rp8r2HBj_vrsr6RX6GeiSxxjbEzDFA2VIcSKbS" +
                "HvbNBEeW2KjLXkz6QG2LjKz5XsYLp6kv_-k9lPQBy_V7Ci4ZkhAN-6j1S1Kcq58aLbp0wDNKQ\",\"e\":\"AQAB\"," +
                "\"x5c\":[\"MIIDBTCCAe2gAwIBAgIQH4FlYNA+UJlF0G3vy9ZrhTANBgkqhkiG9w0BAQsFADAtMSswKQYDVQQDEyJhY2" +
                "NvdW50cy5hY2Nlc3Njb250cm9sLndpbmRvd3MubmV0MB4XDTIyMDUyMjIwMDI0OVoXDTI3MDUyMjIwMDI0OVowLTErMCkGA" +
                "1UEAxMiYWNjb3VudHMuYWNjZXNzY29udHJvbC53aW5kb3dzLm5ldDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM" +
                "BDDCbY/cjEHfEEulZ5ud/CuRjdT6/yN9fy1JffjgmLvvfw6w7zxo1YkCvZDogowX8qqAC/qQXnJ/fl12kvguMWU59WUcPvh" +
                "hC2m7qNLvlOq90yo+NsRQxD/v0eUaThrIaAveZayolObXroZ+HwTN130dhgdHVTHKczd4ePtDjLwSv/2a/bZEAlPys102zQ" +
                "o8gO8m7W6/NzRfZNyo6U8jsmNkvqrxW2PgKKjIS/UafK9hwY/767K+kV+hnokscY2xMwxQNlSHEim0h72zQRHltioy15M+k" +
                "Bti4ys+V7GC6epL//pPZT0Acv1ewouGZIQDfuo9UtSnKufGi26dMAzSkCAwEAAaMhMB8wHQYDVR0OBBYEFLFr+sjUQ+IdzGh" +
                "3eaDkzue2qkTZMA0GCSqGSIb3DQEBCwUAA4IBAQCiVN2A6ErzBinGYafC7vFv5u1QD6nbvY32A8KycJwKWy1sa83CbLFbFi92" +
                "SGkKyPZqMzVyQcF5aaRZpkPGqjhzM+iEfsR2RIf+/noZBlR/esINfBhk4oBruj7SY+kPjYzV03NeY0cfO4JEf6kXpCqRCgp" +
                "9VDRM44GD8mUV/ooN+XZVFIWs5Gai8FGZX9H8ZSgkIKbxMbVOhisMqNhhp5U3fT7VPsl94rilJ8gKXP/KBbpldrfmOAdVDg" +
                "UC+MHw3sSXSt+VnorB4DU4mUQLcMriQmbXdQc8d1HUZYZEkcKaSgbygHLtByOJF44XUsBotsTfZ4i/zVjnYcjgUQ" +
                "mwmAWD\"]},{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"-KI3Q9nNR7bRofxmeZoXqbHZGew\"," +
                "\"x5t\":\"-KI3Q9nNR7bRofxmeZoXqbHZGew\",\"n\":\"tJL6Wr2JUsxLyNezPQh1J6zn6wSoDAhgRYSDkaMuEHy75" +
                "VikiB8wg25WuR96gdMpookdlRvh7SnRvtjQN9b5m4zJCMpSRcJ5DuXl4mcd7Cg3Zp1C5-JmMq8J7m7OS9HpUQbA1yhtCHqP" +
                "7XA4UnQI28J-TnGiAa3viPLlq0663Cq6hQw7jYo5yNjdJcV5-FS-xNV7UHR4zAMRruMUHxte1IZJzbJmxjKoEjJwDTtcd6DkI" +
                "3yrkmYt8GdQmu0YBHTJSZiz-M10CY3LbvLzf-tbBNKQ_gfnGGKF7MvRCmPA_YF_APynrIG7p4vPDRXhpG3_CIt317NyvGoIwiv" +
                "0At83kQ\",\"e\":\"AQAB\",\"x5c\":[\"MIIDBTCCAe2gAwIBAgIQGQ6YG6NleJxJGDRAwAd/ZTANBgkqhkiG9w0BAQsFADA" +
                "tMSswKQYDVQQDEyJhY2NvdW50cy5hY2Nlc3Njb250cm9sLndpbmRvd3MubmV0MB4XDTIyMTAwMjE4MDY0OVoXDTI3MTAwMjE" +
                "4MDY0OVowLTErMCkGA1UEAxMiYWNjb3VudHMuYWNjZXNzY29udHJvbC53aW5kb3dzLm5ldDCCASIwDQYJKoZIhvcNAQEBBQ" +
                "ADggEPADCCAQoCggEBALSS+lq9iVLMS8jXsz0IdSes5+sEqAwIYEWEg5GjLhB8u+VYpIgfMINuVrkfeoHTKaKJHZUb4e0p0b7" +
                "Y0DfW+ZuMyQjKUkXCeQ7l5eJnHewoN2adQufiZjKvCe5uzkvR6VEGwNcobQh6j+1wOFJ0CNvCfk5xogGt74jy5atOutwquoU" +
                "MO42KOcjY3SXFefhUvsTVe1B0eMwDEa7jFB8bXtSGSc2yZsYyqBIycA07XHeg5CN8q5JmLfBnUJrtGAR0yUmYs/jNdAmNy27y" +
                "83/rWwTSkP4H5xhihezL0QpjwP2BfwD8p6yBu6eLzw0V4aRt/wiLd9ezcrxqCMIr9ALfN5ECAwEAAaMhMB8wHQYDVR0OBBYEF" +
                "JcSH+6Eaqucndn9DDu7Pym7OA8rMA0GCSqGSIb3DQEBCwUAA4IBAQADKkY0PIyslgWGmRDKpp/5PqzzM9+TNDhXzk6pw8aES" +
                "WoLPJo90RgTJVf8uIj3YSic89m4ftZdmGFXwHcFC91aFe3PiDgCiteDkeH8KrrpZSve1pcM4SNjxwwmIKlJdrbcaJfWRsSo" +
                "GFjzbFgOecISiVaJ9ZWpb89/+BeAz1Zpmu8DSyY22dG/K6ZDx5qNFg8pehdOUYY24oMamd4J2u2lUgkCKGBZMQgBZFwk+q7" +
                "H86B/byGuTDEizLjGPTY/sMms1FAX55xBydxrADAer/pKrOF1v7Dq9C1Z9QVcm5D9G4DcenyWUdMyK43NXbVQLPxLOng51KO" +
                "9icp2j4U7pwHP\"]},{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"lHLIu4moKqzPcokwlfCRPHyjl5g\"," +
                "\"x5t\":\"lHLIu4moKqzPcokwlfCRPHyjl5g\",\"n\":\"xlc-u9LJvOdbwAsgsYZpaJrgmrGHaEkoa_3_7Jvu4-Hb8L" +
                "NtszrQy5Ik4CXgQ_uiLPt4-ePprX3klFAx91ahfd5LwX6mEQPT8WuHMDunx8MaNQrYNVvnOI1L5NxFBFV_6ghi_0d-cOslE" +
                "rcTMML2lbMCSjQ8jwltxz1Oy-Hd9wdY2pz2YC3WR4tHzAGreWGeOB2Vs2NLGv0U3CGSCMqpM9vxbWLZQPuCNpKF93RkxHj5b" +
                "Lng9U_rM6YScacEnTFlKIOOrk4pcVVdoSNNIK2uNUs1hHS1mBXuQjfceghzj3QQYHfp1Z5qWXPRIw3PDyn_1Sowe5UljLur" +
                "kpj_8m3KnQ\",\"e\":\"AQAB\",\"x5c\":[\"MIIC6TCCAdGgAwIBAgIIT3fcexMa3ggwDQYJKoZIhvcNAQELBQAwIzEhMB" +
                "8GA1UEAxMYbG9naW4ubWljcm9zb2Z0b25saW5lLnVzMB4XDTIzMDcxNDAwNDU0NFoXDTI4MDcxNDAwNDU0NFowIzEhMB8GA1U" +
                "EAxMYbG9naW4ubWljcm9zb2Z0b25saW5lLnVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxlc+u9LJvOdbwAsgsY" +
                "ZpaJrgmrGHaEkoa/3/7Jvu4+Hb8LNtszrQy5Ik4CXgQ/uiLPt4+ePprX3klFAx91ahfd5LwX6mEQPT8WuHMDunx8MaNQrYNVvn" +
                "OI1L5NxFBFV/6ghi/0d+cOslErcTMML2lbMCSjQ8jwltxz1Oy+Hd9wdY2pz2YC3WR4tHzAGreWGeOB2Vs2NLGv0U3CGSCMqp" +
                "M9vxbWLZQPuCNpKF93RkxHj5bLng9U/rM6YScacEnTFlKIOOrk4pcVVdoSNNIK2uNUs1hHS1mBXuQjfceghzj3QQYHfp1Z5qW" +
                "XPRIw3PDyn/1Sowe5UljLurkpj/8m3KnQIDAQABoyEwHzAdBgNVHQ4EFgQUCSJrrznFYz1BLqd17S8HFjGrAOAwDQYJKoZIhv" +
                "cNAQELBQADggEBAAQHNudtmYpeh9x5+rGDVy6OYpTnQ2D5+rmgOHM5yRvgEnFBNuZ6bnr3Ap9nb6EM08juYKPaVyhkV+5axM" +
                "l+dT8KOuCgrfcKvXqzdQ3BgVFkyU9XfajHzq3JALYpNkixCs/BvqRhXx2ecYxFHB2D671cOwhYIaMZdGtbmOOk8puYSgJ9" +
                "DBqqn3pLksHmxLP656l/U3hPATTCdfDaNcTagIPx+Q2d9RBn8zOIa/p4CLsu3E0aJfDw3ljPD8inLJ2mpKq06TBfd5Rr/auwi" +
                "pb4J8Y/PHhef8b2kOf42fikIKAP538k9lLsXSowyPWn7KZDTEsku7xpyqvKvEiFkmaV+RY=\"]}]}";
        WireMock.stubFor(WireMock.get("/azure/jwks")
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(azureJWKSPayload)));

        JWKSet jwkSet = null;
        try {
            jwkSet = JWTUtils.retrieveJWKSConfiguration("http://localhost:8088/azure/jwks");
        } catch (IOException | RemoteKeySourceException e) {
            Assert.fail("JWKS Configuration could not be retrieved : " + e.getMessage());
        }
        Assert.assertNotNull(jwkSet);
        Assert.assertNotNull(jwkSet.getKeyByKeyId("2ZQpJ3UpbjAYXYGaXEJl8lV0TOI"));
        JWK jwk = jwkSet.getKeyByKeyId("2ZQpJ3UpbjAYXYGaXEJl8lV0TOI");
        Assert.assertEquals(jwk.getKeyType().getValue(), "RSA");
        Assert.assertEquals(jwk.toRSAKey().getPublicExponent().toString(), "AQAB");
    }

    @After
    public void stopMockServer() {
        wireMockServer.stop();
    }
}
