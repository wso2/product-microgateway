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

import org.wso2.choreo.connect.enforcer.config.ConfigDefaults;

public class TracingDTO {

    private boolean isTracingEnabled = ConfigDefaults.TRACING_ENABLED_VALUE;
    private String connectionString = null;
    private String instrumentationName = ConfigDefaults.TRACING_INSTRUMENTATION_NAME;
    private int maximumTracesPerSecond = 2;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public boolean isTracingEnabled() {
        return isTracingEnabled;
    }

    public void setTracingEnabled(boolean enabled) {
        this.isTracingEnabled = enabled;
    }

    public String getInstrumentationName() {
        return instrumentationName;
    }

    public void setInstrumentationName(String instrumentationName) {
        this.instrumentationName = instrumentationName;
    }

    public int getMaximumTracesPerSecond() {
        return maximumTracesPerSecond;
    }

    public void setMaximumTracesPerSecond(int maximumTracesPerSecond) {
        this.maximumTracesPerSecond = maximumTracesPerSecond;
    }
}
