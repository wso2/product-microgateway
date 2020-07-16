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

import ballerina/runtime;
import ballerina/jwt;
import ballerina/http;

# Refactoring method for setting JWT header
#
# + payload - The payload of the authentication token
# + req - The `Request` instance.
# + cacheKey - key for the jwt generator cache
# + enabledCaching - jwt generator caching enabled
# + apiDetails - extracted api details for the current api
# + remoteUserClaimRetrievalEnabled - true if remoteUserClaimRetrieval is enabled
# + return - Returns `true` if the token generation and setting the header completed successfully
# or the `AuthenticationError` in case of an error.
public function setJWTHeader(jwt:JwtPayload payload,
                                http:Request req,
                                string cacheKey,
                                boolean enabledCaching,
                                map<string> apiDetails,
                                boolean remoteUserClaimRetrievalEnabled)
                                returns @tainted boolean {
    AuthenticationContext authContext = 
        <AuthenticationContext> runtime:getInvocationContext().attributes[AUTHENTICATION_CONTEXT];
    (handle|error) generatedToken = generateBackendTokenForJWT(authContext, payload, apiDetails, remoteUserClaimRetrievalEnabled);
    return setGeneratedTokenAsHeader(req, cacheKey, enabledCaching, generatedToken);
}

# Setting backend JWT header when there is no JWT Token is present.
#
# + req - The `Request` instance.
# + authContext - Authentication Context
# + cacheKey - key for the jwt generator cache
# + enabledCaching - jwt generator caching enabled
# + apiDetails - extracted api details for the current api
# + return - Returns `true` if the token generation and setting the header completed successfully
# or the `AuthenticationError` in case of an error.
public function setJWTHeaderForOauth2(http:Request req,
                                AuthenticationContext authContext,
                                string cacheKey,
                                boolean enabledCaching,
                                map<string> apiDetails)
                                returns @tainted boolean {

    (handle|error) generatedToken = generateBackendJWTTokenForOauth(authContext, apiDetails);
    if (generatedToken is error) {
        return false;
    } else {
        return setGeneratedTokenAsHeader(req, cacheKey, enabledCaching, generatedToken);
    }
}

# Setting backend JWT header when there is no JWT Token is present.
#
# + authContext - Authentication Context
# + payload - The payload of the authentication token
# + apiDetails - extracted api details for the current api
# + return - JWT Token
# or the `AuthenticationError` in case of an error.
function generateBackendTokenForJWT(AuthenticationContext authContext, jwt:JwtPayload payload, map<string> apiDetails, 
                boolean remoteUserClaimRetrievalEnabled) returns handle | error {
    (handle|error) generatedToken;
    if (isSelfContainedToken(payload)) {
        generatedToken = generateJWTToken(payload, apiDetails);
    } else {
        ClaimsMapDTO claimsMapDTO = createMapFromRetrievedUserClaimsListDTO(authContext,
                                                                            remoteUserClaimRetrievalEnabled,
                                                                            payload);
        generatedToken = generateJWTTokenFromUserClaimsMap(claimsMapDTO, apiDetails);
    }
    return generatedToken;
}

# Setting backend JWT header when there is no JWT Token is present.
#
# + apiDetails - extracted api details for the current api
# + return - JWT Token
# or the `AuthenticationError` in case of an error.
function generateBackendJWTTokenForOauth(AuthenticationContext authContext, map<string> apiDetails) returns handle | error {
    (handle|error) generatedToken;
    boolean remoteUserClaimRetrievalEnabled = getConfigBooleanValue(KM_CONF_INSTANCE_ID, 
                                                                    REMOTE_USER_CLAIM_RETRIEVAL_ENABLED, 
                                                                    DEFAULT_JWT_REMOTE_USER_CLAIM_RETRIEVAL_ENABLED);
    ClaimsMapDTO claimsMapDTO = createMapFromRetrievedUserClaimsListDTO(authContext, remoteUserClaimRetrievalEnabled);
    generatedToken = generateJWTTokenFromUserClaimsMap(claimsMapDTO, apiDetails);
    return generatedToken;
}

# Setting backend JWT header when there is no JWT Token is present.
#
# + req - The `Request` instance.
# + cacheKey - key for the jwt generator cache
# + enabledCaching - jwt generator caching enabled
# + generatedToken - generated Backend JWT
# + return - Returns `true` if the token generation and setting the header completed successfully
function setGeneratedTokenAsHeader(http:Request req,
                                string cacheKey,
                                boolean enabledCaching,
                                handle | error generatedToken)
                                returns @tainted boolean {

    if (generatedToken is error) {
        printError(KEY_JWT_AUTH_PROVIDER, "Token not generated due to error", generatedToken);
        return false;
    }
    printDebug(KEY_JWT_AUTH_PROVIDER, "Generated jwt token");
    printDebug(KEY_JWT_AUTH_PROVIDER, "Token: " + generatedToken.toString());

    if (enabledCaching) {
        error? err = jwtGeneratorCache.put(<@untainted>cacheKey, <@untainted>generatedToken.toString());
        if (err is error) {
            printError(KEY_JWT_AUTH_PROVIDER, "Error while adding entry to jwt generator cache", err);
        }
        printDebug(KEY_JWT_AUTH_PROVIDER, "Added to jwt generator token cache.");
    }
    req.setHeader(jwtheaderName, generatedToken.toString());
    return true;
}

