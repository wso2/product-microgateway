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
    private String terminalMsg;
    private static final int DEFAULT_EXIT_CODE = 1;
    private int exitCode;

    /**
     * Creates a CLICompileTimeException using error message.
     *
     * @param message error message
     */
    public CLICompileTimeException(String message) {
        this(message, message, DEFAULT_EXIT_CODE);
    }

    public CLICompileTimeException(String message, Throwable e) {
        this(message, message, DEFAULT_EXIT_CODE, e);
    }

    public CLICompileTimeException(String message, int exitCode) {
        this(message, message, exitCode);
    }

    public CLICompileTimeException(String terminalMsg, String internalMsg, int exitCode) {
        super(internalMsg);
        this.exitCode = exitCode;
        this.terminalMsg = terminalMsg;
    }

    public CLICompileTimeException(String terminalMsg, String internalMsg, int exitCode, Throwable e) {
        super(internalMsg, e);
        this.exitCode = exitCode;
        this.terminalMsg = terminalMsg;
    }

    public String getTerminalMsg() {
        return terminalMsg;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
