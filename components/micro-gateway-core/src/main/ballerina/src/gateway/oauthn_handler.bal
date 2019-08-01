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

xmlns "http://schemas.xmlsoap.org/soap/envelope/" as soapenv;
xmlns "http://org.apache.axis2/xsd" as xsd;
xmlns "http://dto.impl.apimgt.carbon.wso2.org/xsd" as apim;

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
    string encodedBasicAuthHeader;

    public function __init() {
        string base64Header = getConfigValue(KM_CONF_INSTANCE_ID, USERNAME, "admin") + ":" +
            getConfigValue(KM_CONF_INSTANCE_ID, PASSWORD, "admin");
        self.encodedBasicAuthHeader = encoding:encodeBase64(base64Header.toByteArray(UTF_8));
    }

    public function authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns (APIKeyValidationDto);

    public function doKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (xml | error);

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
                        apiKeyValidationDtoFromcache.authorized = false;
                        printDebug(KEY_OAUTH_PROVIDER, "Token has expired");
                        return apiKeyValidationDtoFromcache;
                    }
                    authorized = apiKeyValidationDtoFromcache.authorized;
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
    xml|error keyValidationResponseXML = self.doKeyValidation(apiRequestMetaDataDto);
    if (keyValidationResponseXML is xml) {
        printTrace(KEY_OAUTH_PROVIDER, "key Validation json " + keyValidationResponseXML.getTextValue());
        xml keyValidationInfoXML = keyValidationResponseXML[soapenv:Body][xsd:validateKeyResponse][xsd:^"return"];
        string authorizeValue = keyValidationInfoXML[apim:authorized].getTextValue();
        boolean auth = boolean.convert(authorizeValue);
        printDebug(KEY_OAUTH_PROVIDER, "Authorized value from key validation service: " + auth);
        if (auth) {
            apiKeyValidationDto = convertXmlToKeyValidationObject(keyValidationInfoXML);
            printDebug(KEY_OAUTH_PROVIDER, "key type: " + apiKeyValidationDto.keyType);
            authorized = auth;
            if (getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
                string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
                self.gatewayCache.addToGatewayKeyValidationCache(cacheKey, apiKeyValidationDto);
                self.gatewayCache.addToTokenCache(accessToken, true);
            }
        } else {
            apiKeyValidationDto.authorized = false;
            apiKeyValidationDto.validationStatus = keyValidationInfoXML[apim:validationStatus].getTextValue();
            if (getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, true)) {
                self.gatewayCache.addToInvalidTokenCache(accessToken, apiKeyValidationDto);
            }
        }
    } else if(keyValidationResponseXML is error){
        string errorMessage = "Error occurred while the key validation request";
        log:printError(errorMessage, err=keyValidationResponseXML);
        panic error(errorMessage);
    }

    return (authorized, apiKeyValidationDto);

}

public function OAuthAuthProvider.doKeyValidation (APIRequestMetaDataDto apiRequestMetaDataDto) returns (xml|error) {

    http:Request keyValidationRequest = new;
    http:Response keyValidationResponse = new;
    xml soapEnvelope = xml `<soapenv:Envelope>
                                <soapenv:Body>
                                    <xsd:validateKey>
                                        <xsd:context>{{apiRequestMetaDataDto.context}}</xsd:context>
                                        <xsd:version>{{apiRequestMetaDataDto.apiVersion}}</xsd:version>
                                        <xsd:accessToken>{{apiRequestMetaDataDto.accessToken}}</xsd:accessToken>
                                        <xsd:requiredAuthenticationLevel>{{apiRequestMetaDataDto.requiredAuthenticationLevel}}</xsd:requiredAuthenticationLevel>
                                        <xsd:clientDomain>{{apiRequestMetaDataDto.clientDomain}}</xsd:clientDomain>
                                        <xsd:matchingResource>{{apiRequestMetaDataDto.matchingResource}}</xsd:matchingResource>
                                        <xsd:httpVerb>{{apiRequestMetaDataDto.httpVerb}}</xsd:httpVerb>
                                    </xsd:validateKey>
                                </soapenv:Body>
                            </soapenv:Envelope>`;
    keyValidationRequest.setXmlPayload(soapEnvelope, contentType=TEXT_XML);
    keyValidationRequest.setHeader(AUTHORIZATION_HEADER, BASIC_PREFIX_WITH_SPACE + self.encodedBasicAuthHeader);
    keyValidationRequest.setHeader(SOAP_ACTION, VALIDATE_KEY_SOAP_ACTION);
    time:Time time = time:currentTime();
    int startTimeMills = time.time;
    var result = keyValidationEndpoint -> post(KEY_VALIDATION_SERVICE_CONTEXT, keyValidationRequest);
    time = time:currentTime();
    int endTimeMills = time.time;
    printDebug(KEY_OAUTH_PROVIDER, "Total time taken for the key validation service call : " + (endTimeMills -
                startTimeMills) + "ms");
    if(result is http:Response) {
        keyValidationResponse = result;
    } else if(result is error) {
        string message = "Error occurred while reading the key validation response";
        log:printError(message ,err =result);
        return result;
    }
    var responseXml =  keyValidationResponse.getXmlPayload();
    if(responseXml is xml) {
        printTrace(KEY_OAUTH_PROVIDER, "Key validation response:" + responseXml.getTextValue());

    } else if(responseXml is error){
        string message = "Error occurred while getting the key validation service XML response payload";
        log:printError(message,err=responseXml);
    }
    return responseXml;

}

