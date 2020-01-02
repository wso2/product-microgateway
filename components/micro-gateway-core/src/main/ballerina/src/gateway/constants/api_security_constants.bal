// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/runtime;

public const string API_AUTH_FAILURE_HANDLER = "_auth_failure_handler_";
public const int API_AUTH_GENERAL_ERROR = 900900;
public const string API_AUTH_GENERAL_ERROR_MESSAGE = "Unclassified Authentication Failure";

public const string API_AUTH_INVALID_CREDENTIALS_STRING = "900901";
public const int API_AUTH_INVALID_CREDENTIALS = 900901;
public const string API_AUTH_INVALID_CREDENTIALS_MESSAGE = "Invalid Credentials";
public const string API_AUTH_INVALID_CREDENTIALS_DESCRIPTION =
"Make sure you have given the correct access token";

public const int API_AUTH_MISSING_CREDENTIALS = 900902;
public const string API_AUTH_MISSING_CREDENTIALS_MESSAGE = "Missing Credentials";
public const string API_AUTH_MISSING_CREDENTIALS_DESCRIPTION =
"Make sure your API invocation call has a header: \"Authorization: Bearer ACCESS_TOKEN\"";

public const int API_AUTH_ACCESS_TOKEN_EXPIRED = 900903;
public const string API_AUTH_ACCESS_TOKEN_EXPIRED_MESSAGE = "Access Token Expired";
public const string API_AUTH_ACCESS_TOKEN_EXPIRED_DESCRIPTION =
"Renew the access token and try again";

public const int API_AUTH_ACCESS_TOKEN_INACTIVE = 900904;
public const string API_AUTH_ACCESS_TOKEN_INACTIVE_MESSAGE = "Access Token Inactive";
public const string API_AUTH_ACCESS_TOKEN_INACTIVE_DESCRIPTION =
"Generate a new access token and try again";

public const int API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE = 900905;
public const string API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE_MESSAGE = "Incorrect Access Token Type is provided";

public const int API_AUTH_INCORRECT_API_RESOURCE = 900906;
public const string API_AUTH_INCORRECT_API_RESOURCE_MESSAGE =
"No matching resource found in the API for the given request";
public const string API_AUTH_INCORRECT_API_RESOURCE_DESCRIPTION =
"Check the API documentation and add a proper REST resource path to the invocation URL";

public const int API_BLOCKED = 900907;
public const string API_BLOCKED_MESSAGE = "The requested API is temporarily blocked";

public const int API_AUTH_FORBIDDEN = 900908;
public const string API_AUTH_FORBIDDEN_MESSAGE = "Resource forbidden ";

public const int SUBSCRIPTION_INACTIVE = 900909;
public const string SUBSCRIPTION_INACTIVE_MESSAGE = "The subscription to the API is inactive";

public const int INVALID_SCOPE = 900910;
public const string INVALID_SCOPE_MESSAGE = "The access token does not allow you to access the requested resource";

public const string API_AUTH_INVALID_BASICAUTH_CREDENTIALS_STRING = "900911";
public const int API_AUTH_INVALID_BASICAUTH_CREDENTIALS = 900911;
public const string API_AUTH_INVALID_BASICAUTH_CREDENTIALS_MESSAGE = "Invalid Credentials";
public const string API_AUTH_INVALID_BASICAUTH_CREDENTIALS_DESCRIPTION =
"Make sure you have given the correct username and password";

public const string API_AUTH_BASICAUTH_INVALID_FORMAT_STRING = "900912";
public const int API_AUTH_BASICAUTH_INVALID_FORMAT = 900912;
public const string API_AUTH_BASICAUTH_INVALID_FORMAT_STRING_MESSAGE = "Invalid Format";
public const string API_AUTH_BASICAUTH_INVALID_FORMAT_STRING_DESCRIPTION =
"Make sure you have given the credentials in correct format with \":\" character";

public const string API_AUTH_INVALID_COOKIE_STRING = "900913";
public const int API_AUTH_INVALID_COOKIE = 900913;
public const string API_AUTH_INVALID_COOKIE_STRING_MESSAGE = "Invalid Cookie is Provided";
public const string API_AUTH_INVALID_COOKIE_STRING_DESCRIPTION =
"Make sure you have given the valid cookie at the Server startup";

public const string API_AUTH_NO_COOKIE_PROVIDED_STRING = "900914";
public const int API_AUTH_NO_COOKIE_PROVIDED = 900914;
public const string API_AUTH_NO_COOKIE_PROVIDED_STRING_MESSAGE = "No Cookies are provided at Server startup.";
public const string API_AUTH_NO_COOKIE_PROVIDED_STRING_DESCRIPTION =
"Make sure you have given the authorized cookie at the server startup";

public const string API_AUTH_PROVIDER_INVALID_STRING = "900917";
public const int API_AUTH_PROVIDER_INVALID = 900917;
public const string API_AUTH_PROVIDER_INVALID_MESSAGE = "Invalid Authentication scheme";
public const string API_AUTH_PROVIDER_INVALID_DESCRIPTION = "API can not be used with this authentication scheme";

public const string DESCRIPTION_SEPARATOR = ". ";

public const int INVALID_ENTITY = 900915;
public const string INVALID_ENTITY_MESSAGE = "Unprocessable entity";

