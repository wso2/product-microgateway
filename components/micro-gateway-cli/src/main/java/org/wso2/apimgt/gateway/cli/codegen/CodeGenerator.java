/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.apimgt.gateway.cli.codegen;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.hashing.HashUtils;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.template.GenSrcFile;
import org.wso2.apimgt.gateway.cli.model.template.service.BallerinaService;
import org.wso2.apimgt.gateway.cli.model.template.service.ListenerEndpoint;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates Ballerina Services/Clients for a provided OAS definition.
 */
public class CodeGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    private static PrintStream outStream = System.out;

    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be used for generating Ballerina mock services and clients</p>
     *
     * @throws IOException                  when file operations fail
     * @throws BallerinaServiceGenException when code generator fails
     */
    public void generate(String projectName, List<ExtendedAPI> apis, boolean overwrite)
            throws IOException, BallerinaServiceGenException {

        String projectSrcPath = GatewayCmdUtils.getProjectGenSrcDirectoryPath((projectName));
        BallerinaService definitionContext;

        List<GenSrcFile> genFiles = new ArrayList<>();
        List<BallerinaService> serviceList = new ArrayList<>();
        for (ExtendedAPI api : apis) {
            outStream.println("ID for API " + api.getName() + " : " + api.getId());
            String apiHashId = HashUtils.generateAPIId(api.getName(), api.getVersion());
            OpenAPI openApi = new OpenAPIV3Parser().read(GatewayCmdUtils
                    .getProjectGenSwaggerPath(projectName, apiHashId));
            OpenAPICodegenUtils.setAdditionalConfig(api);
            BallerinaService ballerinaService = new BallerinaService();
            ballerinaService.setIsDevFirst(false);
            definitionContext = ballerinaService.buildContext(openApi, api);
            // we need to generate the bal service for default versioned apis as well
            if (api.getIsDefaultVersion()) {
                definitionContext.setQualifiedServiceName(CodegenUtils.trim(api.getName()));
                genFiles.add(generateService(definitionContext));
                // without building the definitionContext again we use the same context to build default version as
                // well. Hence setting the default version as false to generate the api with base path having version.
                api.setIsDefaultVersion(false);
                OpenAPICodegenUtils.setAdditionalConfig(api);
                definitionContext.setQualifiedServiceName(CodegenUtils.trim(api.getName() + "_" + api.getVersion()));
            }
            serviceList.add(definitionContext);
            genFiles.add(generateService(definitionContext));
            genFiles.add(generateSwagger(definitionContext));

        }
        genFiles.add(generateMainBal(serviceList));
        genFiles.add(generateCommonEndpoints());
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);

        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER);

        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);

    }


    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be used for generating Ballerina mock services and clients</p>
     *
     * @throws IOException when file operations fail
     */
    public void generate(String projectName, boolean overwrite)
            throws IOException {
        String projectSrcPath = GatewayCmdUtils.getProjectTargetModulePath((projectName));
        List<GenSrcFile> genFiles = new ArrayList<>();
        List<BallerinaService> serviceList = new ArrayList<>();
        List<String> openAPIDirectoryLocations = new ArrayList<>();
        String projectAPIDefGenLocation = GatewayCmdUtils.getProjectGenAPIDefinitionPath(projectName);
        openAPIDirectoryLocations.add(GatewayCmdUtils.getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR);

        if (Files.exists(Paths.get(projectAPIDefGenLocation))) {
            openAPIDirectoryLocations.add(projectAPIDefGenLocation);
        }

        //to store the available interceptors for validation purposes
        OpenAPICodegenUtils.setInterceptors(projectName);
        openAPIDirectoryLocations.forEach(openApiPath -> {
            try {
                Files.walk(Paths.get(openApiPath)).filter(path -> {
                    Path fileName = path.getFileName();
                    return fileName != null &&
                            (fileName.toString().endsWith(".json") || fileName.toString().endsWith(".yaml"));
                }).forEach(path -> {
                    try {
                        OpenAPI openAPI = new OpenAPIV3Parser().read(path.toString());
                        OpenAPICodegenUtils.validateOpenAPIDefinition(openAPI, path.toString());
                        ExtendedAPI api = OpenAPICodegenUtils.generateAPIFromOpenAPIDef(openAPI, path);
                        BallerinaService definitionContext;
                        OpenAPICodegenUtils.setAdditionalConfigsDevFirst(api, openAPI, path.toString());

                        definitionContext = new BallerinaService().buildContext(openAPI, api);
                        genFiles.add(generateService(definitionContext));

                        serviceList.add(definitionContext);
                    } catch (BallerinaServiceGenException e) {
                        throw new CLIRuntimeException("Swagger definition cannot be parsed to ballerina code", e);
                    } catch (IOException e) {
                        throw new CLIInternalException("File write operations failed during ballerina code "
                                + "generation", e);
                    }
                });
            } catch (IOException e) {
                throw new CLIInternalException("File write operations failed during ballerina code generation", e);
            }
        });

        genFiles.add(generateMainBal(serviceList));
        genFiles.add(generateOpenAPIJsonConstantsBal(serviceList));
        genFiles.add(generateCommonEndpoints());
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + GatewayCliConstants.GW_DIST_START_UP_EXTENSION,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_START_UP_EXTENSION);
    }

    /**
     * Generate code for rest ballerina rest.
     *
     * @param context model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateService(BallerinaService context) throws IOException {
        String concatTitle = context.getQualifiedServiceName();
        String srcFile = concatTitle + GeneratorConstants.BALLERINA_EXTENSION;
        String mainContent = getContent(context,
                GeneratorConstants.SERVICE_TEMPLATE_NAME);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, mainContent);
    }

    /**
     * Generate code for Main ballerina file
     *
     * @param services list of model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateMainBal(List<BallerinaService> services) throws IOException {
        String srcFile = GeneratorConstants.MAIN_TEMPLATE_NAME + GeneratorConstants.BALLERINA_EXTENSION;
        String mainContent = getContent(services, GeneratorConstants.MAIN_TEMPLATE_NAME);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, mainContent);
    }

    /**
     * Generate bal file with open API definitions as ballerina json variables.
     *
     * @param services list of model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateOpenAPIJsonConstantsBal(List<BallerinaService> services) throws IOException {
        String srcFile = GeneratorConstants.OPEN_API_JSON_CONSTANTS + GeneratorConstants.BALLERINA_EXTENSION;
        String mainContent = getContent(services, GeneratorConstants.OPEN_API_JSON_CONSTANTS);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, mainContent);
    }

    /**
     * Generate common endpoint
     *
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateCommonEndpoints() throws IOException {
        String srcFile = GeneratorConstants.LISTENERS + GeneratorConstants.BALLERINA_EXTENSION;
        ListenerEndpoint listenerEndpoint = new ListenerEndpoint().buildContext();
        String endpointContent = getContent(listenerEndpoint, GeneratorConstants.LISTENERS_TEMPLATE_NAME);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, endpointContent);
    }

    /**
     * Retrieve generated source content as a String value.
     *
     * @param endpoints    context to be used by template engine
     * @param templateName name of the template to be used for this code generation
     * @return String with populated template
     * @throws IOException when template population fails
     */
    private String getContent(Object endpoints, String templateName) throws IOException {
        Template template = CodegenUtils.compileTemplate(GeneratorConstants.DEFAULT_TEMPLATE_DIR, templateName);
        Context context = Context.newBuilder(endpoints)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();
        return template.apply(context);
    }

    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be user for generating Ballerina mock services and clients</p>
     *
     * @param projectName name of the project being set up
     * @param apiDef      api definition string
     * @param overwrite   whether existing files overwrite or not
     * @throws IOException                  when file operations fail
     */
    @SuppressWarnings("unused")
    public void generateGrpc(String projectName, String apiDef, boolean overwrite)
            throws IOException {
        BallerinaService definitionContext;

        //apiId is not considered as the method is not functioning
        String projectSrcPath = GatewayCmdUtils
                .getProjectGenSwaggerPath(projectName, "");

        String projectGrpcPath = GatewayCmdUtils.getProjectGrpcDirectoryPath();
        List<GenSrcFile> genFiles = new ArrayList<>();
        File dir = new File(projectGrpcPath);
        genFiles.add(generateCommonEndpoints());

        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);

        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getFiltersFolderLocation() + File.separator
                        + GatewayCliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getFiltersFolderLocation() + File.separator
                        + GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);

        File[] fileList = dir.listFiles();
        if (fileList == null) {
            // this is temporary. need to re-evaluate what to do in this case, when revisiting GRPC support
            return;
        }

        for (File file : fileList) {
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();
            FileSystem fileSys = FileSystems.getDefault();
            Path source = fileSys.getPath(filePath);
            Path destination = fileSys.getPath(projectSrcPath + File.separator + fileName);
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        File temp = new File(GatewayCmdUtils.getProjectGrpcSoloDirectoryPath());
        if (!dir.delete() || !temp.delete()) {
            logger.debug("Failed to delete GRPC temp files");
        }
    }

    /**
     * Generate swagger files
     *
     * @param context model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateSwagger(BallerinaService context) throws IOException {
        String concatTitle = context.getName();
        String srcFile = concatTitle + GeneratorConstants.SWAGGER_FILE_SUFFIX + GeneratorConstants.JSON_EXTENSION;
        String mainContent = getContent(context, GeneratorConstants.GENERATESWAGGER_TEMPLATE_NAME);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, mainContent);
    }
}
