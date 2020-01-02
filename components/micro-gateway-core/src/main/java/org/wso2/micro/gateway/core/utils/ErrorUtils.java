/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.utils;

import org.ballerinalang.jvm.BallerinaErrors;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.values.ErrorValue;
import org.wso2.micro.gateway.core.Constants;

/**
 * Util class for handle error related to native functions.
 */
public class ErrorUtils {

    private static final String UNKNOWN_MESSAGE = "Unknown Error";
    private static final BPackage PROTOCOL_PACKAGE_GATEWAY = new BPackage(Constants.ORG_NAME, Constants.PACKAGE_NAME,
            getGatewayVersion());

    /**
     * Returns error object for input reason.
     * Error type is generic ballerina error type. This utility to construct error object from message.
     *
     * @param error Reason for creating the error object. If the reason is null, "UNKNOWN" sets by
     *              default.
     * @param ex    Java throwable object to capture description of error struct. If throwable object is null,
     *              "Unknown Error" sets to message by default.
     * @return Ballerina error object.
     */
    public static ErrorValue getBallerinaError(String error, Throwable ex) {
        String errorMsg = error != null && ex.getMessage() != null ? ex.getMessage() : UNKNOWN_MESSAGE;
        return getBallerinaError(error, errorMsg);
    }

    /**
     * Returns error object for input reason and details.
     * Error type is generic ballerina error type. This utility to construct error object from message.
     *
     * @param error   The specific error type.
     * @param details Java throwable object to capture description of error struct. If throwable object is null,
     *                "Unknown Error" is set to message by default.
     * @return Ballerina error object.
     */
    public static ErrorValue getBallerinaError(String error, String details) {
        return BallerinaErrors.createError(error, details);
    }

    public static String getGatewayVersion() {
        String version = Constants.PACKAGE_VERSION;
        if (System.getenv(Constants.GATEWAY_VERSION) != null) {
            version = System.getenv(Constants.GATEWAY_VERSION);
        }
        return version;
    }


}
