/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package org.wso2.apimgt.gateway.cli.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.rest.apim4x.ApiProjectDto;
import org.wso2.apimgt.gateway.cli.model.rest.apim4x.ApictlProjectDTO;
import org.wso2.apimgt.gateway.cli.model.rest.apim4x.Apim4xApiDto;
import org.wso2.apimgt.gateway.cli.model.rest.apim4x.DeploymentsDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.rest.RESTAPIServiceImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility functions to communicate with import REST APIs.
 */
public final class RESTAPIUtils {
    private static final Logger logger = LoggerFactory.getLogger(RESTAPIServiceImpl.class);

    private RESTAPIUtils() {

    }

    /**
     * Get inputStream string as string.
     *
     * @param input input stream
     * @return inout stream content as string
     * @throws IOException if read went wrong
     */
    public static String getResponseString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String str;
            while ((str = buffer.readLine()) != null) {
                content.append(str);
            }
            return content.toString();
        }
    }

    /**
     * Get input stream and convert it to an API project Object
     *
     * @param input Input stream
     * @param projectName project name
     * @return API project object
     */
    public static List<ExtendedAPI> getResponseAsExtendedApis(InputStream input, String projectName) {
        List<ExtendedAPI> extendedAPIs = new ArrayList<>();
        File tempDir = null;
        try {
            String projectPath = CmdUtils.getProjectDirectoryPath(projectName);
            tempDir = Files.createTempDirectory(new File(projectPath).toPath(), CliConstants.EXPORTED_API).toFile();
            ZipUtils.unzipFromInputStream(tempDir.getPath(), input);
            String deploymentsJsonPath = tempDir.getAbsolutePath() + File.separator + CliConstants.DEPLOYMENTS_JSON;
            File deploymentJsonFile = new File(deploymentsJsonPath);
            if (deploymentJsonFile.exists() && deploymentJsonFile.isFile()) {
                logger.debug("Deployment file exists");
                String jsonContent = FileUtils.readFileToString(deploymentJsonFile, StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                DeploymentsDTO deploymentsDTO = objectMapper.readValue(jsonContent, DeploymentsDTO.class);
                if (deploymentsDTO != null && deploymentsDTO.getData() != null) {
                    Set<ApiProjectDto> deployments = deploymentsDTO.getData().getDeployments();
                    if (!deployments.isEmpty()) {
                        logger.debug("Deployments size: " + deployments.size());
                        for (ApiProjectDto deployment : deployments) {
                            String apiFileName = tempDir.getAbsolutePath() + File.separator + deployment.getApiFile();
                            ZipUtils.unzipFromInputStream(tempDir.getPath(),
                                    Files.newInputStream(Paths.get(apiFileName)));
                        }
                    } else {
                        throw new CLIRuntimeException("No deployments found in the deployments file");
                    }
                }
                File[] apiDirectories = new File(tempDir.getPath()).listFiles(File::isDirectory);
                if (apiDirectories != null) {
                    for (File apiDirectory : apiDirectories) {
                        String apiJsonPath = apiDirectory.getPath() + File.separator + CliConstants.API_JSON;
                        File apiJsonFile = new File(apiJsonPath);
                        ObjectMapper apiObjectMapper = new ObjectMapper();
                        ApictlProjectDTO apiProject = apiObjectMapper.readValue(apiJsonFile, ApictlProjectDTO.class);
                        String definitionsPath = apiDirectory.getPath() + File.separator + CliConstants.DEFINITIONS_DIR;
                        File definitionsDir = new File(definitionsPath);
                        Apim4xApiDto apiInfo = apiProject.getData();
                        if (definitionsDir.exists() && definitionsDir.isDirectory()) {
                            String swaggerPath = definitionsPath + File.separator + CliConstants.API_SWAGGER;
                            File swaggerFile = new File(swaggerPath);
                            apiInfo.setApiDefinition(FileUtils.readFileToString(swaggerFile,
                                    StandardCharsets.UTF_8));
                        }
                        extendedAPIs.add(new ExtendedAPI(apiInfo));
                    }
                }
            } else {
                throw new CLIRuntimeException("Deployment file does not exist");
            }
        } catch (IOException e) {
            throw new CLIRuntimeException("Error while reading the response as extended APIs", e);
        } finally {
            if (tempDir.exists()) {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException e) {
                    logger.error("Error while deleting temp directory for the project: " + projectName);
                }
            }
        }
        return extendedAPIs;
    }
}
