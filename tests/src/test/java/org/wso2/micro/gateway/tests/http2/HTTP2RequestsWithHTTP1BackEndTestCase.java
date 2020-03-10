/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.tests.http2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.util.HTTP2Client.Http2ClientRequest;
import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTPS_PORT;
import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTP_PORT;

public class HTTP2RequestsWithHTTP1BackEndTestCase extends HTTP2RequestsWithHTTP2BackEndTestCase {

    private static final Log log = LogFactory.getLog(HTTP2RequestsWithHTTP2BackEndTestCase.class);
    protected Http2ClientRequest http2ClientRequest;

    @Test(description = "Test API invocation with an HTTP/2.0 request via insecure connection sending to HTTP/1.1 BE")
    public void testHTTP2RequestsViaInsecureConnectionWithHTTP1BE() throws Exception {
        http2ClientRequest = new Http2ClientRequest(false, GATEWAY_LISTENER_HTTP_PORT, jwtTokenProd);
        http2ClientRequest.start();
    }

    @Test(description = "Test API invocation with an HTTP/2.0 request via secure connection sending to HTTP/1.1 BE")
    public void testHTTP2RequestsViaSecureConnectionWithHTTP1BE() throws Exception {
        http2ClientRequest = new Http2ClientRequest(true, GATEWAY_LISTENER_HTTPS_PORT, jwtTokenProd);
        http2ClientRequest.start();
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
