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
package org.wso2.apimgt.gateway.cli.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.wso2.apimgt.gateway.cli.exception.ConfigParserException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TOMLConfigParser {

    /**
     * Parses the given TOML configuration file and de-serialize the content into given config type.
     *
     * @param configFilePath path to toml file
     * @param type           class of the config to be used when de-serializing
     * @param <T>            type of the config class to be used when de-serializing
     * @return returns the populated config instance
     * @throws ConfigParserException if cannot read or parse the content of the specified TOML file
     */
    public static <T> T parse(String configFilePath, Class<T> type) throws ConfigParserException {
        Path configurationFile = Paths.get(configFilePath);
        if (!Files.exists(configurationFile)) {
            throw new ConfigParserException("Mandatory configuration file '" + configurationFile + "' does not exists.");
        }

        T loadedBean;
        String content;
        try {
            content = new String(Files.readAllBytes(configurationFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigParserException("Cannot read the content of configuration file '" + configurationFile + "'.",
                    e);
        }
        loadedBean = parseString(content, type);
        return loadedBean;
    }

    /**
     * Write the given config type to a file.
     *
     * @param configFilePath path to toml file
     * @param content            type of the config class to be used when de-serializing
     * @throws ConfigParserException if cannot write the content to file or specified file does not exist
     */
    public static void write(String configFilePath, Object content) throws ConfigParserException {
        Path configurationFile = Paths.get(configFilePath);
        if (!Files.exists(configurationFile)) {
            throw new ConfigParserException("Mandatory configuration file '" + configurationFile + "' does not exists.");
        }
        TomlWriter tomlWriter = new TomlWriter();
        try {
            tomlWriter.write(content, new File(configFilePath));
        } catch (IOException e) {
            throw new ConfigParserException("Cannot write the content to configuration file '" + configurationFile + "'.",
                    e);
        }
    }

    private static <T> T parseString(String configFileContent, Class<T> type) {
        return new Toml().read(configFileContent).to(type);
    }

}
