/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.choreo.connect.tests.setup.withapim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;

public class ApimClientsPreparer {
    private static final Logger log = LoggerFactory.getLogger(ApimClientsPreparer.class);

    @BeforeTest // Not using the @BeforeSuite annotation to make sure this runs after startAPIM()
    private void setSystemPropertiesForApimClientsAndCheckIfSet() throws MalformedURLException {
        setSSlSystemProperties();
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
    }

    /**
     * Helper method to set the SSL context.
     */
    protected void setSSlSystemProperties() {
        URL certificatesTrustStore = getClass().getClassLoader()
                .getResource("keystore/client-truststore.jks");
        if (certificatesTrustStore != null) {
            System.setProperty("javax.net.ssl.trustStore", certificatesTrustStore.getPath());
        } else {
            log.error("Truststore is not set.");
        }
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }
}
