/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.core;

/**
 * Constants for gateway package native functions.
 */
public class Constants {
    /**
     * Organization name.
     */
    public static final String ORG_NAME = "wso2";

    /**
     * Package name.
     */
    public static final String PACKAGE_NAME = "gateway";

    /**
     * gateway package version.
     */
    public static final String PACKAGE_VERSION = "3.1.0";

    public static final String GATEWAY_VERSION = "MGW_VERSION";

    public static final String FILE_NOT_FOUND_ERROR = "{wso2/gateway}FileNotFoundError";

    public static final String SCHEMA_REFERENCE = "$ref";
    public static final String PATHS = "$..paths..";
    public static final String BODY_CONTENT = ".requestBody.content.application/json.schema";
    public static final String JSON_PATH = "$.";
    public static final String ITEMS = "items";
    public static final String OPEN_API = ".openapi";
    public static final char JSONPATH_SEPARATE = '.';
    public static final String PARAM_SCHEMA = ".parameters..schema";
    public static final String REQUEST_BODY = "..requestBody";
    public static final String JSON_RESPONSES = ".responses.";
    public static final String DEFAULT = "default";
    public static final String CONTENT = ".content";
    public static final String JSON_CONTENT = ".application/json.schema.$ref";
    public static final String SCHEMA = ".schema";
    public static final String EMPTY_ARRAY = "[]";
    public static final String DEFINITIONS = "definitions";
    public static final String COMPONENT_SCHEMA = "components/schemas";
    public static final char HASH = '#';
    public static final String EMPTY = "";
    public static final String BACKWARD_SLASH = "\"";
    public static final char FORWARD_SLASH = '/';
    public static final String REQUESTBODY_SCHEMA = "components.requestBodies.";
    public static final String REQUESTBODIES = "requestBodies";
    public static final String JSONPATH_SCHEMAS = "$..components.schemas.";
    public static final String JSON_SCHEMA = ".content.application/json.schema";
    public static final String VALIDATED_STATUS = "validated";
    public static final String RUNTIME_HOME_PATH = "mgw-runtime.home";
}
