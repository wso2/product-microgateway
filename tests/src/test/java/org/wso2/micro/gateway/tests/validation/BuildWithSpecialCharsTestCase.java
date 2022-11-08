package org.wso2.micro.gateway.tests.validation;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;


public class BuildWithSpecialCharsTestCase extends BaseTestCase {

//
    @Test(description = "Test invalid response body for the Get request")
    private void testOpenAPIBuildWithSpecialCharacters() throws Exception {
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        super.init("special-character-project", new String[]{"validation/specialCharacterAPI.json"}, null);
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
