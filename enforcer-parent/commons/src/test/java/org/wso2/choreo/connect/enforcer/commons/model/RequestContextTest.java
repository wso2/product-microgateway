/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.commons.model;

import org.junit.Assert;
import org.junit.Test;

public class RequestContextTest {

    public RequestContextTest() {}

    @Test
    public void testPathParameterGenerationBasic() {
        testPathParamValues("/v2/pet/12", "/v2", "/pet/{petId}",
                "petId", "12");
    }

    @Test
    public void testPathParameterGenerationMultiple() {
        testPathParamValues("/v2/pet/12/status/available", "/v2", "/pet/{petId}/status/{statusType}",
                "petId", "12");
        testPathParamValues("/v2/pet/12/status/available", "/v2", "/pet/{petId}/status/{statusType}",
                "statusType", "available");
    }

    @Test
    public void testPathParametersWithTrailingSlashInTemplate() {
        testPathParamValues("/v2/pet/12/status/available", "/v2", "/pet/{petId}/status/{statusType}/",
                "petId", "12");
    }

    @Test
    public void testPathParametersWithTrailingSlashInRawPath() {
        testPathParamValues("/v2/pet/12/status/available/", "/v2", "/pet/{petId}/status/{statusType}",
                "petId", "12");
    }

    @Test
    public void testPathParametersWithHyphen() {
        testPathParamValues("/v2/pet/12/status/available", "/v2", "/pet/{pet-Id}/status/{statusType}",
                "pet-Id", "12");
    }

    @Test
    public void testPathParametersWithUnderscore() {
        testPathParamValues("/v2/pet/12/status/available", "/v2", "/pet/{pet_Id}/status/{statusType}",
                "pet_Id", "12");
    }

    @Test
    public void testPathParametersWithNumbers() {
        testPathParamValues("/v2/pet/12/status/available", "/v2", "/pet/{petId2}/status/{statusType}",
                "petId2", "12");
    }

    @Test
    public void testPathParameterGenerationWithWildcard() {
        testPathParamValues("/v2/pet/12/2/random/random2", "/v2", "/pet/{petId}/{imageId}/*",
                "petId", "12");
        testPathParamValues("/v2/pet/12/2/random/random2/random3", "/v2", "/pet/{petId}/{imageId}/*",
                "petId", "12");
        testPathParamValues("/v2/pet/12/image/2/random/random2", "/v2", "/pet/{petId}/image/{imageId}/*",
                "imageId", "2");
        testPathParamValues("/v2/pet/12/2/image", "/v2", "/pet/{petId}/{imageId}/image/*",
                "petId", "12");
        testPathParamValues("/v2/pet/pet-1/image/*", "/v2", "/pet/{imageId}/image/*",
                "imageId", "pet-1");
        testMismatchedPaths("/v2/pet/12/2/image123", "/v2", "/pet/{petId}/{imageId}/image/*");
    }

    @Test
    public void testPathParameterGenerationWithQueryParam() {
        testPathParamValues("/v2/pet/12?abc=xyz", "/v2", "/pet/{petId}",
                "petId", "12");
    }

    @Test
    public void testPathParameterGenerationWithSuffixedPath() {
        testPathParamValues("/v2/pet/12.api", "/v2", "/pet/{petId}.api",
                "petId", "12");
    }

    @Test
    public void testPathParameterGenerationWithPrefixedPath() {
        testPathParamValues("/v2/pet/api12", "/v2", "/pet/api{petId}",
                "petId", "12");
    }

    @Test
    public void testPathParameterWithDefaultAPIRequests() {
        testPathParamValues("/testapi/pet/12", "/testapi/v1", "/pet/{petId}",
                "petId", "12");
    }

    private void testPathParamValues(String rawPath, String basePath, String pathTemplate, String pathParamName,
                                     String expectedValue) {
        RequestContext.Builder builder = new RequestContext.Builder(rawPath);
        builder.matchedAPI(new APIConfig.Builder("Petstore").basePath(basePath).build());
        builder.pathTemplate(pathTemplate);
        RequestContext requestContext = builder.build();
        Assert.assertNotNull(requestContext.getPathParameters());
        Assert.assertTrue(requestContext.getPathParameters().containsKey(pathParamName));
        Assert.assertEquals("Path Parameter mismatch for the template" + pathTemplate,
                requestContext.getPathParameters().get(pathParamName), expectedValue);
    }

    private void testMismatchedPaths(String rawPath, String basePath, String pathTemplate) {
        RequestContext.Builder builder = new RequestContext.Builder(rawPath);
        builder.matchedAPI(new APIConfig.Builder("Petstore").basePath(basePath).build());
        builder.pathTemplate(pathTemplate);
        RequestContext requestContext = builder.build();
        Assert.assertNotNull(requestContext.getPathParameters());
        Assert.assertEquals(0, requestContext.getPathParameters().size());
    }

}
