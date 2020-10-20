/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.filter.core.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.wso2.micro.gateway.filter.core.auth.jwt.JWTValidationInfo;
import org.wso2.micro.gateway.filter.core.auth.jwt.SignedJWTInfo;
import org.wso2.micro.gateway.filter.core.auth.jwt.validator.JWTConstants;

import java.util.concurrent.TimeUnit;

/**
 * Class for initiating and returning caches.
 */
public class CacheProvider {
    private static LoadingCache<String, SignedJWTInfo> gatewaySignedJWTParseCache;
    private static LoadingCache<String, String> gatewayTokenCache;
    private static LoadingCache<String, JWTValidationInfo> gatewayKeyCache;
    private static LoadingCache<String, Boolean> invalidTokenCache;
    private static LoadingCache<String, JWTValidationInfo> gatewayJWTTokenCache;
    public static void init() {
        gatewaySignedJWTParseCache = initCache(100, 15);
        gatewayTokenCache = initCache(100, 15);
        gatewayKeyCache = initCache(100, 15);
        invalidTokenCache = initCache(100, 15);
        gatewayJWTTokenCache = initCache(100, 15);
    }

    private static LoadingCache initCache(int maxSize, int expiryTime) {
        return CacheBuilder.newBuilder()
                .maximumSize(maxSize)                                     // maximum 100 tokens can be cached
                .expireAfterAccess(expiryTime, TimeUnit.MINUTES)      // cache will expire after 30 minutes of access
                .build(new CacheLoader<String, String>() {            // build the cacheloader
                    @Override public String load(String s) throws Exception {
                        return JWTConstants.UNAVAILABLE;
                    }

                });
    }


    /**
     *
     * @return SignedJWT ParsedCache
     */
    public static LoadingCache getGatewaySignedJWTParseCache() {
        return gatewaySignedJWTParseCache;
    }

    /**
     * @return gateway token cache
     */
    public static LoadingCache getGatewayTokenCache() {
        return gatewayTokenCache;
    }

    /**
     * @return gateway key cache
     */
    public static LoadingCache getGatewayKeyCache() {
        return gatewayKeyCache;
    }

    /**
     * @return gateway invalid token cache
     */
    public static LoadingCache getInvalidTokenCache() {
        return invalidTokenCache;
    }

    /**
     * @return JWT token cache
     */
    public static LoadingCache getGatewayJWTTokenCache() {
        return gatewayJWTTokenCache;
    }


}
