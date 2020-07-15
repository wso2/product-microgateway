/*
*  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.micro.gateway.tests.util;

/**
 * Constants used in test cases.
 */
public class TestConstant {
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CHARSET_NAME = "UTF-8";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String HTTP_METHOD_HEAD = "HEAD";

    public static final String KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String KEY_TYPE_SANDBOX = "SANDBOX";

    public static final String TOKEN_TYPE_INVALID_SCOPES = "INVALID_SCOPES";
    public static final String TOKEN_TYPE_INVALID_SUBSCRIPTION = "INVALID_SUBSCRIPTION";
    public static final String TOKEN_TYPE_SUBSCRIPTION_THROTTLING = "SUBSCRIPTION_THROTTLING";
    public static final String TOKEN_TYPE_APPLICATION_THROTTLING = "APPLICATION_THROTTLING";
    public static final String TOKEN_TYPE_5_MIN_SUB = "5_MIN_SUB_THROTTLING";
    public static final String TOKEN_TYPE_5_MIN_APP = "5_MIN_APP_THROTTLING";
    public static final String TOKEN_TYPE_INVALID_APP_POLICY = "INVALID_APP_POLICY";
    public static final String TOKEN_TYPE_INVALID_SUB_POLICY = "INVALID_SUB_POLICY";

    public static final int GATEWAY_LISTENER_HTTP_PORT = 9590;
    public static final int GATEWAY_LISTENER_HTTPS_PORT = 9595;
    public static final int GATEWAY_LISTENER_HTTPS_TOKEN_PORT = 9596;
}
