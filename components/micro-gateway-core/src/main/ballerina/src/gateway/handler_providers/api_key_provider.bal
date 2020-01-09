// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/jwt;
import ballerina/auth;
import ballerina/runtime;
import ballerina/io;

# Represents inbound api key auth provider.
#
# + apiKeyValidatorConfig - api key validator configurations
# + inboundJwtAuthProviderforAPIKey - Reference to b7a inbound auth provider
public type APIKeyProvider object {
    *auth:InboundAuthProvider;

    public jwt:JwtValidatorConfig apiKeyValidatorConfig;
    public jwt:InboundJwtAuthProvider inboundJwtAuthProviderforAPIKey;

    # Provides authentication based on the provided api key token.
    #
    # + apiKeyValidatorConfig - api key validator configurations
    public function __init(jwt:JwtValidatorConfig apiKeyValidatorConfig) {
        self.apiKeyValidatorConfig = apiKeyValidatorConfig;
        self.inboundJwtAuthProviderforAPIKey = new(apiKeyValidatorConfig);
    }

    public function authenticate(string credential) returns @tainted (boolean|auth:Error) {
        //Start a span attaching to the system span.
        int | error | () spanIdAuth = startSpan(API_KEY_PROVIDER_AUTHENTICATE);
        var handleVar = self.inboundJwtAuthProviderforAPIKey.authenticate(credential);
        //finishing span
        finishSpan(API_KEY_PROVIDER_AUTHENTICATE, spanIdAuth);       
        if(handleVar is boolean) {
            boolean validated = false;
            if (!handleVar) {            
                setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                return false;
            } 
            validated = validateAPIKey(credential);
            if (!validated) {
                setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
            }
            return validated;
        } else {
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
            return prepareError("Failed to authenticate with api key auth provider.", handleVar);
        }  
    }  
};

# api key authorization
#
# + apiKeyToken - api key token string.
# + return - Returns boolean value.
public function validateAPIKey(string apiKeyToken) returns boolean {

    runtime:InvocationContext invocationContext = runtime:getInvocationContext();  
    runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;             
    if (authContext is runtime:AuthenticationContext) {
        printDebug(API_KEY_UTIL, "Set authContext scheme to " + AUTH_SCHEME_API_KEY);
        authContext.scheme = AUTH_SCHEME_API_KEY;
    }
    //decode jwt
    [jwt:JwtHeader,jwt:JwtPayload]|jwt:Error decodedJWT = jwt:decodeJwt(apiKeyToken);
    if (decodedJWT is error) {
        printDebug(API_KEY_UTIL, "Error while decoding the JWT token");
        return false;
    }
    [jwt:JwtHeader,jwt:JwtPayload] [jwtHeader,payload] = <[jwt:JwtHeader,jwt:JwtPayload]> decodedJWT;

    //invocation context 
    AuthenticationContext authenticationContext = {};
    authenticationContext.apiKey = apiKeyToken;
    authenticationContext.callerToken = apiKeyToken;
    authenticationContext.authenticated = false;

    string? username = payload?.sub;
    if (username is string) {
        printDebug(API_KEY_UTIL, "set username : " + username);
        authenticationContext.username = username;
    }

    map<json>? customClaims = payload?.customClaims;
    //set keytype
    if (customClaims is map<json> && customClaims.hasKey(KEY_TYPE)) {
        json keyType = customClaims.get(KEY_TYPE);
        printDebug(API_KEY_UTIL, "set keytype as " + keyType.toString());
        authenticationContext.keyType = keyType.toString();
        invocationContext.attributes[KEY_TYPE_ATTR] = keyType;
    }
    //set application attribs if present in token
    if (customClaims is map<json> && customClaims.hasKey("application")) {
        json? application = customClaims.get("application");
        if (application is map<json>) {
            if (application.id != null) {
                printDebug(API_KEY_UTIL, "set application ID to " + application.id.toString());
                authenticationContext.applicationId = application.id.toString();
            }
            if (application.name != null) {
                printDebug(API_KEY_UTIL, "set application name to " + application.name.toString());
                authenticationContext.applicationName = application.name.toString();
            }
            if (application.tier != null) {
                printDebug(API_KEY_UTIL, "set application tier to " + application.tier.toString());
                authenticationContext.applicationTier = application.tier.toString();
            }
            if (application.owner != null) {
                printDebug(API_KEY_UTIL, "set application owner to " + application.owner.toString());
                authenticationContext.subscriber = application.owner.toString();
            }
        }
    }
    //validate allowed apis
    boolean validateAllowedAPIs = getConfigBooleanValue(API_KEY_INSTANCE_ID, API_KEY_VALIDATE_ALLOWED_APIS,false);
    if (validateAllowedAPIs) {
        //get allowed apis
        json subscribedAPIList = ();
        if (customClaims is map<json>) {
            if (customClaims.hasKey(SUBSCRIBED_APIS)) {
                printDebug(API_KEY_UTIL, "subscribedAPIs claim found in the jwt");
                subscribedAPIList = customClaims.get(SUBSCRIBED_APIS);
            } else if (customClaims.hasKey(ALLOWED_APIS)) {
                printDebug(API_KEY_UTIL, "allowedAPIs claim found in the jwt");
                subscribedAPIList = customClaims.get(ALLOWED_APIS);
            }
        }
            
        if (subscribedAPIList is json[]) {  
            APIConfiguration? apiConfig = apiConfigAnnotationMap[<string>invocationContext.attributes["SERVICE_NAME"]];
            if (apiConfig is APIConfiguration) {
                string apiName = apiConfig.name;
                string apiVersion = apiConfig.apiVersion;
                int l = subscribedAPIList.length();
                int index = 0;
                while (index < l) {
                    var subscription = subscribedAPIList[index];
                    if (subscription.name.toString() == apiName && subscription.'version.toString() == apiVersion) {
                        authenticationContext.authenticated = true;
                        printDebug(API_KEY_UTIL, "Found a matching allowed api with name:" + subscription.name.toString() + " version:" + subscription.'version.toString());
                        
                        //set throttling attribs if present
                        if (subscription.subscriptionTier is json) {
                            printDebug(API_KEY_UTIL, "set application tier to " + subscription.subscriptionTier.toString());
                            authenticationContext.tier = subscription.subscriptionTier.toString();
                        }
                        if (subscription.subscriptionTier is json) {
                            printDebug(API_KEY_UTIL, "set apiTier to "+ subscription.subscriptionTier.toString());
                            authenticationContext.apiTier = subscription.subscriptionTier.toString();
                        }
                        if (subscription.publisher is json) {
                            printDebug(API_KEY_UTIL, "set apiPublisher to " + subscription.publisher.toString());
                            authenticationContext.apiPublisher = subscription.publisher.toString();
                        }
                        if (subscription.subscriberTenantDomain is json) {
                            printDebug(API_KEY_UTIL, "set subscriberTenantDomain to " + subscription.subscriberTenantDomain.toString());
                            authenticationContext.subscriberTenantDomain = subscription.subscriberTenantDomain.toString();
                        } 

                        invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
                        io:println(authenticationContext);
                        return true;
                    }
                    index += 1;
                }
            }
        }
    }
    invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
    return false;
}
