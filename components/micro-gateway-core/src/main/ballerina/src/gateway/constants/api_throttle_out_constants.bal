// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

public const int API_THROTTLE_OUT_ERROR_CODE = 900800;
public const int RESOURCE_THROTTLE_OUT_ERROR_CODE = 900802;
public const int APPLICATION_THROTTLE_OUT_ERROR_CODE = 900803;
public const int SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE = 900804;
public const int INTERNAL_ERROR_CODE = 900808;
public const int INTERNAL_ERROR_CODE_POLICY_NOT_FOUND = 900809;
public const int BLOCKING_ERROR_CODE = 900805;
public const int CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE = 900806;

public const string THROTTLE_OUT_MESSAGE = "Message throttled out";
public const string THROTTLE_OUT_DESCRIPTION = "You have exceeded your quota";
public const string BLOCKING_MESSAGE = "Message blocked";
public const string BLOCKING_DESCRIPTION = "You have been blocked from accesing the resource";

const string THROTTLE_OUT_REASON_API_LIMIT_EXCEEDED = "API_LIMIT_EXCEEDED";
const string THROTTLE_OUT_REASON_RESOURCE_LIMIT_EXCEEDED = "RESOURCE_LIMIT_EXCEEDED";
const string THROTTLE_OUT_REASON_SUBSCRIPTION_LIMIT_EXCEEDED = "SUBSCRIPTION_LIMIT_EXCEEDED";
const string THROTTLE_OUT_REASON_APPLICATION_LIMIT_EXCEEDED = "APPLICATION_LIMIT_EXCEEDED";
const string POLICY_NOT_FOUND_DESCRIPTION = "POLICY ENFORCEMENT ERROR";
