// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type JWTGeneratorConfigDTO record {|
    boolean jwtGeneratorEnabled = getConfigBooleanValue(JWT_GENERATOR_ID,
                                                        JWT_GENERATOR_ENABLED,
                                                        DEFAULT_JWT_GENERATOR_ENABLED);
    string dialectURI = getConfigValue(JWT_GENERATOR_ID,
                                        JWT_GENERATOR_DIALECT,
                                        DEFAULT_JWT_GENERATOR_DIALECT);
    string signingAlgorithm = getConfigValue(JWT_GENERATOR_ID,
                                            JWT_GENERATOR_SIGN_ALGO,
                                            DEFAULT_JWT_GENERATOR_SIGN_ALGO);
    string certificateAlias = getConfigValue(JWT_GENERATOR_ID,
                                                JWT_GENERATOR_CERTIFICATE_ALIAS,
                                                DEFAULT_JWT_GENERATOR_CERTIFICATE_ALIAS);
    string privateKeyAlias = getConfigValue(JWT_GENERATOR_ID,
                                            JWT_GENERATOR_PRIVATE_KEY_ALIAS,
                                            DEFAULT_JWT_GENERATOR_PRIVATE_KEY_ALIAS);
    int tokenExpiry = getConfigIntValue(JWT_GENERATOR_ID,
                                            JWT_GENERATOR_TOKEN_EXPIRY,
                                            DEFAULT_JWT_GENERATOR_TOKEN_EXPIRY);
    any[] restrictedClaims = getConfigArrayValue(JWT_GENERATOR_ID,
                                                JWT_GENERATOR_RESTRICTED_CLAIMS);
    string issuer = getConfigValue(JWT_GENERATOR_ID,
                                        JWT_GENERATOR_TOKEN_ISSUER,
                                        DEFAULT_JWT_GENERATOR_TOKEN_ISSUER);
    any[] tokenAudience = getConfigArrayValue(JWT_GENERATOR_ID,
                                                JWT_GENERATOR_TOKEN_AUDIENCE);
    string generatorImpl = getConfigValue(JWT_GENERATOR_ID, 
                                            JWT_GENERATOR_IMPLEMENTATION,
                                            DEFAULT_JWT_GENERATOR_IMPLEMENTATION);                                            
    JWTGeneratorConfig_jwtGeneratorCaching jwtGeneratorCaching = {};
    JWTGeneratorConfig_claimRetrieval claimRetrieval = {};
|};

//todo: Check if it is required declare new default constants specifically for jwt generator cache
public type JWTGeneratorConfig_jwtGeneratorCaching record {|
    boolean tokenCacheEnable = getConfigBooleanValue(JWT_GENERATOR_CACHING_ID,
                                                    JWT_GENERATOR_TOKEN_CACHE_ENABLED,
                                                    DEFAULT_JWT_GENERATOR_TOKEN_CACHE_ENABLED);
    int tokenCacheExpiryTime = getConfigIntValue(JWT_GENERATOR_CACHING_ID,
                                                JWT_GENERATOR_TOKEN_CACHE_EXPIRY,
                                                DEFAULT_TOKEN_CACHE_EXPIRY);   
    int tokenCacheCapacity = getConfigIntValue(JWT_GENERATOR_CACHING_ID,
                                                JWT_GENERATOR_TOKEN_CACHE_CAPACITY,
                                                DEFAULT_TOKEN_CACHE_CAPACITY);
    float tokenCacheEvictionFactor = getConfigFloatValue(JWT_GENERATOR_CACHING_ID,
                                                JWT_GENERATOR_TOKEN_CACHE_EVICTION_FACTOR,
                                                DEFAULT_TOKEN_CACHE_EVICTION_FACTOR);                                                                                         
|};

public type JWTGeneratorConfig_claimRetrieval record {|
    //todo: decide if we are going to keep an empty string instead
    string retrieverImpl = getConfigValue(JWT_GENERATOR_CLAIM_RETRIEVAL_INSTANCE_ID,
                                            JWT_GENERATOR_CLAIM_RETRIEVAL_IMPLEMENTATION,
                                            DEFAULT_JWT_GENERATOR_CLAIM_RETRIEVAL_IMPLEMENTATION);
    map<any> configuration = getConfigMapValue(JWT_GENERATOR_CLAIM_RETRIEVAL_CONFIGURATION);                                        
|};
