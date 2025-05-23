/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.enforcer.mcp;

import org.wso2.choreo.connect.enforcer.mcp.response.PayloadGenerator;

/**
 * This class is used to throw MCP related errors
 */
public class McpException extends Exception {
    private final int errorCode;
    private final String errorMessage;
    private final String data;

    public McpException(int errorCode, String errorMessage, String data) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getData() {
        return data;
    }

    public String toJsonRpcErrorPayload() {
        return PayloadGenerator.getErrorResponse(errorCode, errorMessage, data);
    }
}