function convertXmlToKeyValidationObject(xml keyValidationInfoXML) returns APIKeyValidationDto {
     APIKeyValidationDto apiKeyValidationDto = {};
     apiKeyValidationDto.apiName = keyValidationInfoXML[apim:apiName].getTextValue();
     apiKeyValidationDto.apiPublisher = keyValidationInfoXML[apim:apiPublisher].getTextValue();
     apiKeyValidationDto.apiTier = keyValidationInfoXML[apim:apiTier].getTextValue();
     apiKeyValidationDto.applicationId = keyValidationInfoXML[apim:applicationId].getTextValue();
     apiKeyValidationDto.applicationName = keyValidationInfoXML[apim:applicationName].getTextValue();
     apiKeyValidationDto.applicationTier = keyValidationInfoXML[apim:applicationTier].getTextValue();
     apiKeyValidationDto.authorized = boolean.convert(keyValidationInfoXML[apim:authorized].getTextValue());
     apiKeyValidationDto.authorizedDomains = keyValidationInfoXML[apim:authorizedDomains].getTextValue();
     apiKeyValidationDto.consumerKey = keyValidationInfoXML[apim:consumerKey].getTextValue();
     apiKeyValidationDto.contentAware = keyValidationInfoXML[apim:contentAware].getTextValue();
     apiKeyValidationDto.endUserName = keyValidationInfoXML[apim:endUserName].getTextValue();
     apiKeyValidationDto.endUserToken = keyValidationInfoXML[apim:endUserToken].getTextValue();
     apiKeyValidationDto.issuedTime = keyValidationInfoXML[apim:issuedTime].getTextValue();
     apiKeyValidationDto.spikeArrestLimit = keyValidationInfoXML[apim:spikeArrestLimit].getTextValue();
     apiKeyValidationDto.spikeArrestUnit = keyValidationInfoXML[apim:spikeArrestUnit].getTextValue();
     apiKeyValidationDto.stopOnQuotaReach = keyValidationInfoXML[apim:stopOnQuotaReach].getTextValue();
     apiKeyValidationDto.subscriber = keyValidationInfoXML[apim:subscriber].getTextValue();
     apiKeyValidationDto.subscriberTenantDomain = keyValidationInfoXML[apim:subscriberTenantDomain].getTextValue();
     apiKeyValidationDto.throttlingDataList = keyValidationInfoXML[apim:throttlingDataList].getTextValue();
     apiKeyValidationDto.tier = keyValidationInfoXML[apim:tier].getTextValue();
     apiKeyValidationDto.keyType = keyValidationInfoXML[apim:^"type"].getTextValue();
     apiKeyValidationDto.userType = keyValidationInfoXML[apim:userType].getTextValue();
     apiKeyValidationDto.validationStatus = keyValidationInfoXML[apim:validationStatus].getTextValue();
     apiKeyValidationDto.validityPeriod = keyValidationInfoXML[apim:validityPeriod].getTextValue();
     return apiKeyValidationDto;
}

