/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import org.apache.commons.io.FileUtils;
import org.wso2.apimgt.gateway.codegen.exception.BallerinaServiceGenException;
import org.wso2.apimgt.gateway.codegen.model.GenSrcFile;
import org.wso2.apimgt.gateway.codegen.model.ThrottlePolicy;
import org.wso2.apimgt.gateway.codegen.model.ThrottlePolicyInitializer;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.codegen.utils.CodegenUtils;
import org.wso2.apimgt.gateway.codegen.utils.GeneratorConstants;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates Ballerina Services/Clients for a provided OAS definition.
 */

public class ThrottlePolicyGenerator {
    private String srcPackage;
    private String modelPackage;

   /* *//**
     * Generate ballerina and stream source for a given app and subs policies
     *
     * @param outPath              Destination file path to save generated source files. If not provided
     *                             {@code definitionPath} will be used as the default destination path
     * @param applicationPolicies  list of app policies
     * @param subscriptionPolicies list of subs policies
     * @throws IOException                  when file operations fail
     * @throws BallerinaServiceGenException when code generator fails
     *//*
    public void generate(String outPath, List<ApplicationThrottlePolicyDTO> applicationPolicies,
            List<SubscriptionThrottlePolicyDTO> subscriptionPolicies) throws IOException, BallerinaServiceGenException {
        Path srcPath = CodegenUtils.getSourcePath(srcPackage, outPath);
        Path implPath = CodegenUtils.getImplPath(srcPackage, srcPath);
        List<GenSrcFile> genFiles = generateApplicationPolicies(applicationPolicies);

        List<GenSrcFile> genSubsFiles = generateSubscriptionPolicies(subscriptionPolicies);
        genFiles.addAll(genSubsFiles);

        GenSrcFile initGenFile = generateInitBal(applicationPolicies, subscriptionPolicies);
        genFiles.add(initGenFile);
        writeGeneratedSources(genFiles, srcPath, implPath);
    }
*/
    /**
     * Generate application policies source
     *
     * @param applicationPolicies list of application policies
     * @return list of {@code GenSrcFile}
     * @throws IOException when file operations fail
     */
    public List<GenSrcFile> generateApplicationPolicies(List<ApplicationThrottlePolicyDTO> applicationPolicies)
            throws IOException {
        ThrottlePolicy policyContext;
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        for (ApplicationThrottlePolicyDTO applicationPolicy : applicationPolicies) {
            policyContext = new ThrottlePolicy().buildContext(applicationPolicy).srcPackage(srcPackage)
                    .modelPackage(srcPackage);
            sourceFiles.add(generateMock(policyContext));
        }
        return sourceFiles;
    }

    /**
     * Generate application policies source
     *
     * @param subscriptionPolicies list of subscription policies
     * @return list of {@code GenSrcFile}
     * @throws IOException when file operations fail
     */
    public List<GenSrcFile> generateSubscriptionPolicies(List<SubscriptionThrottlePolicyDTO> subscriptionPolicies)
            throws IOException, BallerinaServiceGenException {
        ThrottlePolicy policyContext;
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        for (SubscriptionThrottlePolicyDTO subscriptionPolicy : subscriptionPolicies) {
            policyContext = new ThrottlePolicy().buildContext(subscriptionPolicy).srcPackage(srcPackage)
                    .modelPackage(srcPackage);
            sourceFiles.add(generateMock(policyContext));
        }
        return sourceFiles;
    }

    /**
     * Generate init ballerina source which start all other policy ballerina
     *
     * @param applicationPolicies  list of application policies
     * @param subscriptionPolicies list of subscription policies
     * @return GenSrcFile
     * @throws IOException                  when file operations fail
     * @throws BallerinaServiceGenException when code generator fails
     */
    public GenSrcFile generateInitBal(List<ApplicationThrottlePolicyDTO> applicationPolicies,
            List<SubscriptionThrottlePolicyDTO> subscriptionPolicies) throws IOException, BallerinaServiceGenException {
        ThrottlePolicyInitializer context = new ThrottlePolicyInitializer().buildAppContext(applicationPolicies)
                .buildSubsContext(subscriptionPolicies).srcPackage(srcPackage).modelPackage(srcPackage);
        return generateInitBalMock(context);
    }

