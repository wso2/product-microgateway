/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides required functionality to validate a given
 * yaml file with a given <a href="https://json-schema.org/specification.html">Json Schema</a>
 */
public class YamlValidator {

    public static <T> T parse(File definitionFile, InputStream isSchema, Class<T> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode jsonNode = mapper.readTree(definitionFile);

        if (jsonNode == null) {
            throw new IOException("Input file is invalid or empty");
        }

        JSONObject rawSchema = new JSONObject(new JSONTokener(isSchema));
        Schema schema = SchemaLoader.load(rawSchema);
        schema.validate(new JSONObject(jsonNode.toString()));

        return mapper.readValue(definitionFile, type);
    }
}
