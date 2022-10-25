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

package org.wso2.choreo.connect.enforcer.config;

import org.apache.commons.lang3.StringUtils;
import org.wso2.choreo.connect.enforcer.constants.Constants;

/**
 * Holds and returns the configuration values retrieved from the environment variables.
 */
public class EnvVarConfig {
    private static final String TRUSTED_CA_CERTS_PATH = "TRUSTED_CA_CERTS_PATH";
    private static final String TRUST_DEFAULT_CERTS = "TRUST_DEFAULT_CERTS";
    private static final String ADAPTER_HOST_NAME = "ADAPTER_HOST_NAME";
    private static final String ENFORCER_PRIVATE_KEY_PATH = "ENFORCER_PRIVATE_KEY_PATH";
    private static final String ENFORCER_PUBLIC_CERT_PATH = "ENFORCER_PUBLIC_CERT_PATH";
    private static final String OPA_CLIENT_PRIVATE_KEY_PATH = "OPA_CLIENT_PRIVATE_KEY_PATH";
    private static final String OPA_CLIENT_PUBLIC_CERT_PATH = "OPA_CLIENT_PUBLIC_CERT_PATH";
    private static final String ADAPTER_HOST = "ADAPTER_HOST";
    private static final String ADAPTER_XDS_PORT = "ADAPTER_XDS_PORT";
    private static final String ENFORCER_LABEL = "ENFORCER_LABEL";
    private static final String ENFORCER_REGION_ID = "ENFORCER_REGION";
    public static final String XDS_MAX_MSG_SIZE = "XDS_MAX_MSG_SIZE";
    public static final String XDS_MAX_RETRIES = "XDS_MAX_RETRIES";
    public static final String XDS_RETRY_PERIOD = "XDS_RETRY_PERIOD";
    public static final String HOSTNAME = "HOSTNAME";

    // Since the container is running in linux container, path separator is not needed.
    private static final String DEFAULT_TRUSTED_CA_CERTS_PATH = "/home/wso2/security/truststore";
    private static final String DEFAULT_TRUST_DEFAULT_CERTS = "true";
    private static final String DEFAULT_ADAPTER_HOST_NAME = "adapter";
    private static final String DEFAULT_ENFORCER_PRIVATE_KEY_PATH = "/home/wso2/security/keystore/mg.key";
    private static final String DEFAULT_ENFORCER_PUBLIC_CERT_PATH = "/home/wso2/security/keystore/mg.pem";
    private static final String DEFAULT_ENFORCER_REGION_ID = "UNKNOWN";
    private static final String DEFAULT_ADAPTER_HOST = "adapter";
    private static final String DEFAULT_ADAPTER_XDS_PORT = "18000";
    private static final String DEFAULT_ENFORCER_LABEL = "enforcer";
    public static final String DEFAULT_XDS_MAX_MSG_SIZE = "4194304";
    public static final String DEFAULT_XDS_MAX_RETRIES = Integer.toString(Constants.MAX_XDS_RETRIES);
    public static final String DEFAULT_XDS_RETRY_PERIOD = Integer.toString(Constants.XDS_DEFAULT_RETRY);
    public static final String DEFAULT_HOSTNAME = "Unassigned";

    private static EnvVarConfig instance;
    private final String trustedAdapterCertsPath;
    private final String trustDefaultCerts;
    private final String enforcerPrivateKeyPath;
    private final String enforcerPublicKeyPath;
    private final String opaClientPrivateKeyPath;
    private final String opaClientPublicKeyPath;
    private final String adapterHost;
    private final String enforcerLabel;
    private final String adapterXdsPort;
    private final String adapterHostName;
    // TODO: (VirajSalaka) Enforcer ID should be picked from router once envoy 1.18.0 is released and microgateway
    // is updated.
    private final String enforcerRegionId;
    private final String xdsMaxMsgSize;
    private final String xdsMaxRetries;
    private final String xdsRetryPeriod;
    private final String instanceIdentifier;

