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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.throttle.dto;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the applicable conditions and the types of conditions for a conditional throttle policy.
 */
public class ThrottleCondition {
    private IPCondition ipCondition;
    private IPCondition ipRangeCondition;
    private HeaderConditions headerConditions;
    private QueryParamConditions queryParameterConditions;
    private JWTClaimConditions jwtClaimConditions;

    public IPCondition getIpCondition() {
        return ipCondition;
    }

    public void setIpCondition(IPCondition ipCondition) {
        this.ipCondition = ipCondition;
    }

    public IPCondition getIpRangeCondition() {
        return ipRangeCondition;
    }

    public void setIpRangeCondition(IPCondition ipRangeCondition) {
        this.ipRangeCondition = ipRangeCondition;
    }

    public HeaderConditions getHeaderConditions() {
        return headerConditions;
    }

    public void setHeaderConditions(HeaderConditions headerConditions) {
        this.headerConditions = headerConditions;
    }

    public QueryParamConditions getQueryParameterConditions() {
        return queryParameterConditions;
    }

    public void setQueryParameterConditions(QueryParamConditions queryParameterConditions) {
        this.queryParameterConditions = queryParameterConditions;
    }

    public JWTClaimConditions getJwtClaimConditions() {
        return jwtClaimConditions;
    }

    public void setJwtClaimConditions(JWTClaimConditions jwtClaimConditions) {
        this.jwtClaimConditions = jwtClaimConditions;
    }

    /**
     * Throttle condition based on request headers.
     */
    public static class HeaderConditions {
        private Map<String, String> values = new HashMap<>();
        private boolean invert;

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values;
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }
    }

    /**
     * Throttle condition based on request query parameters.
     */
    public static class QueryParamConditions {
        private Map<String, String> values = new HashMap<>();
        private boolean invert;

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values;
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }
    }

    /**
     * Throttle condition based on jwt claim conditions.
     */
    public static class JWTClaimConditions {
        private Map<String, String> values = new HashMap<>();
        private boolean invert;

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values;
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }
    }

    /**
     * Throttle condition based on request IP address.
     */
    public static class IPCondition {
        private BigInteger specificIp;
        private BigInteger startingIp;
        private BigInteger endingIp;
        private boolean invert;

        public IPCondition() {

        }

        public IPCondition(BigInteger specificIp, boolean invert) {

            this.specificIp = specificIp;
            this.invert = invert;
        }

        public IPCondition(BigInteger startingIp, BigInteger endingIp, boolean invert) {

            this.startingIp = startingIp;
            this.endingIp = endingIp;
            this.invert = invert;
        }

        public BigInteger getSpecificIp() {
            return specificIp;
        }

        public void setSpecificIp(BigInteger specificIp) {
            this.specificIp = specificIp;
        }

        public BigInteger getStartingIp() {
            return startingIp;
        }

        public void setStartingIp(BigInteger startingIp) {
            this.startingIp = startingIp;
        }

        public BigInteger getEndingIp() {
            return endingIp;
        }

        public void setEndingIp(BigInteger endingIp) {
            this.endingIp = endingIp;
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }
    }
}
