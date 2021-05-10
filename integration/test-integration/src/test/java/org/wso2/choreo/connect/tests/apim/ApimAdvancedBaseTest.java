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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.xml.xpath.XPathExpressionException;

public class ApimAdvancedBaseTest extends ApimBaseTest {
    private static final Logger log = LoggerFactory.getLogger(ApimAdvancedBaseTest.class);

    protected UserManagementClient userManagementClient;
    protected RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient;
    protected ClaimMetaDataMgtAdminClient remoteClaimMetaDataMgtAdminClient;
    protected OAuthAdminServiceClient oAuthAdminServiceClient;
    protected ApplicationManagementClient applicationManagementClient;
    protected String keyManagerSessionCookie;



    protected String apimServiceURLHttp;

    protected ApimAdvancedBaseTest(TestUserMode userMode) throws CCTestException {
        super.initWithSuperTenant();

        //apimServiceURLHttp = apimServiceUrls.getWebAppURLHttp();

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
     * @param expectedResponse - Expected response
     * @throws CCTestException if something goes wrong when getting the tenant identifier
     */
    protected void waitForAPIDeploymentSync(String apiProvider, String apiName, String apiVersion,
                                            String expectedResponse)
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
                    + " with expected response : " + expectedResponse);

            if (response != null) {
                if (response.getData().contains(expectedResponse)) {
                    log.info("API :" + apiName + " with version: " + apiVersion + " with expected response "
                            + expectedResponse + " found");
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





//    /**
//     * Cleaning up the API manager by removing all APIs and applications other than default application
//     *
//     * @throws Exception - occurred when calling the apis
//     */
//    protected void cleanUp() throws Exception {
//
//        if (Objects.isNull(apimStoreClient)) {
//            return;
//        }
//        ApplicationListDTO applicationListDTO = apimStoreClient.getAllApps();
//        if (applicationListDTO.getList() != null) {
//            for (ApplicationInfoDTO applicationInfoDTO : applicationListDTO.getList()) {
//                SubscriptionListDTO subsDTO = apimStoreClient
//                        .getAllSubscriptionsOfApplication(applicationInfoDTO.getApplicationId());
//                if (subsDTO != null && subsDTO.getList() != null) {
//                    for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
//                        apimStoreClient.removeSubscription(subscriptionDTO.getSubscriptionId());
//                    }
//                }
//                if (!APIMIntegrationConstants.OAUTH_DEFAULT_APPLICATION_NAME.equals(applicationInfoDTO.getName())) {
//                    apimStoreClient.deleteApplication(applicationInfoDTO.getApplicationId());
//                }
//            }
//        }
//
//        if (Objects.isNull(apimPublisherClient)) {
//            return;
//        }
//        APIProductListDTO allApiProducts = apimPublisherClient.getAllApiProducts();
//        List<APIProductInfoDTO> apiProductListDTO = allApiProducts.getList();
//
//        if (apiProductListDTO != null) {
//            for (APIProductInfoDTO apiProductInfoDTO : apiProductListDTO) {
//                apimPublisherClient.deleteApiProduct(apiProductInfoDTO.getId());
//            }
//        }
//
//        APIListDTO apiListDTO = apimPublisherClient.getAllAPIs();
//        if (apiListDTO != null && apiListDTO.getList() != null) {
//            for (APIInfoDTO apiInfoDTO : apiListDTO.getList()) {
//                apimPublisherClient.deleteAPI(apiInfoDTO.getId());
//            }
//        }
//    }
}
