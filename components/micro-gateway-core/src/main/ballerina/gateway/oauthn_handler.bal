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
    public {
        string name= "oauth2";
        OAuthAuthProvider oAuthAuthenticator = new;
    }

    public function canHandle (http:Request req) returns (boolean);
    public function handle (http:Request req, APIRequestMetaDataDto apiRequestMetaDataDto)
    returns (APIKeyValidationDto| error);

};

@Description {value:"Intercepts a HTTP request for authentication"}
@Param {value:"req: Request object"}
@Return {value:"boolean: true if authentication is a success, else false"}
public function OAuthnAuthenticator::canHandle (http:Request req) returns (boolean) {
    string authHeader;
    try {
        authHeader = req.getHeader(AUTH_HEADER);
        io:println("auth header" + authHeader);
    } catch (error e) {
        log:printDebug("Error in retrieving header " + AUTH_HEADER + ": " + e.message);
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
public function OAuthnAuthenticator::handle (http:Request req, APIRequestMetaDataDto apiRequestMetaDataDto)
                                   returns (APIKeyValidationDto| error) {
    APIKeyValidationDto apiKeyValidationDto;
    try {
        apiKeyValidationDto = self.oAuthAuthenticator.authenticate(apiRequestMetaDataDto);
    } catch (error err) {
        log:printError("Error while getting key validation info for access token" +
                apiRequestMetaDataDto.accessToken, err = err);
        return err;
    }
    return apiKeyValidationDto;
}



function  getAccessTokenCacheKey(APIRequestMetaDataDto dto) returns string {
    return dto.accessToken + ":" + dto.context + "/" + dto.apiVersion + dto.matchingResource + ":" + dto.httpVerb;
}



@Description {value:"Represents a OAuth2 Authenticator"}
@Field {value:"gatewayTokenCache: Authentication cache object"}
public type OAuthAuthProvider object {
    public {
        APIGatewayCache gatewayTokenCache;
    }


    public function authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns (APIKeyValidationDto);

    public function doKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns (json);

};


@Description {value:"Authenticate with a oauth2 token"}
@Param {value:"apiRequestMetaDataDto: Object containig data to call the key validation service"}
@Return {value:"boolean: true if authentication is a success, else false"}
public function OAuthAuthProvider::authenticate (APIRequestMetaDataDto apiRequestMetaDataDto) returns
                                                                                                              (APIKeyValidationDto) {
    string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
    boolean authorized;
    APIKeyValidationDto apiKeyValidationDto;
    match self.gatewayTokenCache.authenticateFromGatewayKeyValidationCache(cacheKey) {
        APIKeyValidationDto apiKeyValidationDtoFromcache => {
        if(isAccessTokenExpired(apiKeyValidationDtoFromcache)) {
            self.gatewayTokenCache.removeFromGatewayKeyValidationCache(cacheKey);
            self.gatewayTokenCache.addToInvalidTokenCache(cacheKey, true);
            apiKeyValidationDtoFromcache.authorized= "false";
            log:printDebug("Access token found in cache. But token is expired");
            return apiKeyValidationDtoFromcache;
        }
        authorized = < boolean > apiKeyValidationDtoFromcache.authorized;
        apiKeyValidationDto = apiKeyValidationDtoFromcache;
        log:printDebug("Access token " + apiRequestMetaDataDto.accessToken + " found in cache.");
        }
        () => {
            match self.gatewayTokenCache.retrieveFromInvalidTokenCache(cacheKey) {
                boolean cacheAuthorizedValue => {
                    APIKeyValidationDto apiKeyValidationInfoDTO = { authorized: "false", validationStatus:API_AUTH_INVALID_CREDENTIALS };
                    log:printDebug("Access token found in invalid
                    token cache.");
                    return apiKeyValidationInfoDTO;
                }
                () => {
                    log:printDebug("Access token not found in cache.
                    Hence calling the key vaidation service.");
                    json keyValidationInfoJson = self.doKeyValidation(apiRequestMetaDataDto);
                    match <string>keyValidationInfoJson.authorized {
                        string authorizeValue => {
                            boolean auth = <boolean>authorizeValue;
                            if (auth) {
                                match <APIKeyValidationDto>keyValidationInfoJson {
                                    APIKeyValidationDto dto => {
                                        apiKeyValidationDto = dto;
                                        // specifically setting the key type since type is a keyword in ballerina.
                                        apiKeyValidationDto.keyType = check <string>keyValidationInfoJson["type"];
                                    }
                                    error err => {
                                        log:printError("Error while converting key validation response json to type APIKeyValidationDto"
                                            , err = err);
                                        throw err;
                                    }
                                }
                                authorized = auth;
                                self.gatewayTokenCache.addToGatewayKeyValidationCache(cacheKey, apiKeyValidationDto);
                            } else {
                                self.gatewayTokenCache.addToInvalidTokenCache(cacheKey, true);
                                apiKeyValidationDto.authorized="false";
                                apiKeyValidationDto.validationStatus = check <string>keyValidationInfoJson
                                    .validationStatus;
                            }
                        }
                        error err => {
                            log:printError("Error while converting authorzed value from key vaidation respnse to a
                            string value", err=err);
                            throw err;
                        }
                    }
                }
            }

        }
    }
    if (authorized) {
        // set username
        runtime:getInvocationContext().userPrincipal.username = apiKeyValidationDto.endUserName;

    }
    return apiKeyValidationDto;
}


public function OAuthAuthProvider::doKeyValidation (APIRequestMetaDataDto apiRequestMetaDataDto)
                                       returns (json) {
    try {
        string base64Header = getGatewayConfInstance().getKeyManagerConf().credentials.username + ":" +
            getGatewayConfInstance().getKeyManagerConf().credentials.password;
        string encodedBasicAuthHeader = check base64Header.base64Encode();

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
        var result1 = keyValidationEndpoint -> post("/services/APIKeyValidationService", request= keyValidationRequest);
        time = time:currentTime();
        int endTimeMills = time.time;
        log:printDebug("Total time taken for key validation service call : " + (endTimeMills- startTimeMills) +
                "ms");

        match result1 {
            error err => {
                log:printError("Error occurred while reading key validation response",err =err);
                return {};
            }
            http:Response prod => {
                keyValidationResponse = prod;
            }
        }
        xml responsepayload;
        match keyValidationResponse.getXmlPayload() {
            error err => {
                log:printError("Error occurred while getting key validation service xml response payload ",err=err);
                return {};
            }
            xml responseXml => {
                responsepayload = responseXml;
            }
        }
        json payloadJson = responsepayload.toJSON({attributePrefix: "", preserveNamespaces: false});
        payloadJson = payloadJson["Envelope"]["Body"]["validateKeyResponse"]["return"];
        return(payloadJson);

    } catch (error err) {
        log:printError("Error occurred while validating token",err =err);
        return {};
    }
    return {};

}



