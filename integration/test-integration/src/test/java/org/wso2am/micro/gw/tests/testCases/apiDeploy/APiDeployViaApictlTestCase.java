package org.wso2am.micro.gw.tests.testCases.apiDeploy;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.ApictlUtils;

import java.io.IOException;

public class APiDeployViaApictlTestCase {

    @BeforeClass
    public void createApiProject() throws IOException, MicroGWTestException {
        ApictlUtils.createProject( "deploy_openAPI.yaml",
                "apictl_petstore", null);
        ApictlUtils.addEnv("test");
    }

    @Test
    public void deployAPI() throws MicroGWTestException {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("apictl_petstore", "test");
    }

    @Test
    public void undeployAPI() throws MicroGWTestException {
        ApictlUtils.undeployAPI("SwaggerPetstoreDeploy", "1.0.5", "test");
    }

    @AfterClass
    public void logoutAndRemoveEnv() throws MicroGWTestException {
        ApictlUtils.logout("test");
        ApictlUtils.removeEnv("test");
    }
}
