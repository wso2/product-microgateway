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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.throttle;

/**
 * Constants related to Throttling
 */
public class ThrottleConstants {
    public static final int API_THROTTLE_OUT_ERROR_CODE = 900800;
    public static final int RESOURCE_THROTTLE_OUT_ERROR_CODE = 900802;
    public static final int APPLICATION_THROTTLE_OUT_ERROR_CODE = 900803;
    public static final int SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE = 900804;
    public static final int BLOCKED_ERROR_CODE = 900805;
    public static final int CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE = 900806;
    // This value is used to assign when the actual throttle policy is not properly assigned. If this
    public static final int THROTTLE_CONDITION_UNKNOWN = 900807;

    public static final String THROTTLE_OUT_MESSAGE = "Message throttled out";
    public static final String THROTTLE_OUT_DESCRIPTION = "You have exceeded your quota";
    public static final String BLOCKING_MESSAGE = "Message blocked";
    public static final String BLOCKING_DESCRIPTION = "You have been blocked from accessing the resource";

    public static final String THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED = "API_LIMIT_EXCEEDED";
    public static final String THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED = "RESOURCE_LIMIT_EXCEEDED";
    public static final String THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED = "SUBSCRIPTION_LIMIT_EXCEEDED";
    public static final String THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED = "APPLICATION_LIMIT_EXCEEDED";
    public static final String THROTTLE_OUT_REASON_CUSTOM_LIMIT_EXCEED = "CUSTOM_POLICY_LIMIT_EXCEED";
    public static final String THROTTLE_OUT_REASON_REQUEST_BLOCKED = "REQUEST_BLOCKED";

    public static final String UNLIMITED_TIER = "Unlimited";
    public static final String IP = "ip";
    public static final String IPV6 = "ipv6";
    public static final String THROTTLE_KEY = "throttleKey";
    public static final String THROTTLE_OUT_REASON = "THROTTLED_OUT_REASON";
    public static final String TOPIC_THROTTLE_DATA = "throttleData";
    public static final String IS_THROTTLED = "isThrottled";
    public static final String EXPIRY_TIMESTAMP = "expiryTimeStamp";
    public static final String EVALUATED_CONDITIONS = "evaluatedConditions";
    public static final String TRUE = "true";
    public static final String ADD = "add";
    public static final String DEFAULT_THROTTLE_CONDITION = "default";
    public static final String HEADER_RETRY_AFTER = "Retry-After";
    public static final String GMT = "GMT";

    // blocking constants
    public static final String BLOCKING_CONDITIONS_IP = "IP";
    public static final String BLOCK_CONDITION_IP_RANGE = "IPRANGE";
    public static final String CUSTOM_THROTTLE_PROPERTIES = "customProperty";
}