    /**
     * Write ballerina definition of a <code>object</code> to a file as described by <code>template.</code>
     *
     * @param object       Context object to be used by the template parser
     * @param templateDir  Directory with all the templates required for generating the source file
     * @param templateName Name of the parent template to be used
     * @param outPath      Destination path for writing the resulting source file
     * @throws IOException when file operations fail
     *                     file write functionality your self, if you need to customize file writing steps.
     *                     to a ballerina package.
     */
    @Deprecated
    public void writeBallerina(Object object, String templateDir, String templateName, String outPath)
            throws IOException {
        PrintWriter writer = null;

        try {
            Template template = compileTemplate(templateDir, templateName);
            Context context = Context.newBuilder(object)
                    .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                    .build();
            writer = new PrintWriter(outPath, "UTF-8");
            writer.println(template.apply(context));
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
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

    private void writeGeneratedSources(List<GenSrcFile> sources, Path srcPath, Path implPath) throws IOException {
        // Remove old generated files - if any - before regenerate
        // if srcPackage was not provided and source was written to main package nothing will be deleted.
        if (srcPackage != null && !srcPackage.isEmpty() && Files.exists(srcPath)) {
            FileUtils.deleteDirectory(srcPath.toFile());
        }

        Files.createDirectories(srcPath);
        for (GenSrcFile file : sources) {
            Path filePath;

            // We only overwrite files of overwritable type.
            // So non overwritable files will be written to disk only once.
            if (!file.getType().isOverwritable()) {
                filePath = implPath.resolve(file.getFileName());
                if (Files.notExists(filePath)) {
                    CodegenUtils.writeFile(filePath, file.getContent());
                }
            } else {
                filePath = srcPath.resolve(file.getFileName());
                CodegenUtils.writeFile(filePath, file.getContent());
            }
        }
    }

    /**
     * Generate code for mock ballerina service.
     *
     * @param context model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generateMock(ThrottlePolicy context) throws IOException {
        if (srcPackage == null || srcPackage.isEmpty()) {
            srcPackage = GeneratorConstants.DEFAULT_SERVICE_PKG;
        }

        String concatTitle = context.getPolicyType() + "_" + context.getName();
        String srcFile = concatTitle + ".bal";

        String mainContent = getContent(context, GeneratorConstants.DEFAULT_SERVICE_DIR,
                GeneratorConstants.THROTTLE_POLICY_TEMPLATE_NAME);
        GenSrcFile sourceFiles = new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcPackage, srcFile, mainContent);

        return sourceFiles;
    }

    private GenSrcFile generateInitBalMock(ThrottlePolicyInitializer context) throws IOException {
        if (srcPackage == null || srcPackage.isEmpty()) {
            srcPackage = GeneratorConstants.DEFAULT_SERVICE_PKG;
        }

        String concatTitle = "throttle_policy_initializer";
        String srcFile = concatTitle + ".bal";

        String mainContent = getPolicyInitContent(context, GeneratorConstants.DEFAULT_SERVICE_DIR,
                GeneratorConstants.THROTTLE_POLICY_INIT_TEMPLATE_NAME);
        GenSrcFile sourceFiles = new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcPackage, srcFile, mainContent);

        return sourceFiles;
    }

    /**
     * Retrieve generated source content as a String value.
     *
     * @param object       context to be used by template engine
     * @param templateDir  templates directory
     * @param templateName name of the template to be used for this code generation
     * @return String with populated template
     * @throws IOException when template population fails
     */
    private String getContent(ThrottlePolicy object, String templateDir, String templateName) throws IOException {
        Template template = compileTemplate(templateDir, templateName);
        Context context = Context.newBuilder(object)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();
        return template.apply(context);
    }

    private String getPolicyInitContent(ThrottlePolicyInitializer object, String templateDir, String templateName)
            throws IOException {
        Template template = compileTemplate(templateDir, templateName);
        Context context = Context.newBuilder(object)
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