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
import ballerina/encoding;

// Authentication handler

public type OAuthnAuthenticator object {
    public string name= "oauth2";
    public OAuthAuthProvider oAuthAuthenticator = new;


    public function canHandle (http:Request req) returns (boolean);
    public function handle (http:Request req,http:FilterContext context) returns (APIKeyValidationDto| error);

};

public function OAuthnAuthenticator.canHandle (http:Request req) returns (boolean) {
    string | error authHeaderResult = trap req.getHeader(AUTH_HEADER);
    string authHeader;

    if (authHeaderResult is error) {
        printDebug(KEY_OAUTH_PROVIDER, "Error in retrieving header " + AUTH_HEADER + ": " + authHeaderResult.reason());
        return false;
    } else {
        authHeader = <string> authHeaderResult;
    }

    if (authHeader.length() > 0 && authHeader.hasPrefix(AUTH_SCHEME_BEARER)) {
        string[] authHeaderComponents = authHeader.split(" ");
        if (authHeaderComponents.length() == 2) {
            return true;
        }
    }
    return false;
}

public function OAuthnAuthenticator.handle (http:Request req, http:FilterContext context)
                                   returns (APIKeyValidationDto| error) {

    APIRequestMetaDataDto apiKeyValidationRequestDto = getKeyValidationRequestObject(context);
    APIKeyValidationDto | error apiKeyValidationDto = trap self.oAuthAuthenticator.authenticate(apiKeyValidationRequestDto);
    if (apiKeyValidationDto is error) {
        log:printError("Error occurred while getting key validation information for the access token", err = apiKeyValidationDto);
        return apiKeyValidationDto;
    }
    return apiKeyValidationDto;
}



function  getAccessTokenCacheKey(APIRequestMetaDataDto dto) returns string {
    return dto.accessToken + ":" + dto.context + "/" + dto.apiVersion + dto.matchingResource + ":" + dto.httpVerb;
}



public type OAuthAuthProvider object {
    public APIGatewayCache gatewayCache= new;



    public function authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns (APIKeyValidationDto);

    public function doKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (json);

    public function invokeKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (boolean,
                APIKeyValidationDto);

};


