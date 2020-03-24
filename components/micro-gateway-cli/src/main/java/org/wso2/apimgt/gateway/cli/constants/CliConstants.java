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
package org.wso2.apimgt.gateway.cli.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Constants used by toolkit CLI.
 */
public class CliConstants {
    public static final String PROJECT_CONF_DIR = "conf";
    public static final String PROJECT_INTERCEPTORS_DIR = "interceptors";
    public static final String PROJECT_GRPC_SERVICE_DIR = "grpc_service";
    public static final String PROJECT_GRPC_CLIENT_DIR = "client";
    public static final String PROJECT_EXTENSIONS_DIR = "extensions";
    public static final String PROJECT_API_DEFINITIONS_DIR = "api_definitions";
    public static final String PROJECT_SERVICES_DIR = "services";
    public static final String PROJECT_POLICIES_FILE = "policies.yaml";
    public static final String PROJECT_TARGET_DIR = "target";
    public static final String CLI_DEPENDENCIES = "dependencies";
    public static final String CLI_VALIDATION_DEPENDENCIES = "validation";
    public static final String PROJECT_GEN_DIR = "gen";
    public static final String GEN_SRC_DIR = "src";
    public static final String GEN_GRPC_SRC_DIR = "gRPCSrc";
    public static final String GEN_GRPC_SRC_OPENAPI_DIR = "openAPIs";
    public static final String GEN_GRPC_SRC_DESC_DIR = "desc";
    public static final String GEN_POLICIES_DIR = "policies";
    public static final String RESOURCES_DIR = "resources";
    public static final String MAIN_CONFIG_FILE_NAME = "toolkit-config.toml";
    public static final String DEPLOYMENT_CONFIG_FILE_NAME = "deployment-config.toml";
    public static final String TEMP_DIR_NAME = "temp";
    public static final String RESOURCE_HASH_HOLDER_FILE_NAME = "hashes.json";
    public static final String DEFAULT_DEPLOYMENT_CONFIG_FILE_NAME = "default-deployment-config.toml";
    public static final String CLI_HOME = "cli.home";
    public static final String CLI_LIB = "lib";
    public static final String CLI_BIN = "bin";
    public static final String CLI_BIR_CACHE = "bir-cache";
    public static final String CLI_CONF = "conf";
    public static final String CLI_PLATFORM = "platform";
    public static final String CLI_GATEWAY = "gateway";
    public static final String CLI_BRE = "bre";
    public static final String DEFAULT_DOCKER_BASE_IMAGE = "wso2/wso2micro-gw:latest";
    public static final String DEFAULT_VERSION = "v1";
    public static final String EXTENSION_ZIP = ".zip";
    public static final String EXTENSION_JAR = ".jar";
    public static final String EXTENSION_BAL = ".bal";
    public static final String GW_DIST_BIN = "bin";
    public static final String GW_DIST_CONF = "conf";
    public static final String GW_DIST_RESOURCES = "resources";
    public static final String GW_DIST_FILTERS = "filters";
    public static final String GW_DIST_DEFINITIONS = "definitions";
    public static final String GW_DIST_EXTERNAL = "external";
    public static final String GW_DIST_POLICIES_FILE = PROJECT_POLICIES_FILE;
    public static final String GW_DIST_EXTENSION_FILTER = "extension_filter.bal";
    public static final String GW_DIST_TOKEN_REVOCATION_EXTENSION = "token_revocation_extension.bal";
    public static final String GW_DIST_START_UP_EXTENSION = "startup_extension.bal";
    public static final String K8S_DEPLOYMENT = "-deployment-";
    public static final String K8S_SERVICE = "-rest-";
    public static final String K8S_INGRESS = "ingress";
    public static final String LABEL_PLACEHOLDER = "${label}";
    public static final String API_NAME_PLACEHOLDER = "${name}";
    public static final String VERSION_PLACEHOLDER = "${version}";
    public static final String EXPAND_PLACEHOLDER = "${expand}";
    public static final String API_ID_PLACEHOLDER = "${apiId}";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String SYS_PROP_USER_DIR = "user.dir";
    public static final String SYS_PROP_CURRENT_DIR = "current.dir";
    public static final String SYS_PROP_SECURITY = "security";
    public static final String WSO2 = "wso2";
    public static final String MGW = "mgw";
    public static final String MICRO_GW = "micro-gw";
    public static final String MICRO_GW_CONF_FILE = "micro-gw.conf";
    public static final String KEEP_FILE = ".keep";
    public static final String BALLERINA_TOML_FILE = "Ballerina.toml";
    public static final String VERSION_FILE = "version.txt";
    public static final String CACERTS_DIR = "cacerts.location";
    public static final String DEFAULT_CACERTS_PASS = "changeit";

    public static final String LOGGING_PROPERTIES_FILENAME = "logging.properties";
    public static final Pattern SYS_PROP_PATTERN = Pattern.compile("\\$\\{([^}]*)}");

    public static final List<String> ACCESS_CONTROL_ALLOW_ORIGINS = Collections.singletonList("*");
    public static final List<String> ACCESS_CONTROL_ALLOW_METHODS = Collections.unmodifiableList(Arrays
            .asList("GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS"));
    public static final List<String> ACCESS_CONTROL_ALLOW_HEADERS = Collections.unmodifiableList(Arrays
            .asList("authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction"));
    public static final boolean ACCESS_CONTROL_ALLOW_CREDENTIALS = false;

    public static final String API_SWAGGER = "swagger.json";
    public static final String API_OPENAPI_YAML = "openAPI.yaml";
    public static final String REST_API_V1_PREFIX = "v1.";

    public static final String JSON_EXTENSION = ".json";
    public static final String YAML_EXTENSION = ".yaml";

    public static final String WARN_LOG_PATTERN = "WARN: ";
    public static final String MICROGW_HOME_PLACEHOLDER = "${MGW-TK_HOME}";

    public static final String PROJECT_GRPC_DEFINITIONS_DIR = "grpc_definitions";
    public static final String RESOURCES_GRPC_DIR = "grpc";
    public static final String MICROGW_PROJECT_PLACEHOLDER = "\\$MGW-PROJECT_HOME";
}
