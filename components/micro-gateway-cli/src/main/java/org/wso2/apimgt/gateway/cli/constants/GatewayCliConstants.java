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
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class GatewayCliConstants {
    public static final String BALLERINA_HOME = "BALLERINA_HOME";
    public static final String MAIN_DIRECTORY_NAME = "micro-gw-resources";
    public static final String CONF_DIRECTORY_NAME = "conf";
    public static final String PROJECTS_DIRECTORY_NAME = "projects";
    public static final String PROJECTS_SRC_DIRECTORY_NAME = "src";
    public static final String PROJECTS_TARGET_DIRECTORY_NAME = "target";
    public static final String MAIN_CONFIG_FILE_NAME = "config.toml";
    public static final String LABEL_CONFIG_FILE_NAME = "label-config.toml";
    public static final String TEMP_DIR_NAME = "temp";
    public static final String PROJECT_ROOT_HOLDER_FILE_NAME = "workspace.txt";
    public static final String DEFAULT_MAIN_CONFIG_FILE_NAME = "default-config.toml";
    public static final String DEFAULT_LABEL_CONFIG_FILE_NAME = "default-label-config.toml";
    public static final String CLI_HOME = "cli.home";
    public static final String CLI_LIB = "lib";
    public static final String CLI_RUNTIME = "runtime";
    public static final String POLICY_DIR = "policies";
    public static final String EXTENSION_BALX = ".balx";
    public static final String EXTENSION_ZIP = ".zip";
    public static final String GW_TARGET_DIST = "distribution";
    public static final String GW_DIST_PREFIX = "micro-gw-";
    public static final String GW_DIST_BIN = "bin";
    public static final String GW_DIST_CONF = "conf";
    public static final String GW_DIST_RESOURCES = "resources";
    public static final String GW_DIST_FILTERS = "filters";
    public static final String GW_DIST_RUNTIME = "runtime";
    public static final String GW_DIST_EXEC = "exec";
    public static final String GW_DIST_SH = "micro-gw.sh";
    public static final String GW_DIST_SH_PATH = "distribution" + File.separator + GW_DIST_BIN + File.separator + GW_DIST_SH;
    public static final String GW_DIST_EXTENSION_FILTER = "extension_filter.bal";
    public static final String GW_DIST_CONF_FILE = "micro-gw.conf";
    public static final String PROJECT_CONF_FILE = "ballerina.conf";
    public static final String K8S_DEPLOYMENT = "-deployment-";
    public static final String K8S_SERVICE = "-rest-";
    public static final String LABEL_PLACEHOLDER = "${label}";
    public static final String CHARSET_UTF8 = "UTF-8";

    public static final List<String> accessControlAllowOrigins = Arrays.asList("*");
    public static final List<String> accessControlAllowMethods = Arrays
            .asList("GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS");
    public static final List<String> accessControlAllowHeaders = Arrays
            .asList("authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction");
    public static final boolean accessControlAllowCredentials = false;
}
