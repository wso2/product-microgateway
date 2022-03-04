/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util;

import org.apache.http.ssl.SSLContexts;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointException;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

/**
 * EndpointUtils class contains the utility methods used for data endpoint.
 */
public class EndpointUtils {

    /**
     * Create SSLContext with the provided truststore.
     *
     * @param trustStore Truststore instance
     * @return SSLContext
     * @throws DataEndpointException
     */
    public static SSLContext createSSLContext(KeyStore trustStore) throws DataEndpointException {
        SSLContext ctx;
        try {
            ctx = SSLContexts.custom().loadTrustMaterial(trustStore, null).build();
            return ctx;
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new DataEndpointException("Error while creating the SSLContext with instance type : TLS.", e);
        }
    }
}
