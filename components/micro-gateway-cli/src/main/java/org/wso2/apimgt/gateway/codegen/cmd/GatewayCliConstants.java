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
package org.wso2.apimgt.gateway.codegen.cmd;

import java.io.File;

public class GatewayCliConstants {
    public static String BALLERINA_HOME = "BALLERINA_HOME";
    public static String MAIN_DIRECTORY_NAME = "micro-gw-resources";
    public static String CONF_DIRECTORY_NAME = "conf";
    public static String PROJECTS_DIRECTORY_NAME = "projects";
    public static String PROJECTS_CONFIG_DIRECTORY_NAME = "gateway-config";
    public static String PROJECTS_SRC_DIRECTORY_NAME = "src";
    public static String PROJECTS_TARGET_DIRECTORY_NAME = "target";
    public static String MAIN_CONFIG_FILE_NAME = "config.toml";
    public static String LABEL_CONFIG_FILE_NAME = "label-config.toml";
    public static String TEMP_DIR_NAME = "temp";
    public static String PROJECT_ROOT_HOLDER_FILE_NAME = "workspace.txt";
    public static String DEFAULT_MAIN_CONFIG_FILE_NAME = "default-config.toml";
    public static String DEFAULT_LABEL_CONFIG_FILE_NAME = "default-label-config.toml";
    public static String CLI_HOME = "CLI_HOME";
    public static String CLI_LIB = "lib";
    public static String CLI_RUNTIME = "runtime";
    public static String POLICY_DIR = "policies";

    public static String EXTENSION_BALX = ".balx";
    public static String EXTENSION_ZIP = ".zip";

    public static String GW_TARGET_DIST = "distribution";

    public static String GW_DIST_PREFIX = "micro-gw-";
    public static String GW_DIST_BIN = "bin";
    public static String GW_DIST_CONF = "conf";
    public static String GW_DIST_RESOURCES = "resources";
    public static String GW_DIST_FILTERS = "filters";
    public static String GW_DIST_RUNTIME = "runtime";
    public static String GW_DIST_EXEC = "exec";
    public static String GW_DIST_SH = "micro-gw.sh";
    public static String GW_DIST_SH_PATH = "distribution" + File.separator + GW_DIST_BIN + File.separator + GW_DIST_SH;
    public static final String GW_DIST_EXTENSION_FILTER = "extension_filter.bal";
    public static final String GW_DIST_CONF_FILE = "micro-gw.conf";
    public static final String PROJECT_CONF_FILE = "ballerina.conf";


    public static String LABEL_PLACEHOLDER = "${label}";
}
