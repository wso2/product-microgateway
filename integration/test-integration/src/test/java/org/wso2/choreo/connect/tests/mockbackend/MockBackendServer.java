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

package org.wso2.choreo.connect.tests.mockbackend;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.FileFileFilter;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.FileFilterUtils;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.IOFileFilter;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

/**
 * Mock backend server class.
 */
public class MockBackendServer {

    /**
     * Get Mock backend server module root path.
     *
     * @param dockerComposePath - path for the mgw setup docker-compose file
     * @param backendServiceFile  backendService different to the default, to be appended to docker-compose file
     * @throws IOException          if something goes wrong while file operations
     * @throws CCTestException if something goes wrong while copying the config file
     */
    public static void addMockBackendServiceToDockerCompose(String dockerComposePath, String backendServiceFile)
            throws IOException, CCTestException {

        File targetClassesDir = new File(MockBackendServer.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();
        String backendService = MockBackendServer.class.getClassLoader()
                .getResource("dockerCompose/backend-service.yaml").getPath();
        if (StringUtils.isNotEmpty(backendServiceFile)) {
            // if tls enabled, the command in docker-compose should be overridden
            backendService = MockBackendServer.class.getClassLoader()
                    .getResource("dockerCompose/" + backendServiceFile).getPath();
        }

        // Input files
        List<Path> inputs = Arrays.asList(
                Paths.get(dockerComposePath),
                Paths.get(backendService)
        );

        // Output file
        String tmpDockerCompose = targetDir + File.separator + System.currentTimeMillis() + ".yaml";
        File fileTmp = new File(tmpDockerCompose);
        fileTmp.createNewFile();
        Path output = Paths.get(tmpDockerCompose);


        // Charset for read and write
        Charset charset = StandardCharsets.UTF_8;

        // Join files (lines)
        for (Path path : inputs) {
            List<String> lines = Files.readAllLines(path, charset);
            Files.write(output, lines, charset, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        }

        Utils.copyFile(tmpDockerCompose, dockerComposePath);
        fileTmp.delete();
    }
}
