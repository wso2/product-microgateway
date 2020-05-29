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

import ballerina/http;
import ballerina/runtime;
import ballerina/stringutils;

# Representation of the mutual ssl handler.
# + gatewayCache - the `APIGatewayCache instence`
# + apiCertificateList - Api Certificate List
# + headerName - Header name append by Loadbalncer
# + isClientCertificateValidationEnabled - Is client certificateValidation enabled
public type MutualSSLHandler object {
    *http:InboundAuthHandler;
    public APIGatewayCache gatewayCache = new;
    public map<anydata>[] | error apiCertificateList;
    public string headerName;
    public boolean isClientCertificateValidationEnabled;

    # Mutual SSL handler.
    # + apiCertificateList - Api Certificate List
    # + headerName - Header name append by Loadbalncer
    # + isClientCertificateValidationEnabled - Is client certificateValidation enabled
    public function __init(map<anydata>[] | error apiCertificateList, string headerName, boolean isClientCertificateValidationEnabled) {
        self.apiCertificateList = apiCertificateList;
        self.headerName = headerName;
        self.isClientCertificateValidationEnabled = isClientCertificateValidationEnabled;
     }

    # Checks if the request can be authenticated with the Bearer Auth header.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if can be authenticated. Else, returns `false`.
    public function canProcess(http:Request req) returns @tainted boolean {
        return true;
    }

    # Authenticates the incoming request knowing that mutual ssl has happened at the trasnport layer.
    #
    # + req - The `Request` instance.
    # + return - Returns `true` if authenticated successfully. Else, returns `false`
    # or the `AuthenticationError` in case of an error.
    public function process(http:Request req) returns boolean | http:AuthenticationError {
        string|error mutualSSLVerifyClient = getMutualSSL();
        if (mutualSSLVerifyClient is string && stringutils:equalsIgnoreCase(MANDATORY, mutualSSLVerifyClient) 
                && req.mutualSslHandshake[STATUS] != PASSED) {
            if (req.mutualSslHandshake[STATUS] == FAILED) {
                printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: FAILED");
            }
            // provided more generic error code to avoid security issues.
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS); 
            return prepareAuthenticationError("Failed to authenticate with MutualSSL handler");            
        }
        if (req.mutualSslHandshake[STATUS] == PASSED) {
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            if (mutualSSLVerifyClient is string && stringutils:equalsIgnoreCase(MANDATORY, mutualSSLVerifyClient)) {
                string apiVersion = invocationContext.attributes[API_VERSION_PROPERTY].toString();
                string apiName = invocationContext.attributes[API_NAME].toString();
                if (self.headerName != "" &&  req.hasHeader(self.headerName)) {
                    if (!self.isClientCertificateValidationEnabled) {
                        string headerValue = req.getHeader(self.headerName);
                        if (headerValue != "") {
                            var cacheKey = headerValue + apiName + apiVersion;
                            var isExistCertCache = self.gatewayCache.retrieveFromMutualSslCertificateCache(cacheKey);
                            if (isExistCertCache is boolean) {
                                if (!isExistCertCache) {
                                    printDebug(KEY_AUTHN_FILTER,"Mutual SSL authentication failure. " +
                                    "API is not associated with the certificate");
                                    setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                                    return false;
                                } else {
                                    printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: PASSED");
                                    doMTSLFilterRequest(req, invocationContext);
                                    return true;
                                }
                            } else {
                                handle|error aliasAFromHeaderCert = getAliasAFromHeaderCert(headerValue);
                                if (aliasAFromHeaderCert is error) {
                                    setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
                                    return prepareAuthenticationError("Unclassified Authentication Failure");
                                }
                                if (aliasAFromHeaderCert is handle) {
                                    boolean isExistAlias = isExistApiAlias(apiVersion, apiName, aliasAFromHeaderCert.toString(),
                                    self.apiCertificateList);
                                    if (!isExistAlias || aliasAFromHeaderCert.toString() == "") {
                                        printDebug(KEY_AUTHN_FILTER, "Mutual SSL authentication failure. API is not associated " +
                                        "with the certificate");
                                        self.gatewayCache.addMutualSslCertificateCache(cacheKey, false);
                                        setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                                        return false;
                                    } else {
                                        printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: PASSED");
                                        doMTSLFilterRequest(req, invocationContext);
                                        self.gatewayCache.addMutualSslCertificateCache(cacheKey, true);
                                        return true;
                                    }
                                }

                            }
                        } else {
                            printDebug(KEY_AUTHN_FILTER, "Header has empty value sent by the payload");
                            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                            return false;
                        }
                    }
                }
                string? cert = req.mutualSslHandshake["base64EncodedCert"];
                var cacheKey = cert.toString() + apiName + apiVersion;
                var isExistCertCache = self.gatewayCache.retrieveFromMutualSslCertificateCache(cacheKey);
                if (isExistCertCache is boolean) {
                    if (!isExistCertCache) {
                        printDebug(KEY_AUTHN_FILTER,"Mutual SSL authentication failure. " +
                        "API is not associated with the certificate");
                        return false;
                     }
                } else {
                    handle|error certificateAlias = getAlias(cert.toString());
                    if (certificateAlias is error) {
                        setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
                        return prepareAuthenticationError("Unclassified Authentication Failure");
                    }
                    if (certificateAlias is handle ) {
                        boolean isExistAlias = isExistApiAlias(apiVersion, apiName, certificateAlias.toString(),
                        self.apiCertificateList);
                        if (!isExistAlias || certificateAlias.toString() == "") {
                            printDebug(KEY_AUTHN_FILTER, "Mutual SSL authentication failure. API is not associated " +
                            "with the certificate");
                            self.gatewayCache.addMutualSslCertificateCache(cacheKey, false);
                            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                            return false;
                        }
                    }
                    self.gatewayCache.addMutualSslCertificateCache(cacheKey, true);
                }
            }
            printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: PASSED");
            doMTSLFilterRequest(req, invocationContext);
        }
        return true;
    }
};

