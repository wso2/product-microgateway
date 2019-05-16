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
package org.wso2.apimgt.gateway.cli.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ThrottlePolicyListMapper;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ThrottlePolicyMapper;
import org.wso2.apimgt.gateway.cli.model.template.GenSrcFile;
import org.wso2.apimgt.gateway.cli.model.template.policy.ThrottlePolicy;
import org.wso2.apimgt.gateway.cli.model.template.policy.ThrottlePolicyInitializer;
import org.wso2.apimgt.gateway.cli.utils.CodegenUtils;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates Ballerina Services/Clients for a provided OAS definition.
 */
public class ThrottlePolicyGenerator {

    public void generate(String outPath, List<ApplicationThrottlePolicyDTO> applicationPolicies,
            List<SubscriptionThrottlePolicyDTO> subscriptionPolicies) throws IOException {
        List<GenSrcFile> genFiles = new ArrayList<>();
        List<GenSrcFile> genAppFiles = generateApplicationPolicies(applicationPolicies);

        if(genAppFiles != null){
            genFiles.addAll(genAppFiles);
        }

        List<GenSrcFile> genSubsFiles = generateSubscriptionPolicies(subscriptionPolicies);

        if(genSubsFiles != null){
            genFiles.addAll(genSubsFiles);
        }

        GenSrcFile initGenFile = generateInitBal(applicationPolicies, subscriptionPolicies);
        genFiles.add(initGenFile);
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(outPath), true);
    }

    /**
     * Generate ballerina and stream source for a given app and subs policies
     *
     * @param outPath  Destination file path to save generated source files. If not provided
     *                 {@code definitionPath} will be used as the default destination path
     * @param projectName  Project name
     * @throws IOException                  when file operations fail
     */
    public void generate(String outPath, String projectName) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String policyFileLocation = GatewayCmdUtils.getProjectDirectoryPath(projectName) + File.separator
                + GatewayCliConstants.GW_DIST_POLICIES_FILE;
        ThrottlePolicyListMapper throttlePolicyListMapper = mapper
                .readValue(new File(policyFileLocation), ThrottlePolicyListMapper.class);

        //read application throttle policies and subscription throttle policies
        List<ThrottlePolicyMapper> applicationPolicies = throttlePolicyListMapper.getApplicationPolicies();
        List<ThrottlePolicyMapper> subscriptionPolicies = throttlePolicyListMapper.getSubscriptionPolicies();
        List<ThrottlePolicyMapper> resourcePolicies = throttlePolicyListMapper.getResourcePolicies();

        if (applicationPolicies == null && subscriptionPolicies == null) {
            return;
        }

        List<GenSrcFile> genFiles = new ArrayList<>();
        if (applicationPolicies != null) {
            List<GenSrcFile> genAppFiles = generateGenericPolicies(applicationPolicies,
                    GeneratorConstants.POLICY_TYPE.APPLICATION);
            genFiles.addAll(genAppFiles);
        } else {
            //declare empty object to avoid null pointer issue
            applicationPolicies = new ArrayList<>();
        }

        if (subscriptionPolicies != null) {
            List<GenSrcFile> genSubsFiles = generateGenericPolicies(subscriptionPolicies,
                    GeneratorConstants.POLICY_TYPE.SUBSCRIPTION);
            genFiles.addAll(genSubsFiles);
        } else {
            //declare empty object to avoid null pointer issue
            subscriptionPolicies = new ArrayList<>();
        }

        if (resourcePolicies != null) {
            List<GenSrcFile> genSubsFiles = generateGenericPolicies(resourcePolicies,
                    GeneratorConstants.POLICY_TYPE.RESOURCE);
            genFiles.addAll(genSubsFiles);
        } else {
            //declare empty object to avoid null pointer issue
            resourcePolicies = new ArrayList<>();
        }

        GenSrcFile initGenFile = generateInitBal(applicationPolicies, subscriptionPolicies, resourcePolicies);
        genFiles.add(initGenFile);
        CodegenUtils.writeGeneratedSources(genFiles, Paths.get(outPath), true);

    }

    /**
     * Generate generic policies source
     *
     * @param policies list of application policies
     * @return list of {@code GenSrcFile}
     * @throws IOException when file operations fail
     */
    private List<GenSrcFile> generateGenericPolicies(List<ThrottlePolicyMapper> policies, GeneratorConstants
            .POLICY_TYPE type)
            throws IOException {
        ThrottlePolicy policyContext;

        if(policies == null){
            return null;
        }
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        for (ThrottlePolicyMapper policy : policies) {
            policyContext = new ThrottlePolicy().buildContext(policy, type);
            sourceFiles.add(generatePolicy(policyContext));
        }
        return sourceFiles;
    }

    /**
     * Generate application policies source
     *
     * @param applicationPolicies list of application policies
     * @return list of {@code GenSrcFile}
     * @throws IOException when file operations fail
     */
    private List<GenSrcFile> generateApplicationPolicies(List<ApplicationThrottlePolicyDTO> applicationPolicies)
            throws IOException {
        ThrottlePolicy policyContext;

        if(applicationPolicies == null){
            return null;
        }
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        for (ApplicationThrottlePolicyDTO applicationPolicy : applicationPolicies) {
            policyContext = new ThrottlePolicy().buildContext(applicationPolicy);
            sourceFiles.add(generatePolicy(policyContext));
        }
        return sourceFiles;
    }

    /**
     * Generate subscription policies source
     *
     * @param subscriptionPolicies list of subscription policies
     * @return list of {@code GenSrcFile}
     * @throws IOException when file operations fail
     */
    private List<GenSrcFile> generateSubscriptionPolicies(List<SubscriptionThrottlePolicyDTO> subscriptionPolicies)
            throws IOException {
        ThrottlePolicy policyContext;
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        if(subscriptionPolicies == null){
            return null;
        }
        for (SubscriptionThrottlePolicyDTO subscriptionPolicy : subscriptionPolicies) {
            policyContext = new ThrottlePolicy().buildContext(subscriptionPolicy);
            sourceFiles.add(generatePolicy(policyContext));
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
     * @throws IOException when code generator fails
     */
    private GenSrcFile generateInitBal(List<ApplicationThrottlePolicyDTO> applicationPolicies,
            List<SubscriptionThrottlePolicyDTO> subscriptionPolicies) throws IOException {
        ThrottlePolicyInitializer context = new ThrottlePolicyInitializer();

        if(applicationPolicies != null) {
            context = context.buildAppContext(applicationPolicies);
        }
        if(subscriptionPolicies != null){
            context = context.buildSubsContext(subscriptionPolicies);
        }
        return generateInitBalFile(context);
    }


    /**
     * Generate init ballerina source which start all other policy ballerina
     *
     * @param applicationPolicies  list of application policies
     * @param subscriptionPolicies list of subscription policies
     * @param resourcePolicies   list of resource policies
     * @return GenSrcFile
     * @throws IOException                  when file operations fail
     * @throws IOException when code generator fails
     */
    private GenSrcFile generateInitBal(List<ThrottlePolicyMapper> applicationPolicies,
            List<ThrottlePolicyMapper> subscriptionPolicies, List<ThrottlePolicyMapper> resourcePolicies) throws
            IOException {
        ThrottlePolicyInitializer context = new ThrottlePolicyInitializer();

        if(applicationPolicies != null) {
            context = context.buildPolicyContext(applicationPolicies,GeneratorConstants.POLICY_TYPE.APPLICATION);
        }
        if(subscriptionPolicies != null){
            context = context.buildPolicyContext(subscriptionPolicies,GeneratorConstants.POLICY_TYPE.SUBSCRIPTION);
        }
        if(resourcePolicies != null){
            context = context.buildPolicyContext(resourcePolicies,GeneratorConstants.POLICY_TYPE.RESOURCE);
        }

        return generateInitBalFile(context);
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
            Template template = CodegenUtils.compileTemplate(templateDir, templateName);
            Context context = Context.newBuilder(object)
                    .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                    .build();
            writer = new PrintWriter(outPath, GeneratorConstants.UTF_8);
            writer.println(template.apply(context));
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Generate code for throttle policy
     *
     * @param context model context to be used by the templates
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private GenSrcFile generatePolicy(ThrottlePolicy context) throws IOException {
        String concatTitle = context.getPolicyType() + "_" + context.getName();
        String srcFile = concatTitle + ".bal";
        String mainContent = getContent(context);

        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, mainContent);
    }

    private GenSrcFile generateInitBalFile(ThrottlePolicyInitializer context) throws IOException {
        String concatTitle = GeneratorConstants.THROTTLE_POLICY_INITIALIZER;
        String srcFile = concatTitle + GeneratorConstants.BALLERINA_EXTENSION;
        String mainContent = getPolicyInitContent(context);

        return new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, srcFile, mainContent);
    }

    /**
     * Retrieve generated source content as a String value.
     *
     * @param object context to be used by template engine
     * @return String with populated template
     * @throws IOException when template population fails
     */
    private String getContent(ThrottlePolicy object) throws IOException {
        Template template = CodegenUtils.compileTemplate(GeneratorConstants.DEFAULT_TEMPLATE_DIR,
                GeneratorConstants.THROTTLE_POLICY_TEMPLATE_NAME);
        Context context = Context.newBuilder(object)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();

        return template.apply(context);
    }

    private String getPolicyInitContent(ThrottlePolicyInitializer object)
            throws IOException {
        Template template = CodegenUtils.compileTemplate(GeneratorConstants.DEFAULT_TEMPLATE_DIR,
                GeneratorConstants.THROTTLE_POLICY_INIT_TEMPLATE_NAME);
        Context context = Context.newBuilder(object)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();

        return template.apply(context);
    }
}