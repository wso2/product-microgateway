package org.wso2am.micro.gw.tests.testCases.apiDeploy;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.ApictlUtils;

import java.io.File;
import java.io.IOException;

public class APiDeployViaApictlTestCase {

    @Test
    public void checkVersion() throws IOException {
        String versionByApictl = ApictlUtils.getVersion();
        Assert.assertEquals(versionByApictl, "4.0.0-alpha2","Expected apictl version not downloaded");
    }

    @Test
    public void createApiProject() throws IOException, MicroGWTestException {
        ApictlUtils.createProject( "openAPI.yaml",
                "petstore", null);
    }
}