public const int INVALID_RESPONSE = 900916;
public const string INVALID_RESPONSE_MESSAGE = "Unprocessable entity";

public function getAuthenticationFailureMessage(int errorCode) returns string {
    string errorMessage;
    if (errorCode == API_AUTH_ACCESS_TOKEN_EXPIRED) {
        errorMessage = API_AUTH_ACCESS_TOKEN_EXPIRED_MESSAGE;
    } else if (errorCode == API_AUTH_ACCESS_TOKEN_INACTIVE) {
        errorMessage = API_AUTH_ACCESS_TOKEN_INACTIVE_MESSAGE;
    } else if (errorCode == API_AUTH_GENERAL_ERROR) {
        errorMessage = API_AUTH_GENERAL_ERROR_MESSAGE;
    } else if (errorCode == API_AUTH_INVALID_CREDENTIALS) {
        errorMessage = API_AUTH_INVALID_CREDENTIALS_MESSAGE;
    } else if (errorCode == API_AUTH_MISSING_CREDENTIALS) {
        errorMessage = API_AUTH_MISSING_CREDENTIALS_MESSAGE;
    } else if (errorCode == API_AUTH_INCORRECT_API_RESOURCE) {
        errorMessage = API_AUTH_INCORRECT_API_RESOURCE_MESSAGE;
    } else if (errorCode == API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE) {
        errorMessage = API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE_MESSAGE;
    } else if (errorCode == API_BLOCKED) {
        errorMessage = API_BLOCKED_MESSAGE;
    } else if (errorCode == API_AUTH_FORBIDDEN) {
        errorMessage = API_AUTH_FORBIDDEN_MESSAGE;
    } else if (errorCode == SUBSCRIPTION_INACTIVE) {
        errorMessage = SUBSCRIPTION_INACTIVE_MESSAGE;
    } else if (errorCode == INVALID_SCOPE) {
        errorMessage = INVALID_SCOPE_MESSAGE;
    } else if (errorCode == API_AUTH_INVALID_BASICAUTH_CREDENTIALS) {
        errorMessage = API_AUTH_INVALID_BASICAUTH_CREDENTIALS_MESSAGE;
    } else if (errorCode == API_AUTH_BASICAUTH_INVALID_FORMAT) {
        errorMessage = API_AUTH_BASICAUTH_INVALID_FORMAT_STRING_MESSAGE;
    } else if (errorCode == API_AUTH_INVALID_COOKIE) {
        errorMessage = API_AUTH_INVALID_COOKIE_STRING_MESSAGE;
    } else if (errorCode == API_AUTH_NO_COOKIE_PROVIDED) {
        errorMessage = API_AUTH_NO_COOKIE_PROVIDED_STRING_MESSAGE;
    } else if (errorCode == INVALID_ENTITY) {
        errorMessage = INVALID_ENTITY_MESSAGE;
    } else if (errorCode == INVALID_RESPONSE) {
        errorMessage = INVALID_RESPONSE_MESSAGE;
    } else if (errorCode == API_AUTH_PROVIDER_INVALID) {
        errorMessage = API_AUTH_PROVIDER_INVALID_MESSAGE;
    } else {
        errorMessage = API_AUTH_GENERAL_ERROR_MESSAGE;
    }
    return errorMessage;
}

public function getFailureMessageDetailDescription(int errorCode, string errorMessage) returns string {
    string errorDescription = errorMessage;
    if (API_AUTH_INCORRECT_API_RESOURCE == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_INCORRECT_API_RESOURCE_DESCRIPTION;
    } else if (API_AUTH_INCORRECT_API_RESOURCE == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_ACCESS_TOKEN_INACTIVE_DESCRIPTION;
    } else if (API_AUTH_MISSING_CREDENTIALS == errorCode) {
        string authHeaderName = getAuthorizationHeader(runtime:getInvocationContext());
        errorDescription += DESCRIPTION_SEPARATOR + "Make sure your API invocation call has a header: \"" + authHeaderName + "\"";
    } else if (API_AUTH_ACCESS_TOKEN_EXPIRED == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_ACCESS_TOKEN_EXPIRED_DESCRIPTION;
    } else if (API_AUTH_INVALID_CREDENTIALS == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_INVALID_CREDENTIALS_DESCRIPTION;
    } else if (API_AUTH_INVALID_BASICAUTH_CREDENTIALS == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_INVALID_BASICAUTH_CREDENTIALS_DESCRIPTION;
    } else if (API_AUTH_BASICAUTH_INVALID_FORMAT == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_BASICAUTH_INVALID_FORMAT_STRING_DESCRIPTION;
    } else if (API_AUTH_INVALID_COOKIE == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_INVALID_COOKIE_STRING_DESCRIPTION;
    } else if (API_AUTH_NO_COOKIE_PROVIDED == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_NO_COOKIE_PROVIDED_STRING_DESCRIPTION;
    } else if (API_AUTH_PROVIDER_INVALID == errorCode) {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_PROVIDER_INVALID_DESCRIPTION;
    } else {
        errorDescription += DESCRIPTION_SEPARATOR + API_AUTH_GENERAL_ERROR_MESSAGE;
    }
    return errorDescription;
}

