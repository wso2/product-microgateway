/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.enforcer.commons.logging;

/**
 * Represents a model for Error logs to hold few additional attributes
 */
public class ErrorDetails {
    private String severity;
    private int code;

    /**
     * @param severity severity level of the error
     * @param code     unique code to troubleshoot the error
     */
    private ErrorDetails(String severity, int code) {
        this.severity = severity;
        this.code = code;
    }

    /**
     * static method to initiate a errorLog with details, without 'new' operator
     *
     * @param severity severity level of the error
     * @param code     unique code to troubleshoot the error
     * @return ErrorDetails object with given parameters
     */
    public static ErrorDetails errorLog(String severity, int code) {
        return new ErrorDetails(severity, code);
    }

    /**
     * Get severity of the error
     * @return Severity
     */
    public String getSeverity() {
        return this.severity;
    }

    /**
     * Set severity of the error
     * @param severity
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * Get error code for logging
     * @return error code
     */
    public int getCode() {
        return this.code;
    }

    /**
     * Set error code with given value
     * @param code
     */
    public void setCode(int code) {
        this.code = code;
    }
}
