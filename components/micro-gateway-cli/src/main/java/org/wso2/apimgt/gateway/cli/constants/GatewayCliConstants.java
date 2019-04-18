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

import java.io.File;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

public class GatewayCliConstants {
    public static final String PROJECT_CONF_DIR = "conf";
    public static final String PROJECT_INTERCEPTORS_DIR = "interceptors";
    public static final String PROJECT_GRPC_SERVICE_DIR = "grpc_service";
    public static final String PROJECT_GRPC_CLIENT_DIR = "client";
    public static final String PROJECT_LOGS_DIR = "logs";
    public static final String PROJECT_API_USAGE_DIR = "api-usage-data";
    public static final String PROJECT_API_DEFINITIONS_DIR = "api_definitions";
    public static final String PROJECT_DEFINITION_FILE = "definition.yaml";
    public static final String PROJECT_POLICIES_FILE = "policies.yaml";
    public static final String PROJECT_TARGET_DIR = "target";
    public static final String PROJECT_GEN_DIR = "gen";
    public static final String GEN_API_DEFINITIONS_DIR = PROJECT_API_DEFINITIONS_DIR;
    public static final String GEN_SRC_DIR = "src";
    public static final String GEN_POLICIES_DIR = "policies";
    public static final String ACCESS_LOG_FILE = "access_logs";
    public static final String MAIN_CONFIG_FILE_NAME = "toolkit-config.toml";
    public static final String DEPLOYMENT_CONFIG_FILE_NAME = "deployment-config.toml";
    public static final String TEMP_DIR_NAME = "temp";
    public static final String RESOURCE_HASH_HOLDER_FILE_NAME = "hashes.json";
    public static final String LIB_HASH_HOLDER_FILE_NAME = "hashes";
    public static final String DEFAULT_DEPLOYMENT_CONFIG_FILE_NAME = "default-deployment-config.toml";
    public static final String CLI_HOME = "cli.home";
    public static final String CLI_LIB = "lib";
    public static final String CLI_REPO = "repo";
    public static final String CLI_CONF = "conf";
    public static final String CLI_RUNTIME = "runtime";
    public static final String CLI_PLATFORM = "platform";
    public static final String CLI_GATEWAY = "gateway";
    public static final String CLI_BALO = "balo";
    public static final String CLI_BRE = "bre";
    public static final String POLICY_DIR = "policies";
    public static final String EXTENSION_BALX = ".balx";
    public static final String EXTENSION_ZIP = ".zip";
    public static final String EXTENSION_JAR = ".jar";
    public static final String GW_TARGET_DIST = "distribution";
    public static final String GW_DIST_PREFIX = "micro-gw-";
    public static final String GW_DIST_BIN = "bin";
    public static final String GW_DIST_CONF = "conf";
    public static final String GW_DIST_RESOURCES = "resources";
    public static final String GW_DIST_FILTERS = "filters";
    public static final String GW_DIST_DEFINITIONS = "definitions";
    public static final String GW_DIST_POLICIES = "policies";
    public static final String GW_DIST_POLICIES_FILE = PROJECT_POLICIES_FILE;
    public static final String GW_DIST_RUNTIME = "runtime";
    public static final String GW_DIST_EXEC = "exec";
    public static final String GW_DIST_SH = "gateway";
    public static final String GW_DIST_BAT = "gateway.bat";
    public static final String GW_DIST_SH_PATH = "distribution" + File.separator + GW_DIST_BIN + File.separator
            + GW_DIST_SH;
    public static final String GW_DIST_BAT_PATH = "distribution" + File.separator + GW_DIST_BIN + File.separator
            + GW_DIST_BAT;
    public static final String GW_DIST_EXTENSION_FILTER = "extension_filter.bal";
    public static final String GW_DIST_CONF_FILE = "micro-gw.conf";
    public static final String K8S_DEPLOYMENT = "-deployment-";
    public static final String K8S_SERVICE = "-rest-";
    public static final String K8S_INGRESS = "ingress";
    public static final String LABEL_PLACEHOLDER = "${label}";
    public static final String API_NAME_PLACEHOLDER = "${name}";
    public static final String VERSION_PLACEHOLDER = "${version}";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String SYS_PROP_USER_DIR = "user.dir";
    public static final String SYS_PROP_CURRENT_DIR = "current.dir";
    public static final String SYS_PROP_SECURITY = "security";

    public static final String LOGGING_PROPERTIES_FILENAME = "logging.properties";
    public static final Pattern SYS_PROP_PATTERN = Pattern.compile("\\$\\{([^}]*)}");

    public static final int EXIT_CODE_NOT_MODIFIED = 34;

    public static final List<String> accessControlAllowOrigins = Collections.singletonList("*");
    public static final List<String> accessControlAllowMethods = Arrays
            .asList("GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS");
    public static final List<String> accessControlAllowHeaders = Arrays
            .asList("authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction");
    public static final boolean accessControlAllowCredentials = false;

    public static final String[] PROJECTS_TARGET_DELETE_FILES = new String[] {
            GW_TARGET_DIST,
            "Ballerina.lock"
    };

    public static final String SUBSCRIPTION_THROTTLE_POLICIES_FILE = "subscription-throttle-policies.json";
    public static final String APPLICATION_THROTTLE_POLICIES_FILE = "application-throttle-policies.json";
    public static final String CLIENT_CERT_METADATA_FILE = "client-cert-metadata.json";
    public static final String API_METADATA_FILE = "api-metadata.yaml";
    public static final String API_SWAGGER = "swagger.json";
    public static final String PROJECT_FILE_NAME = ".PROJECT";
}