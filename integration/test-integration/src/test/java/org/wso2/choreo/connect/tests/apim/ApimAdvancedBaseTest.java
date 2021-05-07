/*
Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.apim;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.admin.clients.application.ApplicationManagementClient;
import org.wso2.am.admin.clients.claim.ClaimMetaDataMgtAdminClient;
import org.wso2.am.admin.clients.oauth.OAuthAdminServiceClient;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

/**
 * A base testcase class for advanced use cases
 *
 * This class only needs to be used if at least one of the following clients are required for the test case.
 * ApimBaseTest can be extended by any testcase class that interacts only with Admin, Publisher and/or Store
 */
public class ApimAdvancedBaseTest extends ApimBaseTest {
    private static final Logger log = LoggerFactory.getLogger(ApimAdvancedBaseTest.class);

    protected UserManagementClient userManagementClient;
    protected RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient;
    protected ClaimMetaDataMgtAdminClient remoteClaimMetaDataMgtAdminClient;
    protected OAuthAdminServiceClient oAuthAdminServiceClient;
    protected ApplicationManagementClient applicationManagementClient;
    protected TenantManagementServiceClient tenantManagementServiceClient;
    protected String keyManagerSessionCookie;
    protected String keyManagerSuperTenantSessionCookie;

    /**
     * Initialize all REST clients for API Manager in Super Tenant Admin user mode
     *
     * @throws CCTestException if an error occurs while initializing a client
     */
    public void initWithSuperTenant() throws CCTestException {
        TestUserMode userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * Initialize all REST clients for API Manager including Admin, Publisher and Store client that
     * are defined in the super class
     *
     * @param userMode - a enum value of TestUserMode
     * @throws CCTestException if an error occurs while initializing a client
     */
    public void  init(TestUserMode userMode) throws CCTestException {
        super.init(userMode);
        try {
            superTenantKeyManagerContext = new AutomationContext(TestConstant.AM_PRODUCT_GROUP_NAME,
                    TestConstant.AM_ALL_IN_ONE_INSTANCE,
                    TestUserMode.SUPER_TENANT_ADMIN);

            keyManagerSessionCookie = new LoginLogoutClient(apimServerContext).login();
            keyManagerSuperTenantSessionCookie = new LoginLogoutClient(superTenantKeyManagerContext).login();
            tenantManagementServiceClient = new TenantManagementServiceClient(
                    superTenantKeyManagerContext.getContextUrls().getBackEndUrl(),
                    keyManagerSuperTenantSessionCookie);

            userManagementClient = new UserManagementClient(
                    apimServerContext.getContextUrls().getBackEndUrl(), keyManagerSessionCookie);
            remoteUserStoreManagerServiceClient = new RemoteUserStoreManagerServiceClient(
                    apimServerContext.getContextUrls().getBackEndUrl(), keyManagerSessionCookie);
            remoteClaimMetaDataMgtAdminClient =
                    new ClaimMetaDataMgtAdminClient(apimServerContext.getContextUrls().getBackEndUrl(),
                                                    keyManagerSessionCookie);
            oAuthAdminServiceClient =
                    new OAuthAdminServiceClient(apimServerContext.getContextUrls().getBackEndUrl(),
                                                keyManagerSessionCookie);
            applicationManagementClient =
                    new ApplicationManagementClient(apimServerContext.getContextUrls().getBackEndUrl(),
                                                    keyManagerSessionCookie);
        } catch (Exception e) {
            throw new CCTestException(e.getMessage(), e);
        }
    }

    /**
     * This method can be used to wait for API deployment sync in distributed and clustered environment APIStatusMonitor
     * will be invoked to get API related data and then verify that data matches with expected response provided.
     *
     * @param apiProvider      - Provider of the API
     * @param apiName          - API name
     * @param apiVersion       - API version
     * @throws CCTestException if something goes wrong when getting the tenant identifier
     */
    protected void waitForAPIDeploymentSync(String apiProvider, String apiName, String apiVersion)
            throws XPathExpressionException, CCTestException {

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + (60 * 1000);

        String colonSeparatedHeader =
                apimServerContext.getContextTenant().getTenantAdmin().getUserName() + ":" + apimServerContext
                        .getContextTenant().getTenantAdmin().getPassword();

        String authorizationHeader = "Basic " + new String(Base64.encodeBase64(colonSeparatedHeader.getBytes()));
        Map<String, String> headerMap = new HashMap();
        headerMap.put("Authorization", authorizationHeader);
        String tenantIdentifier = getTenantIdentifier(apiProvider);

        while (waitTime > System.currentTimeMillis()) {
            HttpResponse response = null;
            try {
                response = HttpRequestUtil.doGet(
                        apimServiceURLHttps + "APIStatusMonitor/apiInformation/api/" + tenantIdentifier + apiName + "/"
                                + apiVersion, headerMap);
            } catch (IOException ignored) {
                log.warn("WebAPP: APIStatusMonitor not yet deployed or API :" + apiName + " not yet "
                        + "deployed with provider: " + apiProvider);
            }

            log.info("WAIT for availability of API: " + apiName + " with version: " + apiVersion
                    + " with provider: " + apiProvider + " with Tenant Identifier: " + tenantIdentifier
                    + " with expected response : " + APIMIntegrationConstants.IS_API_EXISTS);

            if (response != null) {
                if (response.getData().contains(APIMIntegrationConstants.IS_API_EXISTS)) {
                    log.info("API :" + apiName + " with version: " + apiVersion + " with expected response "
                            + APIMIntegrationConstants.IS_API_EXISTS + " found");
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Generate the tenant identifier.
     *
     * @param apiProvider - Provider of the API
     * @return tenatDomain/tenantId/
     */
    private String getTenantIdentifier(String apiProvider) throws CCTestException {
        int tenantId = -1234;
        String providerTenantDomain = MultitenantUtils.getTenantDomain(apiProvider);
        try {
            if (!TestConstant.SUPER_TENANT_DOMAIN.equals(providerTenantDomain)) {
                TenantInfoBean tenant = tenantManagementServiceClient.getTenant(providerTenantDomain);
                if (tenant == null) {
                    log.info("tenant is null: " + providerTenantDomain);
                } else {
                    tenantId = tenant.getTenantId();
                }
                //forced tenant loading
                new LoginLogoutClient(apimServerContext).login();
            }
        } catch (Exception e) {
            throw new CCTestException(e.getMessage(), e);
        }
        return providerTenantDomain + "/" + tenantId + "/";
    }
}
