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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constants for swagger code generator.
 */
public class GeneratorConstants {

    /**
     * Enum to select the code generation mode.
     * Ballerina service, mock and client generation is available
     */
    public enum GenType {
        MOCK("mock"),
        CLIENT("client");

        private String name;

        GenType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static final String SERVICE_TEMPLATE_NAME = "service";
    public static final String THROTTLE_POLICY_TEMPLATE_NAME = "policy";
    public static final String ENDPOINT_TEMPLATE_NAME = "endpoints";
    public static final String ENDPOINTS = "endpoints";
    public static final String BALLERINA_EXTENSION = ".bal";
    public static final String THROTTLE_POLICY_INIT_TEMPLATE_NAME = "policy_init";

    public static final String SCHEMA_FILE_NAME = "schema.bal";
    public static final String COMMON_MODELS_FILE_NAME = "common.bal";

    public static final String TEMPLATES_SUFFIX = ".mustache";
    public static final String TEMPLATES_DIR_PATH_KEY = "templates.dir.path";
    public static final String DEFAULT_TEMPLATE_DIR = File.separator + "templates";
    public static final String DEFAULT_SERVICE_DIR = DEFAULT_TEMPLATE_DIR + File.separator + "mock";

    public static final String GEN_SRC_DIR = "gen";
    public static final String DEFAULT_SERVICE_PKG = "service";
    public static final String APPLICATION_POLICY_TYPE = "application";
    public static final String SUBSCRIPTION_POLICY_TYPE = "subscription";
    public static final String APPLICATION_INIT_FUNC_PREFIX = "initApplication";
    public static final String SUBSCRIPTION_INIT_FUNC_PREFIX = "initSubscription";
    public static final String APPLICATION_KEY = "appKey";
    public static final String SUBSCRIPTION_KEY = "subscriptionKey";
    public static final String INIT_FUNC_SUFFIX = "Policy";

}
