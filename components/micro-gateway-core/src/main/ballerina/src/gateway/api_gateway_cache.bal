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

// TODO: Refactor the cache
int cacheExpiryTime = getConfigIntValue(CACHING_ID, TOKEN_CACHE_EXPIRY, DEFAULT_TOKEN_CACHE_EXPIRY);
int cacheSize = getConfigIntValue(CACHING_ID, TOKEN_CACHE_CAPACITY, DEFAULT_TOKEN_CACHE_CAPACITY);
float evictionFactor = getConfigFloatValue(CACHING_ID, TOKEN_CACHE_EVICTION_FACTOR, DEFAULT_TOKEN_CACHE_EVICTION_FACTOR);
// Caches are globally defined in order to initialize them before the authentication handlers are initialized.
// These cache objects are passed in authentication handlers while handler init phase.
cache:Cache gatewayTokenCache = new (cacheExpiryTime, cacheSize, evictionFactor);
cache:Cache gatewayKeyValidationCache = new (cacheExpiryTime, cacheSize, evictionFactor);
cache:Cache invalidTokenCache = new (cacheExpiryTime, cacheSize, evictionFactor);
cache:Cache jwtCache = new (cacheExpiryTime, cacheSize, evictionFactor);
cache:Cache introspectCache = new (cacheExpiryTime, cacheSize, evictionFactor);
cache:Cache gatewayClaimsCache = new (cacheExpiryTime, cacheSize, evictionFactor);


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
        gatewayKeyValidationCache.put(tokenCacheKey, <@untainted>apiKeyValidationDto);
        printDebug(KEY_GW_CACHE, "Added key validation information to the key validation cache. key: " + mask(tokenCacheKey));
    }

    public function removeFromGatewayKeyValidationCache(string tokenCacheKey) {
        gatewayKeyValidationCache.remove(tokenCacheKey);
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
        invalidTokenCache.put(tokenCacheKey, <@untainted>apiKeyValidationDto);
        printDebug(KEY_GW_CACHE, "Added key validation information to the invalid token cache. key: " + mask(tokenCacheKey));
    }

    public function removeFromInvalidTokenCache(string tokenCacheKey) {
        invalidTokenCache.remove(tokenCacheKey);
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
        gatewayTokenCache.put(accessToken, isValid);
        printDebug(KEY_GW_CACHE, "Added validity information to the token cache. key: " + mask(accessToken));
    }

    public function removeFromTokenCache(string accessToken) {
        gatewayTokenCache.remove(accessToken);
        printDebug(KEY_GW_CACHE, "Removed from the token cache. key: " + mask(accessToken));
    }

    public function addClaimMappingCache(string jwtTokens, boolean isModifiedclaims) {
        gatewayClaimsCache.put(jwtTokens,isModifiedclaims);
        printDebug(KEY_GW_CACHE, "Added modified claims information to the token cache. key: " + mask(jwtTokens));
    }

    public function retrieveClaimMappingCache(string jwtTokens) returns (boolean | ()) {
        var isclaims = gatewayClaimsCache.get(jwtTokens);
        if (isclaims is boolean) {
            return isclaims;
        } else {
            return ();
        }
    }
};

