// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


import ballerina/log;
import ballerina/auth;
import ballerina/http;

documentation {
    Representation of Authentication handler chain. Originally taken from ballerina/http package as the ballarina
    version 0.981.1 http:AuthnHandlerChain does not validate against all the AuthHandlers in the AuthHandlerRegistry.
    This implementation is based on the original fix for this issue.
    See https://github.com/ballerina-platform/ballerina-lang/pull/11139
    Once the Ballerina version is updated to 0.983.0 or above, we can reuse the http:AuthnHandlerChain

    F{{authHandlerRegistry}} `AuthHandlerRegistry` instance
}
public type AuthnHandlerChain object {
    private http:AuthHandlerRegistry authHandlerRegistry;

    public new (authHandlerRegistry) {
    }

    documentation {
        Tries to authenticate against any one of the available authentication handlers

        P{{req}} `Request` instance
        R{{}} true if authenticated successfully, else false
    }
    public function handle (http:Request req) returns (boolean);

    documentation {
        Tries to authenticate against a specifc sub set of the authentication handlers, using the given array of auth provider ids

        P{{authProviderIds}} array of auth provider ids
        P{{req}} `Request` instance
        R{{}} true if authenticated successfully, else false
    }
    public function handleWithSpecificAuthnHandlers (string[] authProviderIds, http:Request req) returns (boolean);
};

function AuthnHandlerChain::handle (http:Request req) returns (boolean) {
    foreach currentAuthProviderType, currentAuthHandler in self.authHandlerRegistry.getAll() {
        var authnHandler = <http:HttpAuthnHandler> currentAuthHandler;
        if (authnHandler.canHandle(req)) {
            log:printDebug("Trying to authenticate with the auth provider: " + currentAuthProviderType);
            boolean authnSuccessful = authnHandler.handle(req);
            if (authnSuccessful) {
                return true;
            }
        }
    }
    return false;
}

function AuthnHandlerChain::handleWithSpecificAuthnHandlers (string[] authProviderIds, http:Request req)
                                returns (boolean) {
    foreach authProviderId in authProviderIds {
        match self.authHandlerRegistry.get(authProviderId) {
            http:HttpAuthnHandler authnHandler => {
                if (authnHandler.canHandle(req)) {
                    log:printDebug("Trying to authenticate with the auth provider: " + authProviderId);
                    boolean authnSuccessful = authnHandler.handle(req);
                    if (authnSuccessful) {
                        return true;
                    }
                }
            }
            () => {
                // nothing to do
            }
        }
    }
    return false;
}
