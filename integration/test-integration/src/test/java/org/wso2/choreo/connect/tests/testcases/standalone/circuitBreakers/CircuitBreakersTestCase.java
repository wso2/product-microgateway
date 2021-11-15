/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.standalone.circuitBreakers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class CircuitBreakersTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "Get Prod and Sandbox tokens")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Test max requests circuit breaker")
    public void testMaxRequest() throws Exception {
        ArrayList<Future<Integer>> reqs = executeConcurrentCalls(2, jwtTokenProd, "/circuit-breakers/req-cb");
        int firstResponse = reqs.get(0).get();
        int secondResponse = reqs.get(1).get();
        if (HttpStatus.SC_OK == firstResponse) {
            Assert.assertEquals(secondResponse, HttpStatus.SC_SERVICE_UNAVAILABLE, "Response code mismatched");
        } else {
            Assert.assertEquals(firstResponse, HttpStatus.SC_SERVICE_UNAVAILABLE, "Response code mismatched");
            Assert.assertEquals(secondResponse, HttpStatus.SC_OK, "Response code mismatched");
        }
    }

    @Test(description = "Test max requests circuit breaker in api level")
    public void testMaxRequestSand() throws Exception {
        ArrayList<Integer> responses = new ArrayList<>();
        ArrayList<Future<Integer>> reqs = executeConcurrentCalls(3, jwtTokenSand, "/circuit-breakers/req-cb");
        responses.add(reqs.get(0).get());
        responses.add(reqs.get(1).get());
        responses.add(reqs.get(2).get());
        if (Collections.frequency(responses, HttpStatus.SC_OK) != 2) {
            Assert.fail("Two request must get passed as Max requests is 2");
        }
        if (!responses.contains( HttpStatus.SC_SERVICE_UNAVAILABLE)) {
            Assert.fail("Max pending requests circuit breaker has not opened");
        }
    }

    private Integer executeGet(String token, String path) throws Exception {
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                path), prodHeaders);
        Assert.assertNotNull(response);
        return response.getResponseCode();
    }

    private ArrayList<Future<Integer>> executeConcurrentCalls(int times, String token, String path) {
        ArrayList<Future<Integer>> reqs = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            Future<Integer> callFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeGet(token, path);
                } catch (Exception e) {
                    return 0;
                }
            });
            reqs.add(callFuture);
        }
        return reqs;
    }
}
