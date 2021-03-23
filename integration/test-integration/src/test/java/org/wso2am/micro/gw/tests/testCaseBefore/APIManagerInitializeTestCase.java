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

package org.wso2am.micro.gw.tests.testCaseBefore;

import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2am.micro.gw.tests.apim.APIMLifecycleBaseTest;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.HttpClientRequest;
import org.wso2am.micro.gw.tests.util.HttpResponse;
import org.wso2am.micro.gw.tests.util.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class APIManagerInitializeTestCase extends APIMLifecycleBaseTest {

    @BeforeTest(description = "start the mgw along with apim server")
    void start() throws Exception {
        URL configToml = getClass().getClassLoader().getResource("apim/config.toml");
        if (configToml == null) {
            throw new MicroGWTestException(
                    "Config toml cannot be found. Hence, not starting the API Manager server with Mgw server");
        }
        String configTomlPath = configToml.getPath();

        try {
            super.startAPIMWithMGW(configTomlPath, true);
        } catch (MicroGWTestException | IOException e) {
            Assert.fail("Error starting the APIM server with MGW", e);
        }

        // set ssl properties needed to run the apim server. eg: truststore cert, truststore password, https protocols
        setSSlSystemProperties();

        Awaitility.await().atMost(2, TimeUnit.MINUTES);
        Awaitility.await().pollInterval(20, TimeUnit.SECONDS).atMost(2, TimeUnit.MINUTES).until(isAPIMServerStarted());
        Assert.assertTrue(checkForAPIMServerStartup(), "APIM server has not started properly");

        String dcrURL = Utils.getAPIMServiceURLHttps("/client-registration/v0.17/register");
        //DCR call for publisher app
        DCRParamRequest publisherParamRequest = new DCRParamRequest(RestAPIPublisherImpl.appName,
                                                                    RestAPIPublisherImpl.callBackURL,
                                                                    RestAPIPublisherImpl.tokenScope,
                                                                    RestAPIPublisherImpl.appOwner,
                                                                    RestAPIPublisherImpl.grantType, dcrURL,
                                                                    RestAPIPublisherImpl.username,
                                                                    RestAPIPublisherImpl.password,
                                                                    APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(publisherParamRequest);

        //DCR call for dev portal app
        DCRParamRequest devPortalParamRequest = new DCRParamRequest(RestAPIStoreImpl.appName,
                                                                    RestAPIStoreImpl.callBackURL,
                                                                    RestAPIStoreImpl.tokenScope,
                                                                    RestAPIStoreImpl.appOwner,
                                                                    RestAPIStoreImpl.grantType, dcrURL,
                                                                    RestAPIStoreImpl.username,
                                                                    RestAPIStoreImpl.password,
                                                                    APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(devPortalParamRequest);

        // DCR call for admin portal app
        DCRParamRequest adminPortalParamRequest = new DCRParamRequest(RestAPIAdminImpl.appName,
                                                                      RestAPIAdminImpl.callBackURL,
                                                                      RestAPIAdminImpl.tokenScope,
                                                                      RestAPIAdminImpl.appOwner,
                                                                      RestAPIAdminImpl.grantType, dcrURL,
                                                                      RestAPIAdminImpl.username,
                                                                      RestAPIAdminImpl.password,
                                                                      APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(adminPortalParamRequest);

        super.init();
        Assert.assertNotNull(restAPIPublisher, "restAPIPublisher");
        Assert.assertNotNull(restAPIStore, "restAPIStore");
    }

    @AfterTest(description = "stop the setup")
    public void destroy() {
        super.stopAPIMWithMGW();
    }

    private Callable<Boolean> isAPIMServerStarted() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return checkForAPIMServerStartup();
            }
        };
    }

    private Boolean checkForAPIMServerStartup() throws IOException {
        HttpResponse response = HttpClientRequest.doGet(Utils.getAPIMServiceURLHttp("/services/Version"));
        return Objects.nonNull(response) && response.getResponseCode() == 200;
    }
}
