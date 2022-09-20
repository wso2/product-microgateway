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

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class CcWithMtlsDefault {
    CcInstance ccInstance;

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {

        ccInstance = new CcInstance.Builder().withNewConfig("mtls-enabled-config.toml").
                withNewDockerCompose("cc-cacert-mounted-mtls.yaml").withClientCertValidation().build(false);
        ccInstance.start();

        ApictlUtils.createProject("mutual_ssl_openAPI.yaml", "MutualSSL", null, null, null, "mutual_ssl_api.yaml", false, "client_certificate.crt", "client_certificates.yaml");
        ApictlUtils.createProject("mutual_ssl_optional_openAPI.yaml", "MutualSSLOptional", null, null, null, "mutual_ssl_optional_api.yaml", false, "client_certificate.crt", "client_certificates_optional.yaml");
        ApictlUtils.createProject("mutual_ssl_mandatory_openAPI.yaml", "MutualSSLMandatory", null, null, null, "mutual_ssl_mandatory_api.yaml", false, "client_certificate.crt", "client_certificates_mandatory.yaml");

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("MutualSSL", "test");
        ApictlUtils.deployAPI("MutualSSLOptional", "test");
        ApictlUtils.deployAPI("MutualSSLMandatory", "test");

        TimeUnit.SECONDS.sleep(5);
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
