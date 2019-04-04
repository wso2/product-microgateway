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
package org.wso2.apimgt.gateway.cli.constants;

import java.io.File;

/**
 * Constants for swagger code generator.
 */
public class GeneratorConstants {

    public static final String SERVICE_TEMPLATE_NAME = "service";
    public static final String MAIN_TEMPLATE_NAME = "main";
    public static final String GENERATESWAGGER_TEMPLATE_NAME = "generateSwagger";
    public static final String THROTTLE_POLICY_TEMPLATE_NAME = "policy";
    public static final String LISTENERS_TEMPLATE_NAME = "listeners";
    public static final String LISTENERS = "listeners";
    public static final String BALLERINA_EXTENSION = ".bal";
    public static final String JSON_EXTENSION = ".json";
    public static final String THROTTLE_POLICY_INIT_TEMPLATE_NAME = "policy_init";
    public static final String SWAGGER_FILE_SUFFIX = "_swagger";

    public static final String TEMPLATES_SUFFIX = ".mustache";
    public static final String TEMPLATES_DIR_PATH_KEY = "templates.dir.path";
    public static final String DEFAULT_TEMPLATE_DIR = File.separator + "templates";

    public static final String APPLICATION_POLICY_TYPE = "application";
    public static final String SUBSCRIPTION_POLICY_TYPE = "subscription";
    public static final String APPLICATION_INIT_FUNC_PREFIX = "initApplication";
    public static final String SUBSCRIPTION_INIT_FUNC_PREFIX = "initSubscription";
    public static final String APPLICATION_KEY = "appKey";
    public static final String SUBSCRIPTION_KEY = "subscriptionKey";
    public static final String APPLICATION_TIER_TYPE = "appTier";
    public static final String SUBSCRIPTION_TIER_TYPE = "subscriptionTier";
    public static final String INIT_FUNC_SUFFIX = "Policy";
    public static final String THROTTLE_POLICY_INITIALIZER = "throttle_policy_initializer";
    public static final String UTF_8 = "UTF-8";

}
