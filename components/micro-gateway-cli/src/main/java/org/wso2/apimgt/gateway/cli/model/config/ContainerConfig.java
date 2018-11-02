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

public class ContainerConfig {
    private Docker docker;
    private Kubernetes kubernetes;

    public Docker getDocker() {
        return docker;
    }

    public void setDocker(Docker docker) {
        this.docker = docker;
    }

    public Kubernetes getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    public boolean getHasDocker() {
        return docker != null && (
                (docker.getDockerConfig() != null && docker.getDockerConfig().isEnable()) ||
                        (docker.getDockerCopyFiles() != null && docker.getDockerCopyFiles().isEnable())
        );
    }

    public boolean getHasKubernetes() {
        return kubernetes != null && (
                (kubernetes.getKubernetesConfigMap() != null && kubernetes.getKubernetesConfigMap().isEnable()) ||
                        (kubernetes.getKubernetesDeployment() != null && kubernetes.getKubernetesDeployment()
                                .isEnable()) ||
                        (kubernetes.getKubernetesHpa() != null && kubernetes.getKubernetesHpa().isEnable()) ||
                        (kubernetes.getKubernetesIngress() != null && kubernetes.getKubernetesIngress().isEnable()) ||
                        (kubernetes.getSecureKubernetesIngress() != null &&
                                kubernetes.getSecureKubernetesIngress().isEnable()) ||
                        (kubernetes.getKubernetesJob() != null && kubernetes.getKubernetesJob().isEnable()) ||
                        (kubernetes.getKubernetesPersistentVolumeClaim() != null && kubernetes
                                .getKubernetesPersistentVolumeClaim().isEnable()) ||
                        (kubernetes.getKubernetesSecret() != null && kubernetes.getKubernetesSecret().isEnable()) ||
                        (kubernetes.getKubernetesService() != null && kubernetes.getKubernetesService().isEnable())
        );
    }
}
