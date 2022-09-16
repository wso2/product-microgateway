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

require 'home.wso2.interceptor.lib.consts'
require 'home.wso2.interceptor.lib.encoders'
require 'home.wso2.interceptor.lib.utils'
local utils = require 'home.wso2.interceptor.lib.utils'

local function modify_headers(handle, interceptor_response_body)
    -- priority: headersToAdd, headersToReplace, headersToRemove
    if interceptor_response_body[RESPONSE.HEADERS_TO_REMOVE] then
        for _, key in ipairs(interceptor_response_body[RESPONSE.HEADERS_TO_REMOVE]) do
            if is_valid_header(key) then
                handle:headers():remove(key)
            end
        end
    end
    if interceptor_response_body[RESPONSE.HEADERS_TO_REPLACE] then
        for key, val in pairs(interceptor_response_body[RESPONSE.HEADERS_TO_REPLACE]) do
            if is_valid_header(key) then
                handle:headers():replace(key, val)
            end
        end
    end
    if interceptor_response_body[RESPONSE.HEADERS_TO_ADD] then
        for key, val in pairs(interceptor_response_body[RESPONSE.HEADERS_TO_ADD]) do
            if is_valid_header(key) then
                handle:headers():add(key, val)
            end
        end
    end
end

local function modify_trailers(handle, interceptor_response_body)
    -- priority: trailersToAdd, trailersToReplace, trailersToRemove
    if interceptor_response_body[RESPONSE.TRAILERS_TO_REMOVE] then
        for _, key in ipairs(interceptor_response_body[RESPONSE.TRAILERS_TO_REMOVE]) do
            handle:trailers():remove(key)
        end
    end
    if interceptor_response_body[RESPONSE.TRAILERS_TO_REPLACE] then
        for key, val in pairs(interceptor_response_body[RESPONSE.TRAILERS_TO_REPLACE]) do
            handle:trailers():replace(key, val)
        end
    end
    if interceptor_response_body[RESPONSE.TRAILERS_TO_ADD] then
        for key, val in pairs(interceptor_response_body[RESPONSE.TRAILERS_TO_ADD]) do
            handle:trailers():add(key, val)
        end
    end
end

local function handle_dynamic_endpoint(handle, interceptor_response_body, inv_context)
    local dynamicEp = interceptor_response_body[RESPONSE.DYNAMIC_ENDPOINT]
    if dynamicEp and dynamicEp[DYNAMIC_ENDPOINT.ENDPOINT_NAME] ~= "" then
        local dynamicEpName = dynamicEp[DYNAMIC_ENDPOINT.ENDPOINT_NAME]
        handle:logDebug("dynamic endpoint found: " .. dynamicEpName)
        -- template: <organizationID>_<EndpointName>_xwso2cluster_<vHost>_<API name><API version>
        local endpoint = string.format("%s_%s_xwso2cluster_%s_%s%s", inv_context[INV_CONTEXT.ORG_ID],
                dynamicEpName, inv_context[INV_CONTEXT.VHOST], inv_context[INV_CONTEXT.API_NAME],
                inv_context[INV_CONTEXT.API_VERSION])
        handle:logDebug('Setting header "x-wso2-cluster-header": ' .. endpoint)
        handle:headers():replace("x-wso2-cluster-header", endpoint)
    end
end

--- modify body
---@param handle table
---@param interceptor_response_body table
---@param request_id string
---@param shared_info table
---@param is_buffered boolean
---@param is_request_flow boolean
---@return boolean - return true if error
local function modify_body(handle, interceptor_response_body, request_id, shared_info, is_buffered, is_request_flow)
    -- if "body" is not defined or null (i.e. {} or {"body": null}) do not update the body
    -- request/response body should be buffered before modify the body https://github.com/envoyproxy/envoy/issues/13985#issuecomment-725724707
    if interceptor_response_body[RESPONSE.BODY] then
        handle:logDebug("Updating body for the request_id: " .. request_id)

        --#region handle error if body is not buffered before updating it
        if not is_buffered then
            -- invalid operation, body should buffered first before updating it
            log_interceptor_service_error(
                    handle,
                    request_id,
                    is_request_flow,
                    'Invalid operation: "Update Body". Request|Response body should be added in includes section of OAS definition'
            )
            -- can not respond_error because, can not update response body. Hence log only.

            return true
        end
        --#endregion

        local body, err = base64_decode(interceptor_response_body[RESPONSE.BODY], handle, shared_info, request_id, is_request_flow)
        if err then
            return true
        end

        local content_length = handle:body(true):setBytes(body)
        handle:headers():replace("content-length", content_length)
        if not is_request_flow then
            local status_code = "200"
            if content_length == 0 then
                status_code = "204"
            end
            handle:headers():replace(STATUS, status_code)
        end
        return false
    end

    return false
