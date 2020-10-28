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

package org.wso2.micro.gateway.filter.core.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

/**
 * Holds data related to claim mapping of a jwt token.
 */
public class ClaimMappingDto implements Serializable {

    @SerializedName("remoteClaim") private String remoteClaim;
    @SerializedName("localClaim") private String localClaim;

    public ClaimMappingDto() {

    }

    public ClaimMappingDto(String remoteClaim, String localClaim) {

        this.remoteClaim = remoteClaim;
        this.localClaim = localClaim;
    }

    public String getRemoteClaim() {

        return remoteClaim;
    }

    public void setRemoteClaim(String remoteClaim) {

        this.remoteClaim = remoteClaim;
    }

    public String getLocalClaim() {

        return localClaim;
    }

    public void setLocalClaim(String localClaim) {

        this.localClaim = localClaim;
    }

    @Override public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClaimMappingDto that = (ClaimMappingDto) o;
        return Objects.equals(localClaim, that.localClaim);
    }

    @Override public int hashCode() {

        return Objects.hash(localClaim);
    }
}