# populate and return ClaimsMapDTO object which is required to the further processing of Jwt generator implementation.
#
# + authContext - Authentication Context
# + remoteUserClaimRetrievalEnabled - true if remoteUserClaimRetrieval is enabled
# + payload - For the jwt, payload of the decoded jwt
# + return - ClaimsMapDTO
function createMapFromRetrievedUserClaimsListDTO(AuthenticationContext authContext, 
                                                    boolean remoteUserClaimRetrievalEnabled, 
                                                    jwt:JwtPayload? payload = ())
                                                    returns @tainted ClaimsMapDTO {
    ClaimsMapDTO claimsMapDTO = {};
    CustomClaimsMapDTO customClaimsMapDTO = {};
    UserAuthContextDTO? userInfo = ();
    if (payload is ()) {
        //if payload is empty, this is from oauth2 flow
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        runtime:Principal? principal = invocationContext?.principal;
        if (principal is runtime:Principal) {
            string[]? scopes = principal?.scopes;
            if (scopes is string[]) {
                string concatenatedScope = "";
                foreach string scope in scopes {
                    concatenatedScope += scope + " ";
                }
                customClaimsMapDTO["scope"] = concatenatedScope.trim();
            }
            userInfo = generateAuthContextInfoFromPrincipal(authContext, principal);
        } else {
            printDebug(JWT_GEN_UTIL, "Claim retrieval implementation is not executed due to the unavailability " +
                            "of the principal component");
        }
        claimsMapDTO.iss = getConfigValue(KM_CONF_INSTANCE_ID, KM_CONF_ISSUER, DEFAULT_KM_CONF_ISSUER);
    } else  {
        string? iss = payload?.iss;
        if (iss is string) {
            claimsMapDTO.iss = iss;
        }
        map<json>? customClaims = payload?.customClaims;
        if (customClaims is map<json>) {
            foreach var [key, value] in customClaims.entries() {
                string | error claimValue = trap <string> value;
                if (claimValue is string) {
                    customClaimsMapDTO[key] = claimValue;
                }
            }
        }
        userInfo = generateAuthContextInfoFromJWT(authContext, payload);
    }

    if (remoteUserClaimRetrievalEnabled) {
        RetrievedUserClaimsListDTO ? claimsListDTO = retrieveClaims(userInfo);
        if (claimsListDTO is RetrievedUserClaimsListDTO) {
            ClaimDTO[] claimList = claimsListDTO.list;
            foreach ClaimDTO claim in claimList {
                customClaimsMapDTO[claim.uri] = claim.value;
            }
        }
    }

    ApplicationClaimsMapDTO applicationClaimsMapDTO = {};
    applicationClaimsMapDTO.id = emptyStringIfUnknownValue(authContext.applicationId);
    applicationClaimsMapDTO.owner = emptyStringIfUnknownValue(authContext.subscriber);
    applicationClaimsMapDTO.name = emptyStringIfUnknownValue(authContext.applicationName);
    applicationClaimsMapDTO.tier = emptyStringIfUnknownValue(authContext.applicationTier);

    customClaimsMapDTO.application = applicationClaimsMapDTO;
    claimsMapDTO.sub = emptyStringIfUnknownValue(authContext.username);
    claimsMapDTO.customClaims = customClaimsMapDTO;
    return claimsMapDTO;
}

# Populate Map to keep API related information for JWT generation process
#
# + invocationContext - ballerina runtime invocationContext
# + return - String map with the properties: apiName, apiVersion, apiTier, apiContext, apiPublisher and
#               subscriberTenantDomain
function createAPIDetailsMap (runtime:InvocationContext invocationContext) returns map<string> {
    map<string> apiDetails = {};
    AuthenticationContext authenticationContext = <AuthenticationContext>invocationContext.attributes[AUTHENTICATION_CONTEXT];
    APIConfiguration? apiConfig = apiConfigAnnotationMap[<string>invocationContext.attributes[http:SERVICE_NAME]];
    if (apiConfig is APIConfiguration) {
        apiDetails["apiName"] = apiConfig.name;
        apiDetails["apiVersion"] = apiConfig.apiVersion;
        apiDetails["apiTier"] = apiConfig.apiTier;
        apiDetails["apiContext"] = <string> invocationContext.attributes[API_CONTEXT];
        apiDetails["apiPublisher"] = apiConfig.publisher;
        apiDetails["subscriberTenantDomain"] = authenticationContext.subscriberTenantDomain;
    }
    return apiDetails;
}

function emptyStringIfUnknownValue (string value) returns string {
    return value != UNKNOWN_VALUE ? value : "";
}
