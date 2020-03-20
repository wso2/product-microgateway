// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/auth;
import ballerina/crypto;
import ballerina/lang.'array as arrays;
import ballerina/lang.'string as strings;
import ballerina/runtime;
import ballerina/config;

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
            self.basicAuthConfig = {tableName: CONFIG_USER_SECTION};
        }
        self.inboundBasicAuthProvider = new (basicAuthConfig);
    }

    # Attempts to authenticate with credentials.
    #
    # + credential - Credential
    # + return - `true` if authentication is successful, otherwise `false` or `Error` occurred while extracting credentials
    public function authenticate(string credential) returns (boolean | auth:Error) {
        boolean isAuthenticated;
        //API authentication info
        AuthenticationContext authenticationContext = {};
        printDebug(KEY_AUTHN_FILTER, "Processing request with the Basic authentication provider");
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        string[] providerIds = [AUTHN_SCHEME_BASIC];
        //set Username from the request
        string encodedCredentials = credential;
        byte[] | error decodedCredentials = arrays:fromBase64(encodedCredentials);

        //Extract username and password from the request
        string userName;
        string password;
        if (decodedCredentials is byte[]) {
            string | error decodedCredentialsString = strings:fromBytes(decodedCredentials);
            if (decodedCredentialsString is string) {
                if (decodedCredentialsString.indexOf(":", 0) == ()) {
                    setErrorMessageToInvocationContext(API_AUTH_BASICAUTH_INVALID_FORMAT);
                    return false;
                }
                string[] decodedCred = split(decodedCredentialsString.trim(), ":");
                userName = decodedCred[0];
                printDebug(KEY_AUTHN_FILTER, "Decoded user name from the header : " + userName);
                if (decodedCred.length() < 2) {
                    setErrorMessageToInvocationContext(API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                    return false;
                }
                password = decodedCred[1];
            } else {
                printError(KEY_AUTHN_FILTER, "Error while decoding the authorization header for basic authentication");
                setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
                return false;
            }
        } else {
            printError(KEY_AUTHN_FILTER, "Error while decoding the authorization header for basic authentication");
            setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
            return false;
        }
        //Starting a new span
        int | error | () spanHash = startSpan(HASHING_MECHANISM);
        //Hashing mechanism
        string passwordFromConfig = readPassword(userName);
        string hashedPass = password;
        // this is to support backward compatibility with 3.0.x where only sha1 was supported for hashing.
        if(passwordFromConfig != "" && !passwordFromConfig.startsWith(SHA_PREFIX)) {
            hashedPass = crypto:hashSha1(password.toBytes()).toBase16();
        }
        printDebug(KEY_AUTHN_FILTER, "Hashed password value : " + hashedPass);
        string credentials = userName + ":" + hashedPass;
        string hashedRequest;
        string encodedVal = credentials.toBytes().toBase64();
        printDebug(KEY_AUTHN_FILTER, "Encoded Auth header value : " + encodedVal);
        hashedRequest = BASIC_PREFIX_WITH_SPACE + encodedVal;
        //finishing span
        finishSpan(HASHING_MECHANISM, spanHash);
        //Starting a new span
        int | error | () spanInbound = startSpan(BALLERINA_INBOUND_BASICAUTH);
        var isAuthorized = self.inboundBasicAuthProvider.authenticate(encodedVal);
        //finishing span
        finishSpan(BALLERINA_INBOUND_BASICAUTH, spanInbound);
        if (isAuthorized is boolean) {
            printDebug(KEY_AUTHN_FILTER, "Basic auth provider returned with value : " + isAuthorized.toString());
            if (!isAuthorized) {
                //TODO: Handle the error message properly
                setErrorMessageToInvocationContext(API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                //sendErrorResponse(caller, request, <@untainted> context);
                return false;
            }
            int startingTimeReq = getCurrentTimeForAnalytics();
            invocationContext.attributes[REQUEST_TIME] = startingTimeReq;
            invocationContext.attributes[FILTER_FAILED] = false;
            //Set authenticationContext data
            authenticationContext.authenticated = true;
            //Authentication context data is set to default value bacuase in basic authentication we cannot have informtaion on subscription and applications
            authenticationContext.tier = DEFAULT_SUBSCRIPTION_TIER;
            authenticationContext.applicationTier = UNLIMITED_TIER;
            authenticationContext.apiKey = ANONYMOUS_APP_ID;
            //Username is extracted from the request
            authenticationContext.username = userName;
            authenticationContext.applicationId = ANONYMOUS_APP_ID;
            authenticationContext.applicationName = ANONYMOUS_APP_NAME;
            authenticationContext.subscriber = ANONYMOUS_APP_OWNER;
            authenticationContext.consumerKey = ANONYMOUS_CONSUMER_KEY;
            authenticationContext.apiTier = UNLIMITED_TIER;
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

# Reads the password hash for a user.
#
# + username - Username
# + return - Password hash read from userstore, or nil if not found
function readPassword(string username) returns string {
    // first read the user id from user->id mapping
    // read the hashed password from the user-store file, using the user id
    return config:getAsString(CONFIG_USER_SECTION + "." + username + "." + PASSWORD, "");
}
