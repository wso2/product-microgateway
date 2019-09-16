/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.endpoints;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

public class EndpointWithSecurityTestCase extends EndpointsByReferenceTestCase {
    @Override
    @BeforeClass
    public void start() throws Exception {

        String project = "EndpointWithSecurityProject";
        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        //generate apis with CLI and start the micro gateway server
        String[] args = {"--myEndpoint1_prod_basic_password=admin", "--myEndpoint2_prod_basic_password=admin",
                 "--myEndpoint3_prod_basic_password=admin", "--myEndpoint4_prod_basic_password=admin"};
        super.init(project, new String[]{"endpoints/endpoint_security.yaml"}, args);
    }
}
