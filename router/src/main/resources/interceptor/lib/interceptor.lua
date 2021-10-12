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

--- interceptor module
-- @module interceptor
local interceptor = {}

local json = require 'home.wso2.interceptor.lib.json'
-- json library: https://github.com/rxi/json.lua
local base64 = require 'home.wso2.interceptor.lib.base64'
-- base64 library: https://github.com/iskolbin/lbase64

-- keys of includes table
local INCLUDES = {
    HEADERS = "headers",
    BODY = "body",
    TRAILERS = "trailers"
}
local HEADERS = "headers"
local BODY = "body"
local TRAILERS = "trailers"

-- keys of the payload to the interceptor service
local REQUEST = {
    INTCPT_CONTEXT = "interceptorContext",
    REQ_HEADERS = "requestHeaders",
    REQ_BODY = "requestBody",
    REQ_TRAILERS = "requestTrailers",
    RESP_HEADERS = "responseHeaders",
    RESP_BODY = "responseBody",
    RESP_TRAILERS = "responseTrailers"
}
local INTCPT_CONTEXT = "interceptorContext"
local REQ_HEADERS = "requestHeaders"
local REQ_BODY = "requestBody"
local REQ_TRAILERS = "requestTrailers"
local RESP_HEADERS = "responseHeaders"
local RESP_BODY = "responseBody"
local RESP_TRAILERS = "responseTrailers"

-- keys of the payload from the interceptor service
local DIRECT_RESPOND = "directRespond"
local HEADERS_TO_ADD = "headersToAdd"
local HEADERS_TO_REPLACE = "headersToReplace"
local HEADERS_TO_REMOVE = "headersToRemove"

-- table of information shared between request and response flow
local shared_info = {}
local REQUEST_ID = "requestId"
local CONTINUE_RESPONSE_PATH = "continueResponsePath"

-- envoy headers
local STATUS = ":status"

local function modify_headers(handle, interceptor_response_body)
    -- priority: headersToRemove, headersToReplace, headersToAdd
    if interceptor_response_body[HEADERS_TO_ADD] ~= nil then
        for key, val in pairs(interceptor_response_body[HEADERS_TO_ADD]) do
            handle:headers():add(key, val)
        end
    end
    if interceptor_response_body[HEADERS_TO_REPLACE] ~= nil then
        for key, val in pairs(interceptor_response_body[HEADERS_TO_REPLACE]) do
            handle:headers():replace(key, val)
        end
    end
    if interceptor_response_body[HEADERS_TO_REMOVE] ~= nil then
        for _, key in ipairs(interceptor_response_body[HEADERS_TO_REMOVE]) do
            handle:headers():remove(key)
        end
    end
end

local function modify_body(handle, interceptor_response_body, request_id)
    -- if "body" is not defined or null (i.e. {} or {"body": null}) do not update the body
    if interceptor_response_body.body ~= nil then
        handle:logDebug("Updating body for the request_id: " .. request_id)
        local content_length = handle:body():setBytes(base64.decode(interceptor_response_body.body))
        handle:headers():replace("content-length", content_length)
    end
end

local function direct_respond(handle, headers, body)
    shared_info[DIRECT_RESPOND] = true
    handle:respond(
        headers,
        body
    )
end

--- check for errors from interceptor response and handle error with responding to client
---@param handle any
---@param headers any
---@param body_str any
---@param is_request_flow any
---@return boolean returns true if there are errors
local function check_interceptor_call_errors(handle, headers, body_str, is_request_flow)
    if headers[STATUS] == "200" then -- TODO: (renuka) we only check 200
        return false
    end

    -- TODO: (renuka) check the error code and should we send the response from interceptor
    handle:logErr('Invalid interceptor service response, HTTP status_code: "' .. headers[STATUS] .. '", message: "' .. body_str .. '"')

    local message = ""
    local description = ""
    local err_code = "000" -- TODO: check error code
    local resp_headers = {[STATUS] = "500"} -- TODO: check error status code
    local resp_body = '{"error_message": "' .. message ..'", "error_description": "' .. description .. '", "code": "' .. err_code .. '", ""}'
    
    if is_request_flow then
        direct_respond(
            handle,
            resp_headers,
            resp_body
        )
        return true
    end

    modify_body(handle, {body = resp_body}, shared_info[REQUEST_ID])
    return true
end

-- TODO: (renuka) check what to log as trace and debug

--- send an HTTP request to the interceptor
-- @param handle request/response handler object
-- @param interceptor_request_body a table of request body for the interceptor service
-- @param intercept_service a table of connection details for the interceptor service
-- @return a table of respose headers
-- @return a string of response body
local function send_http_call(handle, interceptor_request_body, intercept_service)
    local headers, interceptor_response_body_str = handle:httpCall(
        intercept_service["cluster_name"],
        {
            [":method"] = "POST",
            [":path"] = intercept_service["resource_path"],
            [":authority"] = "cc-interceptor",
            ["content-type"] = "application/json",
            ["accept"] = "application/json",
        },
        json.encode(interceptor_request_body),
        intercept_service["timeout"],
        false -- async false, wait for response from interceptor service
    )

    return headers, interceptor_response_body_str
end

local function update_request_body(includes, include_key, body, body_key)
    if includes[include_key] then
        body[body_key] = shared_info[body_key]
    end
end

