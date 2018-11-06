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
package org.wso2.apimgt.gateway.cli.utils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import org.wso2.apimgt.gateway.cli.constants.GeneratorConstants;
import org.wso2.apimgt.gateway.cli.model.template.GenSrcFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Utilities used by ballerina code generator.
 */
public class CodegenUtils {

    /**
     * Writes a file with content to specified {@code filePath}.
     *
     * @param filePath valid file path to write the content
     * @param content content of the file
     * @throws IOException when a file operation fails
     */
    private static void writeFile(Path filePath, String content) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filePath.toString(),  GeneratorConstants.UTF_8);
            writer.print(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Compile given template
     *
     * @param defaultTemplateDir template directory
     * @param templateName template name
     * @return Generated template
     * @throws IOException if file read went wrong
     */
    public static Template compileTemplate(String defaultTemplateDir, String templateName) throws IOException {
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

    /**
     * Write generated templates
     *
     * @param sources list of source files
     * @param srcPath source location
     * @param overwrite whether existing files overwrite or not
     * @throws IOException if file write went wrong
     */
    public static void writeGeneratedSources(List<GenSrcFile> sources, Path srcPath, boolean overwrite) throws IOException {
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

    public static String trim(String key) {
        if (key == null) {
            return null;
        }
        key = key.replaceAll("(\\.)|(-)|(\\{)|(})|(\\s)|(/)", "_");
        if (key.contains("*")) {
            key = key.replaceAll("\\*", UUID.randomUUID().toString().replaceAll("-", "_"));
        }
        return key;
    }
}
