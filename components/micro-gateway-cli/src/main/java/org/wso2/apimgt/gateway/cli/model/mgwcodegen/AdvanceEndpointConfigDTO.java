/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

/**
 * This DTO holds the data related to endpoint timeout, retries and circuit breaking capabilities.
 */
public class AdvanceEndpointConfigDTO {
    private int timeoutInMillis = 60000;
    private EndpointRetryDTO retryConfig = null;
    private CircuitBreakerConfigDTO circuitBreaker = null;
    private PoolConfigDTO poolConfig = null;

    public int getTimeoutInMillis() {
        return timeoutInMillis;
    }

    public void setTimeoutInMillis(int timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
    }

    public EndpointRetryDTO getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(EndpointRetryDTO retryConfig) {
        this.retryConfig = retryConfig;
    }

    public CircuitBreakerConfigDTO getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfigDTO circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public PoolConfigDTO getPoolConfig() {
        return poolConfig;
    }

    public void setPoolConfig(PoolConfigDTO poolConfig) {
        this.poolConfig = poolConfig;
    }
}
