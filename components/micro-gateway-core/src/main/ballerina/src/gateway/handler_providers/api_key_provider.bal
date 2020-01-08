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
            runtime:InvocationContext invocationContext = runtime:getInvocationContext();
            runtime:AuthenticationContext? authContext = invocationContext?.authenticationContext;             
            if (authContext is runtime:AuthenticationContext) {
                authContext.scheme = AUTH_SCHEME_API_KEY;
                validated = validateAPIKey(credential);
            }
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
