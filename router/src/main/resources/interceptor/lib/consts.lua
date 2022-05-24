--[[
Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

]]

-- keys of includes table
INCLUDES = {
    INV_CONTEXT = "invocationContext",
    REQ_HEADERS = "requestHeaders",
    REQ_BODY = "requestBody",
    REQ_TRAILERS = "requestTrailers",
    RESP_HEADERS = "responseHeaders",
    RESP_BODY = "responseBody",
    RESP_TRAILERS = "responseTrailers"
}

-- keys of the payload to the interceptor service
REQUEST = {
    REQ_HEADERS = "requestHeaders",
    REQ_BODY = "requestBody",
    REQ_TRAILERS = "requestTrailers",
    RESP_HEADERS = "responseHeaders",
    RESP_BODY = "responseBody",
    RESP_CODE = "responseCode",
    RESP_TRAILERS = "responseTrailers",
    INTCPT_CONTEXT = "interceptorContext",
    INV_CONTEXT = "invocationContext"
}

-- keys of the payload to the invocation context
INV_CONTEXT = {
    PROTOCOL = "protocol",
    SCHEME = "scheme",
    PATH = "path",
    METHOD = "method",
    REQ_ID = "requestId",
    SOURCE = "source",
    DESTINATION = "destination",
    ORG_ID = "organizationId",
    VHOST = "vhost",
    API_NAME = "apiName",
    API_VERSION = "apiVersion",
    AUTH_CTX = "authenticationContext"
}

-- keys of the payload to the auth context
AUTH_CTX = {
    TOKEN_TYPE = "tokenType", -- API Key|JWT Auth|Internal Key
    TOKEN = "token", -- raw token
    KEY_TYPE = "keyType" -- PRODUCTION|SANDBOX
}

-- keys of the payload from the interceptor service
RESPONSE = {
    DIRECT_RESPOND = "directRespond",
    BODY = "body",
    RESPONSE_CODE = "responseCode",
    HEADERS_TO_ADD = "headersToAdd",
    HEADERS_TO_REPLACE = "headersToReplace",
    HEADERS_TO_REMOVE = "headersToRemove",
    TRAILERS_TO_ADD = "trailersToAdd",
    TRAILERS_TO_REPLACE = "trailersToReplace",
    TRAILERS_TO_REMOVE = "trailersToRemove",
    INTCPT_CONTEXT = "interceptorContext",
    DYNAMIC_ENDPOINT = "dynamicEndpoint"
}

DYNAMIC_ENDPOINT = {
    ENDPOINT_NAME = "endpointName"
}

-- table of information shared between request and response flow
SHARED = {
    REQUEST_ID = "requestId",
}

ERROR_CODES = {
    INVALID_RESPONSE_HTTP_CODE = "103500", -- Interceptor service connect failure or invalid response status code
    DECODE_ERROR = "103501" -- Invalid encoded body from interceptor service
}

-- envoy headers
STATUS = ":status"
LUA_FILTER_NAME = "envoy.filters.http.lua"
EXT_AUTHZ_FILTER = "envoy.filters.http.ext_authz"
SHARED_INFO_META_KEY = "shared.info"
