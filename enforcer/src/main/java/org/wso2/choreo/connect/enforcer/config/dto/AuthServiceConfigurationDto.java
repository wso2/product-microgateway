/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.config.dto;

/**
 * Holds the configurations related to gRPC netty server that implements the enforcer functionality.
 */
public class AuthServiceConfigurationDto {

    private int port;
    private int maxMessageSize;
    private int maxHeaderLimit;
    private int keepAliveTime;
    private ThreadPoolConfig threadPool;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public int getMaxHeaderLimit() {
        return maxHeaderLimit;
    }

    public void setMaxHeaderLimit(int maxHeaderLimit) {
        this.maxHeaderLimit = maxHeaderLimit;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public ThreadPoolConfig getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPoolConfig threadPool) {
        this.threadPool = threadPool;
    }
}

