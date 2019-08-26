// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file   except
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

import ballerina/crypto;
import ballerina/encoding;
import ballerina/internal;
import ballerina/runtime;
import ballerina/auth;



# Represents an inbound basic Auth provider, which is a configuration-file-based Auth store provider.
# + basicAuthConfig - The Basic Auth provider configurations.
# + inboundBasicAuthProvider - The InboundBasicAUthProvider.
public type BasicAuthProvider object {

    *auth:InboundAuthProvider;

    public auth:BasicAuthConfig basicAuthConfig;
    public auth:InboundBasicAuthProvider inboundBasicAuthProvider;

    # Provides authentication based on the provided configuration.
    #
    # + basicAuthConfig - The Basic Auth provider configurations.
    public function __init(auth:BasicAuthConfig? basicAuthConfig = ()) {
        if (basicAuthConfig is auth:BasicAuthConfig) {
            self.basicAuthConfig = basicAuthConfig;
        } else {
            self.basicAuthConfig = { tableName: CONFIG_USER_SECTION };
        }
        self.inboundBasicAuthProvider = new(basicAuthConfig);
    }

    # Attempts to authenticate with credentials.
    #
    # + credential - Credential
    # + return - `true` if authentication is successful, otherwise `false` or `Error` occurred while extracting credentials
    public function authenticate(string credential) returns (boolean|auth:Error) {
        boolean isAuthenticated;
        //API authentication info
        AuthenticationContext authenticationContext = {};
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        string[] providerIds = [AUTHN_SCHEME_BASIC];
        //set Username from the request
        string encodedCredentials = credential[1];
        byte[]|error decodedCredentials =  encoding:decodeBase64(encodedCredentials);
        //Extract username and password from the request
        string userName;
        string passWord;
        if(decodedCredentials is byte[]){
            string  decodedCredentialsString =  encoding:byteArrayToString(decodedCredentials);
            if (decodedCredentialsString.indexOf(":", 0)== ()){
                //TODO: Handle the error message properly 
                //setErrorMessageToFilterContext(context, API_AUTH_BASICAUTH_INVALID_FORMAT);
                //sendErrorResponse(caller, request, <@untainted> context);
                return false;
            }
            string[] decodedCred = internal:split(decodedCredentialsString.trim(), ":");
            userName = decodedCred[0];
            printDebug(KEY_AUTHN_FILTER, "Decoded user name from the header : " + userName);
            if (decodedCred.length() < 2) {
                //TODO: Handle the error message properly 
                //setErrorMessageToFilterContext(context, API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                //sendErrorResponse(caller, request, context);
                return false;
            }
            passWord = decodedCred[1];
        } else {
            printError(KEY_AUTHN_FILTER, "Error while decoding the authorization header for basic authentication");
            //TODO: Handle the error message properly 
            //setErrorMessageToFilterContext(context, API_AUTH_GENERAL_ERROR);
            //sendErrorResponse(caller, request, context);
            return false;
        }

        //Hashing mechanism
        string hashedPass = encoding:encodeHex(crypto:hashSha1(passWord.toBytes()));
        printDebug(KEY_AUTHN_FILTER, "Hashed password value : " + hashedPass);
        string credentials = userName + ":" + hashedPass;
        string hashedRequest;
        string encodedVal = encoding:encodeBase64(credentials.toBytes());
        printDebug(KEY_AUTHN_FILTER, "Encoded Auth header value : " + encodedVal);
        hashedRequest = BASIC_PREFIX_WITH_SPACE + encodedVal;

        printDebug(KEY_AUTHN_FILTER, "Processing request with the Authentication handler chain");
        var isAuthorized = self.inboundBasicAuthProvider.authenticate(encodedVal);
        if (isAuthorized is boolean) {
            printDebug(KEY_AUTHN_FILTER, "Authentication handler chain returned with value : " + isAuthorized.toString());
            if (!isAuthorized) {
                //TODO: Handle the error message properly 
                //setErrorMessageToFilterContext(context, API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                //sendErrorResponse(caller, request, <@untainted> context);
                return false;
            }
            int startingTime = getCurrentTime();
            invocationContext.attributes[REQUEST_TIME] = startingTime;
            invocationContext.attributes[FILTER_FAILED] = false;
            //Set authenticationContext data
            authenticationContext.authenticated = true;
            //Authentication context data is set to default value bacuase in basic authentication we cannot have informtaion on subscription and applications
            authenticationContext.tier = UNAUTHENTICATED_TIER;
            authenticationContext.applicationTier = UNLIMITED_TIER;
            authenticationContext.apiKey = ANONYMOUS_APP_ID;
            //Username is extracted from the request
            authenticationContext.username = userName;
            authenticationContext.applicationId = ANONYMOUS_APP_ID;
            authenticationContext.applicationName = ANONYMOUS_APP_NAME;
            authenticationContext.subscriber = ANONYMOUS_APP_OWNER;
            authenticationContext.consumerKey = ANONYMOUS_CONSUMER_KEY;
            authenticationContext.apiTier = UNAUTHENTICATED_TIER;
            authenticationContext.apiPublisher = USER_NAME_UNKNOWN;
            authenticationContext.subscriberTenantDomain = ANONYMOUS_USER_TENANT_DOMAIN;
            authenticationContext.keyType = PRODUCTION_KEY_TYPE;
            invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
            invocationContext.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
            isAuthenticated = true;
            return isAuthenticated;
        } else {
            return prepareError("Failed to authenticate with basic auth hanndler.", isAuthorized);
        }
        
        

        
    }

};    