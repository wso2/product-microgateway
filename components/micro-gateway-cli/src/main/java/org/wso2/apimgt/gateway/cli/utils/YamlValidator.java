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
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This class provides required functionality to validate a given
 * yaml file with a given <a href="https://json-schema.org/specification.html">Json Schema</a>
 */
public class YamlValidator {
    private static final Logger logger = LoggerFactory.getLogger(YamlValidator.class);

    /**
     * Parse the yaml {@code file} to required DTO {@code type} using jackson library.
     * Before parsing the input file, schema validation will be done to validate the file syntax.
     *
     * @param file yaml file to be parsed
     * @param isSchema json schema definition to be used for schema validation
     * @param type Jackson object model to parse the yaml content
     * @param <T> Jackson object model to parse the yaml content
     * @return Parsed yaml file as a DTO
     * @throws IOException when failed to read input files
     */
    public static <T> T parse(File file, InputStream isSchema, Class<T> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode jsonNode = mapper.readTree(file);

        if (jsonNode == null) {
            throw new IOException("Input file is invalid or empty");
        }

        JSONObject rawSchema = new JSONObject(new JSONTokener(isSchema));
        Schema schema = SchemaLoader.load(rawSchema);
        logger.debug("Loaded json schema");

        try {
            schema.validate(new JSONObject(jsonNode.toString()));
        } catch (ValidationException e) {
            handleValidationException(e, file.getName());
        }

        return mapper.readValue(file, type);
    }

    private static void handleValidationException(ValidationException ex, String fileName) {
        StringBuilder msgBuilder = new StringBuilder();
        List<String> allMsg = ex.getAllMessages();

        for (String msg : allMsg) {
            msgBuilder.append('\n' + msg);
        }
        throw new CLIRuntimeException("Invalid file: " + fileName + '\n' + msgBuilder.toString(), 1);
    }

}
