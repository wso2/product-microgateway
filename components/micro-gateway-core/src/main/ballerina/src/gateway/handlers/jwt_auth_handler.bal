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

import ballerina/http;
import ballerina/runtime;

# Representation of the jwt self validating handler
#
# + jwtAuthProvider - The reference to the jwt auth provider instance
# + remoteUserClaimRetrievalEnabled - true if remoteUserClaimRetrieval is enabled for the issuer
public type JWTAuthHandler object {

    *http:InboundAuthHandler;

    public JwtAuthProvider jwtAuthProvider;

    //todo(VirajSalaka): remove the redundant config reading at both handlers.
    private boolean enabledJWTGenerator = false;
    private boolean classLoaded = false;
    private int skewTime = 0;
    private boolean enabledCaching = false;
    private boolean remoteUserClaimRetrievalEnabled = false;

    public function __init(JwtAuthProvider jwtAuthProvider, boolean remoteUserClaimRetrievalEnabled) {
        self.jwtAuthProvider = jwtAuthProvider;
        GatewayConf gatewayConf = getGatewayConfInstance();
        // initiating generator class if enabled
        self.enabledJWTGenerator = gatewayConf.jwtGeneratorConfig.jwtGeneratorEnabled;
        if (self.enabledJWTGenerator) {
            // provide backward compatibility for skew time
            self.skewTime = getConfigIntValue(SERVER_CONF_ID, 
                                                SERVER_TIMESTAMP_SKEW, 
                                                DEFAULT_SERVER_TIMESTAMP_SKEW);
            if (self.skewTime == DEFAULT_SERVER_TIMESTAMP_SKEW) {
                self.skewTime = gatewayConf.getKeyManagerConf().timestampSkew;
            }
            self.enabledCaching = gatewayConf.jwtGeneratorConfig.jwtGeneratorCaching.tokenCacheEnable;
            self.classLoaded = jwtGeneratorClassLoaded;
            self.remoteUserClaimRetrievalEnabled = remoteUserClaimRetrievalEnabled;
        }
    }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        string authHeader = runtime:getInvocationContext().attributes[AUTH_HEADER].toString();
        if (req.hasHeader(authHeader)) {
            string headerValue = req.getHeader(authHeader).toLowerAscii();
            if (headerValue.startsWith(AUTH_SCHEME_BEARER_LOWERCASE)) {
                string credential = headerValue.substring(6, headerValue.length()).trim();
                string[] splitContent = split(credential, "\\.");
                if (splitContent.length() == 3) {
                    printDebug(KEY_AUTHN_FILTER, "Request will authenticated via jwt handler");
                    return true;
                }
            }
        }
        return false;
    }

    # Authenticates the incoming request with the use of credentials passed as the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns @tainted boolean | http:AuthenticationError {
        runtime:InvocationContext invocationContext = runtime:getInvocationContext();
        string authHeader = invocationContext.attributes[AUTH_HEADER].toString();
        string headerValue = req.getHeader(authHeader);
        string credential = headerValue.substring(6, headerValue.length()).trim();
        var authenticationResult = self.jwtAuthProvider.authenticate(credential);
        if (authenticationResult is boolean) {
            string issuer = self.jwtAuthProvider.jwtValidatorConfig?.issuer ?: DEFAULT_JWT_ISSUER;
            boolean backendJWTfromClaim = setBackendJwtHeader(credential, req, issuer);
            if (!backendJWTfromClaim) {
                boolean generationStatus = generateAndSetBackendJwtHeader(credential,
                                                                            req,
                                                                            self.enabledJWTGenerator,
                                                                            self.classLoaded,
                                                                            self.skewTime,
                                                                            self.enabledCaching,
                                                                            issuer,
                                                                            self.remoteUserClaimRetrievalEnabled,
                                                                            true);
                if (!generationStatus) {
                    printError(KEY_JWT_AUTH_PROVIDER, "JWT Generation failed");
                }
                return authenticationResult;
            } else {
                printDebug(KEY_JWT_AUTH_PROVIDER, "JWT is set from the payload claim");
                return true;
            }
        } else {
            return prepareAuthenticationError("Failed to authenticate with jwt bearer auth handler.", authenticationResult);
        }
    }

};

# Check whether backendJwt claim is in the payload and set the header if avaialable.
#
# + credential - Credential
# + req - The `Request` instance.
# + issuer - The jwt issuer who issued the token and comes in the iss claim.
# + return - Returns boolean based on backend jwt setting.
public function setBackendJwtHeader(string credential, http:Request req, string? issuer) returns @tainted boolean {
    runtime:Principal? principal = runtime:getInvocationContext()?.principal;
    if (principal is runtime:Principal) {
        map<any>? customClaims = principal?.claims;
        // validate backend jwt claim and set it to jwt header
        if (customClaims is map<any> && customClaims.hasKey(BACKEND_JWT)) {
            printDebug(KEY_JWT_AUTH_PROVIDER, "Set backend jwt header from payload claim.");
            req.setHeader(jwtheaderName, customClaims.get(BACKEND_JWT).toString());
            return true;
        }
    }
    return false;
}
