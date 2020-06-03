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
    //Name of the system property define the location of the toolkit distribution
    public static final String SYSTEM_PROP_TOOLKIT = "toolkit";
    //Name of the system property define the current dir of maven execution
    public static final String SYSTEM_PROP_BASE_DIR = "basedir";
    //Name of the system property define the location of the mac OS runtime distribution
    public static final String SYSTEM_PROP_MACOS_RUNTIME = "runtime_macos";
    //Name of the system property define the location of the windows runtime distribution
    public static final String SYSTEM_PROP_WINDOWS_RUNTIME = "runtime_windows";
    //Name of the system property define the location of the linux runtime distribution
    public static final String SYSTEM_PROP_LINUX_RUNTIME = "runtime_linux";
    // Name of the system property define the custom value  transformer jar file location
    public static final String SYSTEM_PROP_JWTTRANSFORMER_JAR = "jwt_transformer_jar";
    // Name of the system property define the custom interceptor jar file location
    public static final String SYSTEM_PROP_INTERCEPTOR_JAR = "interceptor_jar";
    //Microgateway runtime executable file name
    public static final String GATEWAY_SCRIPT_NAME = "gateway";
    //File extension the the ballerina rest file
    public static final String SERVICE_FILE_EXTENSION = ".bal";
    // open API folder name
    public static final String OPEN_APIS = "openAPIs";
}
