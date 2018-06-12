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
package org.wso2.apimgt.gateway.codegen.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    public static void writeFile(Path filePath, String content) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filePath.toString(), "UTF-8");
            writer.print(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
