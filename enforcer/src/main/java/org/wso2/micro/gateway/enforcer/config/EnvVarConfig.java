/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.enforcer.config;

import org.apache.commons.lang3.StringUtils;

/**
 * Holds and returns the configuration values retrieved from the environment variables.
 */
public class EnvVarConfig {
    private static final String TRUSTED_CA_CERTS_PATH = "TRUSTED_CA_CERTS_PATH";
    private static final String ADAPTER_HOST_NAME = "ADAPTER_HOST_NAME";
    private static final String ENFORCER_PRIVATE_KEY_PATH = "ENFORCER_PRIVATE_KEY_PATH";
    private static final String ENFORCER_PUBLIC_CERT_PATH = "ENFORCER_PUBLIC_CERT_PATH";
    private static final String ADAPTER_HOST = "ADAPTER_HOST";
    private static final String ADAPTER_XDS_PORT = "ADAPTER_XDS_PORT";
    private static final String ENFORCER_LABEL = "ENFORCER_LABEL";
    public static final String XDS_MAX_MSG_SIZE = "XDS_MAX_MSG_SIZE";

    // Since the container is running in linux container, path separator is not needed.
    private static final String DEFAULT_TRUSTED_CA_CERTS_PATH = "/home/wso2/security";
    private static final String DEFAULT_ADAPTER_HOST_NAME = "adapter";
    private static final String DEFAULT_ENFORCER_PRIVATE_KEY_PATH = "/home/wso2/security/keystore/mg.key";
    private static final String DEFAULT_ENFORCER_PUBLIC_CERT_PATH = "/home/wso2/security/truststore/mg.pem";
    private static final String DEFAULT_ADAPTER_HOST = "adapter";
    private static final String DEFAULT_ADAPTER_XDS_PORT = "18000";
    private static final String DEFAULT_ENFORCER_LABEL = "enforcer";
    public static final String DEFAULT_XDS_MAX_MSG_SIZE = "4194304";

    private String trustedAdapterCertsPath;
    private String enforcerPrivateKeyPath;
    private String enforcerPublicKeyPath;
    private String adapterHost;
    private String enforcerLabel;
    private String adapterXdsPort;
    private String adapterHostName;
    private final String xdsMaxMsgSize;

    public EnvVarConfig() {
        trustedAdapterCertsPath = retrieveEnvVarOrDefault(TRUSTED_CA_CERTS_PATH,
                DEFAULT_TRUSTED_CA_CERTS_PATH);
        enforcerPrivateKeyPath = retrieveEnvVarOrDefault(ENFORCER_PRIVATE_KEY_PATH,
                DEFAULT_ENFORCER_PRIVATE_KEY_PATH);
        enforcerPublicKeyPath = retrieveEnvVarOrDefault(ENFORCER_PUBLIC_CERT_PATH,
                DEFAULT_ENFORCER_PUBLIC_CERT_PATH);
        enforcerLabel = retrieveEnvVarOrDefault(ENFORCER_LABEL, DEFAULT_ENFORCER_LABEL);
        adapterHost = retrieveEnvVarOrDefault(ADAPTER_HOST, DEFAULT_ADAPTER_HOST);
        adapterHostName = retrieveEnvVarOrDefault(ADAPTER_HOST_NAME, DEFAULT_ADAPTER_HOST_NAME);
        adapterXdsPort = retrieveEnvVarOrDefault(ADAPTER_XDS_PORT, DEFAULT_ADAPTER_XDS_PORT);
        xdsMaxMsgSize = retrieveEnvVarOrDefault(XDS_MAX_MSG_SIZE, DEFAULT_XDS_MAX_MSG_SIZE);
    }

    private String retrieveEnvVarOrDefault(String variable, String defaultValue) {
        if (StringUtils.isEmpty(System.getenv(variable))) {
            return defaultValue;
        }
        return System.getenv(variable);
    }

    public String getTrustedAdapterCertsPath() {
        return trustedAdapterCertsPath;
    }

    public String getEnforcerPrivateKeyPath() {
        return enforcerPrivateKeyPath;
    }

    public String getEnforcerPublicKeyPath() {
        return enforcerPublicKeyPath;
    }

    public String getAdapterHost() {
        return adapterHost;
    }

    public String getEnforcerLabel() {
        return enforcerLabel;
    }

    public String getAdapterXdsPort() {
        return adapterXdsPort;
    }

    public String getAdapterHostName() {
        return adapterHostName;
    }

    public String getXdsMaxMsgSize() {
        return xdsMaxMsgSize;
    }
}
