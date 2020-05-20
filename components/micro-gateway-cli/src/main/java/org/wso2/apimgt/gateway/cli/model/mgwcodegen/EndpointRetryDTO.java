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

import java.util.ArrayList;
import java.util.List;

/**
 * This DTO holds the data related to endpoint retries in case of a failure scenario
 */
public class EndpointRetryDTO {
    private int count = 0;
    private int intervalInMillis = 0;
    private double backOffFactor = 0.0;
    private int maxWaitIntervalInMillis = 0;
    private List<Integer> statusCodes = new ArrayList<>();

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getIntervalInMillis() {
        return intervalInMillis;
    }

    public void setIntervalInMillis(int intervalInMillis) {
        this.intervalInMillis = intervalInMillis;
    }

    public double getBackOffFactor() {
        return backOffFactor;
    }

    public void setBackOffFactor(double backOffFactor) {
        this.backOffFactor = backOffFactor;
    }

    public int getMaxWaitIntervalInMillis() {
        return maxWaitIntervalInMillis;
    }

    public void setMaxWaitIntervalInMillis(int maxWaitIntervalInMillis) {
        this.maxWaitIntervalInMillis = maxWaitIntervalInMillis;
    }

    public List<Integer> getStatusCodes() {
        return statusCodes;
    }

    public void setStatusCodes(List<Integer> statusCodes) {
        this.statusCodes = statusCodes;
    }
}
