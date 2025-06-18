/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.mcp.request;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.config.dto.McpConfigDTO;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

/**
 * This class is used to create a singleton instance of CloseableHttpClient
 * for connecting with the MCP transformation service.
 */
public class McpHttpClient {
    private static CloseableHttpClient instance;

    private McpHttpClient() {}

    public static CloseableHttpClient getInstance() throws Exception {
        if (instance == null) {
            try {
                instance = createHttpClient();
            } catch (Exception e) {
                throw new Exception("Error creating HTTP client for MCP", e);
            }
        }
        return instance;
    }

    private static CloseableHttpClient createHttpClient() throws Exception {
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        McpConfigDTO mcpConfig = enforcerConfig.getMcpConfig();
        SSLConnectionSocketFactory socketFactory = FilterUtils.createSocketFactory();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register(APIConstants.HTTPS_PROTOCOL, socketFactory)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // Set the connection pool size
        connectionManager.setMaxTotal(mcpConfig.getPoolSize());
        connectionManager.setDefaultMaxPerRoute(mcpConfig.getPoolSize());
        RequestConfig params = RequestConfig.custom()
                // Set the initial connection timeout
                .setConnectTimeout(mcpConfig.getConnectionTimeout())
                // Set the timeout for requesting a connection from the pool
                .setConnectionRequestTimeout(mcpConfig.getConnectionRequestTimeout())
                // Set the response timeout after the connection is made
                .setSocketTimeout(mcpConfig.getSocketTimeout())
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(params)
                .build();
    }
}
