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

import ballerina/http;
import ballerina/log;
import ballerina/auth;
import ballerina/cache;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;
import ballerina/io;

// Authentication handler

@Description {value:"Representation of OAuth2 Auth handler for HTTP traffic"}
@Field {value:"oAuthAuthenticator: OAuthAuthProvider instance"}
@Field {value:"name: Authentication handler name"}
public type OAuthnAuthenticator object {
    public string name= "oauth2";
    public OAuthAuthProvider oAuthAuthenticator = new;


    public function canHandle (http:Request req) returns (boolean);
    public function handle (http:Request req)
    returns (APIKeyValidationDto| error);

};

@Description {value:"Intercepts a HTTP request for authentication"}
@Param {value:"req: Request object"}
@Return {value:"boolean: true if authentication is a success, else false"}
function OAuthnAuthenticator::canHandle (http:Request req) returns (boolean) {
    string authHeader;
    try {
        authHeader = req.getHeader(AUTH_HEADER);
    } catch (error e) {
        printDebug(KEY_OAUTH_PROVIDER, "Error in retrieving header " + AUTH_HEADER + ": " + e.message);
        return false;
    }
    if (authHeader != null && authHeader.hasPrefix(AUTH_SCHEME_BEARER)) {
        string[] authHeaderComponents = authHeader.split(" ");
        if (lengthof authHeaderComponents == 2) {
            return true;
        }
    }
    return false;
}

@Description {value:"Checks if the provided HTTP request can be authenticated with JWT authentication"}
@Param {value:"req: Request object"}
@Return {value:"boolean: true if its possible to authenticate with JWT auth, else false"}
function OAuthnAuthenticator::handle (http:Request req)
                                   returns (APIKeyValidationDto| error) {
    APIKeyValidationDto apiKeyValidationDto;
    try {
        APIRequestMetaDataDto apiKeyValidationRequestDto = getKeyValidationRequestObject();
        apiKeyValidationDto = self.oAuthAuthenticator.authenticate(apiKeyValidationRequestDto);
    } catch (error err) {
        log:printError("Error occurred while getting key validation information for the access token", err = err);
        return err;
    }
    return apiKeyValidationDto;
}



function  getAccessTokenCacheKey(APIRequestMetaDataDto dto) returns string {
    return dto.accessToken + ":" + dto.context + "/" + dto.apiVersion + dto.matchingResource + ":" + dto.httpVerb;
}



@Description {value:"Represents a OAuth2 Authenticator"}
@Field {value:"gatewayCache: Authentication cache object"}
public type OAuthAuthProvider object {
    public APIGatewayCache gatewayCache;



    public function authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns (APIKeyValidationDto);

    public function doKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (json);

    public function invokeKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (boolean,
                APIKeyValidationDto);

};


@Description {value:"Authenticate with a oauth2 token"}
@Param {value:"apiRequestMetaDataDto: Object containig data to call the key validation service"}
@Return {value:"boolean: true if authentication is a success, else false"}
function OAuthAuthProvider::authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns
              (APIKeyValidationDto) {

    printDebug(KEY_OAUTH_PROVIDER, "Authenticating request using the request metadata.");
    string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
    string accessToken = apiRequestMetaDataDto.accessToken;
    boolean authorized;
    APIKeyValidationDto apiKeyValidationDto;
    if(getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
        printDebug(KEY_OAUTH_PROVIDER, "Checking for the access token in the gateway token cache.");
        match self.gatewayCache.retrieveFromTokenCache(accessToken) {
            boolean isTokenCached => {
                printDebug(KEY_OAUTH_PROVIDER, "Access token found in the token cache.");
                match self.gatewayCache.authenticateFromGatewayKeyValidationCache(cacheKey) {
                    APIKeyValidationDto apiKeyValidationDtoFromcache => {
                        if (isAccessTokenExpired(apiKeyValidationDtoFromcache)) {
                            self.gatewayCache.removeFromGatewayKeyValidationCache(cacheKey);
                            self.gatewayCache.addToInvalidTokenCache(accessToken, apiKeyValidationDtoFromcache);
                            self.gatewayCache.removeFromTokenCache(accessToken);
                            apiKeyValidationDtoFromcache.authorized = "false";
                            printDebug(KEY_OAUTH_PROVIDER, "Token has expired");
                            return apiKeyValidationDtoFromcache;
                        }
                        authorized = <boolean>apiKeyValidationDtoFromcache.authorized;
                        apiKeyValidationDto = apiKeyValidationDtoFromcache;
                        printDebug(KEY_OAUTH_PROVIDER, "Authorized value from the token cache: " + authorized);
                    }
                    () => {
                        printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the invalid token cache."
                        + " Calling the key validation service.");
                        (authorized, apiKeyValidationDto) = self.invokeKeyValidation(apiRequestMetaDataDto);
                    }
                }
            }
            () => {
                printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the gateway token cache.");

                printDebug(KEY_OAUTH_PROVIDER, "Checking for the access token in the invalid token cache.");
                match self.gatewayCache.retrieveFromInvalidTokenCache(accessToken) {
                    APIKeyValidationDto cacheAuthorizedValue => {
                        printDebug(KEY_OAUTH_PROVIDER, "Access token found in the invalid token cache.");
                        return cacheAuthorizedValue;
                    }
                    () => {
                        printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the invalid token cache." 
                                + " Calling the key validation service.");
                        (authorized, apiKeyValidationDto) = self.invokeKeyValidation(apiRequestMetaDataDto);
                    }
                }
            }
        }
    } else {
        printDebug(KEY_OAUTH_PROVIDER, "Gateway cache disabled. Calling the key validation service.");
        (authorized, apiKeyValidationDto) = self.invokeKeyValidation(apiRequestMetaDataDto);
    }
    if (authorized) {
        // set username
        runtime:getInvocationContext().userPrincipal.username = apiKeyValidationDto.endUserName;

    }
    return apiKeyValidationDto;
}

