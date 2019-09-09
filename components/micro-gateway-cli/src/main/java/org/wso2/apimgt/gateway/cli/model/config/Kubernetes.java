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
 * K8s deployment descriptor.
 */
public class Kubernetes {
    private KubernetesConfigMap kubernetesConfigMap;
    private KubernetesDeployment kubernetesDeployment;
    private KubernetesHpa kubernetesHpa;
    private KubernetesIngress kubernetesIngress;
    private SecureKubernetesIngress secureKubernetesIngress;
    private KubernetesJob kubernetesJob;
    private KubernetesPersistentVolumeClaim kubernetesPersistentVolumeClaim;
    private KubernetesSecret kubernetesSecret;
    private KubernetesService kubernetesService;
    private KubernetesService kubernetesServiceHttps;
    private KubernetesService kubernetesServiceHttp;
    private KubernetesService kubernetesServiceToken;

    public KubernetesConfigMap getKubernetesConfigMap() {
        return kubernetesConfigMap;
    }

    public void setKubernetesConfigMap(KubernetesConfigMap kubernetesConfigMap) {
        this.kubernetesConfigMap = kubernetesConfigMap;
    }

    public KubernetesDeployment getKubernetesDeployment() {
        return kubernetesDeployment;
    }

    public void setKubernetesDeployment(KubernetesDeployment kubernetesDeployment) {
        this.kubernetesDeployment = kubernetesDeployment;
    }

    public KubernetesHpa getKubernetesHpa() {
        return kubernetesHpa;
    }

    public void setKubernetesHpa(KubernetesHpa kubernetesHpa) {
        this.kubernetesHpa = kubernetesHpa;
    }

    public KubernetesIngress getKubernetesIngress() {
        return kubernetesIngress;
    }

    public void setKubernetesIngress(KubernetesIngress kubernetesIngress) {
        this.kubernetesIngress = kubernetesIngress;
    }

    public KubernetesJob getKubernetesJob() {
        return kubernetesJob;
    }

    public void setKubernetesJob(KubernetesJob kubernetesJob) {
        this.kubernetesJob = kubernetesJob;
    }

    public KubernetesPersistentVolumeClaim getKubernetesPersistentVolumeClaim() {
        return kubernetesPersistentVolumeClaim;
    }

    public void setKubernetesPersistentVolumeClaim(KubernetesPersistentVolumeClaim kubernetesPersistentVolumeClaim) {
        this.kubernetesPersistentVolumeClaim = kubernetesPersistentVolumeClaim;
    }

    public KubernetesSecret getKubernetesSecret() {
        return kubernetesSecret;
    }

    public void setKubernetesSecret(KubernetesSecret kubernetesSecret) {
        this.kubernetesSecret = kubernetesSecret;
    }

    public KubernetesService getKubernetesService() {
        return kubernetesService;
    }

    public void setKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    public SecureKubernetesIngress getSecureKubernetesIngress() {
        return secureKubernetesIngress;
    }

    public void setSecureKubernetesIngress(SecureKubernetesIngress secureKubernetesIngress) {
        this.secureKubernetesIngress = secureKubernetesIngress;
    }

    public KubernetesService getKubernetesServiceHttps() {
        return kubernetesServiceHttps;
    }

    public void setKubernetesServiceHttps(KubernetesService kubernetesServiceHttps) {
        this.kubernetesServiceHttps = kubernetesServiceHttps;
    }

    public KubernetesService getKubernetesServiceToken() {
        return kubernetesServiceToken;
    }

    public void setKubernetesServiceToken(KubernetesService kubernetesServiceToken) {
        this.kubernetesServiceToken = kubernetesServiceToken;
    }

    public KubernetesService getKubernetesServiceHttp() {
        return kubernetesServiceHttp;
    }

    public void setKubernetesServiceHttp(KubernetesService kubernetesServiceHttp) {
        this.kubernetesServiceHttp = kubernetesServiceHttp;
    }
}
