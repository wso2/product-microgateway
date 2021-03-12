/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2am.micro.gw.tests.testCaseBefore;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.util.ApiDeployment;
import org.wso2am.micro.gw.tests.util.ApiProjectGenerator;
import org.wso2am.micro.gw.tests.util.TestConstant;
import org.wso2am.micro.gw.tests.util.TestGroup;

import java.io.File;

public class MgwWithJwtConfigAndAPI extends BaseTestCase {

    @BeforeTest(groups = { TestGroup.MGW_WITH_JWT_CONFIG_AND_API }, description = "initialise the setup")
    void start() throws Exception {
        String confPath = TestConstant.BASE_RESOURCE_DIR
                + File.separator + "jwtGenerator" + File.separator + "config.toml";
        super.startMGW(confPath);

        String apiZipfile = ApiProjectGenerator.createApictlProjZip("/apis/openApis/api.yaml",
                "/apis/openApis/swagger.yaml");
        ApiDeployment.deployAPI(apiZipfile);
    }

    @AfterTest(groups = { TestGroup.MGW_WITH_JWT_CONFIG_AND_API }, description = "stop the setup")
    void stop() {
        super.stopMGW();
    }
}