function doMTSLFilterRequest(http:Request request, runtime:InvocationContext context) {
    runtime:InvocationContext invocationContext = runtime:getInvocationContext();
    AuthenticationContext authenticationContext = {};
    printDebug(KEY_AUTHN_FILTER, "Processing request via MutualSSL filter.");
    context.attributes[IS_SECURED] = true;
    int startingTime = getCurrentTimeForAnalytics();
    context.attributes[REQUEST_TIME] = startingTime;
    context.attributes[FILTER_FAILED] = false;
    //Set authenticationContext data
    authenticationContext.authenticated = true;
    authenticationContext.username = USER_NAME_UNKNOWN;
    invocationContext.attributes[KEY_TYPE_ATTR] = authenticationContext.keyType;
    context.attributes[AUTHENTICATION_CONTEXT] = authenticationContext;
}

public function isExistApiAlias(string apiVersionFromRequest, string apiNameFromRequest, string certAliasFromRequest,
    map<anydata>[] | error apiCertificateList) returns boolean {
    if (apiCertificateList is map<anydata>[] && apiCertificateList.length() > 0) {
        foreach map<anydata> apiCertificate in apiCertificateList {
            anydata apiName = apiCertificate[NAME];
            anydata apiVersion = apiCertificate[VERSION];
            string aliasList = apiCertificate[ALIAS_LIST].toString();
            string[] aliasListResult = stringutils:split(aliasList, " ");
            int? index = aliasListResult.indexOf(certAliasFromRequest);
            if (apiName is string && apiVersion is string && stringutils:equalsIgnoreCase(apiName, apiNameFromRequest) &&
                index is int && stringutils:equalsIgnoreCase(apiVersion, apiVersionFromRequest)) {
                printDebug("KEY_AUTHN_FILTER", "Mutual SSL authentication is successful. Certfiacate alias correctly " +
                "validated against per API");
                return true;
            }
        }
    }
    return false;
}
