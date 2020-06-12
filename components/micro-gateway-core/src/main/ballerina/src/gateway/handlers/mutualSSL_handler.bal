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
                printError(KEY_AUTHN_FILTER, "MutualSSL handshake status: FAILED");
            }
            // provided more generic error code to avoid security issues.
            setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS); 
            return prepareAuthenticationError("Failed to authenticate with MutualSSL handler");            
        }
        if (req.mutualSslHandshake[STATUS] == PASSED) {
            boolean | http:AuthenticationError mutualSSLStatus = false;
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            if (mutualSSLVerifyClient is string && stringutils:equalsIgnoreCase(MANDATORY, mutualSSLVerifyClient)) {
                string apiVersion = invocationContext.attributes[API_VERSION_PROPERTY].toString();
                string apiName = invocationContext.attributes[API_NAME].toString();
                if (self.headerName != "" &&  req.hasHeader(self.headerName)) {
                    printDebug(KEY_AUTHN_FILTER, "Mutual ssl expected header " + self.headerName + " present in the request");
                    //If certificate header is present and if validation is disabled for client certificate present
                    //in the context (i.e. 'isClientCertificateValidationEnabled' is false), then we should always
                    //validate the certificate present in the header.
                    //This scenario represents where mtls is required between client and mgw, but mtls is not enabled
                    //between LB and mgw. So microgateway only validates the certificate present in the header, which
                    //is the client certificate not the certificate in the context which is always going to be
                    //LB certificate, but will not be present in the context due to no MTLS between mgw and LB.
                    if (!self.isClientCertificateValidationEnabled) {
                        mutualSSLStatus = self.checkCertificatePresentInHeader(req, apiName, apiVersion);
                    } else { // if client certificate validation enabled for the certificate present in context
                        //((i.e. 'isClientCertificateValidationEnabled' is true))and header is also present then both
                        //should be validated.
                        //This is the scenario where both client certificate is also should be verified and mtls is
                        //also enabled between mgw and LB. So both client certificate present in the header should be
                        //validated and the LB certificate present in the context.
                        // When validating the certificate in the context we do not need to validate it with the alias
                        //list present in the config as this would always be the LB certificate.
                        mutualSSLStatus =  self.checkCertificatePresentInContext(req, apiName, apiVersion, false);
                        if (mutualSSLStatus is boolean && mutualSSLStatus) {
                            mutualSSLStatus = self.checkCertificatePresentInHeader(req, apiName, apiVersion);
                        }
                    }
                } else {
                //If certificate not in the header, then irrespective of the config
                //'isClientCertificateValidationEnabled' validating  the certificate in request context is mandatory.
                //(This case is when there is no LB fronted.)
                //And also cert should be validated against with the API alias list. This is because the certificate
                //available via the context would be the client certificate,not the LB one. Hence the
                //'isValidateCertificateWithAPI' value is set as true.
                    mutualSSLStatus =  self.checkCertificatePresentInContext(req, apiName, apiVersion, true);
                }
                if (mutualSSLStatus is boolean && mutualSSLStatus) {
                    printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status: PASSED");
                    doMTSLFilterRequest(req, invocationContext);
                } else {
                    return mutualSSLStatus;
                }
            }
        }
        return true;
    }

    function checkCertificatePresentInContext(http:Request req, string apiName, string apiVersion,
                    boolean isValidateCertificateWithAPI) returns boolean | http:AuthenticationError {
        printDebug(KEY_AUTHN_FILTER, "Checking the certificate present in the request context.");
        string? cert = req.mutualSslHandshake["base64EncodedCert"];
        var cacheKey = cert.toString() + apiName + apiVersion;
        var isExistCertCache = self.gatewayCache.retrieveFromMutualSslCertificateCache(cacheKey);
        if (isExistCertCache is boolean)    {
            if (!isExistCertCache) {
                printError(KEY_AUTHN_FILTER,"Mutual SSL authentication failure. " +
                "Certificate validity returned as false from cache. This is due to either certificat missing in trust" +
                " store or certificate alias is missing in the config, where apis are mapped with list of aliases.");
                return false;
            }
            return true;
        } else {
            handle|error certificateAlias = getAliasFromRequest(cert.toString());
            if (certificateAlias is error) {
                setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
                return prepareAuthenticationError("Unclassified Authentication Failure");
            }
            if (certificateAlias is handle) {
                //Only if 'isValidateCertificateWithAPI' true then certificate should be cross checked with the
                //alias list provided in the config. Otherwise no need to cross check with the alias list
                //in micro-gw.conf.
                boolean isExistAlias = (isValidateCertificateWithAPI) ? isExistApiAlias(apiVersion, apiName,
                                                        certificateAlias.toString(),self.apiCertificateList) : true;
                if (!isExistAlias || certificateAlias.toString() == "") {
                    if (!isExistAlias) {
                        printError(KEY_AUTHN_FILTER, "Mutual SSL authentication failure. API is not associated " +
                    "with the certificate");
                    } else if (certificateAlias.toString() == "") {
                        printError(KEY_AUTHN_FILTER, "Mutual SSL authentication failure. Certificate alias not " +
                        "found in the trust store");
                    }
                    self.gatewayCache.addMutualSslCertificateCache(cacheKey, false);
                    setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                    return false;
                }
            }
            self.gatewayCache.addMutualSslCertificateCache(cacheKey, true);
            printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status using request context: PASSED ");
            return true;
        }
    }

    function checkCertificatePresentInHeader(http:Request req, string apiName, string apiVersion) returns boolean | http:AuthenticationError {
        printDebug(KEY_AUTHN_FILTER, "Checking the certificate present in the request header.");
        string headerValue = req.getHeader(self.headerName);
        if (headerValue != "") {
            var cacheKey = headerValue + apiName + apiVersion;
            var isExistCertCache = self.gatewayCache.retrieveFromMutualSslCertificateCache(cacheKey);
            if (isExistCertCache is boolean) {
                if (!isExistCertCache) {
                    printError(KEY_AUTHN_FILTER,"Mutual SSL authentication failure. " +
                    "Certificate validity returned as false from cache. This is due to either certificat missing in " +
                    "trust store or certificate alias is missing in the config, where apis are mapped with list " +
                    "of aliases.");
                    setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                    return false;
                } else {
                    printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status using request header: PASSED");
                    return true;
                }
            } else {
                handle|error aliasFromHeaderCert = getAliasFromHeaderCert(headerValue);
                if (aliasFromHeaderCert is error) {
                    setErrorMessageToInvocationContext(API_AUTH_GENERAL_ERROR);
                    return prepareAuthenticationError("Unclassified Authentication Failure");
                } else {
                    boolean isExistAlias = isExistApiAlias(apiVersion, apiName, aliasFromHeaderCert.toString(),
                    self.apiCertificateList);
                    if (!isExistAlias || aliasFromHeaderCert.toString() == "") {
                        if (!isExistAlias) {
                            printError(KEY_AUTHN_FILTER, "Mutual SSL authentication failure. API is not associated " +
                            "with the certificate");
                        } else if (aliasFromHeaderCert.toString() == "") {
                            printError(KEY_AUTHN_FILTER, "Mutual SSL authentication failure. Certificate alias not " +
                            "found in the trust store");
                        }
                        self.gatewayCache.addMutualSslCertificateCache(cacheKey, false);
                        setErrorMessageToInvocationContext(API_AUTH_INVALID_CREDENTIALS);
                        return false;
                    } else {
                        printDebug(KEY_AUTHN_FILTER, "MutualSSL handshake status using request header: PASSED");
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
                printDebug(KEY_AUTHN_FILTER, "Matching alias found from the list : " + aliasListResult[index]);
                printDebug(KEY_AUTHN_FILTER, "Mutual SSL authentication is successful. Certfiacate alias correctly " +
                "validated against per API");
                return true;
            }
        }
    }
    return false;
}
