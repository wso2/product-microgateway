// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


public type APIKeyValidationDto record {
    string apiName?;
    string apiPublisher = UNKNOWN_VALUE;
    string apiTier = "";
    string applicationId = UNKNOWN_VALUE;
    string applicationName = UNKNOWN_VALUE;
    string applicationTier = UNLIMITED_TIER;
    boolean authorized = false;
    string authorizedDomains?;
    string consumerKey = UNKNOWN_VALUE;
    string contentAware?;
    string endUserName = "";
    string endUserToken?;
    string issuedTime?;
    string spikeArrestLimit = "0";
    string spikeArrestUnit = "";
    string stopOnQuotaReach = "false";
    string subscriber = UNKNOWN_VALUE;
    string subscriberTenantDomain = UNKNOWN_VALUE;
    string throttlingDataList?;
    string tier = DEFAULT_SUBSCRIPTION_TIER;
    string keyType = PRODUCTION_KEY_TYPE;
    string userType?;
    string validationStatus = "";
    string validityPeriod?;
};

public type APIRequestMetaDataDto record {
    string context = "";
    string apiVersion = "";
    string accessToken = "";
    string requiredAuthenticationLevel = ANY_AUTHENTICATION_LEVEL;
    string clientDomain = "*";
    string matchingResource = "";
    string httpVerb = "";
    string tenantDomain = "carbon.super";
    string keyManagers = "all";
};

public type AuthenticationContext record {
    boolean authenticated = false;
    string username = "";
    string applicationTier = UNLIMITED_TIER;
    string tier = DEFAULT_SUBSCRIPTION_TIER;
    string apiTier = "";
    boolean isContentAwareTierPresent = false;
    string apiKey = "";
    string keyType = PRODUCTION_KEY_TYPE;
    string callerToken?;
    string applicationUuid = UNKNOWN_VALUE;
    string applicationId = UNKNOWN_VALUE;
    string applicationName = UNKNOWN_VALUE;
    string consumerKey = UNKNOWN_VALUE;
    string subscriber = UNKNOWN_VALUE;
    string[] throttlingDataList?;
    int spikeArrestLimit = 0;
    string subscriberTenantDomain = UNKNOWN_VALUE;
    string spikeArrestUnit = "";
    boolean stopOnQuotaReach = false;
    string apiPublisher = UNKNOWN_VALUE;
};

//todo: update all the properties accordingly.
public type KeyManagerConf record {
    string serverUrl = getConfigValue(KM_CONF_INSTANCE_ID, KM_SERVER_URL, DEFAULT_KM_SERVER_URL);
    Credentials credentials?;
    string tokenContext = getConfigValue(KM_CONF_INSTANCE_ID, KM_TOKEN_CONTEXT, DEFAULT_KM_TOKEN_CONTEXT);
    string issuer = getConfigValue(KM_CONF_INSTANCE_ID, KM_CONF_ISSUER, DEFAULT_KM_CONF_ISSUER);
    int timestampSkew = getConfigIntValue(KM_CONF_INSTANCE_ID, TIMESTAMP_SKEW, DEFAULT_TIMESTAMP_SKEW);
    boolean remoteUserClaimRetrievalEnabled = getConfigBooleanValue(KM_CONF_INSTANCE_ID,
                                                                    REMOTE_USER_CLAIM_RETRIEVAL_ENABLED,
                                                                    DEFAULT_JWT_REMOTE_USER_CLAIM_RETRIEVAL_ENABLED);
};

public type Credentials record {
    string username = "";
    string password = "";
};

