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
 * Class to keep constants used for logging
 */
public class LoggingConstants {
    /**
     * Constants related to log severity
     */
    public final class Severity {
        public static final String BLOCKER = "Blocker";
        public static final String CRITICAL = "Critical";
        public static final String MAJOR = "Major";
        public static final String MINOR = "Minor";
        public static final String TRIVIAL = "Trivial";
        public static final String DEFAULT = "Default";
    }

    /**
     * Constants for log attribute names
     */
    public final class LogAttributes {
        public static final String SEVERITY = "severity";
        public static final String ERROR_CODE = "error_code";
        public static final String TIMESTAMP = "timestamp";
        public static final String LEVEL = "level";
        public static final String LOGGER = "logger";
        public static final String MESSAGE = "message";
        public static final String TRACE_ID = "traceId";
        public static final String CONTEXT = "context";
    }
}