end

--- check for errors from interceptor response and handle error with responding to client
---@param handle table
---@param headers table
---@param body_str string
---@param request_id string
---@param is_request_flow boolean
---@return boolean - returns true if the rest of the interception flow should be terminated (do not continue interception flow)
local function check_interceptor_call_errors(handle, headers, body_str, shared_info, request_id, is_request_flow)
    -- TODO: (renuka) check behaviour 100 continue and handle it
    if headers[STATUS] == "200" then -- success, continue flow
        return false
    end
    if headers[STATUS] == "204" then -- handle empty body, terminate flow
        return true
    end

    local message = 'HTTP status_code: "' .. headers[STATUS] ..'", response_body: "' .. body_str
    log_interceptor_service_error(handle, request_id, is_request_flow, message)

    respond_error(handle, shared_info, {
            error_message = "Internal Server Error",
            error_description = "Internal Server Error",
            error_code = ERROR_CODES.INVALID_RESPONSE_HTTP_CODE
        },
        is_request_flow
    )
    return true
end

---send an HTTP request to the interceptor
---@param handle table - request/response handler object
---@param interceptor_request_body table - request body for the interceptor service
---@param intercept_service {cluster_name: string, resource_path: string, timeout: number, authority_header: string}
---@return table - response headers
---@return string - response body
local function send_http_call(handle, interceptor_request_body, intercept_service)
    local headers, interceptor_response_body_str = handle:httpCall(
        intercept_service["cluster_name"],
        {
            [":method"] = "POST",
            [":path"] = intercept_service["resource_path"],
            [":authority"] = intercept_service["authority_header"],
            ["content-type"] = "application/json",
            ["accept"] = "application/json",
        },
        json_encode(interceptor_request_body),
        intercept_service["timeout"],
        false -- async false, wait for response from interceptor service
    )

    return headers, interceptor_response_body_str
end

local function update_request_body(includes, interceptor_request_body, body_key, body_value)
    if includes[body_key] then
        interceptor_request_body[body_key] = body_value
    end
end

-- include request information in the request body to interceptor service
local function include_request_info(req_includes, interceptor_request_body, request_headers_table, request_body_base64, request_trailers_table)
    update_request_body(req_includes, interceptor_request_body, REQUEST.REQ_HEADERS, request_headers_table)
    update_request_body(req_includes, interceptor_request_body, REQUEST.REQ_BODY, request_body_base64)
    update_request_body(req_includes, interceptor_request_body, REQUEST.REQ_TRAILERS, request_trailers_table)
end

local function include_invocation_context(handle, req_flow_includes, resp_flow_includes, inv_context, interceptor_request_body, shared_info, request_headers)
    if req_flow_includes[INCLUDES.INV_CONTEXT] or resp_flow_includes[INCLUDES.INV_CONTEXT] then
        -- We first read from "x-forwarded-for" which is the actual client IP, when it comes to scenarios like the request is coming through a load balancer
        -- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For the header contains the original clients IP.
        local client_ip = request_headers:get("x-forwarded-for")
        if client_ip == nil or client_ip == "" then
            client_ip = handle:streamInfo():downstreamDirectRemoteAddress()
        end

        --#region runtime invocation details
        local inv_context_body = {
            [INV_CONTEXT.PROTOCOL] = handle:streamInfo():protocol(),
            [INV_CONTEXT.SCHEME] = request_headers:get(":scheme"),
            [INV_CONTEXT.PATH] = request_headers:get(":path"),
            [INV_CONTEXT.METHOD] = request_headers:get(":method"),
            [INV_CONTEXT.REQ_ID] = request_headers:get("x-request-id"),
            [INV_CONTEXT.SOURCE] = client_ip
        }
        table.shallow_copy(inv_context, inv_context_body)
        -- remove organizationId from invocationContext, since it should not be sent to the interceptor service
        inv_context_body[INV_CONTEXT.ORG_ID] = nil

        --#region auth context
        local ext_authz_meta =  handle:streamInfo():dynamicMetadata():get(EXT_AUTHZ_FILTER)
        if ext_authz_meta then
            inv_context_body[INV_CONTEXT.AUTH_CTX] = {
                [AUTH_CTX.TOKEN_TYPE] = ext_authz_meta["tokenType"], -- API Key|JWT Auth|Internal Key
                [AUTH_CTX.TOKEN] = ext_authz_meta["token"],
                [AUTH_CTX.KEY_TYPE] = ext_authz_meta["keyType"] -- PRODUCTION|SANDBOX
            }
        end
        --#endregion
        --#endregion

        if req_flow_includes[INCLUDES.INV_CONTEXT] then
            interceptor_request_body[REQUEST.INV_CONTEXT] = inv_context_body
        end
        if resp_flow_includes[INCLUDES.INV_CONTEXT] then
            shared_info[REQUEST.INV_CONTEXT] = inv_context_body
        end
    end
