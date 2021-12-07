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
package org.wso2.choreo.connect.enforcer.api;

import org.wso2.choreo.connect.enforcer.commons.model.SecurityInfo;

/**
 * APIProcessUtils is used to convert the Endpoint Security DTO used in proto files into Enforcer specific
 * Object.
 */
public class APIProcessUtils {
    public static SecurityInfo convertProtoEndpointSecurity
            (org.wso2.choreo.connect.discovery.api.SecurityInfo protoSecurityInfo) {
        SecurityInfo securityInfo = new SecurityInfo();
        securityInfo.setSecurityType(protoSecurityInfo.getSecurityType());
        securityInfo.setUsername(protoSecurityInfo.getUsername());
        securityInfo.setPassword(protoSecurityInfo.getPassword().toCharArray());
        securityInfo.setCustomParameters(protoSecurityInfo.getCustomParametersMap());
        securityInfo.setEnabled(protoSecurityInfo.getEnabled());
        return securityInfo;
    }
}
