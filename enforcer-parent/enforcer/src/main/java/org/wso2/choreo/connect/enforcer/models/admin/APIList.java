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

package org.wso2.choreo.connect.enforcer.models.admin;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds details about list of APIs.
 */
public class APIList {

    private Integer count = null;
    private List<BasicAPIInfo> list = new ArrayList<>();

    public Integer getCount() {

        return count;
    }

    public void setCount(Integer count) {

        this.count = count;
    }

    public List<BasicAPIInfo> getList() {

        return list;
    }

    public void setList(List<BasicAPIInfo> list) {

        this.list = list;
    }
}
