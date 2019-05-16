/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;

public class MgwEndpointDTO {
    private String endpointUrl;
    private boolean isEtcdEnabled = false;
    private String etcdKey = "";

    public MgwEndpointDTO(String endpointUrl){
        setEndpointUrl(endpointUrl);
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    /**
     * sets endpointUrl and isEtcdEnabled
     * endpointUrl could be in the format of either 'etcd_key, default url' or 'url'.
     *
     * @param endpointUrl endpoint string
     */
    public void setEndpointUrl(String endpointUrl) {
        //todo: introduce more precise way to distinguish the two types
        if (endpointUrl.startsWith("etcd")) {
            if (endpointUrl.endsWith(")") && endpointUrl.contains("(") && endpointUrl.contains(",")) {
                String temp = endpointUrl.substring(endpointUrl.indexOf("(") + 1, endpointUrl.indexOf(")"));
                String[] entries = temp.split(",");
                if (entries.length != 2) {
                    throw new CLIRuntimeException("'etcd' key containing string should be provided as 'etcd " +
                            "( etcd_key, url)'.");
                }
                isEtcdEnabled = true;
                etcdKey = entries[0];
                endpointUrl = entries[1];
            } else {
                throw new CLIRuntimeException("'etcd' key containing string should be provided as 'etcd " +
                        "( etcd_key, url)'.");
            }
        }
        this.endpointUrl = endpointUrl;
    }

    public boolean isEtcdEnabled() {
        return isEtcdEnabled;
    }
}
