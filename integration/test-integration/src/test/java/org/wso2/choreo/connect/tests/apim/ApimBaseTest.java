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

import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;

import javax.xml.xpath.XPathExpressionException;

/**
 * A base testcase class for all testcases that uses REST clients of Admin, Publisher and Store
 * of API Manager
 */
public class ApimBaseTest {
    protected AutomationContext apimServerContext;
    protected AutomationContext superTenantKeyManagerContext;
    protected APIMURLBean apimServiceUrls;
    protected String apimServiceURLHttps;

    protected RestAPIAdminImpl adminRestClient;
    protected RestAPIPublisherImpl publisherRestClient;
    protected RestAPIStoreImpl storeRestClient;

    protected User user;

    /**
     * Initialize Admin, Publisher and Store REST clients for API Manager in Super Tenant Admin user mode
     *
     * @throws CCTestException if an error occurs while initializing a client
     */
    public void initWithSuperTenant() throws CCTestException {
        TestUserMode userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * Initialize Admin, Publisher and Store REST clients for API Manager with a given user mode
     *
     * @param userMode - a enum value of TestUserMode
     * @throws CCTestException if an error occurs while initializing a client
     */
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
    }
}
