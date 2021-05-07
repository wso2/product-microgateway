package org.wso2.choreo.connect.tests.apim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;

import javax.xml.xpath.XPathExpressionException;
import java.net.URL;

public abstract class ApimBaseTestAbstract {
    private static final Logger log = LoggerFactory.getLogger(ApimBaseTestAbstract.class);

    protected AutomationContext apimServerContext;
    protected RestAPIAdminImpl restAPIAdmin;
    protected RestAPIPublisherImpl restAPIPublisher;
    protected RestAPIStoreImpl restAPIStore;
    protected APIMURLBean apimServiceUrls;
    protected String apimServiceURLHttps;

    protected void init() throws CCTestException, XPathExpressionException {
        TestUserMode userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    protected void init(TestUserMode userMode) throws XPathExpressionException {
        apimServerContext = new AutomationContext(TestConstant.AM_PRODUCT_GROUP_NAME,
                TestConstant.AM_ALL_IN_ONE_INSTANCE, userMode);
        apimServiceUrls = new APIMURLBean(apimServerContext.getContextUrls());
        apimServiceURLHttps = apimServiceUrls.getWebAppURLHttps();

        restAPIPublisher = new RestAPIPublisherImpl(
                apimServerContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                apimServerContext.getContextTenant().getContextUser().getPassword(),
                apimServerContext.getContextTenant().getDomain(), apimServiceURLHttps);

        restAPIStore = new RestAPIStoreImpl(
                apimServerContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                apimServerContext.getContextTenant().getContextUser().getPassword(),
                apimServerContext.getContextTenant().getDomain(), apimServiceURLHttps);

        restAPIAdmin = new RestAPIAdminImpl(
                apimServerContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                apimServerContext.getContextTenant().getContextUser().getPassword(),
                apimServerContext.getContextTenant().getDomain(), apimServiceURLHttps);
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