end

local function handle_direct_respond(handle, interceptor_response_body, shared_info, request_id)
    if interceptor_response_body[RESPONSE.DIRECT_RESPOND] then
        handle:logDebug("Directly responding without calling the backend for request_id: " .. request_id)
        local headers = interceptor_response_body[RESPONSE.HEADERS_TO_ADD] or {}

        -- if interceptor_response_body.body is nil send empty, do not send client its payload back
        local body = interceptor_response_body[RESPONSE.BODY] or ""
        local status_code = interceptor_response_body[REQUEST.RESP_CODE]
        if body == "" then
            status_code = status_code or 204
        else
            status_code = status_code or 200
        end
        headers[STATUS] = tostring(status_code)

        local decoded_body, err = base64_decode(body, handle, shared_info, request_id, true)
        if err then
            return
        end

        direct_respond(handle, headers, decoded_body, shared_info)
    end
end

---interceptor handler for request flow
---@param request_handle table - request_handle
---@param intercept_service_list {method: {cluster_name: string, resource_path: string, timeout: number}}
---@param req_flow_includes_list {method: {requestHeaders: boolean, requestBody: boolean, requestTrailer: boolean}}
---@param resp_flow_includes_list {method: {requestHeaders: boolean, requestBody: boolean, requestTrailer: boolean, responseHeaders: boolean, responseBody: boolean, responseTrailers: boolean}}
---@param inv_context table
---@param skip_interceptor_call boolean
function interceptor.handle_request_interceptor(request_handle, intercept_service_list, req_flow_includes_list, resp_flow_includes_list, inv_context, skip_interceptor_call, wire_log_config)
    local shared_info = {}

    local request_headers = request_handle:headers()
    local request_id = request_headers:get("x-request-id")
    local method = request_headers:get(":method")
    shared_info[SHARED.REQUEST_ID] = request_id

    local intercept_service = {}
    local req_flow_includes = {}
    if intercept_service_list[method] == nil or req_flow_includes_list[method] == nil then
        skip_interceptor_call = true
        request_handle:logDebug("Method " .. method .. "is not included in configs. Hence request interceptor is not applied.")
    else
        intercept_service = intercept_service_list[method]
        req_flow_includes = req_flow_includes_list[method]
    end

    local resp_flow_includes = {}
    if resp_flow_includes_list[method] ~= nil then
        resp_flow_includes = resp_flow_includes_list[method]
    end

    local interceptor_request_body = {}
    -- including invocation context first it is required to read headers
    -- setting invocation context done only in the request flow and set it to the shared info to refer in response flow
    include_invocation_context(request_handle, req_flow_includes, resp_flow_includes, inv_context, interceptor_request_body, shared_info, request_headers)

    --#region read request headers and update shared_info
    local request_headers_table = {}
    for key, value in pairs(request_headers) do
        request_headers_table[key] = value
    end
    shared_info[REQUEST.REQ_HEADERS] = request_headers_table
    
    --#endregion

    --#region read request body and update shared_info
    local request_body_base64
    if req_flow_includes[INCLUDES.REQ_BODY] or resp_flow_includes[INCLUDES.REQ_BODY] or wire_log_config.log_body_enabled then
        local request_body = request_handle:body()
        local request_body_str
        if request_body then
            request_body_str = request_body:getBytes(0, request_body:length())
        else
            request_body_str = ""
        end
        request_body_base64 = base64_encode(request_body_str)
    end
    if resp_flow_includes[INCLUDES.REQ_BODY] then
        shared_info[REQUEST.REQ_BODY] = request_body_base64
    end
    --#endregion

    --#region read request trailers and update shared_info
    local request_trailers_table = {}
    if req_flow_includes[INCLUDES.REQ_TRAILERS] or resp_flow_includes[INCLUDES.REQ_TRAILERS] then
        local request_trailers = request_handle:trailers()
        for key, value in pairs(request_trailers) do
            request_trailers_table[key] = value
        end
    end
    if resp_flow_includes[INCLUDES.REQ_TRAILERS] then
        shared_info[REQUEST.REQ_TRAILERS] = request_trailers_table
    end
    --#endregion

    if skip_interceptor_call then
        -- skip calling interceptor service by only setting the shared_info
        -- this is useful when the request interceptor flow is disabled and only the response interceptor flow is enabled.
        request_handle:streamInfo():dynamicMetadata():set(LUA_FILTER_NAME, SHARED_INFO_META_KEY, shared_info)
        return
    end

    -- include request details: request headers, body and trailers to the interceptor_request_body
    include_request_info(req_flow_includes, interceptor_request_body, request_headers_table, request_body_base64, request_trailers_table)

    intercept_service.resource_path = "/api/v1/handle-request"
    local interceptor_response_headers, interceptor_response_body_str = send_http_call(request_handle, interceptor_request_body, intercept_service)
    if check_interceptor_call_errors(request_handle, interceptor_response_headers, interceptor_response_body_str, shared_info, request_id, true) then
        return
    end

    local interceptor_response_body, err = json_decode(interceptor_response_body_str, request_handle, shared_info, request_id, true)
    if err then
        return
    end

    --TODO: (renuka) validate response_body scheme, whether headersToAdd is a table or not (i.e. if it is an int then throw error)

    handle_direct_respond(request_handle, interceptor_response_body, shared_info, request_id)
    if modify_body(request_handle, interceptor_response_body, request_id, shared_info, req_flow_includes[INCLUDES.REQ_BODY], true) then
        -- error thrown, exiting
        return
    end
    
    modify_headers(request_handle, interceptor_response_body)

    utils.wire_log_headers(request_handle, " >> request headers >> ", wire_log_config.log_headers_enabled)
    utils.wire_log_body(request_handle, " >> request body >> ", wire_log_config.log_body_enabled)

    modify_trailers(request_handle, interceptor_response_body)
    
    utils.wire_log_trailers(request_handle, " >> request trailers >> ", wire_log_config.log_trailers_enabled)

    --#region handle dynamic endpoint
    -- handle this after update headers, in case if user modify the header "x-wso2-cluster-header"
    handle_dynamic_endpoint(request_handle, interceptor_response_body, inv_context)
    --#endregion

    if interceptor_response_body[RESPONSE.INTCPT_CONTEXT] then
        request_handle:logDebug("Updating interceptor context for the request_id: " .. request_id)
        shared_info[REQUEST.INTCPT_CONTEXT] = interceptor_response_body[RESPONSE.INTCPT_CONTEXT]
    end

    request_handle:streamInfo():dynamicMetadata():set(LUA_FILTER_NAME, SHARED_INFO_META_KEY, shared_info)
