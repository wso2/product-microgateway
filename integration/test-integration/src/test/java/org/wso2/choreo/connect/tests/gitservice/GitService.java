/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests.gitservice;

import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class GitService {

    public static void addGitServiceToDockerCompose(String dockerComposePath, String gitServiceFile) throws IOException, CCTestException {
        File targetClassesDir = new File(GitService.class.getProtectionDomain().getCodeSource().
                getLocation().getPath());
        String targetDir = targetClassesDir.getParentFile().toString();
        String gitService = GitService.class.getClassLoader()
                .getResource("dockerCompose/" + gitServiceFile).getPath();
        // Input files
        List<Path> inputs = Arrays.asList(
                Paths.get(dockerComposePath),
                Paths.get(gitService)
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
