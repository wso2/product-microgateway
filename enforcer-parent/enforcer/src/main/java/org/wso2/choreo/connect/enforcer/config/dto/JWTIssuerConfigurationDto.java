/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.config.dto;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Holds meta data related to the JWT issuer.
 */
public class JWTIssuerConfigurationDto {
    private boolean enabled = false;
    private String issuer = "http://localhost:8082/token";
    private String signatureAlgorithm = "SHA256withRSA";
    private String consumerDialectUri = "http://wso2.org/claims";
    private Certificate publicCert;
    private PrivateKey privateKey;
    private long ttl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getConsumerDialectUri() {

        return consumerDialectUri;
    }

    public void setConsumerDialectUri(String consumerDialectUri) {

        this.consumerDialectUri = consumerDialectUri;
    }

    public Certificate getPublicCert() {
        return publicCert;
    }

    public void setPublicCert(Certificate publicCert) {
        this.publicCert = publicCert;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
