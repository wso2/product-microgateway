/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.choreo.connect.enforcer.metrics.jmx.api;

/**
 * MBean API for ExtAuth Service metrics.
 */
public interface ExtAuthMetricsMXBean {

    /**
     * Getter for total request count.
     * 
     * @return long
     */
    public long getTotalRequestCount();

    /**
     * Getter for average response time in milli seconds.
     * 
     * @return long
     */
    public long getAverageResponseTimeMillis();

    /**
     * Getter for maximum response time in milliseconds.
     * 
     * @return long
     */
    public long getMaxResponseTimeMillis();

    /**
     * Getter for mimnimum response time in milliseconds.
     * 
     * @return long
     */
    public long getMinResponseTimeMillis();

    /**
     * Resets all the metrics to thier initial values.
     */
    public void resetExtAuthMetrics();

    /**
     * Resets all the metrics to thier initial values.
     */
    public long getRequestCountInLastFiveMinutes();

}
