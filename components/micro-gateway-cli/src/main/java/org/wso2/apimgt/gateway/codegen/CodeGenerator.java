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

package org.wso2.apimgt.gateway.codegen;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.wso2.apimgt.gateway.codegen.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.codegen.model.BallerinaService;
import org.wso2.apimgt.gateway.codegen.model.GenSrcFile;
import org.wso2.apimgt.gateway.codegen.service.bean.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.codegen.utils.CodegenUtils;
import org.wso2.apimgt.gateway.codegen.utils.GeneratorConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates Ballerina Services/Clients for a provided OAS definition.
 */
public class CodeGenerator {
    private String srcPackage;
    private String modelPackage;

    /**
     * Generates ballerina source for provided Open APIDetailedDTO Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina package at {@code outPath}
     * <p>Method can be user for generating Ballerina mock services and clients</p>
     *
     * @throws IOException                  when file operations fail
     * @throws BallerinaServiceGenException when code generator fails
     */
    public void generate(String labelPath, List<ExtendedAPI> apis, boolean overwrite)
            throws IOException, BallerinaServiceGenException {
        BallerinaService definitionContext;
        SwaggerParser parser;
        Swagger swagger;
        List<GenSrcFile> genFiles = new ArrayList<>();
        for (ExtendedAPI api : apis) {
            parser = new SwaggerParser();
            swagger = parser.parse(api.getApiDefinition());
            definitionContext = new BallerinaService().buildContext(swagger, api).srcPackage(srcPackage)
                    .modelPackage(srcPackage);
            genFiles.add(generateService(definitionContext));
        }
        genFiles.add(generateCommonEndpoints());
        writeGeneratedSources(genFiles, Paths.get(labelPath), overwrite);
    }

    private Template compileTemplate(String defaultTemplateDir, String templateName) throws IOException {
        String templatesDirPath = System.getProperty(GeneratorConstants.TEMPLATES_DIR_PATH_KEY, defaultTemplateDir);
        ClassPathTemplateLoader cpTemplateLoader = new ClassPathTemplateLoader((templatesDirPath));
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(templatesDirPath);
        cpTemplateLoader.setSuffix(GeneratorConstants.TEMPLATES_SUFFIX);
        fileTemplateLoader.setSuffix(GeneratorConstants.TEMPLATES_SUFFIX);
        Handlebars handlebars = new Handlebars().with(cpTemplateLoader, fileTemplateLoader);
        handlebars.registerHelpers(StringHelpers.class);
        handlebars.registerHelper("equals", (object, options) -> {
            CharSequence result;
            Object param0 = options.param(0);

            if (param0 == null) {
                throw new IllegalArgumentException("found 'null', expected 'string'");
            }
            if (object != null) {
                if (object.toString().equals(param0.toString())) {
                    result = options.fn(options.context);
                } else {
                    result = options.inverse();
                }
            } else {
                result = null;
            }

            return result;
        });
        return handlebars.compile(templateName);
    }

    private void writeGeneratedSources(List<GenSrcFile> sources, Path srcPath, boolean overwrite) throws IOException {
        Path filePath;
        for (GenSrcFile file : sources) {
            filePath = srcPath.resolve(file.getFileName());
            if (Files.notExists(filePath)) {
                CodegenUtils.writeFile(filePath, file.getContent());
            } else {
                if (overwrite) {
                    Files.delete(filePath);
                    CodegenUtils.writeFile(filePath, file.getContent());
                }
            }
        }
    }

    /**
     * Generate code for service ballerina service.
     *
     * @param context model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateService(BallerinaService context) throws IOException {
        GenSrcFile sourceFile = null;
        String concatTitle = context.getQualifiedServiceName();
        String srcFile = concatTitle + ".bal";
        String mainContent = getContent(context, GeneratorConstants.DEFAULT_SERVICE_DIR,
                GeneratorConstants.SERVICE_TEMPLATE_NAME);
        sourceFile = new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcPackage, srcFile, mainContent);
        return sourceFile;
    }

    /**
     * Generate common endpoint
     *
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateCommonEndpoints() throws IOException {
        GenSrcFile sourceFile = null;
        String srcFile = GeneratorConstants.ENDPOINTS + GeneratorConstants.BALLERINA_EXTENSION;
        String endpointContent = getContent(null, GeneratorConstants.DEFAULT_SERVICE_DIR,
                GeneratorConstants.ENDPOINT_TEMPLATE_NAME);
        sourceFile = new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcPackage, srcFile, endpointContent);
        return sourceFile;
    }


    /**
     * Retrieve generated source content as a String value.
     *
     * @param endpoints       context to be used by template engine
     * @param templateDir  templates directory
     * @param templateName name of the template to be used for this code generation
     * @return String with populated template
     * @throws IOException when template population fails
     */
    private String getContent(Object endpoints, String templateDir, String templateName) throws IOException {
        Template template = compileTemplate(templateDir, templateName);
        Context context = Context.newBuilder(endpoints)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();
        return template.apply(context);
    }

    public String getSrcPackage() {
        return srcPackage;
    }

    public void setSrcPackage(String srcPackage) {
        this.srcPackage = srcPackage;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }
}
