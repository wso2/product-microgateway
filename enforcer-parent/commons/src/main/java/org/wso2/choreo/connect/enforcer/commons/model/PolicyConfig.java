/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.ArrayList;

/**
 * policy configurations
 */
public class PolicyConfig {

    private ArrayList<Policy> request;
    private ArrayList<Policy> response;
    private ArrayList<Policy> fault;

    public ArrayList<Policy> getRequest() {
        return request;
    }

    public void setRequest(ArrayList<Policy> request) {
        this.request = request;
    }

    public ArrayList<Policy> getResponse() {
        return response;
    }

    public void setResponse(ArrayList<Policy> response) {
        this.response = response;
    }

    public ArrayList<Policy> getFault() {
        return fault;
    }

    public void setFault(ArrayList<Policy> fault) {
        this.fault = fault;
    }
}
