/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.setup.standalone;

import org.awaitility.Awaitility;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.SourceControlUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.concurrent.TimeUnit;

public class CcWithSourceControl {

    CcInstance ccInstance;

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        ccInstance = new CcInstance.Builder()
                .withGitServiceFile("git-service.yaml")
                .withNewConfig("cc-with-source-control.toml")
                .withVolumeMountDir("gitea")
                .build();
        ccInstance.start();

        Awaitility.await().pollDelay(10, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(ccInstance.isGitHealthy());

        // Test the status of the Git Service REST API
        SourceControlUtils.testGitStatus();
        // Create a new project
        SourceControlUtils.createRepo();
        // Commit the initial files to the project
        SourceControlUtils.commitApiProjectToRepo(Utils.getTargetDirPath()
                        + TestConstant.TEST_RESOURCES_PATH + SourceControlUtils.ARTIFACTS_DIR + SourceControlUtils.DIRECTORY,
                false, "Add API Project");

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        Utils.delay(5000, "Interrupted while waiting for adapter to sync");
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