public function OAuthAuthProvider.authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns
              (APIKeyValidationDto) {

    printDebug(KEY_OAUTH_PROVIDER, "Authenticating request using the request metadata.");
    string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
    string accessToken = apiRequestMetaDataDto.accessToken;
    boolean authorized;
    APIKeyValidationDto apiKeyValidationDto;
    if(getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
        printDebug(KEY_OAUTH_PROVIDER, "Checking for the access token in the gateway token cache.");
        var  isTokenCached = self.gatewayCache.retrieveFromTokenCache(accessToken);
            if(isTokenCached is boolean) {
                printDebug(KEY_OAUTH_PROVIDER, "Access token found in the token cache.");
                var apiKeyValidationDtoFromcache = self.gatewayCache.authenticateFromGatewayKeyValidationCache
                (cacheKey);
                if(apiKeyValidationDtoFromcache is APIKeyValidationDto ) {
                    if (isAccessTokenExpired(apiKeyValidationDtoFromcache)) {
                        self.gatewayCache.removeFromGatewayKeyValidationCache(cacheKey);
                        self.gatewayCache.addToInvalidTokenCache(accessToken, apiKeyValidationDtoFromcache);
                        self.gatewayCache.removeFromTokenCache(accessToken);
                        apiKeyValidationDtoFromcache.authorized = "false";
                        printDebug(KEY_OAUTH_PROVIDER, "Token has expired");
                        return apiKeyValidationDtoFromcache;
                    }
                    authorized = boolean.convert(apiKeyValidationDtoFromcache.authorized);
                    apiKeyValidationDto = apiKeyValidationDtoFromcache;
                    printDebug(KEY_OAUTH_PROVIDER, "Authorized value from the token cache: " + authorized);
                } else {
                    printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the invalid token cache."
                    + " Calling the key validation service.");
                    (authorized, apiKeyValidationDto) = self.invokeKeyValidation(apiRequestMetaDataDto);
                }
            } else {
                printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the gateway token cache.");

                printDebug(KEY_OAUTH_PROVIDER, "Checking for the access token in the invalid token cache.");
                var cacheAuthorizedValue = self.gatewayCache.retrieveFromInvalidTokenCache(accessToken);
                if(cacheAuthorizedValue is APIKeyValidationDto) {
                    printDebug(KEY_OAUTH_PROVIDER, "Access token found in the invalid token cache.");
                    return cacheAuthorizedValue;
                } else {
                    printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the invalid token cache."
                            + " Calling the key validation service.");
                    (authorized, apiKeyValidationDto) = self.invokeKeyValidation(apiRequestMetaDataDto);
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

public function OAuthAuthProvider.invokeKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (boolean,
            APIKeyValidationDto) {
    APIKeyValidationDto apiKeyValidationDto = {};
    string accessToken = apiRequestMetaDataDto.accessToken;
    boolean authorized = false;
    json keyValidationInfoJson = self.doKeyValidation(apiRequestMetaDataDto);
    printTrace(KEY_OAUTH_PROVIDER, "key Validation json " + keyValidationInfoJson.toString());
    var authorizeValue = keyValidationInfoJson.authorized;
    if(authorizeValue is string) {
        boolean auth = boolean.convert(authorizeValue);
        printDebug(KEY_OAUTH_PROVIDER, "Authorized value from key validation service: " + auth);
        if (auth) {
            //TODO: check whether to convert xml directly to the DTO. No need for xml to json conversion;
            apiKeyValidationDto = convertJsonToKeyValidationObject(keyValidationInfoJson);
            printDebug(KEY_OAUTH_PROVIDER, "key type: " + apiKeyValidationDto.keyType);
            authorized = auth;
            if(getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
                string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
                self.gatewayCache.addToGatewayKeyValidationCache(cacheKey, apiKeyValidationDto);
                self.gatewayCache.addToTokenCache(accessToken, true);
            }
        } else {
            apiKeyValidationDto.authorized = "false";
            apiKeyValidationDto.validationStatus = <string>keyValidationInfoJson.validationStatus;
            if(getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
                self.gatewayCache.addToInvalidTokenCache(accessToken, apiKeyValidationDto);
            }
        }
    } else {
        string errorMessage = "Error occurred while converting the authorized value from the key validation response to a
                        string value";
        log:printError(errorMessage);
        panic error(errorMessage);
    }

    return (authorized, apiKeyValidationDto);

}

public function OAuthAuthProvider.doKeyValidation (APIRequestMetaDataDto apiRequestMetaDataDto) returns (json) {

        string base64Header = getGatewayConfInstance().getKeyManagerConf().credentials.username + ":" +
            getGatewayConfInstance().getKeyManagerConf().credentials.password;
        string encodedBasicAuthHeader = encodeValueToBase64(base64Header);

        http:Request keyValidationRequest = new;
        http:Response keyValidationResponse = new;
        xmlns "http://schemas.xmlsoap.org/soap/envelope/" as soapenv;
        xmlns "http://org.apache.axis2/xsd" as xsd;
        xml contextXml = xml `<xsd:context>{{apiRequestMetaDataDto.context}}</xsd:context>`;
        xml versionXml = xml `<xsd:version>{{apiRequestMetaDataDto.apiVersion}}</xsd:version>`;
        xml tokenXml = xml `<xsd:accessToken>{{apiRequestMetaDataDto.accessToken}}</xsd:accessToken>`;
        xml authLevelXml = xml `<xsd:requiredAuthenticationLevel>{{apiRequestMetaDataDto
        .requiredAuthenticationLevel}}</xsd:requiredAuthenticationLevel>`;
        xml clientDomainXml = xml `<xsd:clientDomain>{{apiRequestMetaDataDto.clientDomain}}</xsd:clientDomain>`;
        xml resourceXml = xml `<xsd:matchingResource>{{apiRequestMetaDataDto.matchingResource}}</xsd:matchingResource>`;
        xml httpVerbXml = xml `<xsd:httpVerb>{{apiRequestMetaDataDto.httpVerb}}</xsd:httpVerb>`;
        xml soapBody = xml`<soapenv:Body></soapenv:Body>`;
        xml validateXml = xml`<xsd:validateKey></xsd:validateKey>`;
        xml requestValuesxml = contextXml + versionXml + tokenXml + authLevelXml + clientDomainXml + resourceXml +
            httpVerbXml;
        validateXml.setChildren(requestValuesxml);
        soapBody.setChildren(validateXml);
        xml soapEnvelope = xml `<soapenv:Envelope></soapenv:Envelope>`;
        soapEnvelope.setChildren(soapBody);
        keyValidationRequest.setXmlPayload(soapEnvelope);
        keyValidationRequest.setHeader(CONTENT_TYPE_HEADER, "text/xml");
        keyValidationRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE +
                encodedBasicAuthHeader);
        keyValidationRequest.setHeader("SOAPAction", "urn:validateKey");
        time:Time time = time:currentTime();
        int startTimeMills = time.time;
        var result = keyValidationEndpoint -> post("/services/APIKeyValidationService", keyValidationRequest);
        time = time:currentTime();
        int endTimeMills = time.time;
        printDebug(KEY_OAUTH_PROVIDER, "Total time taken for the key validation service call : " + (endTimeMills -
                    startTimeMills) + "ms");
        if(result is error) {
            log:printError("Error occurred while reading the key validation response",err =result);
            return {};
        } else {
            keyValidationResponse = result;
        }
        xml responsepayload;
        json payloadJson = {};
        var responseXml =  keyValidationResponse.getXmlPayload();
        if(responseXml is error) {
            log:printError("Error occurred while getting the key validation service XML response payload ",err=responseXml);
            return {};
        }
        if(responseXml is xml){
            responsepayload = responseXml;
            printTrace(KEY_OAUTH_PROVIDER, "Key validation response:" + responsepayload.getTextValue());
            payloadJson = responsepayload.toJSON({attributePrefix: "", preserveNamespaces: false});
            payloadJson = payloadJson["Envelope"]["Body"]["validateKeyResponse"]["return"];
        }

        return payloadJson;
}

function convertJsonToKeyValidationObject(json keyValidationInfoJson) returns APIKeyValidationDto {
     APIKeyValidationDto apiKeyValidationDto = {};
     apiKeyValidationDto.apiName=getStringValueOnly(keyValidationInfoJson.apiName);
     apiKeyValidationDto.apiPublisher=getStringValueOnly(keyValidationInfoJson.apiPublisher);
     apiKeyValidationDto.apiTier=getStringValueOnly(keyValidationInfoJson.apiTier);
     apiKeyValidationDto.applicationId=getStringValueOnly(keyValidationInfoJson.applicationId);
     apiKeyValidationDto.applicationName=getStringValueOnly(keyValidationInfoJson.applicationName);
     apiKeyValidationDto.applicationTier=getStringValueOnly(keyValidationInfoJson.applicationTier);
     apiKeyValidationDto.authorized=getStringValueOnly(keyValidationInfoJson.authorized);
     apiKeyValidationDto.authorizedDomains=getStringValueOnly(keyValidationInfoJson.authorizedDomains);
     apiKeyValidationDto.consumerKey=getStringValueOnly(keyValidationInfoJson.consumerKey);
     apiKeyValidationDto.contentAware=getStringValueOnly(keyValidationInfoJson.contentAware);
     apiKeyValidationDto.endUserName=getStringValueOnly(keyValidationInfoJson.endUserName);
     apiKeyValidationDto.endUserToken=getStringValueOnly(keyValidationInfoJson.endUserToken);
     apiKeyValidationDto.issuedTime=getStringValueOnly(keyValidationInfoJson.issuedTime);
     apiKeyValidationDto.spikeArrestLimit=getStringValueOnly(keyValidationInfoJson.spikeArrestLimit);
     apiKeyValidationDto.spikeArrestUnit=getStringValueOnly(keyValidationInfoJson.spikeArrestUnit);
     apiKeyValidationDto.stopOnQuotaReach=getStringValueOnly(keyValidationInfoJson.stopOnQuotaReach);
     apiKeyValidationDto.subscriber=getStringValueOnly(keyValidationInfoJson.subscriber);
     apiKeyValidationDto.subscriberTenantDomain=getStringValueOnly(keyValidationInfoJson.subscriberTenantDomain);
     apiKeyValidationDto.throttlingDataList=getStringValueOnly(keyValidationInfoJson.throttlingDataList);
     apiKeyValidationDto.tier=getStringValueOnly(keyValidationInfoJson.tier);
     apiKeyValidationDto.keyType=getStringValueOnly(keyValidationInfoJson["type"]);
     apiKeyValidationDto.userType=getStringValueOnly(keyValidationInfoJson.userType);
     apiKeyValidationDto.validationStatus=getStringValueOnly(keyValidationInfoJson.validationStatus);
     apiKeyValidationDto.validityPeriod=getStringValueOnly(keyValidationInfoJson.validityPeriod);
     return apiKeyValidationDto;
}

function getStringValueOnly(any value) returns (string) {
    return (value is string)? value : "";
}
