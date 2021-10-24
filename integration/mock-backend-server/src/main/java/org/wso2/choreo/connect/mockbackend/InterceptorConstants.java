/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.mockbackend;

public class InterceptorConstants {
    public enum Handler {
        NONE,
        REQUEST_ONLY,
        RESPONSE_ONLY,
        BOTH
    }

    public static class StatusPayload {
        public static final String HANDLER = "handler";
        public static final String REQUEST_FLOW_REQUEST_BODY = "requestFlowRequestBody";
        public static final String RESPONSE_FLOW_REQUEST_BODY = "responseFlowRequestBody";
    }
}
