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

import org.wso2.apimgt.gateway.cli.codegen.CodeGenerationContext;
import org.wso2.apimgt.gateway.cli.constants.CliConstants;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.util.UUID;

/**
 * K8s deployment descriptor.
 */
public class KubernetesDeployment {

    private String name;
    private String labels;
    private String replicas;
    private String enableLiveness;
    private String initialDelaySeconds;
    private String periodSeconds;
    private String livenessPort;
    private String imagePullPolicy;
    private String image;
    private String env;
    private String buildImage;
    private CopyFileConfig copyFiles;
    private String dockerHost;
    private String dockerCertPath;
    private String push;
    private String username;
    private String password;
    private String baseImage;
    private String singleYAML;
    private boolean enable = false;

    public String getName() {
        if (name == null) {
            CodeGenerationContext codeGenerationContext = GatewayCmdUtils.getCodeGenerationContext();
            return codeGenerationContext.getProjectName() + CliConstants.K8S_DEPLOYMENT + UUID.randomUUID()
                    .toString();
        } else {
            return name;
        }
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

    public String getReplicas() {
        return replicas;
    }

    public void setReplicas(String replicas) {
        this.replicas = replicas;
    }

    public String getEnableLiveness() {
        return enableLiveness;
    }

    public void setEnableLiveness(String enableLiveness) {
        this.enableLiveness = enableLiveness;
    }

    public String getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(String initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }

    public String getPeriodSeconds() {
        return periodSeconds;
    }

    public void setPeriodSeconds(String periodSeconds) {
        this.periodSeconds = periodSeconds;
    }

    public String getLivenessPort() {
        return livenessPort;
    }

    public void setLivenessPort(String livenessPort) {
        this.livenessPort = livenessPort;
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public void setImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBuildImage() {
        return buildImage;
    }

    public void setBuildImage(String buildImage) {
        this.buildImage = buildImage;
    }

    public CopyFileConfig getCopyFiles() {
        return copyFiles;
    }

    public void setCopyFiles(CopyFileConfig copyFiles) {
        this.copyFiles = copyFiles;
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getDockerCertPath() {
        return dockerCertPath;
    }

    public void setDockerCertPath(String dockerCertPath) {
        this.dockerCertPath = dockerCertPath;
    }

    public String getPush() {
        return push;
    }

    public void setPush(String push) {
        this.push = push;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }

    public String getSingleYAML() {
        return singleYAML;
    }

    public void setSingleYAML(String singleYAML) {
        this.singleYAML = singleYAML;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
