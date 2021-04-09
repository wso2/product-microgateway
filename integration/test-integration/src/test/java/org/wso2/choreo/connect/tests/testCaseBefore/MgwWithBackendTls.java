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
package org.wso2.choreo.connect.tests.testCaseBefore;

import java.io.File;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.choreo.connect.tests.common.BaseTestCase;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.util.concurrent.TimeUnit;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

public class MgwWithBackendTls extends BaseTestCase {

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        String targetDir = Utils.getTargetDirPath();
        String confPath = targetDir + TestConstant.TEST_RESOURCES_PATH + File.separator + "cors" + File.separator
                + "cors-disabled-config.toml";
        super.startMGW(confPath, true);
        ApictlUtils.createProject("backend_tsl_openAPI.yaml", "backend_tsl_petstore", "backend_tls.crt");
        ApictlUtils.createProject( "cors_openAPI.yaml", "cors_petstore", null);

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("backend_tsl_petstore", "test");
        ApictlUtils.deployAPI("cors_petstore", "test");
        TimeUnit.SECONDS.sleep(5);
    }

    @AfterTest(description = "stop the setup")
    void stop() throws MicroGWTestException {
        super.stopMGW();
        ApictlUtils.removeEnv("test");
    }
}
