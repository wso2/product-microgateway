/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Objects;

/**
 * A definitions of IP Range. Used for defining IP Range in blocking IP conditions.
 */
public class IPRange {
    private int id;
    private String tenantDomain;
    private String type;
    private String fixedIp;
    private String startingIP;
    private BigInteger startingIpBigIntValue;
    private String endingIp;
    private BigInteger endingIpBigIntValue;
    private boolean invert;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFixedIp() {
        return fixedIp;
    }

    public void setFixedIp(String fixedIp) {
        this.fixedIp = fixedIp;
    }

    public String getStartingIP() {
        return startingIP;
    }

    public void setStartingIP(String startingIP) {
        this.startingIP = startingIP;
    }

    public String getEndingIp() {
        return endingIp;
    }

    public void setEndingIp(String endingIp) {
        this.endingIp = endingIp;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public BigInteger getStartingIpBigIntValue() {
        return startingIpBigIntValue;
    }

    public void setStartingIpBigIntValue(BigInteger startingIpBigIntValue) {
        this.startingIpBigIntValue = startingIpBigIntValue;
    }

    public BigInteger getEndingIpBigIntValue() {
        return endingIpBigIntValue;
    }

    public void setEndingIpBigIntValue(BigInteger endingIpBigIntValue) {
        this.endingIpBigIntValue = endingIpBigIntValue;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IPRange ipRange = (IPRange) o;
        return id == ipRange.id
                && Objects.equals(tenantDomain, ipRange.tenantDomain)
                && Objects.equals(type, ipRange.type);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, tenantDomain, type);
    }
}