@Description {value:"Do a key validation request to Key Manager using the request metadata"}
@Param {value:"apiRequestMetaDataDto: Object containig data to call the key validation service"}
@Return {value:"boolean: true if authentication is a success, else false. APIKeyValidationDto: key validation response"}
function OAuthAuthProvider::invokeKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (boolean,
            APIKeyValidationDto) {
    APIKeyValidationDto apiKeyValidationDto;
    string accessToken = apiRequestMetaDataDto.accessToken;
    boolean authorized = false;
    json keyValidationInfoJson = self.doKeyValidation(apiRequestMetaDataDto);
    printTrace(KEY_OAUTH_PROVIDER, "key Validation json " + keyValidationInfoJson.toString());
    string authorizeValue = keyValidationInfoJson.active.toString();
            boolean auth = <boolean>authorizeValue;
            printDebug(KEY_OAUTH_PROVIDER, "Authorized value from key validation service: " + auth);
            if (auth) {
                apiKeyValidationDto.authorized = "true";
                apiKeyValidationDto.consumerKey = keyValidationInfoJson.client_id.toString();
                apiKeyValidationDto.endUserName = keyValidationInfoJson.username.toString();
                apiKeyValidationDto.validityPeriod = keyValidationInfoJson.exp.toString();
                apiKeyValidationDto.issuedTime = keyValidationInfoJson.iat.toString();
                authorized = auth;
                if(getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
                    string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
                    self.gatewayCache.addToGatewayKeyValidationCache(cacheKey, apiKeyValidationDto);
                    self.gatewayCache.addToTokenCache(accessToken, true);
                }
            } else {
                apiKeyValidationDto.authorized = "false";
                apiKeyValidationDto.validationStatus = API_AUTH_INVALID_CREDENTIALS_STRING;
                if(getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
                    self.gatewayCache.addToInvalidTokenCache(accessToken, apiKeyValidationDto);
                }
            }


    return (authorized, apiKeyValidationDto);

}

function OAuthAuthProvider::doKeyValidation (APIRequestMetaDataDto apiRequestMetaDataDto)
                                       returns (json) {
    try {
        string base64Header = getGatewayConfInstance().getKeyManagerConf().credentials.username + ":" +
            getGatewayConfInstance().getKeyManagerConf().credentials.password;
        string encodedBasicAuthHeader = check base64Header.base64Encode();

        http:Request keyValidationRequest = new;
        http:Response keyValidationResponse = new;
        string payload  = "token=" + apiRequestMetaDataDto.accessToken;
        keyValidationRequest.setTextPayload(payload);
        keyValidationRequest.setHeader(CONTENT_TYPE_HEADER, X_WWW_FORM_URLENCODED);
        keyValidationRequest.setHeader(ACCEPT, APPLICATION_JSON);
        time:Time time = time:currentTime();
        int startTimeMills = time.time;
        var result1 = keyValidationEndpoint -> post("/api/identity/oauth2/introspect/v1.0/introspect", keyValidationRequest);
        time = time:currentTime();
        int endTimeMills = time.time;
        printDebug(KEY_OAUTH_PROVIDER, "Total time taken for the key introspect call : " + (endTimeMills -
                    startTimeMills) + "ms");
        match result1 {
            error err => {
                log:printError("Error occurred while reading the key validation response",err =err);
                return {};
            }
            http:Response prod => {
                keyValidationResponse = prod;
            }
        }
        json responsepayload;
        match keyValidationResponse.getJsonPayload() {
            error err => {
                log:printError("Error occurred while getting the key validation service JSON response payload ",
                    err=err);
                return {};
            }
            json responseJson => {
                responsepayload = responseJson;
                printTrace(KEY_OAUTH_PROVIDER, "Key validation response:" + responsepayload.toString());
            }
        }
        return responsepayload;

    } catch (error err) {
        log:printError("Error occurred while validating the token",err =err);
        return {};
    }
}