end

---interceptor handler for response flow
---@param response_handle table - response_handle
---@param intercept_service_list {method: {cluster_name: string, resource_path: string, timeout: number}}
---@param resp_flow_includes_list {method: {requestHeaders: boolean, requestBody: boolean, requestTrailer: boolean, responseHeaders: boolean, responseBody: boolean, responseTrailers: boolean}}
function interceptor.handle_response_interceptor(response_handle, intercept_service_list, resp_flow_includes_list, wire_log_config)
    local meta = response_handle:streamInfo():dynamicMetadata():get(LUA_FILTER_NAME)
    local shared_info = meta and meta[SHARED_INFO_META_KEY]
    if not shared_info then
        response_handle:logDebug("Meta data 'shared_info' set in request flow not found (e.g. auth failure), skipping interceptor response flow.")
        -- no shared info found, request interceptor flow is not executed (eg: enforcer validation failed)
        -- TODO: (renuka) check again, if we want to go response flow if enforcer validation failed, have to set request info metadata from enforcer
        return
    end
    local method = shared_info[REQUEST.REQ_HEADERS][":method"]
    if method == nil then
        response_handle:logErr("Error getting header ':method' in response flow interceptor. The header is not found in shared info's request headers.")
        return
    end
    local resp_flow_includes = resp_flow_includes_list[method]
    local intercept_service = intercept_service_list[method]
    if resp_flow_includes == nil or intercept_service == nil then
        response_handle:logDebug("Method " .. method .. " is missing in response configs. Hence response interceptor is not applied.")
        return
    end

    local request_id = shared_info[SHARED.REQUEST_ID]
    if shared_info[RESPONSE.DIRECT_RESPOND] then
        response_handle:logDebug("Ignoring response path intercept since direct responded for the request_id: " .. request_id)
        return
    end

    local interceptor_request_body = {}

    --#region read backend headers
    if resp_flow_includes[INCLUDES.RESP_HEADERS] then
        local backend_headers = response_handle:headers()
        local backend_headers_table = {}
        for key, value in pairs(backend_headers) do
            backend_headers_table[key] = value
        end
        interceptor_request_body[REQUEST.RESP_HEADERS] = backend_headers_table
    end
    --#endregion

    --#region read backend body
    if resp_flow_includes[INCLUDES.RESP_BODY] or wire_log_config.log_body_enabled then
        local request_body = response_handle:body():getBytes(0, response_handle:body():length())
        interceptor_request_body[REQUEST.RESP_BODY] = base64_encode(request_body)
    end
    --#endregion

    --#region read backend trailers
    if resp_flow_includes[INCLUDES.RESP_TRAILERS] then
        local backend_trailers = response_handle:trailers()
        local backend_trailers_table = {}
        for key, value in pairs(backend_trailers) do
            backend_trailers_table[key] = value
        end
        interceptor_request_body[REQUEST.RESP_TRAILERS] = backend_trailers_table
    end
    --#endregion

    --#region status code
    interceptor_request_body[REQUEST.RESP_CODE] = tonumber(response_handle:headers():get(STATUS))
    --#endregion

    include_request_info(resp_flow_includes, interceptor_request_body, shared_info[REQUEST.REQ_HEADERS], shared_info[REQUEST.REQ_BODY], shared_info[REQUEST.REQ_TRAILERS])
    interceptor_request_body[REQUEST.INTCPT_CONTEXT] = shared_info[REQUEST.INTCPT_CONTEXT]

    --#region set invocation context
    if resp_flow_includes[REQUEST.INV_CONTEXT] then
        interceptor_request_body[REQUEST.INV_CONTEXT] = shared_info[REQUEST.INV_CONTEXT]
    end
    --#endregion

    intercept_service.resource_path = "/api/v1/handle-response"
    local interceptor_response_headers, interceptor_response_body_str = send_http_call(response_handle, interceptor_request_body, intercept_service)
    if check_interceptor_call_errors(response_handle, interceptor_response_headers, interceptor_response_body_str, shared_info, request_id, false) then
        return
    end

    --TODO: (renuka) validate response_body scheme, whether headersToAdd is a table or not (i.e. if it is an int then throw error)

    local interceptor_response_body, err = json_decode(interceptor_response_body_str, response_handle, shared_info, request_id, false)
    if err then
        return
    end

    if modify_body(response_handle, interceptor_response_body, request_id, shared_info, resp_flow_includes[INCLUDES.RESP_BODY], false) then
        -- error thrown, exiting
        return
    end
    
    modify_headers(response_handle, interceptor_response_body)

    utils.wire_log_headers(response_handle, " << response headers << ", wire_log_config.log_headers_enabled)
    utils.wire_log_body(response_handle, " << response body << ", wire_log_config.log_body_enabled)

    modify_trailers(response_handle, interceptor_response_body)

    --#region status code
    if interceptor_response_body[RESPONSE.RESPONSE_CODE] then
        response_handle:headers():replace(STATUS, tostring(interceptor_response_body[RESPONSE.RESPONSE_CODE]))
    end
    --#endregion

    utils.wire_log_trailers(response_handle, " >> response trailers >> ", wire_log_config.log_trailers_enabled)
end

return interceptor
