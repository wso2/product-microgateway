/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * Exception class to throw when runtime exception happen and to exit the system
 */
public class CLIRuntimeException extends RuntimeException {
    private final static int DEFAULT_EXIT_CODE = 1;
    private int exitCode;
    private String terminalMsg;

    public CLIRuntimeException(String message) {
        this(message, message, DEFAULT_EXIT_CODE);
    }

    public CLIRuntimeException(String message, Throwable e) {
        this(message, message, DEFAULT_EXIT_CODE, e);
    }

    public CLIRuntimeException(String message, int exitCode) {
        this (message, message, exitCode);
    }

    public CLIRuntimeException(String terminalMsg, String internalMsg, int exitCode) {
        super(internalMsg);
        this.exitCode = exitCode;
        this.terminalMsg = terminalMsg;
    }

    public CLIRuntimeException(String terminalMsg, String internalMsg, int exitCode, Throwable e) {
        super(internalMsg, e);
        this.exitCode = exitCode;
        this.terminalMsg = terminalMsg;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getTerminalMsg() {
        return terminalMsg;
    }

    public void setTerminalMsg(String terminalMsg) {
        this.terminalMsg = terminalMsg;
    }
}
