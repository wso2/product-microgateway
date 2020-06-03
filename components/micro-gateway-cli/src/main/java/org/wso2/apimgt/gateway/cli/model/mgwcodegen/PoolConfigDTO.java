/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

/**
 * This DTO holds the data related to endpoint pool configurations.
 */
public class PoolConfigDTO {
    private int maxActiveConnections = 0;
    private int maxIdleConnections = -1;
    private int waitTimeInMillis = -1;
    private int maxActiveStreamsPerConnection = -1;

    public int getMaxActiveConnections() {
        return maxActiveConnections;
    }

    public void setMaxActiveConnections(int maxActiveConnections) {
        this.maxActiveConnections = maxActiveConnections;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public int getWaitTimeInMillis() {
        return waitTimeInMillis;
    }

    public void setWaitTimeInMillis(int waitTimeInMillis) {
        this.waitTimeInMillis = waitTimeInMillis;
    }

    public int getMaxActiveStreamsPerConnection() {
        return maxActiveStreamsPerConnection;
    }

    public void setMaxActiveStreamsPerConnection(int maxActiveStreamsPerConnection) {
        this.maxActiveStreamsPerConnection = maxActiveStreamsPerConnection;
    }
}
