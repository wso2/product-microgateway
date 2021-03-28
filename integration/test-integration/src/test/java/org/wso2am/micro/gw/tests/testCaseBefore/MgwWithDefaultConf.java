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

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.ApictlUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MgwWithDefaultConf extends BaseTestCase {

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        super.startMGW();
        ApictlUtils.createProject( "prod_and_sand_openAPI.yaml", "prod_and_sand_petstore", null);
        ApictlUtils.createProject( "prod_openAPI.yaml", "prod_petstore", null);
        ApictlUtils.createProject( "sand_openAPI.yaml", "sand_petstore", null);

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("petstore", "test");
        ApictlUtils.deployAPI("prod_and_sand_petstore", "test");
        ApictlUtils.deployAPI("prod_petstore", "test");
        ApictlUtils.deployAPI("sand_petstore", "test");
        TimeUnit.SECONDS.sleep(5);
    }

    @AfterTest(description = "stop the setup")
    void stop() throws MicroGWTestException {
        super.stopMGW();
        ApictlUtils.removeEnv("test");
    }
}
