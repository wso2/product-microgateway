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

boolean enableBasicAuthStrictValidation = getConfigBooleanValue(BASIC_AUTH_CONFIG, ENABLE_BASIC_AUTH_STRICT_VALIDATION, false);

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
        byte[] | error decodedCredentials = arrays:fromBase64(credential);

        //Extract username and password from the request
        byte[] userName = [];
        byte[] password = [];
        string userNameAsString = "";
        if (decodedCredentials is byte[]) {
            int colonIndex = indexOfColon(decodedCredentials);
            if (colonIndex < 0) {
                setErrorMessageToInvocationContext(API_AUTH_BASICAUTH_INVALID_FORMAT);
                return false;
            }

            userName = extractCredentials(decodedCredentials, 0, colonIndex);
            string | error userNameString = strings:fromBytes(userName);
            if (userNameString is string) {
                userNameAsString = userNameString;
                printDebug(KEY_AUTHN_FILTER, "Decoded user name from the header : " + userNameAsString);
            }
            password = extractCredentials(decodedCredentials, colonIndex + 1, decodedCredentials.length());

            if (password.length() == 0) {
                setErrorMessageToInvocationContext(API_AUTH_INVALID_BASICAUTH_CREDENTIALS);
                return false;
            }
        } else if (enableBasicAuthStrictValidation) {
            printError(KEY_AUTHN_FILTER, "Invalid authorization header for basic authentication");
            setErrorMessageToInvocationContext(API_AUTH_MALFORMED_TOKEN);
            return false;
        } else {
            printError(KEY_AUTHN_FILTER, "Error while decoding the authorization header for basic authentication");
            setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
            return false;
        }
        //Starting a new span
        int | error | () spanHash = startSpan(HASHING_MECHANISM);
        //Hashing mechanism
        string hashedPasswordFromConfig = readPassword(userNameAsString);
        byte[] hashedPass = password;
        // this is to support backward compatibility with 3.0.x where only sha1 was supported for hashing.
        if(hashedPasswordFromConfig != "" && !hashedPasswordFromConfig.startsWith(SHA_PREFIX)) {
            hashedPass = crypto:hashSha1(password).toBase16().toBytes();
            string|error hashedPassStr = strings:fromBytes(hashedPass);
            if (hashedPassStr is string) {
                //password is logged only when it is present as a hash value
                printDebug(KEY_AUTHN_FILTER, "Hashed password value : " + mask(hashedPassStr));
            }
        }

        byte[] credentials = [];
        credentials.push(...userName);
        credentials.push(58);
        credentials.push(...hashedPass);
        string hashedRequest;
        string encodedVal = credentials.toBase64();
        printDebug(KEY_AUTHN_FILTER, "Encoded Auth header value : " + encodedVal);
        hashedRequest = BASIC_PREFIX_WITH_SPACE + encodedVal;
        //finishing span
        finishSpan(HASHING_MECHANISM, spanHash);
        //Starting a new span
        int | error | () spanInbound = startSpan(BALLERINA_INBOUND_BASICAUTH);
        var isAuthorized = self.inboundBasicAuthProvider.authenticate(encodedVal);
        //finishing span
        finishSpan(BALLERINA_INBOUND_BASICAUTH, spanInbound);
        resetCredentials(password);
        resetCredentials(hashedPass);
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
            authenticationContext.username = userNameAsString;
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

# Finds the index of : in a basic auth header
#
# + authHeader - Basic Auth Header Value
# + return - index of ':' character
function indexOfColon(byte[] authHeader) returns int {
    // Iterate through the byte array
    int index = 0;
    foreach byte b in authHeader {
        if (b == 58) {
            return index;
        }
        index = index + 1;
    }
    // Colon not found
    return -1;
}

# Extracts username and password
#
# + authHeader - Basic Auth Header Value
# + startIndex - start index
# + endIndex - end index
# + return - extracted username or password
function extractCredentials(byte[] authHeader, int startIndex, int endIndex) returns byte[] {
    int i = startIndex;
    byte[] credential = [];
    while i < endIndex {
        credential[i - startIndex] = authHeader[i];
        i = i + 1;
    }
    return credential;
}

# Resets byte[] fields to all '0'
#
# + credentials - byte[] to be zeroed
function resetCredentials(byte[] credentials) {
    int size = credentials.length();
    int i = 0;
    while (i < size) {
        credentials[i] = 0;
        i = i + 1;
    }
}
