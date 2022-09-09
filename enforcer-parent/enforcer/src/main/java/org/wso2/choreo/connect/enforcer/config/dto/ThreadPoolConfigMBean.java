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

package org.wso2.choreo.connect.enforcer.config.dto;

/**
 * MBean API for Thread Pool Configuration.
 */
public interface ThreadPoolConfigMBean {
    /**
     * Getter for core size.
     * 
     * @return int
     */
    public int getCoreSize();

    /**
     * Getter for max size.
     * 
     * @return int
     */
    public int getMaxSize();

    /**
     * Getter for keep alive size.
     * 
     * @return int
     */
    public int getKeepAliveTime();

    /**
     * Getter for queue size.
     * 
     * @return int
     */
    public int getQueueSize();
}
