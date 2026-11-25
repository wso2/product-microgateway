/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.protobuf;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for resolving the protoc executable used to compile gRPC service definitions.
 */
public class ProtocCommandBuilderTest {

    private static final String MAVEN_PROTOC_BASE_URL = "https://repo1.maven.org/maven2/com/google/protobuf/protoc/";

    private String originalOsName;
    private String originalOsArch;

    @BeforeMethod
    public void saveSystemProperties() {
        originalOsName = System.getProperty(OSDetector.OS_NAME_SYSTEM_PROPERTY);
        originalOsArch = System.getProperty(OSDetector.OS_ARCH_SYSTEM_PROPERTY);
    }

    @AfterMethod
    public void restoreSystemProperties() {
        System.setProperty(OSDetector.OS_NAME_SYSTEM_PROPERTY, originalOsName);
        System.setProperty(OSDetector.OS_ARCH_SYSTEM_PROPERTY, originalOsArch);
    }

    @Test(description = "protoc version must have a native Apple Silicon (osx-aarch_64) binary in maven central")
    public void testProtocVersion() {
        Assert.assertEquals(ProtocCommandBuilder.PROTOC_VERSION, "3.21.12");
    }

    @DataProvider(name = "classifiers")
    public Object[][] classifiers() {
        return new Object[][]{
                {"osx-aarch_64"},
                {"osx-x86_64"},
                {"linux-x86_64"},
                {"linux-aarch_64"},
                {"windows-x86_64"}
        };
    }

    @Test(description = "Download URL points to the protoc artifact of the configured version and classifier",
            dataProvider = "classifiers")
    public void testProtocDownloadUrl(String classifier) {
        Assert.assertEquals(ProtocCommandBuilder.getProtocDownloadUrl(classifier),
                MAVEN_PROTOC_BASE_URL + "3.21.12/protoc-3.21.12-" + classifier + ".exe");
    }

    @Test(description = "Local protoc file name contains the version so that an executable of an older version "
            + "downloaded earlier is not reused", dataProvider = "classifiers")
    public void testProtocFilenameIsVersioned(String classifier) {
        String filename = ProtocCommandBuilder.getProtocFilename(classifier);
        Assert.assertEquals(filename, "protoc-3.21.12-" + classifier + ".exe");
        Assert.assertNotEquals(filename, "protoc-" + classifier + ".exe");
    }

    @DataProvider(name = "platforms")
    public Object[][] platforms() {
        return new Object[][]{
                {"Mac OS X", "aarch64", "osx-aarch_64"},
                {"Mac OS X", "x86_64", "osx-x86_64"},
                {"Linux", "amd64", "linux-x86_64"},
                {"Linux", "aarch64", "linux-aarch_64"},
                {"Windows 10", "amd64", "windows-x86_64"}
        };
    }

    @Test(description = "OS classifier detection used to select the protoc artifact", dataProvider = "platforms")
    public void testDetectedClassifier(String osName, String osArch, String expectedClassifier) {
        System.setProperty(OSDetector.OS_NAME_SYSTEM_PROPERTY, osName);
        System.setProperty(OSDetector.OS_ARCH_SYSTEM_PROPERTY, osArch);
        Assert.assertEquals(OSDetector.getDetectedClassifier(), expectedClassifier);
        Assert.assertEquals(ProtocCommandBuilder.getProtocDownloadUrl(OSDetector.getDetectedClassifier()),
                MAVEN_PROTOC_BASE_URL + "3.21.12/protoc-3.21.12-" + expectedClassifier + ".exe");
    }
}
