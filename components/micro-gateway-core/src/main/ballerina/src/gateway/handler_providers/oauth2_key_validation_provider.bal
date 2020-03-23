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
import ballerina/auth;
import ballerina/http;
import ballerina/lang.'int;
import ballerina/log;
import ballerina/runtime;
import ballerina/stringutils;
import ballerina/time;
import ballerina/oauth2;


xmlns "http://schemas.xmlsoap.org/soap/envelope/" as soapenv;
xmlns "http://org.apache.axis2/xsd" as xsd;
xmlns "http://dto.impl.apimgt.carbon.wso2.org/xsd" as apim;

# Represents inbound OAuth2 provider, which calls the key validation service of the WSO2 Key manager
#
# + keyValidationClient - key validation client endpoint
# + gatewayCache - the `APIGatewayCache instence`
# 
public type OAuth2KeyValidationProvider object {

    *auth:InboundAuthProvider;

    public http:Client keyValidationClient;
    public APIGatewayCache gatewayCache = new;

    public function __init(oauth2:IntrospectionServerConfig config) {
        self.keyValidationClient = new (config.url, config.clientConfig);
    }


    public function authenticate(string credential) returns @tainted (boolean | auth:Error) {
        AuthenticationContext authenticationContext = {};
        boolean isAuthorized;
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        APIRequestMetaDataDto apiKeyValidationRequestDto = getKeyValidationRequestObject(invocationContext, credential);
        //Start a span attaching to the system span.
        int | error | () spanId_cacheCheck = startSpan(OAUTH_VALIDATION_PROVIDER_CACHE_CHECK);
        APIKeyValidationDto | error apiKeyValidationDto = trap self.checkCacheAndAuthenticate(apiKeyValidationRequestDto, invocationContext);
        //Finish span.
        finishSpan(OAUTH_VALIDATION_PROVIDER_CACHE_CHECK, spanId_cacheCheck);
        if (apiKeyValidationDto is APIKeyValidationDto) {
            isAuthorized = apiKeyValidationDto.authorized;
            printDebug(KEY_AUTHN_FILTER, "Authentication handler returned with value : " +
            isAuthorized.toString());
            if (isAuthorized) {
                authenticationContext.authenticated = true;
                authenticationContext.tier = apiKeyValidationDto?.tier;
                authenticationContext.apiKey = credential;
                if (apiKeyValidationDto.endUserName != "") {
                    authenticationContext.username = apiKeyValidationDto.endUserName;
                } else {
                    authenticationContext.username = END_USER_ANONYMOUS;
                }
                authenticationContext.apiPublisher = apiKeyValidationDto.apiPublisher;
                authenticationContext.keyType = apiKeyValidationDto.keyType;

                if (apiKeyValidationDto?.endUserToken is string) {
                    authenticationContext.callerToken = <string>apiKeyValidationDto?.endUserToken;
                }
                authenticationContext.applicationId = apiKeyValidationDto.applicationId;
                authenticationContext.applicationName = apiKeyValidationDto.applicationName;
                authenticationContext.applicationTier = apiKeyValidationDto.applicationTier;
                authenticationContext.subscriber = apiKeyValidationDto.subscriber;
                authenticationContext.consumerKey = apiKeyValidationDto.consumerKey;
                authenticationContext.apiTier = apiKeyValidationDto.apiTier;
                authenticationContext.subscriberTenantDomain = apiKeyValidationDto.
                subscriberTenantDomain;
                int | error spikeArrestLimit = 'int:fromString(apiKeyValidationDto.spikeArrestLimit);
                authenticationContext.spikeArrestLimit = (spikeArrestLimit is int) ? spikeArrestLimit : 0;
                authenticationContext.spikeArrestUnit = apiKeyValidationDto.spikeArrestUnit;
                authenticationContext.stopOnQuotaReach = stringutils:toBoolean(apiKeyValidationDto.
                stopOnQuotaReach);

                invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;

                // setting keytype to invocationContext
                invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext
                .keyType;
                runtime:AuthenticationContext authContext = {scheme: AUTH_SCHEME_OAUTH2, authToken: credential};

                printDebug(KEY_AUTHN_FILTER, "Set the auth context schema as : " + AUTH_SCHEME_OAUTH2);
                invocationContext.authenticationContext = authContext;
                return isAuthorized;
            } else {
                int | error status = 'int:fromString(apiKeyValidationDto.validationStatus);
                int errorStatus = (status is int) ? status : INTERNAL_SERVER_ERROR;
                printDebug(KEY_AUTHN_FILTER,
                "Authentication handler returned with validation status : " + errorStatus.toString());
                //TODO: Send proper error messages
                setErrorMessageToInvocationContext(errorStatus);
                //sendErrorResponse(caller, request, <@untainted>  context);
                return false;
            }
        } else {
            log:printError(<string>apiKeyValidationDto.reason(), err = apiKeyValidationDto);
            //TODO: Send proper error messages
            setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
            //sendErrorResponse(caller, request, <@untainted>  context);
            return false;
        }
    }

    public function checkCacheAndAuthenticate(APIRequestMetaDataDto apiRequestMetaDataDto,@tainted runtime:InvocationContext invocationContext)
    returns @tainted (APIKeyValidationDto) {
        printDebug(KEY_OAUTH_PROVIDER, "Authenticating request using the request metadata.");
        string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
        string accessToken = apiRequestMetaDataDto.accessToken;
        boolean authorized;
        APIKeyValidationDto apiKeyValidationDto;
        if (getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, DEFAULT_CACHING_ENABLED)) {
            printDebug(KEY_OAUTH_PROVIDER, "Checking for the access token in the gateway token cache.");
            var isTokenCached = self.gatewayCache.retrieveFromTokenCache(accessToken);
            if (isTokenCached is boolean) {
                printDebug(KEY_OAUTH_PROVIDER, "Access token found in the token cache.");
                var apiKeyValidationDtoFromcache = self.gatewayCache.authenticateFromGatewayKeyValidationCache
                (cacheKey);
                if (apiKeyValidationDtoFromcache is APIKeyValidationDto) {
                    if (isAccessTokenExpired(apiKeyValidationDtoFromcache)) {
                        self.gatewayCache.removeFromGatewayKeyValidationCache(cacheKey);
                        self.gatewayCache.addToInvalidTokenCache(cacheKey, apiKeyValidationDtoFromcache);
                        self.gatewayCache.removeFromTokenCache(accessToken);
                        apiKeyValidationDtoFromcache.authorized = false;
                        printDebug(KEY_OAUTH_PROVIDER, "Token has expired");
                        return apiKeyValidationDtoFromcache;
                    }
                    authorized = apiKeyValidationDtoFromcache.authorized;
                    apiKeyValidationDto = apiKeyValidationDtoFromcache;
                    printDebug(KEY_OAUTH_PROVIDER, "Authorized value from the token cache: " + authorized.toString());
                } else {
                    printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the invalid token cache."
                    + " Calling the key validation service.");
                    [authorized, apiKeyValidationDto] = self.invokeKeyValidation(apiRequestMetaDataDto);
                }
            } else {
                printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the gateway token cache.");

                printDebug(KEY_OAUTH_PROVIDER, "Checking for the access token in the invalid token cache.");
                var cacheAuthorizedValue = self.gatewayCache.retrieveFromInvalidTokenCache(cacheKey);
                if (cacheAuthorizedValue is APIKeyValidationDto) {
                    printDebug(KEY_OAUTH_PROVIDER, "Access token found in the invalid token cache.");
                    return cacheAuthorizedValue;
                } else {
                    printDebug(KEY_OAUTH_PROVIDER, "Access token not found in the invalid token cache."
                    + " Calling the key validation service.");
                    [authorized, apiKeyValidationDto] = self.invokeKeyValidation(apiRequestMetaDataDto);
                }
            }

        } else {
            printDebug(KEY_OAUTH_PROVIDER, "Gateway cache disabled. Calling the key validation service.");
            [authorized, apiKeyValidationDto] = self.invokeKeyValidation(apiRequestMetaDataDto);
        }
        if (authorized) {
            // set username
            invocationContext.principal.username = apiKeyValidationDto.endUserName;
        }
        return apiKeyValidationDto;
    }

    public function doKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns @tainted (xml | error) {
        http:Request keyValidationRequest = new;
        http:Response keyValidationResponse = new;
        xml soapEnvelope = xml `<soapenv:Envelope>
                                    <soapenv:Body>
                                        <xsd:validateKey>
                                            <xsd:context>${apiRequestMetaDataDto.context}</xsd:context>
                                            <xsd:version>${apiRequestMetaDataDto.apiVersion}</xsd:version>
                                            <xsd:accessToken>${apiRequestMetaDataDto.accessToken}</xsd:accessToken>
                                            <xsd:requiredAuthenticationLevel>${apiRequestMetaDataDto.requiredAuthenticationLevel}</xsd:requiredAuthenticationLevel>
                                            <xsd:clientDomain>${apiRequestMetaDataDto.clientDomain}</xsd:clientDomain>
                                            <xsd:matchingResource>${apiRequestMetaDataDto.matchingResource}</xsd:matchingResource>
                                            <xsd:httpVerb>${apiRequestMetaDataDto.httpVerb}</xsd:httpVerb>
                                        </xsd:validateKey>
                                    </soapenv:Body>
                                </soapenv:Envelope>`;
        keyValidationRequest.setXmlPayload(soapEnvelope, contentType = TEXT_XML);
        keyValidationRequest.setHeader(SOAP_ACTION, VALIDATE_KEY_SOAP_ACTION);
        time:Time time = time:currentTime();
        int startTimeMills = time.time;
        var result = self.keyValidationClient->post(KEY_VALIDATION_SERVICE_CONTEXT, keyValidationRequest);
        time = time:currentTime();
        int endTimeMills = time.time;
        int timeDiff = endTimeMills - startTimeMills;
        printDebug(KEY_OAUTH_PROVIDER, "Total time taken for the key validation service call : " + timeDiff.toString() + "ms");
        if (result is http:Response) {
            keyValidationResponse = result;
        } else {
            string message = "Error occurred while reading the key validation response";
            log:printError(message, err = result);
            return result;
        }
        var responseXml = keyValidationResponse.getXmlPayload();
        if (responseXml is xml) {
            printTrace(KEY_OAUTH_PROVIDER, "Key validation response:" + responseXml.getTextValue());

        } else {
            string message = "Error occurred while getting the key validation service XML response payload";
            log:printError(message, err = responseXml);
        }
        return responseXml;

    }

    public function invokeKeyValidation(APIRequestMetaDataDto apiRequestMetaDataDto) returns @tainted [boolean,
 APIKeyValidationDto] {
        APIKeyValidationDto apiKeyValidationDto = {};
        string accessToken = apiRequestMetaDataDto.accessToken;
        boolean authorized = false;
        //Start a new child span for the span.
        int | error | () spanId_KeyValidate = startSpan(OAUTH_AUTHPROVIDER_INVOKEKEYVALIDATION);
        xml | error keyValidationResponseXML = self.doKeyValidation(apiRequestMetaDataDto);
        //finishing span
        finishSpan(OAUTH_AUTHPROVIDER_INVOKEKEYVALIDATION, spanId_KeyValidate);
        if (keyValidationResponseXML is xml) {
            printTrace(KEY_OAUTH_PROVIDER, "key Validation json " + keyValidationResponseXML.getTextValue());
            xml keyValidationInfoXML = keyValidationResponseXML[soapenv:Body][xsd:validateKeyResponse][xsd:'return];
            string authorizeValue = keyValidationInfoXML[apim:authorized].getTextValue();
            boolean auth = stringutils:toBoolean(authorizeValue);
            printDebug(KEY_OAUTH_PROVIDER, "Authorized value from key validation service: " + auth.toString());
            string cacheKey = getAccessTokenCacheKey(apiRequestMetaDataDto);
            if (auth) {
                apiKeyValidationDto = convertXmlToKeyValidationObject(keyValidationInfoXML);
                printDebug(KEY_OAUTH_PROVIDER, "key type: " + apiKeyValidationDto.keyType);
                authorized = auth;
                if (getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, DEFAULT_CACHING_ENABLED)) {
                    self.gatewayCache.addToGatewayKeyValidationCache(cacheKey, apiKeyValidationDto);
                    self.gatewayCache.addToTokenCache(accessToken, true);
                }
            } else {
                apiKeyValidationDto.authorized = false;
                apiKeyValidationDto.validationStatus = keyValidationInfoXML[apim:validationStatus].getTextValue();
                if (getConfigBooleanValue(CACHING_ID, TOKEN_CACHE_ENABLED, DEFAULT_CACHING_ENABLED)) {
                    self.gatewayCache.addToInvalidTokenCache(cacheKey, apiKeyValidationDto);
                }
            }
        } else {
            string errorMessage = "Error occurred while the key validation request";
            log:printError(errorMessage, err = keyValidationResponseXML);
            panic error(errorMessage);
        }

        return [authorized, apiKeyValidationDto];

    }

};

