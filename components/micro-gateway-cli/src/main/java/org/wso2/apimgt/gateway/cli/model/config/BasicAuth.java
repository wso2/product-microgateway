/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.model.config;

public class BasicAuth {

    boolean isOptional;
    boolean isRequired;

    public boolean getOptional() {
        boolean optional;
        if (isOptional == true) {
            optional = true;
        } else {
            optional = false;
        }
        return optional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }

    public boolean getRequired() {
        boolean required;
        if (isRequired == true) {
            required = true;
        } else {
            required = false;
        }
        return required;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }
}
