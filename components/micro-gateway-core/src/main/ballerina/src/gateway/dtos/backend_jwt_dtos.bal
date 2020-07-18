// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# This represents a single key-value pair returned from the APIM.
# + uri - claim
# + value - value
public type ClaimDTO record {|
    string uri;
    string value;
|};

# This represents the DTO which is mapped with the result received from user specific claim retrieval process.
# + count - number of claims
# + list - claims list
public type RetrievedUserClaimsListDTO record {|
    int count;
    ClaimDTO[] list = [];
|};

# This DTO is used to pass the Claims to JWT generation (preserving ballerina jwt payload structure where
# the self contained access token structure), when there is no self contained token is involved.
# + iss - token issuer
# + sub - subscription claim
# + customClaims - custom claims
public type ClaimsMapDTO record {
    string iss = "";
    string sub = "";
    CustomClaimsMapDTO customClaims = {};
};

# This DTO is used to pass specifically the customClaims (preserving ballerina jwt payload structure where there
# is self contained access token), when there is no self contained token is involved.
# + application - application claim
public type CustomClaimsMapDTO record {
    ApplicationClaimsMapDTO application = {};
};

# This DTO is used to pass the claims related to application, when there is no self contained token is involved.
# + id - application id
# + owner - application owner
# + name - application name
# + tier - application tier
public type ApplicationClaimsMapDTO record {|
    string id = "";
    string owner = "";
    string name = "";
    string tier = "";
|};

# This DTO is to pass the required information for user specific claim retrieval process.
# + issuer - issuer
# + token - token
# + client_id - client ID
# + username - username
# + token_type - token type ('bearer jwt' or 'bearer opaque')
# + customClaims - customClaims of the JWT or the information received from introspection response.
public type UserClaimRetrieverContextDTO record {|
    string issuer = "";
    string token = "";
    string client_id = "";
    string username = "";
    string token_type = "";
    map<any> customClaims = {};
|};

# This DTO is used to pass the information required for the Jwt Info Map used in JWT generation implementation.
# Payload property should remain null, if the user is authenticated via Opaque Token.
# + issuer - Token issuer
# + remoteUserClaimRetrievalEnabled - `true` If remote User Claim Retrieval is enabled.
# + payload - decoded payload if the user is authenticated from a JWT token, else keep it as null.
public type BackendJWTGenUserContextDTO record {|
    string issuer;
    boolean remoteUserClaimRetrievalEnabled = false;
    jwt:JwtPayload? payload = ();
|};
