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

import java.util.ArrayList;
import java.util.List;

/**
 * K8s config map descriptor.
 */
public class KubernetesConfigMap {
    private String ballerinaConf;
    private boolean enable = false;
    private List<KubernetesConfigMapConfigItem> configMaps = new ArrayList<>();

    public String getBallerinaConf() {
        return ballerinaConf;
    }

    public void setBallerinaConf(String ballerinaConf) {
        this.ballerinaConf = ballerinaConf;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public List<KubernetesConfigMapConfigItem> getConfigMaps() {
        return configMaps;
    }

    public void setConfigMaps(List<KubernetesConfigMapConfigItem> files) {
        this.configMaps = files;
    }
}