    private EnvVarConfig() {
        trustedAdapterCertsPath = retrieveEnvVarOrDefault(TRUSTED_CA_CERTS_PATH,
                DEFAULT_TRUSTED_CA_CERTS_PATH);
        trustDefaultCerts = retrieveEnvVarOrDefault(TRUST_DEFAULT_CERTS,
                DEFAULT_TRUST_DEFAULT_CERTS);
        enforcerPrivateKeyPath = retrieveEnvVarOrDefault(ENFORCER_PRIVATE_KEY_PATH,
                DEFAULT_ENFORCER_PRIVATE_KEY_PATH);
        enforcerPublicKeyPath = retrieveEnvVarOrDefault(ENFORCER_PUBLIC_CERT_PATH,
                DEFAULT_ENFORCER_PUBLIC_CERT_PATH);
        opaClientPrivateKeyPath = retrieveEnvVarOrDefault(OPA_CLIENT_PRIVATE_KEY_PATH,
                DEFAULT_ENFORCER_PRIVATE_KEY_PATH);
        opaClientPublicKeyPath = retrieveEnvVarOrDefault(OPA_CLIENT_PUBLIC_CERT_PATH,
                DEFAULT_ENFORCER_PUBLIC_CERT_PATH);
        enforcerLabel = retrieveEnvVarOrDefault(ENFORCER_LABEL, DEFAULT_ENFORCER_LABEL);
        adapterHost = retrieveEnvVarOrDefault(ADAPTER_HOST, DEFAULT_ADAPTER_HOST);
        adapterHostName = retrieveEnvVarOrDefault(ADAPTER_HOST_NAME, DEFAULT_ADAPTER_HOST_NAME);
        adapterXdsPort = retrieveEnvVarOrDefault(ADAPTER_XDS_PORT, DEFAULT_ADAPTER_XDS_PORT);
        xdsMaxMsgSize = retrieveEnvVarOrDefault(XDS_MAX_MSG_SIZE, DEFAULT_XDS_MAX_MSG_SIZE);
        enforcerRegionId = retrieveEnvVarOrDefault(ENFORCER_REGION_ID, DEFAULT_ENFORCER_REGION_ID);
        xdsMaxRetries = retrieveEnvVarOrDefault(XDS_MAX_RETRIES, DEFAULT_XDS_MAX_RETRIES);
        xdsRetryPeriod = retrieveEnvVarOrDefault(XDS_RETRY_PERIOD, DEFAULT_XDS_RETRY_PERIOD);
        // HOSTNAME environment property is readily available in docker and kubernetes, and it represents the Pod
        // name in Kubernetes context, containerID in docker context.
        instanceIdentifier = retrieveEnvVarOrDefault(HOSTNAME, DEFAULT_HOSTNAME);
    }

    public static EnvVarConfig getInstance() {
        if (instance == null) {
            synchronized (EnvVarConfig.class) {
                if (instance == null) {
                    instance = new EnvVarConfig();
                }
            }
        }
        return instance;
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

    public boolean isTrustDefaultCerts() {
        return Boolean.valueOf(trustDefaultCerts);
    }

    public String getEnforcerPrivateKeyPath() {
        return enforcerPrivateKeyPath;
    }

    public String getEnforcerPublicKeyPath() {
        return enforcerPublicKeyPath;
    }

    public String getOpaClientPrivateKeyPath() {
        return opaClientPrivateKeyPath;
    }

    public String getOpaClientPublicKeyPath() {
        return opaClientPublicKeyPath;
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


    public String getEnforcerRegionId() {
        return enforcerRegionId;
    }

    public String getXdsMaxRetries() {
        return xdsMaxRetries;
    }

    public String getXdsRetryPeriod() {
        return xdsRetryPeriod;
    }

    public String getInstanceIdentifier() {
        return instanceIdentifier;
    }
}
