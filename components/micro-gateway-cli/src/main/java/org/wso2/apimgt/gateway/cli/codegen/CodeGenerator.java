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
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.template.BallerinaToml;
import org.wso2.apimgt.gateway.cli.model.template.GenSrcFile;
import org.wso2.apimgt.gateway.cli.model.template.service.BallerinaService;
import org.wso2.apimgt.gateway.cli.model.template.service.ListenerEndpoint;
import org.wso2.apimgt.gateway.cli.protobuf.ProtobufParser;
import org.wso2.apimgt.gateway.cli.utils.CmdUtils;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenAPICodegenUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
    public static String projectName;

    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be used for generating Ballerina mock services and clients</p>
     *
     * @throws IOException when file operations fail
     */
    public void generate(String projectName, boolean overwrite)
            throws IOException {
        String projectSrcPath = CmdUtils.getProjectTargetModulePath((projectName));
        List<GenSrcFile> genFiles = new ArrayList<>();
        List<BallerinaService> serviceList = new ArrayList<>();
        List<BallerinaService> openAPIServiceList = new ArrayList<>();
        List<String> openAPIDirectoryLocations = new ArrayList<>();
        String projectAPIDefGenLocation = CmdUtils.getProjectGenAPIDefinitionPath(projectName);
        openAPIDirectoryLocations.add(CmdUtils.getProjectDirectoryPath(projectName) + File.separator
                + CliConstants.PROJECT_API_DEFINITIONS_DIR);
        String grpcDirLocation = CmdUtils.getGrpcDefinitionsDirPath(projectName);
        if (Files.exists(Paths.get(projectAPIDefGenLocation))) {
            openAPIDirectoryLocations.add(projectAPIDefGenLocation);
        }
        CodeGenerator.projectName = projectName;
        BallerinaToml ballerinaToml = new BallerinaToml();
        ballerinaToml.setToolkitHome(CmdUtils.getCLIHome());

        //to store the available interceptors for validation purposes
        OpenAPICodegenUtils.setInterceptors(projectName);
        openAPIDirectoryLocations.forEach(openApiPath -> {
            try {
                Files.walk(Paths.get(openApiPath)).filter(path -> {
                    Path fileName = path.getFileName();
                    return fileName != null && (fileName.toString().endsWith(CliConstants.JSON_EXTENSION) ||
                            fileName.toString().endsWith(CliConstants.YAML_EXTENSION));
                }).forEach(path -> {
                    try {
                        OpenAPI openAPI = new OpenAPIV3Parser().read(path.toString());
                        String openAPIContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                        String openAPIAsJson = OpenAPICodegenUtils.getOpenAPIAsJson(openAPI, openAPIContent, path);
                        String openAPIContentAsJson = openAPIAsJson;
                        if (path.toString().endsWith(CliConstants.YAML_EXTENSION)) {
                            openAPIContentAsJson = OpenAPICodegenUtils.convertYamlToJson(openAPIContent);
                        }
                        String openAPIVersion = OpenAPICodegenUtils.findSwaggerVersion(openAPIContentAsJson, false);
                        OpenAPICodegenUtils.validateOpenAPIDefinition(openAPI, path.toString(), openAPIVersion);
                        OpenAPICodegenUtils.setOauthSecuritySchemaList(openAPI);
                        OpenAPICodegenUtils.setSecuritySchemaList(openAPI);
                        OpenAPICodegenUtils.setOpenAPIDefinitionEndpointReferenceExtensions(openAPI.getExtensions());
                        ExtendedAPI api = OpenAPICodegenUtils.generateAPIFromOpenAPIDef(openAPI, openAPIAsJson);
                        BallerinaService definitionContext;
                        OpenAPICodegenUtils.setAdditionalConfigsDevFirst(api, openAPI, path.toString());

                        definitionContext = new BallerinaService().buildContext(openAPI, api);
                        genFiles.add(generateService(definitionContext));
                        serviceList.add(definitionContext);
                        openAPIServiceList.add(definitionContext);
                        ballerinaToml.addDependencies(definitionContext);
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

        // to process protobuf files
        if (Paths.get(grpcDirLocation).toFile().exists()) {
            Files.walk(Paths.get(grpcDirLocation)).filter(path -> {
                Path filename = path.getFileName();
                return filename != null && (filename.toString().endsWith(".proto"));
            }).forEach(path -> {
                String descriptorPath = CmdUtils.getProtoDescriptorPath(projectName, path.getFileName().toString());
                try {
                    ArrayList<OpenAPI> openAPIs = new ProtobufParser().generateOpenAPI(path.toString(), descriptorPath);
                    if (openAPIs.size() > 0) {
                        for (OpenAPI openAPI : openAPIs) {
                            String openAPIContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            OpenAPICodegenUtils.setOauthSecuritySchemaList(openAPI);
                            OpenAPICodegenUtils.setSecuritySchemaList(openAPI);
                            createProtoOpenAPIFile(projectName, openAPI);
                            BallerinaService definitionContext = generateDefinitionContext(openAPI, openAPIContent,
                                    path, true);
                            genFiles.add(generateService(definitionContext));
                            serviceList.add(definitionContext);
                            ballerinaToml.addDependencies(definitionContext);
                        }
                    }
                } catch (IOException e) {
                    throw new CLIRuntimeException("Protobuf file cannot be parsed to " +
                            "ballerina code", e);
                } catch (BallerinaServiceGenException e) {
                    throw new CLIInternalException("File write operations failed during the ballerina code "
                            + "generation for the protobuf files", e);
                }
            });
        }

        genFiles.add(generateMainBal(serviceList));
        genFiles.add(generateOpenAPIJsonConstantsBal(serviceList));
        genFiles.add(generateTokenServices());
        genFiles.add(generateHealthCheckService());
        genFiles.add(generateCommonEndpoints());
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);

        // generate Ballerina.toml file
        ballerinaToml.addLibs(projectName);
        GenSrcFile toml = generateBallerinaTOML(ballerinaToml);
        String tomlPath = CmdUtils.getProjectTargetGenDirectoryPath(projectName)
                + File.separator + CliConstants.BALLERINA_TOML_FILE;
        CodegenUtils.writeFile(Paths.get(tomlPath), toml.getContent());

        CmdUtils.copyFilesToSources(CmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + CliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + CliConstants.GW_DIST_EXTENSION_FILTER);
        CmdUtils.copyFilesToSources(CmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + CliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION,
                projectSrcPath + File.separator + CliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);
        CmdUtils.copyFilesToSources(CmdUtils.getProjectExtensionsDirectoryPath(projectName)
                        + File.separator + CliConstants.GW_DIST_START_UP_EXTENSION,
                projectSrcPath + File.separator + CliConstants.GW_DIST_START_UP_EXTENSION);
    }

    private BallerinaService generateDefinitionContext(OpenAPI openAPI, String openAPIContent, Path path,
                                                       boolean isGrpc) throws IOException,
            BallerinaServiceGenException {
        ExtendedAPI api;
        if (isGrpc) {
            api = OpenAPICodegenUtils.generateGrpcAPIFromOpenAPI(openAPI);
        } else {
            api = OpenAPICodegenUtils.generateAPIFromOpenAPIDef(openAPI, openAPIContent);
        }
        BallerinaService definitionContext;
        OpenAPICodegenUtils.setAdditionalConfigsDevFirst(api, openAPI, path.toString());
        definitionContext = new BallerinaService().buildContext(openAPI, api);
        return definitionContext;
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
     * Generate code for Main ballerina file.
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
     * Generate code for ballerina toml.
     *
     * @param toml Mustache data holder for Ballerina.toml file
     * @return generated source file as a {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateBallerinaTOML(BallerinaToml toml) throws IOException {
        String srcFile = GeneratorConstants.BALLERINA_TOML_TEMPLATE_NAME + GeneratorConstants.TOML_EXTENSION;
        String mainContent = getContent(toml, GeneratorConstants.BALLERINA_TOML_TEMPLATE_NAME);
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
        String srcFile = GeneratorConstants.OPEN_API_JSON_CONSTANTS + GeneratorConstants.JSON_EXTENSION;
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
     * Generate token proxy services. For ex: /token, /authorize, /revoke and etc
     *
     * @return generated source file  {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateTokenServices() throws IOException {
        String srcFile = GeneratorConstants.TOKEN_SERVICES + GeneratorConstants.BALLERINA_EXTENSION;
        String endpointContent = getContent(CmdUtils.getConfig(), GeneratorConstants.TOKEN_SERVICES);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, endpointContent);
    }

    /**
     * Generate health check  services. For ex: /health
     *
     * @return generated source file  {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateHealthCheckService() throws IOException {
        String srcFile = GeneratorConstants.HEALTH_CHECK + GeneratorConstants.BALLERINA_EXTENSION;
        String endpointContent = getContent(CmdUtils.getConfig(), GeneratorConstants.HEALTH_CHECK);
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
        String projectSrcPath = CmdUtils
                .getProjectGenSwaggerPath(projectName, "");

        String projectGrpcPath = CmdUtils.getProjectGrpcDirectoryPath();
        List<GenSrcFile> genFiles = new ArrayList<>();
        File dir = new File(projectGrpcPath);
        genFiles.add(generateCommonEndpoints());

        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);

        CmdUtils.copyFilesToSources(CmdUtils.getFiltersFolderLocation() + File.separator
                        + CliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + CliConstants.GW_DIST_EXTENSION_FILTER);
        CmdUtils.copyFilesToSources(CmdUtils.getFiltersFolderLocation() + File.separator
                        + CliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION,
                projectSrcPath + File.separator + CliConstants.GW_DIST_TOKEN_REVOCATION_EXTENSION);

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

        File temp = new File(CmdUtils.getProjectGrpcSoloDirectoryPath());
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

    /**
     * Save the openAPI definition created from the services included in the .proto files.
     *
     * @param projectName projectName
     * @param openAPI {@link OpenAPI} object corresponding to the gRPC service
     */
    private void createProtoOpenAPIFile(String projectName, OpenAPI openAPI) {
        String protoOpenAPIDirPath =  CmdUtils.getProjectTargetGenGrpcSrcOpenAPIsDirectory(projectName);
        String fileName = openAPI.getInfo().getTitle() + "_" +
                openAPI.getInfo().getVersion().replace(".", "_") + CliConstants.YAML_EXTENSION;
        String protoOpenAPIFilePath =  protoOpenAPIDirPath + File.separator + fileName;
        try {
            CmdUtils.createFile(protoOpenAPIDirPath, fileName, true);
            CmdUtils.writeContent(Yaml.pretty(openAPI), new File(protoOpenAPIFilePath));
        } catch (IOException e) {
            throw new CLIInternalException("Error while writing openAPI files to the directory: " +
                    protoOpenAPIDirPath + ".");
        }
    }
}
