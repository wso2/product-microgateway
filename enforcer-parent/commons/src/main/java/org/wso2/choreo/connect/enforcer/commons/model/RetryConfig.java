//  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
//  WSO2 Inc. licenses this file to you under the Apache License,
//  Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing,
//  software distributed under the License is distributed on an
//  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//  KIND, either express or implied.  See the License for the
//  specific language governing permissions and limitations
//  under the License.

package org.wso2.choreo.connect.enforcer.commons.model;

/**
 * The Retry configuration of the cluster which will be referred when setting the route headers
 */
public class RetryConfig {
    int count;
    Integer[] statusCodes;

    /**
     * @param count Number of times to retry
     * @param statusCodes Http status codes on which retrying must be done
     */
    public RetryConfig(int count, Integer[] statusCodes) {
        this.count = count;
        this.statusCodes = statusCodes;
    }

    /**
     * @return Number of times to retry
     */
    public int getCount() {
        return count;
    }

    /**
     * @return Http status codes on which retrying must be done
     */
    public Integer[] getStatusCodes() {
        return statusCodes;
    }
}
