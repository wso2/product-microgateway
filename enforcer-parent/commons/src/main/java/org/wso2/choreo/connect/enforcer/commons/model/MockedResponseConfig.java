/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.List;
import java.util.Map;

/**
 * Defines mock API response structure.
 */
public class MockedResponseConfig {
    private List<MockedHeaderConfig> headers;
    private Map<String, MockedContentExamples> contentMap;

    public Map<String, MockedContentExamples> getContentMap() {
        return contentMap;
    }

    public void setContentMap(Map<String, MockedContentExamples> contentMap) {
        this.contentMap = contentMap;
    }

    public List<MockedHeaderConfig> getHeaders() {
        return headers;
    }

    public void setHeaders(List<MockedHeaderConfig> headers) {
        this.headers = headers;
    }
}
