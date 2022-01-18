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

/**
 * Defines JSON script structure used in the prototyped API implementations.
 */
public class PrototypeConfig {
    private String in;                          // position of prototype API invocation value (query/header)
    private String name;                        // value name expected to use when invoking prototype API
    private List<PrototypeResponse> responses;  // prototype responses defined in the JSON script

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PrototypeResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<PrototypeResponse> responses) {
        this.responses = responses;
    }
}
