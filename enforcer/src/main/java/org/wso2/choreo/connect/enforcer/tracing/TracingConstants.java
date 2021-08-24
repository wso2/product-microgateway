/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.wso2.choreo.connect.enforcer.tracing;

public class TracingConstants {

    public static final String REQUEST_ID = "request-id";
    public static final String EXT_AUTH_SERVICE = "ExtAuthService:check()";
    public static final String DECODE_TOKEN_HEADER = "JWTAuthenticator:authenticate():Decode token header";
    public static final String JWT_VALIDATION = "JWTAuthenticator:authenticate():JWT validation";
    public static final String SUBSCRIPTION_VALIDATION = "JWTAuthenticator:authenticate():Validate subscription using key manager";
    public static final String SCOPES_VALIDATION = "JWTAuthenticator:authenticate():Validate scopes";
    public static final String DO_THROTTLE = "ThrottleFilter:doThrottle():Throttling function";

}
