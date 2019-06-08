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

import java.net.MalformedURLException;
import java.net.URL;

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
        if (endpointUrl.trim().matches("etcd\\s*\\(.*,.*\\)")) {
            String temp = endpointUrl.substring(endpointUrl.indexOf("(") + 1, endpointUrl.indexOf(")"));
            String[] entries = temp.split(",");
            if (entries.length != 2) {
                throw new CLIRuntimeException("'etcd' key containing string should be provided as 'etcd " +
                        "(etcd_key, default_url)'.");
            }
            isEtcdEnabled = true;
            etcdKey = entries[0];
            this.endpointUrl = entries[1];
        } else {
            this.endpointUrl = endpointUrl;
        }
        validateURL(this.endpointUrl);
    }

    public boolean isEtcdEnabled() {
        return isEtcdEnabled;
    }

    private void validateURL(String urlString) {
        try {
            new URL(urlString);
        } catch (MalformedURLException e) {
            throw new CLIRuntimeException("Malformed URL is provided: '" + urlString + "'.");
        }
    }
}
