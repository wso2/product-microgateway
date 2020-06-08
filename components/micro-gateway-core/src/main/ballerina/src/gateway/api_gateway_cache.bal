// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/cache;
import ballerina/runtime;

// TODO: Refactor the cache
int cacheExpiryTime = getConfigIntValue(CACHING_ID, TOKEN_CACHE_EXPIRY, DEFAULT_TOKEN_CACHE_EXPIRY);
int cacheSize = getConfigIntValue(CACHING_ID, TOKEN_CACHE_CAPACITY, DEFAULT_TOKEN_CACHE_CAPACITY);
float evictionFactor = getConfigFloatValue(CACHING_ID,
                                            TOKEN_CACHE_EVICTION_FACTOR,
                                            DEFAULT_TOKEN_CACHE_EVICTION_FACTOR);

cache:CacheConfig genericCacheConfig = {
    capacity: cacheSize,
    evictionFactor: evictionFactor,
    defaultMaxAgeInSeconds: cacheExpiryTime
};
int jwtGeneratorCacheExpiryTime = getConfigIntValue(JWT_GENERATOR_CACHING_ID,
                                                    JWT_GENERATOR_TOKEN_CACHE_EXPIRY,
                                                    DEFAULT_TOKEN_CACHE_EXPIRY);
int jwtGeneratorCacheSize = getConfigIntValue(JWT_GENERATOR_CACHING_ID,
                                                JWT_GENERATOR_TOKEN_CACHE_CAPACITY,
                                                DEFAULT_TOKEN_CACHE_CAPACITY);
float jwtGeneratorEvictionFactor = getConfigFloatValue(JWT_GENERATOR_CACHING_ID,
                                                        JWT_GENERATOR_TOKEN_CACHE_EVICTION_FACTOR,
                                                        DEFAULT_TOKEN_CACHE_EVICTION_FACTOR);

cache:CacheConfig jwtGenerationCacheConfig = {
    capacity: jwtGeneratorCacheSize,
    evictionFactor: jwtGeneratorEvictionFactor,
    defaultMaxAgeInSeconds: jwtGeneratorCacheExpiryTime
};

// Caches are globally defined in order to initialize them before the authentication handlers are initialized.
// These cache objects are passed in authentication handlers while handler init phase.
cache:Cache gatewayTokenCache = new (genericCacheConfig);
cache:Cache gatewayKeyValidationCache = new (genericCacheConfig);
cache:Cache invalidTokenCache = new (genericCacheConfig);
cache:Cache jwtCache = new (genericCacheConfig);
cache:Cache introspectCache = new (genericCacheConfig);
cache:Cache gatewayClaimsCache = new (genericCacheConfig);

cache:Cache jwtGeneratorCache = new (jwtGenerationCacheConfig);

public type APIGatewayCache object {

    public function authenticateFromGatewayKeyValidationCache(string tokenCacheKey) returns
    (APIKeyValidationDto | ()) {
        var apikeyValidationDto = gatewayKeyValidationCache.get(tokenCacheKey);
        if (apikeyValidationDto is APIKeyValidationDto) {
            return apikeyValidationDto;
        } else {
            return ();
        }

    }

    public function addToGatewayKeyValidationCache(string tokenCacheKey, APIKeyValidationDto
    apiKeyValidationDto) {
        error? err = gatewayKeyValidationCache.put(tokenCacheKey, <@untainted>apiKeyValidationDto);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while adding token cache key to the gateway key validation cache", err);
        }
        printDebug(KEY_GW_CACHE, "Added key validation information to the key validation cache. key: " + mask(tokenCacheKey));
    }

    public function removeFromGatewayKeyValidationCache(string tokenCacheKey) {
        error? err = gatewayKeyValidationCache.invalidate(tokenCacheKey);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while removing token cache key from gateway key validation cache", err);
        }
        printDebug(KEY_GW_CACHE, "Removed key validation information from the key validation cache. key: " + mask(tokenCacheKey));
    }

    public function retrieveFromInvalidTokenCache(string tokenCacheKey) returns (APIKeyValidationDto | ()) {
        var authorize = invalidTokenCache.get(tokenCacheKey);
        if (authorize is APIKeyValidationDto) {
            return authorize;
        } else {
            return ();
        }
    }

    public function addToInvalidTokenCache(string tokenCacheKey, APIKeyValidationDto apiKeyValidationDto) {
        error? err = invalidTokenCache.put(tokenCacheKey, <@untainted>apiKeyValidationDto);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while adding token cache key to the invalid token cache", err);
        }
        printDebug(KEY_GW_CACHE, "Added key validation information to the invalid token cache. key: " + mask(tokenCacheKey));
    }

    public function removeFromInvalidTokenCache(string tokenCacheKey) {
        error? err = invalidTokenCache.invalidate(tokenCacheKey);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while removing token cache key from invalid token cache", err);
        }
        printDebug(KEY_GW_CACHE, "Removed from the invalid key validation cache. key: " + mask(tokenCacheKey));
    }

    public function retrieveFromTokenCache(string accessToken) returns (boolean | ()) {
        var authorize = gatewayTokenCache.get(accessToken);
        if (authorize is boolean) {
            return authorize;
        } else {
            return ();
        }
    }

    public function addToTokenCache(string accessToken, boolean isValid) {
        error? err = gatewayTokenCache.put(accessToken, isValid);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while adding access token to the gateway token cache", err);
        }
        printDebug(KEY_GW_CACHE, "Added validity information to the token cache. key: " + mask(accessToken));
    }

    public function removeFromTokenCache(string accessToken) {
        error? err = gatewayTokenCache.invalidate(accessToken);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while removing access token from gateway token cache", err);
        }
        printDebug(KEY_GW_CACHE, "Removed from the token cache. key: " + mask(accessToken));
    }

    public function addClaimMappingCache(string jwtTokens, runtime:Principal modifiedPrincipal) {
        error? err = gatewayClaimsCache.put(jwtTokens, <@untainted>modifiedPrincipal);
        if (err is error) {
            printError(KEY_GW_CACHE, "Error while adding modified token to the claim mapping cache", err);
        }
        printDebug(KEY_GW_CACHE, "Added modified claims information to the token cache. ");
    }

    public function retrieveClaimMappingCache(string jwtTokens) returns (runtime:Principal | ()) {
        var modifiedPrincipal = gatewayClaimsCache.get(jwtTokens);
        if (modifiedPrincipal is runtime:Principal ) {
            return modifiedPrincipal;
        } else {
            return ();
        }
    }
};

