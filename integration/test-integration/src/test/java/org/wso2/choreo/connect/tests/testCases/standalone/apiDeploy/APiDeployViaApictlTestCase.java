package org.wso2.choreo.connect.tests.testCases.standalone.apiDeploy;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.io.IOException;

public class APiDeployViaApictlTestCase {

    @BeforeClass
    public void createApiProject() throws IOException, CCTestException {
        ApictlUtils.createProject( "deploy_openAPI.yaml", "apictl_petstore", null, "apictl_test_deploy_env.yaml");
        ApictlUtils.addEnv("apictl_test");
    }

    @Test
    public void deployAPI() throws CCTestException {
        ApictlUtils.login("apictl_test");
        ApictlUtils.deployAPI("apictl_petstore", "apictl_test");
    }

    @Test(description = "Undeploy an API from a specific vhost only")
    public void undeployAPIFromSpecificVhost() throws CCTestException {
        ApictlUtils.undeployAPI("SwaggerPetstoreDeploy", "1.0.5", "apictl_test", "us.wso2.com");
    }

    @Test(description = "Undeploy from all vhosts", dependsOnMethods = {"undeployAPIFromSpecificVhost"})
    public void undeployAPI() throws CCTestException {
        ApictlUtils.undeployAPI("SwaggerPetstoreDeploy", "1.0.5", "apictl_test", null);
    }

    @AfterClass
    public void logoutAndRemoveEnv() throws CCTestException {
        ApictlUtils.logout("apictl_test");
        ApictlUtils.removeEnv("apictl_test");
    }
}
