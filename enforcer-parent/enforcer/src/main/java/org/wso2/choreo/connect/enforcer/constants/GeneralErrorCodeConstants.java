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
 */

package org.wso2.choreo.connect.enforcer.constants;

/**
 * Common set of constants that holds error code, messages and descriptions.
 */
public class GeneralErrorCodeConstants {
    public static final int API_BLOCKED_CODE = 700700;
    public static final String API_BLOCKED_MESSAGE = "API blocked";
    public static final String API_BLOCKED_DESCRIPTION = "This API has been blocked temporarily. "
            + "Please try again later or contact the system administrators.";

    // TODO: (renuka) check error codes with APIM
    public static final int MEDIATION_POLICY_ERROR_CODE = 901100;

    /**
     * Contains mock impl endpoint apis related errors
     */
    public static class MockImpl {
        public static final String BAD_REQUEST_CODE = "900870";
        public static final String NOT_IMPLEMENTED_CODE = "900871";
    }
}
