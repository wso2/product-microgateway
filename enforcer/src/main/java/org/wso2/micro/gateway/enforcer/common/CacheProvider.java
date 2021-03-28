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

package org.wso2.micro.gateway.enforcer.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.config.dto.CacheDto;
import org.wso2.micro.gateway.enforcer.security.jwt.SignedJWTInfo;
import org.wso2.micro.gateway.enforcer.security.jwt.validator.JWTConstants;

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
    private static LoadingCache<String, String> getGatewayInternalKeyCache;
    private static LoadingCache<String, String> getInvalidGatewayInternalKeyCache;
    private static LoadingCache<String, JWTValidationInfo> getGatewayInternalKeyDataCache;

    private static boolean cacheEnabled = true;
    public static void init() {
        CacheDto cacheDto = ConfigHolder.getInstance().getConfig().getCacheDto();
        cacheEnabled = cacheDto.isEnabled();
        int maxSize = cacheDto.getMaximumSize();
        int expiryTime = cacheDto.getExpiryTime();
        gatewaySignedJWTParseCache = initCache(maxSize, expiryTime);
        gatewayTokenCache = initCache(maxSize, expiryTime);
        gatewayKeyCache = initCache(maxSize, expiryTime);
        invalidTokenCache = initCache(maxSize, expiryTime);
        gatewayJWTTokenCache = initCache(maxSize, expiryTime);
        getGatewayInternalKeyCache = initCache(maxSize, expiryTime);
        getGatewayInternalKeyDataCache = initCache(maxSize, expiryTime);
        getInvalidGatewayInternalKeyCache = initCache(maxSize, expiryTime);
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
     * @return Gateway Internal Key cache
     */
    public static LoadingCache getGatewayInternalKeyCache() {
        return getGatewayInternalKeyCache;
    }

    /**
     * @return Gateway Internal Key data cache
     */
    public static LoadingCache getGatewayInternalKeyDataCache() {
        return getGatewayInternalKeyDataCache;
    }

    /**
     * @return Gateway Internal Key invalid data cache
     */
    public static LoadingCache getInvalidGatewayInternalKeyCache() {
        return getInvalidGatewayInternalKeyCache;
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
