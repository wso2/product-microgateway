package org.wso2.micro.gateway.tests.endpoints;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.io.File;

public class EndpointOverrideTestCase extends EndpointsByReferenceTestCase {
    @Override
    @BeforeClass
    public void start() throws Exception {

        String project = "EndpointOverrideProject";
        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        //generate apis with CLI and start the micro gateway server
        String[] args = {"-e", "myEndpoint1_prod_endpoint_0=https://localhost:2380/v1", "-e",
                "myEndpoint2_prod_endpoint_0=https://localhost:2380/v2", "-e",
                "myEndpoint3_prod_endpoint_0=https://localhost:2380/v1", "-e",
                "myEndpoint3_prod_endpoint_1=https://localhost:2380/v2", "-e",
                "myEndpoint4_prod_endpoint_1=https://localhost:2380/v2",
        };
        super.init(project, new String[]{"endpoints" + File.separator + "endpoint_override.yaml"}, args);
    }
}
