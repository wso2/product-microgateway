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
 * This DTO holds the data related to rolling window configurations used for the circuit breaking.
 */
public class RollingWindowConfigDTO {
    private int requestVolumeThreshold = 10;
    private int timeWindowInMillis = 60000;
    private int bucketSizeInMillis = 10000;

    public int getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    public void setRequestVolumeThreshold(int requestVolumeThreshold) {
        this.requestVolumeThreshold = requestVolumeThreshold;
    }

    public int getTimeWindowInMillis() {
        return timeWindowInMillis;
    }

    public void setTimeWindowInMillis(int timeWindowInMillis) {
        this.timeWindowInMillis = timeWindowInMillis;
    }

    public int getBucketSizeInMillis() {
        return bucketSizeInMillis;
    }

    public void setBucketSizeInMillis(int bucketSizeInMillis) {
        this.bucketSizeInMillis = bucketSizeInMillis;
    }
}
