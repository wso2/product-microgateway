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

package org.wso2.choreo.connect.enforcer.mcp.response;

import com.google.gson.annotations.SerializedName;

/**
 * This class is used to represent the error response for the MCP.
 */
public class McpErrorResponse extends McpResponse {
    @SerializedName("error")
    McpError error;

    public McpErrorResponse(Integer id, McpError error) {
        super(id);
        this.error = error;
    }
    public void setError(McpError error) {
        this.error = error;
    }
    public McpError getError() {
        return error;
    }
}
