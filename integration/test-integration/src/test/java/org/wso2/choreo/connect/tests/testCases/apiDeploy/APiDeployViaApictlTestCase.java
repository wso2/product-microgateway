package org.wso2.choreo.connect.tests.testCases.apiDeploy;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.io.IOException;

public class APiDeployViaApictlTestCase {

    @BeforeClass
    public void createApiProject() throws IOException, MicroGWTestException {
        ApictlUtils.createProject( "deploy_openAPI.yaml", "apictl_petstore", null);
        ApictlUtils.addEnv("apictl_test");
    }

    @Test
    public void deployAPI() throws MicroGWTestException {
        ApictlUtils.login("apictl_test");
        ApictlUtils.deployAPI("apictl_petstore", "apictl_test");
    }

    @Test
    public void undeployAPI() throws MicroGWTestException {
        ApictlUtils.undeployAPI("SwaggerPetstoreDeploy", "1.0.5", "apictl_test");
    }

    @AfterClass
    public void logoutAndRemoveEnv() throws MicroGWTestException {
        ApictlUtils.logout("apictl_test");
        ApictlUtils.removeEnv("apictl_test");
    }
}
