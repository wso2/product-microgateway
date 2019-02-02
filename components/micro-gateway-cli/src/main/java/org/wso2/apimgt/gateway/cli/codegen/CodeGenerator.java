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
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.apimgt.gateway.cli.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.template.GenSrcFile;
import org.wso2.apimgt.gateway.cli.model.template.service.BallerinaService;
import org.wso2.apimgt.gateway.cli.model.template.service.ListenerEndpoint;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.OpenApiCodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class generates Ballerina Services/Clients for a provided OAS definition.
 */
public class CodeGenerator {

    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be user for generating Ballerina mock services and clients</p>
     *
     * @throws IOException                  when file operations fail
     * @throws BallerinaServiceGenException when code generator fails
     */
    public void generate(String projectName, List<ExtendedAPI> apis, boolean overwrite)
            throws IOException, BallerinaServiceGenException {

        BallerinaService definitionContext;
        SwaggerParser parser;
        Swagger swagger;
        String projectSrcPath = GatewayCmdUtils
                .getProjectSrcDirectoryPath(projectName);
        List<GenSrcFile> genFiles = new ArrayList<>();
        for (ExtendedAPI api : apis) {
            parser = new SwaggerParser();
            swagger = parser.parse(api.getApiDefinition());
            definitionContext = new BallerinaService().buildContext(swagger, api);
            // we need to generate the bal service for default versioned apis as well
            if (definitionContext.getApi().getIsDefaultVersion()) {
                // without building the definitionContext again we use the same context to build default version as
                // well. Hence setting the default version as false to generate the api with base path having version.
                definitionContext.getApi().setIsDefaultVersion(false);
                genFiles.add(generateService(definitionContext));
                definitionContext.getApi().setIsDefaultVersion(true);
                definitionContext.setQualifiedServiceName(CodegenUtils.trim(api.getName()));
            }

            genFiles.add(generateService(definitionContext));

        }
        genFiles.add(generateCommonEndpoints());
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getFiltersFolderLocation() + File.separator
                        + GatewayCliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER);
    }

    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be user for generating Ballerina mock services and clients</p>
     *
     * @param projectName         name of the project being set up
     * @param apiDefinitions      api definitions
     * @param endpointDefinitions endpoint definitions
     * @param overwrite           whether existing files overwrite or not
     * @throws IOException                  when file operations fail
     * @throws BallerinaServiceGenException when code generator fails
     */
    public void generate(String projectName, String[] apiDefinitions, String[] endpointDefinitions, boolean overwrite)
            throws IOException, BallerinaServiceGenException {

        BallerinaService definitionContext;
        SwaggerParser parser;
        Swagger swagger;
        String projectSrcPath = GatewayCmdUtils.getProjectSrcDirectoryPath(projectName);
        List<GenSrcFile> genFiles = new ArrayList<>();

        parser = new SwaggerParser();
        for (int i = 0; i < apiDefinitions.length; i++) {
            swagger = parser.parse(apiDefinitions[i]);
            ExtendedAPI api = new ExtendedAPI();
            api.setName(swagger.getInfo().getTitle());
            api.setVersion(swagger.getInfo().getVersion());
            api.setContext(swagger.getBasePath());
            api.setEndpointConfig(endpointDefinitions[i]);
            api.setTransport(Arrays.asList("http", "https"));
            OpenApiCodegenUtils.setAdditionalConfigs(api);
            definitionContext = new BallerinaService().buildContext(swagger, api);
            genFiles.add(generateService(definitionContext));
        }
        genFiles.add(generateCommonEndpoints());
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(projectSrcPath), overwrite);
        GatewayCmdUtils.copyFilesToSources(GatewayCmdUtils.getFiltersFolderLocation() + File.separator
                        + GatewayCliConstants.GW_DIST_EXTENSION_FILTER,
                projectSrcPath + File.separator + GatewayCliConstants.GW_DIST_EXTENSION_FILTER);
        GatewayCmdUtils.copyFolder(GatewayCmdUtils.getPoliciesFolderLocation(), projectSrcPath
                + File.separator + GatewayCliConstants.GW_DIST_POLICIES);
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
        String mainContent = getContent(context, GeneratorConstants.DEFAULT_TEMPLATE_DIR,
                GeneratorConstants.SERVICE_TEMPLATE_NAME);
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
        ListenerEndpoint listnerEndpoint = new ListenerEndpoint().buildContext();
        String endpointContent = getContent(listnerEndpoint, GeneratorConstants.DEFAULT_TEMPLATE_DIR,
                GeneratorConstants.LISTENERS_TEMPLATE_NAME);
        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, endpointContent);
    }

    /**
     * Retrieve generated source content as a String value.
     *
     * @param endpoints    context to be used by template engine
     * @param templateDir  templates directory
     * @param templateName name of the template to be used for this code generation
     * @return String with populated template
     * @throws IOException when template population fails
     */
    private String getContent(Object endpoints, String templateDir, String templateName) throws IOException {

        Template template = CodegenUtils.compileTemplate(templateDir, templateName);
        Context context = Context.newBuilder(endpoints)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();
        return template.apply(context);
    }
}
