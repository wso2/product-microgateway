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
package org.wso2.apimgt.gateway.codegen.config;

import org.wso2.apimgt.gateway.codegen.exception.ConfigParserException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigYAMLParser {
    /**
     * Parses the given YAML configuration file and de-serialize the content into given bean type.
     *
     * @param configFilePath path to YAML file
     * @param type           class of the bean to be used when de-serializing
     * @param <T>            type of the bean class to be used when de-serializing
     * @return returns the populated bean instance
     * @throws ConfigParserException if cannot read or parse the content of the specified YAML file
     */
    public static <T> T parse(String configFilePath, Class<T> type) throws ConfigParserException {
        Path configurationFile = Paths.get(configFilePath);
        if (!Files.exists(configurationFile)) {
            throw new ConfigParserException("Mandatory configuration file '" + configurationFile + "' does not exists.");
        }

        T loadedBean;
        try {
            String content = new String(Files.readAllBytes(configurationFile), StandardCharsets.UTF_8);
            loadedBean = parseString(content, type);
        } catch (IOException e) {
            throw new ConfigParserException("Cannot read the content of configuration file '" + configurationFile + "'.",
                    e);
        } catch (Exception e) {
            throw new ConfigParserException("Cannot parse the configuration file '" + configurationFile + "'.",
                    e);
        }
        if (loadedBean == null) {
            // Either configuration file is empty or has comments only.
            throw new ConfigParserException(
                    "Cannot parse the configuration file '" + configurationFile + "' as it is empty.");
        }
        return loadedBean;
    }

    public static void write(String configFilePath, Object content, Class root) throws ConfigParserException {
        Path configurationFile = Paths.get(configFilePath);
        if (!Files.exists(configurationFile)) {
            throw new ConfigParserException("Mandatory configuration file '" + configurationFile + "' does not exists.");
        }

        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setCanonical(false);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            FileWriter writer = new FileWriter(configFilePath);
            Representer representer = new Representer() {
                @Override
                protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                                                                                Object propertyValue, Tag customTag) {
                    // if value of property is null, ignore it.
                    if (propertyValue == null) {
                        return super.representJavaBeanProperty(javaBean, property, "", customTag);
                    }
                    else {
                        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                    }
                }
            };
            representer.addClassTag(root, Tag.MAP);
            Yaml yaml=new Yaml(representer, options);
            yaml.dump(content, writer);
        } catch (IOException e) {
            throw new ConfigParserException("Cannot read the content of configuration file '" + configurationFile + "'.",
                    e);
        } catch (Exception e) {
            throw new ConfigParserException("Cannot parse the configuration file '" + configurationFile + "'.",
                    e);
        }
    }

    static <T> T parseString(String configFileContent, Class<T> type) {
        return new Yaml().loadAs(configFileContent, type);
    }
}
