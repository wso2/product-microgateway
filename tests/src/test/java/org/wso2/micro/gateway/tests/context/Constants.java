/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.context;

public class Constants {
    //Name of the system property define the location of the rest distribution
    public static final String SYSTEM_PROP_SERVER_ZIP = "server.zip";
    //Name of the system property define the current dir of maven execution
    public static final String SYSTEM_PROP_BASE_DIR = "basedir";

    public static final String BALLERINA_PLATFORM_DIR = "/lib/platform/";
    //Name of the script file which start the server
    public static final String BALLERINA_SERVER_SCRIPT_NAME = "ballerina";
    //File extension the the ballerina rest file
    public static final String SERVICE_FILE_EXTENSION = ".bal";
    //policies file name
    public static final String POLICIES_FILE = "policies.yaml";
    //definitions folder location in the cli
    public static final String DEFINITION_FOLDER = "definitions";
    //resources folder location in the cli
    public static final String RESOURCES_FOLDER = "resources";
    // open API folder name
    public static final String OPEN_APIS = "openAPIs";
}
