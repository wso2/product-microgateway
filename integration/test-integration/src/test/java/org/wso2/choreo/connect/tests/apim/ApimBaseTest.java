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

package org.wso2.choreo.connect.tests.apim;

import org.apache.axis2.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;

public class ApimBaseTest {

    protected AutomationContext apimServerContext;
    protected AutomationContext superTenantKeyManagerContext;
    protected APIMURLBean apimServiceUrls;
    protected String apimServiceURLHttps;

    protected RestAPIAdminImpl adminRestClient;
    protected RestAPIPublisherImpl publisherRestClient;
    protected RestAPIStoreImpl storeRestClient;
    protected TenantManagementServiceClient tenantManagementServiceClient;

    protected User user;
    protected Application sampleApp;
    protected String keyManagerSuperTenantSessionCookie;

    public void initWithSuperTenant() throws CCTestException {
        TestUserMode userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    public void init(TestUserMode userMode) throws CCTestException {
        try {
            apimServerContext = new AutomationContext(TestConstant.AM_PRODUCT_GROUP_NAME,
                    TestConstant.AM_ALL_IN_ONE_INSTANCE, userMode);

            apimServiceUrls = new APIMURLBean(apimServerContext.getContextUrls());
            apimServiceURLHttps = apimServiceUrls.getWebAppURLHttps();
            user = apimServerContext.getContextTenant().getContextUser();

            adminRestClient = new RestAPIAdminImpl(
                    apimServerContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                    apimServerContext.getContextTenant().getContextUser().getPassword(),
                    apimServerContext.getContextTenant().getDomain(), apimServiceURLHttps);

            publisherRestClient = new RestAPIPublisherImpl(
                    apimServerContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                    apimServerContext.getContextTenant().getContextUser().getPassword(),
                    apimServerContext.getContextTenant().getDomain(), apimServiceURLHttps);

            storeRestClient = new RestAPIStoreImpl(
                    apimServerContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                    apimServerContext.getContextTenant().getContextUser().getPassword(),
                    apimServerContext.getContextTenant().getDomain(), apimServiceURLHttps);
        } catch (XPathExpressionException e) {
            throw new CCTestException("Error while initializing automation context for APIM REST API clients", e);
        }

        sampleApp = new Application("SampleApp",
                TestConstant.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
    }
}
