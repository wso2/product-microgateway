/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.choreo.connect.enforcer.config.dto;

import java.util.Map;

/**
 * Holder meta data related to tracing.
 */
public class TracingDTO {

    private boolean isTracingEnabled;
    private String exporterType;
    private Map<String, String> configProperties;

    public boolean isTracingEnabled() {
        return isTracingEnabled;
    }

    public void setTracingEnabled(boolean enabled) {
        this.isTracingEnabled = enabled;
    }

    public String getExporterType() {
        return exporterType;
    }

    public void setExporterType(String type) {
        this.exporterType = type;
    }

    public Map<String, String> getConfigProperties() {
        return configProperties;
    }

    public void setConfigProperties(Map<String, String> configProperties) {
        this.configProperties = configProperties;
    }
}
