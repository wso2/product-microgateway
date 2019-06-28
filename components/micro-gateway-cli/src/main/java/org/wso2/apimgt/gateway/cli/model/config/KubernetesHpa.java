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

/**
 * K8s Horizontal Pod Autoscaler (HPA) descriptor.
 */
public class KubernetesHpa {
    private String name;
    private String labels;
    private String minReplicas;
    private String maxReplicas;
    private String cpuPrecentage;
    private boolean enable = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getMinReplicas() {
        return minReplicas;
    }

    public void setMinReplicas(String minReplicas) {
        this.minReplicas = minReplicas;
    }

    public String getMaxReplicas() {
        return maxReplicas;
    }

    public void setMaxReplicas(String maxReplicas) {
        this.maxReplicas = maxReplicas;
    }

    public String getCpuPrecentage() {
        return cpuPrecentage;
    }

    public void setCpuPrecentage(String cpuPrecentage) {
        this.cpuPrecentage = cpuPrecentage;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
