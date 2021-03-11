/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2am.micro.gw.tests.util.TestGroup;

public class MgwWithBackendTlsAndAPI extends BaseTestCase {

    @BeforeTest(groups = TestGroup.MGW_WITH_BACKEND_TLS_AND_API, description = "initialise the setup")
    void start() throws Exception {
        super.startMGW(null, true);

        String apiZipfile = ApiProjectGenerator.createApictlProjZip(
                "backendtls/api.yaml","backendtls/swagger.yaml",
                "backendtls/backend.crt");
        ApiDeployment.deployAPI(apiZipfile);
    }

    @AfterTest(groups = TestGroup.MGW_WITH_BACKEND_TLS_AND_API, description = "stop the setup")
    void stop() {
        super.stopMGW();
    }
}
