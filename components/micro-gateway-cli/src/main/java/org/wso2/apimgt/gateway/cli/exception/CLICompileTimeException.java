/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.apimgt.gateway.cli.exception;

/**
 * Exception type definition for openAPI parsing related errors.
 * This can be used when the provided extensions does not adhere to the rules.
 */
public class CLICompileTimeException extends Exception {
    private String errorMessage;

    /**
     * Creates a CLICompileTimeException using error message.
     *
     * @param message error message
     */
    public CLICompileTimeException(String message) {
        super(message, new RuntimeException());
        this.errorMessage = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
