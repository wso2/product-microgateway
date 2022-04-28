/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.analytics;

/**
 * AnalyticsConstants holds the pre determined configuration keys provided.
 */
public class AnalyticsConstants {

    public static final String PUBLISHER_IMPL_CONFIG_KEY = "publisherImpl";
    protected static final String IS_CHOREO_DEPLOYMENT_CONFIG_KEY = "isChoreoDeployment";
    protected static final String TYPE_CONFIG_KEY = "type";
    protected static final String PUBLISHER_REPORTER_CLASS_CONFIG_KEY = "publisher.reporter.class";
    public static final String AUTH_URL_CONFIG_KEY = "authURL";
    public static final String AUTH_TOKEN_CONFIG_KEY = "authToken";

    public static final String RESPONSE_SCHEMA = "RESPONSE";
    public static final String ERROR_SCHEMA = "ERROR";
    protected static final String CHOREO_RESPONSE_SCHEMA = "CHOREO_RESPONSE";
    protected static final String CHOREO_FAULT_SCHEMA = "CHOREO_ERROR";

    protected static final String ELK_TYPE = "elk";
    protected static final String DEFAULT_ELK_PUBLISHER_REPORTER_CLASS 
            = "org.wso2.am.analytics.publisher.reporter.elk.ELKMetricReporter";
}
