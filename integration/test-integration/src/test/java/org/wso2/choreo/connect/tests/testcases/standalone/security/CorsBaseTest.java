/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testcases.standalone.security;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.testng.Assert;

public class CorsBaseTest {

    protected static final String allowedOrigin1 = "http://test1.com";
    protected static final String allowedOrigin2 = "http://test2.com";
    protected static final String allowedHeaders = "Authorization,X-PINGOTHER";
    protected static final String exposeHeaders = "X-Custom-Header";
    protected String allowedOriginWildCard = "http://foo.wildcard.com";
    protected static final String ORIGIN_HEADER = "Origin";
    protected static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER = "access-control-request-method";
    protected static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "access-control-allow-origin";
    protected static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "access-control-allow-methods";
    protected static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "access-control-allow-headers";
    protected static final String ACCESS_CONTROL_EXPOSE_HEADERS_HEADER = "access-control-expose-headers";
    protected static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "access-control-allow-credentials";
    protected static final String ACCESS_CONTROL_ALLOW_METHODS = "GET,PUT,POST";

    /**
     * Iterates through the HTTP response headers list to search a given header
     *
     * @param headers        HTTP response headers list
     * @param requiredHeader header name as a string
     * @return the required HTTP header (if not found, null will be returned)
     */
    protected static Header pickHeader(Header[] headers, String requiredHeader) {
        if (requiredHeader == null) {
            return null;
        }
        for (Header header : headers) {
            if (requiredHeader.equals(header.getName())) {
                return header;
            }
        }
        return null;
    }

    /**
     * Validates CORS preflight request headers
     *
     * @param response HTTP response after invoking the request
     */
    protected static void validateCORSHeadersInPreFlightResponse(HttpResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).getValue(),
                allowedOrigin1);
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString() + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString())
                        .getValue().replaceAll(" ", "")
                , ACCESS_CONTROL_ALLOW_METHODS);
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString())
                .getValue().replaceAll(" ", ""), allowedHeaders);
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is unavailable");
        Assert.assertTrue(Boolean.parseBoolean(pickHeader(responseHeaders,
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER).getValue()));
    }

    /**
     * Validates API invocation response with CORS
     *
     * @param response
     */
    protected static void validateCORSHeadersInSimpleResponse(HttpResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).getValue(),
                allowedOrigin2);

        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
                ACCESS_CONTROL_EXPOSE_HEADERS_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_EXPOSE_HEADERS_HEADER).getValue(),
                exposeHeaders, ACCESS_CONTROL_EXPOSE_HEADERS_HEADER + " header mismatched.");
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is unavailable");
        Assert.assertTrue(Boolean.parseBoolean(pickHeader(responseHeaders,
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER).getValue()));
    }
}