# Represents key validation server onfigurations.
#
# + url - URL of the introspection server
# + clientConfig - HTTP client configurations which calls the key validation server
public type KeyValidationServerConfig record {|
    string url;
    http:ClientConfiguration clientConfig = {};
|};



function convertXmlToKeyValidationObject(xml keyValidationInfoXML) returns APIKeyValidationDto {
    APIKeyValidationDto apiKeyValidationDto = {};
    apiKeyValidationDto.apiName = keyValidationInfoXML[apim:apiName].getTextValue();
    apiKeyValidationDto.apiPublisher = keyValidationInfoXML[apim:apiPublisher].getTextValue();
    apiKeyValidationDto.apiTier = keyValidationInfoXML[apim:apiTier].getTextValue();
    apiKeyValidationDto.applicationId = keyValidationInfoXML[apim:applicationId].getTextValue();
    apiKeyValidationDto.applicationName = keyValidationInfoXML[apim:applicationName].getTextValue();
    apiKeyValidationDto.applicationTier = keyValidationInfoXML[apim:applicationTier].getTextValue();
    apiKeyValidationDto.authorized = stringutils:toBoolean(keyValidationInfoXML[apim:authorized].getTextValue());
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
    apiKeyValidationDto.keyType = keyValidationInfoXML[apim:'type].getTextValue();
    apiKeyValidationDto.userType = keyValidationInfoXML[apim:userType].getTextValue();
    apiKeyValidationDto.validationStatus = keyValidationInfoXML[apim:validationStatus].getTextValue();
    apiKeyValidationDto.validityPeriod = keyValidationInfoXML[apim:validityPeriod].getTextValue();
    return apiKeyValidationDto;
}

function getAccessTokenCacheKey(APIRequestMetaDataDto dto) returns string {
    return dto.accessToken + ":" + dto.context + "/" + dto.apiVersion + dto.matchingResource + ":" + dto.httpVerb;
}
