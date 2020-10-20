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

package org.wso2.micro.gateway.filter.core.models;

import org.wso2.micro.gateway.filter.core.common.CacheableEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity for keeping Application related information. Represents an Application in APIM.
 */
public class Application implements CacheableEntity<Integer> {

    private Integer id = null;
    private String uuid;
    private String name = null;
    private Integer subId = null;
    private String subName = null;
    private String policy = null;
    private String tokenType = null;
    private List<String> groupIds = new ArrayList<>();
    private Map<String, String> attributes = new ConcurrentHashMap<>();

    public Integer getId() {

        return id;
    }

    public void setId(Integer id) {

        this.id = id;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public Integer getSubId() {

        return subId;
    }

    public void setSubId(Integer subId) {

        this.subId = subId;
    }

    public String getPolicy() {

        return policy;
    }

    public void setPolicy(String policy) {

        this.policy = policy;
    }

    public String getTokenType() {

        return tokenType;
    }

    public void setTokenType(String tokenType) {

        this.tokenType = tokenType;
    }

    public Integer getCacheKey() {

        return getId();
    }

    public List<String> getGroupIds() {

        return groupIds;
    }

    public void addGroupId(String groupId) {

        this.groupIds.add(groupId);
    }

    public void removeGroupId(String groupId) {

        this.groupIds.remove(groupId);
    }

    public Map<String, String> getAttributes() {

        return attributes;
    }

    public void addAttribute(String key, String value) {

        this.attributes.put(key, value);
    }

    public void removeAttribute(String key) {

        this.attributes.remove(key);
    }

    public String getSubName() {

        return subName;
    }

    public void setSubName(String subName) {

        this.subName = subName;
    }

    public void setUUID(String uuid) {

        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "Application [id=" + id + ", name=" + name + ", subId=" + subId + ", policy=" + policy + ", tokenType="
                + tokenType + ", groupIds=" + groupIds + ", attributes=" + attributes + "]";
    }

    public String getUUID() {

        return uuid;
    }
}