local function include_request_info(req_includes, body)
    update_request_body(req_includes, HEADERS, body, REQ_HEADERS)
    update_request_body(req_includes, BODY, body, REQ_BODY)
    update_request_body(req_includes, TRAILERS, body, REQ_TRAILERS)
end

local function handle_direct_respond(handle, interceptor_response_body, request_id)
    if interceptor_response_body[DIRECT_RESPOND] then
        handle:logDebug("Directly responding without calling the backend for request_id: " .. request_id)
        local headers = interceptor_response_body[HEADERS_TO_ADD] or {}
        headers[STATUS] = headers[STATUS] or "200" -- TODO: (renuka) check default status code if direct respond
        direct_respond(handle, headers, base64.decode(interceptor_response_body.body))
    end
end

local function update_interceptor_context(interceptor_response_body)
    shared_info[REQUEST.INTCPT_CONTEXT] = interceptor_response_body[REQUEST.INTCPT_CONTEXT] or {}
end

--- interceptor handler
-- @param request_handle request handle object
-- @param intercept_service a table of connection details for the interceptor service
-- @param req_includes a table which describes what should be included in the request to the iterceptor service in the request flow
-- @param resp_includes a table which describes what should be included in the request to the iterceptor service in the response flow
function interceptor.handle_request_interceptor(request_handle, intercept_service, req_includes, resp_includes)
    request_handle:logInfo("read metadata2")
    -- local proto = request_handle:streamInfo():protocol()
    -- request_handle:logInfo("PROTOCOL: " .. proto)
    -- request_handle:logInfo("downstreamLocalAddress: " .. request_handle:streamInfo():downstreamLocalAddress())
    -- request_handle:logInfo("downstreamDirectRemoteAddress: " .. request_handle:streamInfo():downstreamDirectRemoteAddress())
    -- request_handle:logInfo("requestedServerName: " .. request_handle:streamInfo():requestedServerName())
    -- local meta = request_handle:streamInfo():dynamicMetadata():get("envoy.filters.http.router")
    -- for key, value in pairs(meta) do
    --     request_handle:logInfo("key: " .. key .. " -> value: " .. value)
    -- end

    -- read headers and the payload

    local request_headers = request_handle:headers()
    local request_id = request_headers:get("x-request-id")
    shared_info[REQUEST_ID] = request_id

    if req_includes[HEADERS] or resp_includes[HEADERS] then
        local request_headers_table = {}
        for key, value in pairs(request_headers) do
            request_headers_table[key] = value
        end
        shared_info[REQ_HEADERS] = request_headers_table
    end
    
    if req_includes[BODY] or resp_includes[BODY] then
        local request_body = request_handle:body()
        local request_body_str
        if request_body == nil then
            request_body_str = ""
        else
            request_body_str = request_handle:body():getBytes(0, request_handle:body():length())
        end
        shared_info[REQ_BODY] = base64.encode(request_body_str)
    end

    local interceptor_request_body = {}
    include_request_info(req_includes, interceptor_request_body)

    local interceptor_response_headers, interceptor_response_body_str = send_http_call(request_handle, interceptor_request_body, intercept_service)
    if check_interceptor_call_errors(request_handle, interceptor_response_headers, interceptor_response_body_str, true) then
        return true
    end

    local interceptor_response_body = json.decode(interceptor_response_body_str)

    handle_direct_respond(request_handle, interceptor_response_body, request_id)
    modify_body(request_handle, interceptor_response_body, request_id)
    modify_headers(request_handle, interceptor_response_body)
    update_interceptor_context(interceptor_response_body)

    shared_info[CONTINUE_RESPONSE_PATH] = true
end

function interceptor.handle_response_interceptor(response_handle, intercept_service, req_includes, resp_includes)
    -- TODO: (renuka) ignore response intercept path for auth failure as well <- viraj
    local request_id = shared_info[REQUEST_ID] or "" -- TODO: check getting correlation id <- viraj
    if shared_info[DIRECT_RESPOND] then
        response_handle:logInfo("Ignoring response path intercept since direct responding for request_id: " .. request_id)
        return
    end
    if not shared_info[CONTINUE_RESPONSE_PATH] then -- TODO: do this without checkinng direct response
        response_handle:logInfo("Ignoring response path since interceptor reqeust flow is not completed for reqeust_id: " .. request_id)
        return
    end

    response_handle:logInfo("Continue request_id: " .. request_id)

    local interceptor_request_body = {}

    if resp_includes[HEADERS] then
        local request_headers = response_handle:headers()
        local response_headers_table = {}
        for key, value in pairs(request_headers) do
            response_headers_table[key] = value
        end
        interceptor_request_body[RESP_HEADERS] = response_headers_table
    end

    if resp_includes[BODY] then
        local request_body = response_handle:body():getBytes(0, response_handle:body():length())
        interceptor_request_body[RESP_BODY] = base64.encode(request_body)
    end

    include_request_info(req_includes, interceptor_request_body)
    interceptor_request_body[REQUEST.INTCPT_CONTEXT] = shared_info[REQUEST.INTCPT_CONTEXT]

    local interceptor_response_headers, interceptor_response_body_str = send_http_call(response_handle, interceptor_request_body, intercept_service)
    if check_interceptor_call_errors(response_handle, interceptor_response_headers, interceptor_response_body_str, false) then
        return true
    end
    local interceptor_response_body = json.decode(interceptor_response_body_str)

    modify_body(response_handle, interceptor_response_body, request_id)
    modify_headers(response_handle, interceptor_response_body)
end

return interceptor
